# Paperdesk Launcher

Paperdesk Launcher is the combined workspace for the Paperdesk home apps and the
earlier device-specific projects. It merges
[HolagundiWorks/paperdesk](https://github.com/HolagundiWorks/paperdesk) with
[HolagundiWorks/Launcher3](https://github.com/HolagundiWorks/Launcher3)
(`paperdesk-custom`).

Project code is Apache 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

| Flavor | Target device | Folder | Purpose |
| --- | --- | --- | --- |
| **Paperdesk Launcher** | Android 7.1+ phones and tablets | [`paperdesk-launcher/`](paperdesk-launcher/) | Adaptive HOME launcher with a focused app list and first-class shortcuts for the Human Centric Works app family |
| **Rootless Paperdesk** | Android 5.0+ phones | [`launcher3/`](launcher3/) | Rootless Pixel Launcher customized for Paperdesk: built-in Arcticons, dark circular icons, Panchanga pinned on the home screen |
| **AcharyaOS** | Samsung Galaxy J2 (`SM-J320M`) | [`acharya-os/`](acharya-os/) | Landscape launcher and focused daily environment for dharma, reading, scheduling, notes, and desk tools |
| **VishwakarmaOS** | Samsung Galaxy A10s (`SM-A107F/DS`) | [`vishwakarma-os/`](vishwakarma-os/) | A10s OS/device customization, rooting and recovery tools, USB protocol, and Windows companion |

> **Current development focus:** Paperdesk Launcher.

This git root is the shared workspace. Each flavor has its own README and build
instructions. Device-specific behavior must stay inside the corresponding
folder unless it is deliberately extracted as shared code.

## Repository layout

```text
paperdesk-launcher/
├── paperdesk-launcher/          # Focused Android HOME launcher
├── launcher3/                  # Rootless Pixel Launcher (paperdesk-custom)
├── acharya-os/                 # Active Samsung J2 Android flavor
│   └── resources/reference/    # Curated source documents and catalogues
├── vishwakarma-os/             # Samsung A10s flavor
│   ├── a10s/                   # A10s manager, Android utilities, profiles and docs
│   ├── device/                 # Deployment and lockdown scripts
│   ├── os/                     # Overlay, boot animation and init assets
│   └── companion/              # Optional Kotlin USB companion
└── local-artifacts/            # Ignored local APK/MSI binaries; never source
```

## Paperdesk Launcher

Paperdesk Launcher (`in.hcworks.paperdesk.launcher`) is a selectable Android
Home app. Its initial release provides adaptive portrait/landscape layouts, a
live clock and date, a clean text-row app list, and priority shortcuts for
MadhvaNidhi, Architect Calculator, and Pomodoro.

```powershell
cd paperdesk-launcher
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

After installing the APK, open Paperdesk and choose **Make Paperdesk default**.
Keep the device's existing launcher installed until Paperdesk has been tested.

## Rootless Paperdesk

Rootless Paperdesk (`in.hcworks.paperdesk.rootless`) is the Pixel-style HOME
launcher brought in from Launcher3 branch `paperdesk-custom`. It inherits the
Arcticons 15.0.5 icon library from the bundled archive and applies that full
map, including calendar and clock icons. Paperdesk Launcher is not a fork of
Arcticons. Icon artwork stays under CC BY-SA 4.0 and is attributed in
[`launcher3/assets/ARCTICONS_NOTICE.txt`](launcher3/assets/ARCTICONS_NOTICE.txt).
The launcher also keeps a dark circular icon treatment, pins Panchanga on the
home screen, and removes the fixed search bar, Smartspace, and suggested apps.

Open [`launcher3/`](launcher3/) in Android Studio. Build details and the
upstream feature list are in [`launcher3/README.md`](launcher3/README.md).

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
