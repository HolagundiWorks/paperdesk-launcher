# Vishwakarma OS

Samsung A10s (`SM-A107F/DS`) OS flavor, device customization, USB protocol, and
Windows companion.

This is the earlier Samsung A10s work, now named **VishwakarmaOS**. AcharyaOS is
the separate Samsung J2 flavor and is the current development focus.

## Layout

| Path | Role |
| --- | --- |
| `a10s/` | Imported A10s manager, Android utilities, profiles, recovery docs, and tests |
| `device/` | Deployment, lockdown, and restore scripts |
| `os/` | Device overlay, boot animation, and init assets |
| `companion/` | Optional Kotlin USB companion |
| repository root | WinUI 3 Vishwakarma desktop companion |

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

## Samsung A10s device / OS layer

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

Current active flavor: [`../acharya-os/README.md`](../acharya-os/README.md).
