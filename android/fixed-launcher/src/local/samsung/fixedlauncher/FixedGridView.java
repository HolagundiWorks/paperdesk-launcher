package local.samsung.fixedlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

final class FixedGridView extends View {
    private static final int COLUMNS = 5;
    private static final int ROWS = 3;
    private static final int SLOT_COUNT = COLUMNS * ROWS;
    private static final long DRAG_DELAY_MS = 260;
    private static final String EMPTY = "-";
    private static final String[] DEFAULT_ORDER = {
        "org.fossify.gallery", "org.fossify.calendar", "org.fossify.clock",
        "org.fossify.notes", "org.tasks", "org.fossify.math",
        "org.fossify.filemanager", EMPTY, EMPTY, EMPTY,
        EMPTY, EMPTY, EMPTY, EMPTY, EMPTY
    };

    private final PackageManager packages;
    private final SharedPreferences preferences;
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] slots = new String[SLOT_COUNT];
    private int pressedSlot = -1;
    private int draggedSlot = -1;
    private float downX;
    private float downY;
    private long downAt;

    FixedGridView(Context context) {
        super(context);
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(Color.BLACK);
        packages = context.getPackageManager();
        preferences = context.getSharedPreferences("grid", Context.MODE_PRIVATE);
        tilePaint.setColor(Color.rgb(22, 22, 22));
        borderPaint.setColor(Color.rgb(57, 57, 57));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1));
        loadOrder();
    }

    private void loadOrder() {
        String saved = preferences.getString("order", null);
        String[] parsed = saved == null ? DEFAULT_ORDER : saved.split(",", -1);
        for (int index = 0; index < SLOT_COUNT; index++) {
            slots[index] = index < parsed.length && !parsed[index].isEmpty() ? parsed[index] : EMPTY;
        }
    }

    private void saveOrder() {
        preferences.edit().putString("order", String.join(",", slots)).apply();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gridTop = getHeight() * 0.5f;
        float horizontalPadding = dp(10);
        float gap = dp(6);
        float cell = (getWidth() - horizontalPadding * 2 - gap * (COLUMNS - 1)) / COLUMNS;
        float availableHeight = getHeight() - gridTop - dp(20);
        float verticalGap = Math.max(dp(6), (availableHeight - cell * ROWS) / Math.max(1, ROWS - 1));

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int column = slot % COLUMNS;
            int row = slot / COLUMNS;
            float left = horizontalPadding + column * (cell + gap);
            float top = gridTop + row * (cell + verticalGap);
            RectF tile = new RectF(left, top, left + cell, top + cell);
            tilePaint.setColor(slot == draggedSlot ? Color.rgb(38, 38, 38) : Color.rgb(22, 22, 22));
            canvas.drawRoundRect(tile, dp(3), dp(3), tilePaint);
            canvas.drawRoundRect(tile, dp(3), dp(3), borderPaint);
            drawIcon(canvas, slots[slot], tile);
        }
    }

    private void drawIcon(Canvas canvas, String packageName, RectF tile) {
        if (EMPTY.equals(packageName)) return;
        try {
            Drawable icon = packages.getApplicationIcon(packageName);
            int iconSize = Math.round(tile.width() * 0.56f);
            int left = Math.round(tile.centerX() - iconSize / 2f);
            int top = Math.round(tile.centerY() - iconSize / 2f);
            icon.setBounds(left, top, left + iconSize, top + iconSize);
            icon.draw(canvas);
        } catch (PackageManager.NameNotFoundException ignored) {
            // Missing fixed apps intentionally leave an empty-looking tile.
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int slot = slotAt(event.getX(), event.getY());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedSlot = slot;
                draggedSlot = -1;
                downX = event.getX();
                downY = event.getY();
                downAt = SystemClock.uptimeMillis();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressedSlot >= 0 && draggedSlot < 0 &&
                        (SystemClock.uptimeMillis() - downAt >= DRAG_DELAY_MS ||
                         distance(event.getX(), event.getY(), downX, downY) > dp(12))) {
                    draggedSlot = pressedSlot;
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    invalidate();
                }
                if (draggedSlot >= 0 && slot >= 0 && slot != draggedSlot) {
                    String moving = slots[draggedSlot];
                    slots[draggedSlot] = slots[slot];
                    slots[slot] = moving;
                    draggedSlot = slot;
                    saveOrder();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (draggedSlot < 0 && pressedSlot >= 0 && pressedSlot == slot &&
                        SystemClock.uptimeMillis() - downAt < DRAG_DELAY_MS) {
                    launch(slots[pressedSlot]);
                }
                pressedSlot = -1;
                draggedSlot = -1;
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressedSlot = -1;
                draggedSlot = -1;
                invalidate();
                return true;
            default:
                return false;
        }
    }

    private int slotAt(float x, float y) {
        float gridTop = getHeight() * 0.5f;
        float horizontalPadding = dp(10);
        float gap = dp(6);
        float cell = (getWidth() - horizontalPadding * 2 - gap * (COLUMNS - 1)) / COLUMNS;
        float availableHeight = getHeight() - gridTop - dp(20);
        float verticalGap = Math.max(dp(6), (availableHeight - cell * ROWS) / Math.max(1, ROWS - 1));
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int column = slot % COLUMNS;
            int row = slot / COLUMNS;
            float left = horizontalPadding + column * (cell + gap);
            float top = gridTop + row * (cell + verticalGap);
            if (x >= left && x <= left + cell && y >= top && y <= top + cell) return slot;
        }
        return -1;
    }

    private void launch(String packageName) {
        if (EMPTY.equals(packageName)) return;
        Intent intent = packages.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            getContext().startActivity(intent);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
