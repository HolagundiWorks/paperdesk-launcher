from __future__ import annotations

import shutil
from dataclasses import dataclass
from pathlib import Path, PurePosixPath

from .process import run


@dataclass(frozen=True)
class SyncCollection:
    key: str
    label: str
    phone_path: PurePosixPath
    extensions: tuple[str, ...]


COLLECTIONS = (
    SyncCollection("gallery", "Gallery", PurePosixPath("/sdcard/CarbonSync/Gallery"), (".jpg", ".jpeg", ".png", ".webp", ".gif", ".mp4", ".mkv")),
    SyncCollection("notes", "Notes", PurePosixPath("/sdcard/CarbonSync/Notes"), (".md", ".txt")),
    SyncCollection("pdf", "PDF", PurePosixPath("/sdcard/CarbonSync/Books/PDF"), (".pdf",)),
    SyncCollection("epub", "EPUB", PurePosixPath("/sdcard/CarbonSync/Books/EPUB"), (".epub",)),
)


class AdbSync:
    """Non-destructive USB/ADB library transport.

    Sync never deletes either side. This makes reconnecting after an interrupted USB
    transfer safe and keeps the desktop library as the recoverable source of truth.
    """

    def __init__(self, adb_path: str, desktop_root: Path):
        self.adb = Path(adb_path)
        self.desktop_root = desktop_root

    def connected_serial(self) -> str:
        code, output = run(self.adb, ["devices"], timeout=15)
        if code:
            raise RuntimeError(output)
        devices = [line.split()[0] for line in output.splitlines() if line.strip().endswith("\tdevice")]
        if len(devices) != 1:
            raise RuntimeError("Connect exactly one authorized Android device over USB.")
        return devices[0]

    def prepare(self, serial: str) -> None:
        for collection in COLLECTIONS:
            code, output = run(self.adb, ["-s", serial, "shell", "mkdir", "-p", str(collection.phone_path)])
            if code:
                raise RuntimeError(output)
            (self.desktop_root / collection.key).mkdir(parents=True, exist_ok=True)

    def push(self, serial: str, collection: SyncCollection) -> str:
        local = self.desktop_root / collection.key
        local.mkdir(parents=True, exist_ok=True)
        files = [path for path in local.rglob("*") if path.is_file() and path.suffix.lower() in collection.extensions]
        for source in files:
            relative = source.relative_to(local).as_posix()
            destination = collection.phone_path / relative
            run(self.adb, ["-s", serial, "shell", "mkdir", "-p", str(destination.parent)])
            code, output = run(self.adb, ["-s", serial, "push", str(source), str(destination)], timeout=300)
            if code:
                raise RuntimeError(output)
        return f"{collection.label}: sent {len(files)} file(s)"

    def pull(self, serial: str, collection: SyncCollection) -> str:
        local = self.desktop_root / collection.key
        local.mkdir(parents=True, exist_ok=True)
        code, output = run(self.adb, ["-s", serial, "pull", f"{collection.phone_path}/.", str(local)], timeout=600)
        if code and "does not exist" not in output.lower():
            raise RuntimeError(output)
        return f"{collection.label}: received files from device"

    def open_library(self) -> None:
        self.desktop_root.mkdir(parents=True, exist_ok=True)
        import os
        os.startfile(self.desktop_root)  # type: ignore[attr-defined]

    def adb_available(self) -> bool:
        return self.adb.is_file() or shutil.which(str(self.adb)) is not None
