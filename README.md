# Acharya OS and Vishwakarma OS

Two products that share a USB data channel on the phone (`/sdcard/Vishwakarma`).

| Project | Folder | What it is |
| --- | --- | --- |
| **Acharya OS** | [`acharya-os/`](acharya-os/) | Landscape HOME launcher for daily dharma: clock, panchanga, Sandhyavandana, mantras, pooja, hadugalu |
| **Vishwakarma OS** | [`vishwakarma-os/`](vishwakarma-os/) | Windows companion, USB protocol, and device/OS layer that feeds the phone |

This git root is the workspace. Each product has its own README and build.

## Acharya OS

Android launcher (`com.paperdesk.launcher`). Target device: Samsung SM-J320M (J2), ViperOS, Android 7.1.2, API 25, landscape 1280×720.

```powershell
cd acharya-os
.\gradlew.bat :app:installDebug
adb shell am start -S -n com.paperdesk.launcher/.ui.LauncherActivity
```

See [`acharya-os/README.md`](acharya-os/README.md).

## Vishwakarma OS

WinUI 3 companion named Vishwakarma. Pushes schedule, panchanga, and settings to the phone over USB.

```powershell
cd vishwakarma-os
.\run.ps1
```

Device overlay and lockdown scripts live beside it. See [`vishwakarma-os/README.md`](vishwakarma-os/README.md).

## How they connect

The phone reads and writes under `/sdcard/Vishwakarma/`:

- `os-bundle.json` / `inbox/os-bundle.json`
- `inbox/settings.json`
- `panchanga.json` / `inbox/panchanga.json`
- `weather.json`

The Windows app caches a copy at `%USERPROFILE%\.paperdesk-companion\os-bundle.json`.
