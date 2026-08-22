from .models import DeviceInfo, FirmwareInfo, RootPlan
from .profiles import DeviceProfile, canonical_model


def normalize_model(value: str) -> str:
    return canonical_model(value)


def make_plan(device: DeviceInfo, firmware: FirmwareInfo, profile: DeviceProfile | None = None) -> RootPlan:
    blockers: list[str] = []
    notes = ["Bootloader unlock must be completed by the owner in Developer options/Download Mode."]
    if not firmware.component("AP"):
        blockers.append("Firmware has no AP component.")
    actual, expected = normalize_model(device.model), normalize_model(firmware.model_hint)
    if expected != "UNKNOWN" and actual != "UNKNOWN" and expected not in actual and actual not in expected:
        blockers.append(f"Firmware model {firmware.model_hint} does not match connected model {device.model}.")
    try:
        sdk = int(device.sdk)
    except ValueError:
        sdk = 0
    target = profile.patch_target if profile else ("init_boot.img" if sdk >= 33 else "boot/recovery image within the full AP archive")
    strategy = "Patch the complete AP archive in the official Magisk app, then flash the returned tar as AP."
    if profile:
        firmware_model = normalize_model(firmware.model_hint)
        allowed = {normalize_model(item) for item in profile.firmware_models}
        if firmware_model != "UNKNOWN" and firmware_model not in allowed:
            blockers.append(f"Firmware model {firmware.model_hint} is not allowed by profile {profile.display_name}.")
        notes.extend(profile.notes)
    if sdk and sdk < 29:
        notes.append("Older Samsung layouts may use recovery-mode activation; consult the Magisk Samsung instructions.")
    notes.append("Use HOME_CSC to preserve data only when the firmware transition permits it; CSC normally wipes data.")
    return RootPlan(strategy, target, notes, blockers)
