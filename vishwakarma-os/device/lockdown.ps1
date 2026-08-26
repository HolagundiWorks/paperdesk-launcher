# Vishwakarma cleanup for Samsung A10s
# Run:  .\device\lockdown.ps1
#
# Strips Samsung / Google accounts and apps. Phone stays open for APKs you
# sideload over USB (package installer + Settings stay on).
#
# Never disable: android, systemui, phone, incallui, settings,
# providers.settings, shell, keyboard, MTP, Magisk, SuperSU, this launcher,
# package installer, permission controller, WebView.

$ErrorActionPreference = "Continue"
Write-Host "== Vishwakarma A10s cleanup ==" -ForegroundColor Cyan

$pkg = "com.paperdesk.launcher"

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell wm density 280

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

adb shell settings put system screen_off_timeout 300000
adb shell settings put system screen_brightness 40

adb shell settings put global adb_enabled 1
adb shell settings put global development_settings_enabled 1
adb shell svc data disable
adb shell svc bluetooth disable
adb shell settings put global bluetooth_on 0
adb shell settings put global ble_scan_always_enabled 0

$uninstall = @(
    "com.snapchat.android",
    "in.amazon.mShop.android.shopping",
    "com.eterno.shortvideos",
    "com.samsung.ecomm.global.in",
    "com.facebook.katana",
    "com.facebook.appmanager",
    "com.facebook.system",
    "com.facebook.services",
    "com.microsoft.skydrive",
    "com.mygalaxy",
    "com.samsung.android.themecenter"
)

foreach ($p in $uninstall) {
    Write-Host "uninstall $p"
    adb shell pm uninstall --user 0 $p 2>$null | Out-Null
    adb shell pm disable-user --user 0 $p 2>$null | Out-Null
}

