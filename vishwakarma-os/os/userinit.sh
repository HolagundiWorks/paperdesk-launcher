#!/system/bin/sh
# Vishwakarma OS desk mode. Runs from Viper init.d 90userinit.

log -p i -t vishwakarma "Vishwakarma OS userinit"

settings put global airplane_mode_on 0
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false >/dev/null 2>&1

svc wifi disable
svc data disable
svc nfc disable
settings put global wifi_on 0
settings put global mobile_data 0
settings put global bluetooth_on 0
settings put global nfc_on 0
settings put global wifi_scan_always_enabled 0
settings put global wifi_networks_available_notification_on 0
settings put global stay_on_while_plugged_in 7
settings put global auto_time 1
settings put global auto_time_zone 1
settings put system screen_brightness 140
settings put system accelerometer_rotation 1
settings put secure ui_night_mode 2

# No Android lock screen. The desk launcher is home on wake.
locksettings set-disabled true >/dev/null 2>&1
settings put secure lockscreen.disabled 1
settings put system lockscreen.disabled 1
settings put secure lock_screen_lock_after_timeout 0
settings put system lockscreen_visualizer 0
settings put system lockscreen_scramble_pin_layout 0
settings put system lockscreen_max_noti_count 0
settings put secure lock_screen_show_notifications 0
settings put system show_lock_before_unlock 0
settings put system lockscreen_notifications_new 0

if [ -f /data/local/black.png ]; then
  cp /data/local/black.png /data/system/users/0/wallpaper
  cp /data/local/black.png /data/system/users/0/wallpaper_orig
  chown system:system /data/system/users/0/wallpaper /data/system/users/0/wallpaper_orig
  chmod 600 /data/system/users/0/wallpaper /data/system/users/0/wallpaper_orig
fi

for p in \
  com.android.dialer \
  com.android.messaging \
  com.android.contacts \
  com.android.stk \
  com.android.cellbroadcastreceiver \
  com.android.fmradio \
  org.cyanogenmod.snap \
  org.lineageos.jelly \
  com.android.calendar \
  com.android.calculator2 \
  com.android.deskclock \
  com.android.gallery3d \
  ch.deletescape.lawnchair \
  org.omnirom.omnijaws \
  com.android.terminal \
  com.android.egg \
  com.android.development \
  com.viper.vpapers \
  com.google.android.apps.wallpaper \
  com.google.android.apps.turbo \
  com.android.nfc \
  com.android.apps.tag \
  com.android.printspooler \
  com.android.printservice.recommendation \
  com.android.musicfx \
  com.android.dreams.basic \
  com.android.wallpaper.livepicker \
  org.cyanogenmod.profiles \
  com.android.emergency \
  com.cyanogenmod.samsungservicemode \
  com.android.smspush \
  com.android.managedprovisioning \
  com.android.calllogbackup \
  com.android.wallpaperbackup \
  com.android.bluetoothmidiservice \
  com.android.bookmarkprovider \
  com.android.htmlviewer \
  org.omnirom.omnistyle
do
  pm disable "$p" >/dev/null 2>&1
done

log -p i -t vishwakarma "Vishwakarma OS userinit done"
