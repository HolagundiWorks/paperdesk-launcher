# Vishwakarma OS

Windows companion, USB protocol, and device layer for the phone that runs Acharya OS.

The companion app assembly name is **Vishwakarma**. It syncs over ADB into `/sdcard/Vishwakarma` on the device.

## Windows companion

Requires the .NET 8 SDK and Windows App SDK.

```powershell
cd vishwakarma-os
.\run.ps1
```

Local cache: `%USERPROFILE%\.paperdesk-companion\os-bundle.json`

### Installer

```powershell
.\build-msi.ps1
```

Output: `dist\Vishwakarma-1.1.0-x64.msi`

## USB protocol

Phone paths:

| Path | Use |
| --- | --- |
| `/sdcard/Vishwakarma/os-bundle.json` | Live bundle |
| `/sdcard/Vishwakarma/inbox/os-bundle.json` | Inbox drop |
| `/sdcard/Vishwakarma/inbox/settings.json` | Settings drop |
| `/sdcard/Vishwakarma/panchanga.json` | Panchanga |
| `/sdcard/Vishwakarma/inbox/panchanga.json` | Panchanga inbox |
| `/sdcard/Vishwakarma/weather.json` | Weather / moon |

Schedule, Geethavani, always-on, and SRS panchanga ride in the USB bundle. Mantra, puja, stotra, and namavali libraries ship inside Acharya OS.

## Kotlin companion (optional)

Older Swing USB tool:

```powershell
cd companion
# needs JDK 17; use the Gradle wrapper from acharya-os or your IDE
```

## Device / OS layer

| Path | Role |
| --- | --- |
| `device/lockdown.ps1` | Landscape, density, Home app, strip extra packages |
| `device/unlock.ps1` | Reverse package disables |
| `os/userinit.sh` | Viper init.d desk mode |
| `os/overlay/` | Overlay APK bits |
| `os/bootanimation/` | Boot animation |

Lockdown keeps USB debugging on. It does not disable Magisk or Settings.

```powershell
.\device\lockdown.ps1
```

## Related

Phone launcher: [`../acharya-os/README.md`](../acharya-os/README.md).
