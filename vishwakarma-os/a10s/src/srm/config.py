import json
from pathlib import Path


DEFAULT = {
    "adb_path": r"C:\Program Files\platform-tools\adb.exe",
    "flash_enabled": False,
    "flash_backend": "",
    "flash_arguments": ["flash", "--AP", "{patched_ap}"],
    "workspace": "workspace",
    "sync_library": "library",
}


def load_config(root: Path) -> dict:
    path = root / "config.json"
    if not path.exists():
        return DEFAULT.copy()
    data = json.loads(path.read_text(encoding="utf-8"))
    return {**DEFAULT, **data}
