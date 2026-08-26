from __future__ import annotations

import shutil
from pathlib import Path

from .models import DeviceInfo
from .process import run


PROPERTIES = {
    "model": "ro.product.model",
    "product": "ro.product.device",
    "android": "ro.build.version.release",
    "sdk": "ro.build.version.sdk",
    "build": "ro.build.display.id",
    "csc": "ro.boot.sales_code",
    "bootloader": "ro.bootloader",
    "verified_boot": "ro.boot.verifiedbootstate",
}


class AdbClient:
    def __init__(self, configured_path: str = ""):
        candidates = [configured_path, shutil.which("adb"), r"C:\Program Files\platform-tools\adb.exe"]
        self.path = Path(next((p for p in candidates if p and Path(p).is_file()), "adb"))

    def devices(self) -> list[DeviceInfo]:
        code, output = run(self.path, ["devices", "-l"])
        if code:
            raise RuntimeError(output or "ADB failed")
        found: list[DeviceInfo] = []
        for line in output.splitlines()[1:]:
            parts = line.split()
            if len(parts) < 2 or line.startswith("*"):
                continue
            info = DeviceInfo(serial=parts[0], state=parts[1])
            if info.authorized:
                for field, prop in PROPERTIES.items():
                    rc, value = run(self.path, ["-s", info.serial, "shell", "getprop", prop])
                    if rc == 0 and value.strip():
                        setattr(info, field, value.strip().splitlines()[0])
            found.append(info)
        return found

    def reboot_download(self, serial: str) -> None:
        code, output = run(self.path, ["-s", serial, "reboot", "download"])
        if code:
            raise RuntimeError(output or "Could not request Download Mode")

    def verify_root(self, serial: str) -> tuple[bool, str]:
        code, output = run(self.path, ["-s", serial, "shell", "su", "-c", "id"], timeout=15)
        rooted = code == 0 and "uid=0" in output
        return rooted, output or ("Root access granted" if rooted else "Root access not granted")
