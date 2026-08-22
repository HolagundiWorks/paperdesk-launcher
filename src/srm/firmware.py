from __future__ import annotations

import re
import tarfile
import zipfile
from pathlib import Path, PurePosixPath

from .models import FirmwareComponent, FirmwareInfo


COMPONENT = re.compile(r"^(AP|BL|CP|HOME_CSC|CSC)_", re.IGNORECASE)
# Samsung firmware names commonly omit the "SM-" prefix (A107F, G991B, etc.).
MODEL = re.compile(r"(?:AP|BL|CP|CSC)_(?:SM-)?([A-Z]\d{3,4}[A-Z])", re.IGNORECASE)


def classify(name: str) -> str | None:
    match = COMPONENT.match(PurePosixPath(name).name)
    return match.group(1).upper() if match else None


def inspect_firmware(path: Path) -> FirmwareInfo:
    if not path.is_file():
        raise ValueError("Firmware file does not exist")
    entries: list[tuple[str, int]] = []
    suffix = path.name.lower()
    if suffix.endswith(".zip"):
        with zipfile.ZipFile(path) as archive:
            entries = [(item.filename, item.file_size) for item in archive.infolist() if not item.is_dir()]
    elif suffix.endswith((".tar", ".tar.md5")):
        with tarfile.open(path, "r:*") as archive:
            entries = [(item.name, item.size) for item in archive.getmembers() if item.isfile()]
    else:
        raise ValueError("Select a .zip, .tar, or .tar.md5 Samsung firmware archive")
    components = [FirmwareComponent(kind, PurePosixPath(name).name, size) for name, size in entries if (kind := classify(name))]
    model_match = next((MODEL.search(item.name) for item in components if MODEL.search(item.name)), None)
    model = model_match.group(1).replace("-", "") if model_match else "Unknown"
    build = next((item.name.split("_")[1] for item in components if "_" in item.name), "Unknown")
    return FirmwareInfo(path=path, components=components, model_hint=model, build_hint=build)


def inspect_patched_ap(path: Path) -> list[str]:
    if not path.is_file() or not path.name.lower().endswith((".tar", ".tar.md5")):
        raise ValueError("Magisk output must be a .tar or .tar.md5 archive")
    with tarfile.open(path, "r:*") as archive:
        names = [PurePosixPath(item.name).name for item in archive.getmembers() if item.isfile()]
    relevant = [name for name in names if name.startswith(("boot.img", "init_boot.img", "recovery.img", "vbmeta.img"))]
    if not relevant:
        raise ValueError("Archive contains no boot, init_boot, recovery, or vbmeta image")
    return relevant
