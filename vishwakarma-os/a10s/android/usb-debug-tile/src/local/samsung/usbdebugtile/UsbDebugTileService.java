package local.samsung.usbdebugtile;

import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class UsbDebugTileService extends TileService {
    private boolean enabled() {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
    }

    private void refresh() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean enabled = enabled();
        tile.setLabel("USB debugging");
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setContentDescription(enabled ? "USB debugging on" : "USB debugging off");
        tile.updateTile();
    }

    @Override public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    @Override public void onClick() {
        super.onClick();
        Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, enabled() ? 0 : 1);
        refresh();
    }
}
