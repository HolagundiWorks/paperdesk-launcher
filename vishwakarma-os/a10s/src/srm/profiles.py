from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class DeviceProfile:
    id: str
    display_name: str
    models: tuple[str, ...]
    firmware_models: tuple[str, ...]
    transport: str
    patch_strategy: str
    patch_target: str
    flash_slots: tuple[str, ...]
    notes: tuple[str, ...]


def canonical_model(value: str) -> str:
    value = value.strip().upper().replace(" ", "")
    value = re.sub(r"/DS\d*$", "", value)
    return value.replace("SM-", "").replace("-", "")


def load_profiles(folder: Path) -> list[DeviceProfile]:
    profiles: list[DeviceProfile] = []
    if not folder.is_dir():
        return profiles
    for path in sorted(folder.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        profiles.append(DeviceProfile(
            id=data["id"], display_name=data["display_name"],
            models=tuple(data["models"]), firmware_models=tuple(data["firmware_models"]),
            transport=data["transport"], patch_strategy=data["patch_strategy"],
            patch_target=data["patch_target"], flash_slots=tuple(data["flash_slots"]),
            notes=tuple(data["notes"]),
        ))
    return profiles


def find_profile(model: str, profiles: list[DeviceProfile]) -> DeviceProfile | None:
    candidate = canonical_model(model)
    return next((profile for profile in profiles if candidate in {canonical_model(item) for item in profile.models}), None)
