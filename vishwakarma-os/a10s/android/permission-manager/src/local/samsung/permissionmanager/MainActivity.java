package local.samsung.permissionmanager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MainActivity extends Activity {
    private final ArrayList<ApplicationInfo> apps = new ArrayList<>();
    private PackageManager pm;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        pm = getPackageManager();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(22, 22, 22));

        TextView title = text("PERMISSION MANAGER", 23, Color.rgb(244,244,244));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(20), dp(18), dp(20), dp(10));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(72)));

        TextView hint = text("Select an app to grant or revoke its Android permissions.", 13, Color.rgb(198,198,198));
        hint.setPadding(dp(20), 0, dp(20), dp(14));
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        ListView list = new ListView(this);
        list.setDividerHeight(1);
        list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(82,82,82)));
        list.setBackgroundColor(Color.rgb(22,22,22));
        loadApps();
        list.setAdapter(new AppAdapter());
        list.setOnItemClickListener((parent, view, position, id) -> openPermissions(apps.get(position).packageName));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void loadApps() {
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null && !app.packageName.equals(getPackageName())) apps.add(app);
        }
        Collections.sort(apps, Comparator.comparing(a -> pm.getApplicationLabel(a).toString().toLowerCase()));
    }

    private void openPermissions(String packageName) {
        Intent direct = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
        direct.putExtra("android.intent.extra.PACKAGE_NAME", packageName);
        try { startActivity(direct); }
        catch (Exception ignored) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)));
        }
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(sp); view.setTextColor(color); return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class AppAdapter extends BaseAdapter {
        public int getCount() { return apps.size(); }
        public Object getItem(int position) { return apps.get(position); }
        public long getItemId(int position) { return position; }
        public View getView(int position, View recycled, ViewGroup parent) {
            LinearLayout row = recycled instanceof LinearLayout ? (LinearLayout) recycled : new LinearLayout(MainActivity.this);
            row.removeAllViews(); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(20), dp(12), dp(20), dp(12)); row.setBackgroundColor(Color.rgb(38,38,38));
            ApplicationInfo app = apps.get(position);
            TextView label = text(pm.getApplicationLabel(app).toString(), 16, Color.rgb(244,244,244));
            TextView pkg = text(app.packageName, 12, Color.rgb(198,198,198));
            row.addView(label); row.addView(pkg); return row;
        }
    }
}