$disable = @(
    "com.android.launcher3",
    "com.android.launcher",
    "org.lineageos.trebuchet",
    "com.sec.android.app.launcher",
    "com.sec.android.emergencylauncher",
    "com.android.dialer",
    "com.samsung.android.dialer",
    "com.android.messaging",
    "com.samsung.android.messaging",
    "com.android.mms.service",
    "com.android.smspush",
    "com.android.stk",
    "com.android.stk2",
    "com.android.cellbroadcastreceiver",
    "com.android.gallery3d",
    "com.sec.android.gallery3d",
    "org.cyanogenmod.snap",
    "com.android.camera2",
    "com.sec.android.app.camera",
    "com.sec.android.app.aftersalecamera",
    "com.sec.android.app.camerasaver",
    "com.android.contacts",
    "com.samsung.android.app.contacts",
    "com.android.calendar",
    "com.samsung.android.calendar",
    "com.android.deskclock",
    "com.sec.android.app.clockpackage",
    "com.samsung.android.app.clockpack",
    "com.android.calculator2",
    "com.android.fmradio",
    "com.sec.android.app.fm",
    "com.android.musicfx",
    "com.android.nfc",
    "org.lineageos.jelly",
    "org.omnirom.omnijaws",
    "org.omnirom.omnistyle",
    "com.viper.vpapers",
    "com.android.wallpaper.livepicker",
    "com.google.android.apps.wallpaper",
    "com.google.android.apps.turbo",
    "ch.deletescape.lawnchair",
    "com.android.egg",
    "com.android.terminal",
    "com.android.development",
    "com.android.chrome",
    "com.sec.android.app.chromecustomizations",
    "com.google.android.youtube",
    "com.google.android.gm",
    "com.google.android.apps.maps",
    "com.google.android.googlequicksearchbox",
    "com.google.android.apps.tachyon",
    "com.google.android.tts",
    "com.google.android.partnersetup",
    "com.google.android.onetimeinitializer",
    "com.google.android.setupwizard",
    "com.google.android.configupdater",
    "com.google.android.feedback",
    "com.google.android.printservice.recommendation",
    "com.google.android.syncadapters.contacts",
    "com.google.android.syncadapters.calendar",
    "com.google.android.apps.restore",
    "com.google.android.projection.gearhead",
    "com.google.android.gms.location.history",
    "com.google.android.apps.carrier.carrierwifi",
    "com.google.android.captiveportallogin",
    "com.android.vending",
    "com.google.android.gms",
    "com.google.android.gsf",
    "android.autoinstalls.config.samsung",
    "com.osp.app.signin",
    "com.samsung.android.mobileservice",
    "com.samsung.android.scloud",
    "com.sec.android.soagent",
    "com.sec.android.app.billing",
    "com.samsung.android.app.updatecenter",
    "com.aura.oobe.samsung",
    "com.sec.android.app.SecSetupWizard",
    "com.sec.android.app.setupwizardlegalprovider",
    "com.samsung.android.setupindiaservicestnc",
    "com.sec.android.app.samsungapps",
    "com.sec.android.widgetapp.samsungapps",
    "com.samsung.android.app.settings.bixby",
    "com.samsung.android.dynamiclock",
    "com.samsung.android.smartswitchassistant",
    "com.sec.android.easyMover.Agent",
    "com.samsung.android.app.galaxyfinder",
    "com.samsung.android.themestore",
    "com.samsung.android.themecenter",
    "com.android.theme.icon_pack.rounded.themepicker",
    "com.android.theme.icon_pack.filled.themepicker",
    "com.android.theme.icon_pack.circular.themepicker",
    "com.samsung.android.app.cocktailbarservice",
    "com.samsung.android.applock",
    "com.samsung.android.mapsagent",
    "com.samsung.android.easysetup",
    "com.samsung.android.aware.service",
    "com.samsung.android.beaconmanager",
    "com.samsung.android.rubin.app",
    "com.samsung.android.mdx.quickboard",
    "com.samsung.android.mdx.kit",
    "com.sec.android.daemonapp",
    "com.samsung.SMT",
    "com.samsung.cmh",
    "com.samsung.klmsagent",
    "com.sec.android.app.ve.vebgm",
    "com.sec.android.app.parser",
    "com.samsung.android.accessibility.talkback",
    "com.samsung.android.service.peoplestripe",
    "com.samsung.android.da.daagent",
    "com.samsung.android.app.taskedge",
    "com.samsung.android.app.clipboardedge",
    "com.samsung.android.app.appsedge",
    "com.samsung.android.stickercenter",
    "com.samsung.android.app.sharelive",
    "com.samsung.android.allshare.service.fileshare",
    "com.samsung.android.allshare.service.mediashare",
    "com.samsung.android.video",
    "com.samsung.android.app.dressroom",
    "com.samsung.android.game.gos",
    "com.samsung.android.forest",
    "com.samsung.android.fmm",
    "com.samsung.android.lool",
    "com.samsung.android.dsms",
    "com.samsung.android.app.omcagent",
    "com.samsung.android.app.watchmanagerstub",
    "com.sec.mhs.smarttethering",
    "com.samsung.android.networkdiagnostic",
    "com.hiya.star",
    "com.sec.android.app.myfiles",
    "com.sec.android.mimage.photoretouching",
    "com.samsung.app.newtrim",
    "com.sec.android.app.quicktool",
    "com.sec.android.widgetapp.webmanual",
    "com.samsung.safetyinformation",
    "com.sec.android.app.personalization",
    "com.samsung.android.sdk.handwriting",
    "com.sec.android.app.magnifier",
    "com.sec.android.app.bluetoothtest",
    "com.sec.android.app.soundalive",
    "com.samsung.android.secsoundpicker",
    "com.samsung.android.app.soundpicker",
    "com.samsung.android.setting.multisound",
    "com.wssyncmldm",
    "com.sec.spp.push",
    "com.samsung.android.mdm",
    "com.samsung.android.mfi",
    "com.samsung.android.container",
    "com.samsung.android.dqagent",
    "com.samsung.aasaservice",
    "com.samsung.android.sdm.config",
    "com.sec.android.easyonehand",
    "com.samsung.storyservice",
    "com.samsung.attribution",
    "com.samsung.faceservice",
    "com.samsung.android.bio.face.service",
    "com.android.companiondevicemanager",
    "com.android.bluetooth",
    "com.android.bluetoothmidiservice",
    "com.android.dreams.basic",
    "com.android.dreams.phototable",
    "com.android.printspooler",
    "com.android.bips",
    "com.android.htmlviewer",
    "com.android.bookmarkprovider",
    "com.android.managedprovisioning",
    "com.android.emergency",
    "com.android.hotspot2.osulogin",
    "com.android.traceur",
    "com.android.wallpaperbackup",
    "com.android.calllogbackup",
    "com.sec.android.app.DataCreate",
    "com.sec.android.app.factorykeystring",
    "com.sec.android.app.servicemodeapp",
    "com.factory.mmigroup"
)

foreach ($p in $disable) {
    Write-Host "disable $p"
    adb shell pm disable-user --user 0 $p 2>$null | Out-Null
}

adb shell "settings put secure sysui_qs_tiles 'custom(com.paperdesk.launcher/com.paperdesk.launcher.bridge.SyncTileService),Flashlight,SoundMode,RotationLock,Dnd'"
adb shell settings put secure sysui_qqs_count 5
adb shell settings put global policy_control "immersive.full=*"

adb shell pm grant $pkg android.permission.READ_EXTERNAL_STORAGE 2>$null | Out-Null
adb shell pm grant $pkg android.permission.WRITE_EXTERNAL_STORAGE 2>$null | Out-Null
adb shell pm grant $pkg android.permission.POST_NOTIFICATIONS 2>$null | Out-Null
adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS 2>$null | Out-Null
adb shell appops set $pkg WRITE_SETTINGS allow 2>$null | Out-Null
adb shell appops set $pkg MANAGE_EXTERNAL_STORAGE allow 2>$null | Out-Null
adb shell appops set $pkg SCHEDULE_EXACT_ALARM allow 2>$null | Out-Null
adb shell dumpsys deviceidle whitelist +$pkg 2>$null | Out-Null
adb shell cmd package set-home-activity $pkg/.ui.LauncherActivity 2>$null | Out-Null

Write-Host ""
Write-Host "Done. Home is Vishwakarma. Settings and USB install stay on." -ForegroundColor Green
Write-Host "Samsung/Google accounts and apps are off. Bluetooth is off." -ForegroundColor Green
