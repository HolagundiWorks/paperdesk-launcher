# PaperDesk OS Workspace

This repository contains two device-specific OS flavors. They share selected
launcher and USB-sync concepts, but are developed and released independently.

| Flavor | Target device | Folder | Purpose |
| --- | --- | --- |
| **AcharyaOS** | Samsung Galaxy J2 (`SM-J320M`) | [`acharya-os/`](acharya-os/) | Landscape launcher and focused daily environment for dharma, reading, scheduling, notes, and desk tools |
| **VishwakarmaOS** | Samsung Galaxy A10s (`SM-A107F/DS`) | [`vishwakarma-os/`](vishwakarma-os/) | Existing A10s OS/device customization, lockdown tooling, USB protocol, and Windows companion |

> **Current development focus:** AcharyaOS.

This git root is the shared workspace. Each flavor has its own README and build
instructions. Device-specific behavior must stay inside the corresponding
folder unless it is deliberately extracted as shared code.

## Acharya OS

Android launcher (`com.paperdesk.launcher`). Target device: Samsung SM-J320M (J2), ViperOS, Android 7.1.2, API 25, landscape 1280×720.

```powershell
cd acharya-os
.\gradlew.bat :app:installDebug
adb shell am start -S -n com.paperdesk.launcher/.ui.LauncherActivity
```

See [`acharya-os/README.md`](acharya-os/README.md).

## VishwakarmaOS

VishwakarmaOS is the Samsung A10s flavor developed before this repository was
split. Its folder currently contains the A10s device/OS scripts and the WinUI 3
Vishwakarma companion, which pushes schedule, panchanga, and settings over USB.

```powershell
cd vishwakarma-os
.\run.ps1
```

Device overlay and lockdown scripts live beside it. See [`vishwakarma-os/README.md`](vishwakarma-os/README.md).

## Shared data channel

The existing protocol path remains `/sdcard/Vishwakarma/` for compatibility.
AcharyaOS currently reads and writes these files, and the Windows companion
syncs them:

- `os-bundle.json` / `inbox/os-bundle.json`
- `inbox/settings.json`
- `panchanga.json` / `inbox/panchanga.json`
- `weather.json`

The Windows app caches a copy at `%USERPROFILE%\.paperdesk-companion\os-bundle.json`.
