package local.samsung.blackwallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Bitmap black = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            black.eraseColor(Color.BLACK);
            WallpaperManager manager = WallpaperManager.getInstance(this);
            manager.setBitmap(black, null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            black.recycle();
            setResult(RESULT_OK);
        } catch (Exception error) {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
