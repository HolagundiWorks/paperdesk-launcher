# Samsung Root Manager

A safety-oriented Windows utility for inspecting Samsung devices and firmware, preparing a Magisk workflow, entering Download Mode, and verifying root.

This project does **not** exploit devices, bypass FRP, or unlock a bootloader. Samsung bootloader unlocking is a destructive, model/region-dependent action that must be completed by the owner on the device. Flashing is deliberately gated and disabled by default.

## Run

```powershell
.\run.ps1
```

The launcher uses `python` when available. A Python 3.11+ installation is recommended. ADB is discovered from `PATH` or `C:\Program Files\platform-tools\adb.exe`.

## Supported MVP workflow

An included device profile recognizes **Samsung Galaxy A10s SM-A107F and SM-A107F/DS**. It accepts only the matching `A107F` firmware family and deliberately rejects other regional A10s packages.

1. Connect an authorized device and select **Scan ADB**.
2. Load an official Samsung firmware `.zip`, `.tar`, or `.tar.md5` archive.
3. Review the detected AP/BL/CP/CSC components and generated strategy.
4. Patch the AP archive with the official Magisk app on the phone. This preserves Samsung's required archive structure.
5. Select the Magisk-produced patched `.tar` and validate it.
6. Reboot into Download Mode when ready.
7. Flash manually with Odin, or explicitly configure an Odin-compatible backend in `config.json` (experimental).
8. Reconnect through ADB and select **Verify root**.

## Safety model

- Read-only operations are the default.
- No automatic bootloader unlock or FRP operation exists.
- Firmware/model mismatch blocks package preparation.
- Binary paths and arguments are never passed through a shell.
- Flashing requires enabling it in configuration and typing the device model.
- Logs are written under `logs/`; command output is redacted for common secret-shaped values.

## Development

```powershell
.\run-tests.ps1
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/RECOVERY.md](docs/RECOVERY.md).
