from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class DeviceInfo:
    serial: str
    state: str
    model: str = "Unknown"
    product: str = "Unknown"
    android: str = "Unknown"
    sdk: str = "Unknown"
    build: str = "Unknown"
    csc: str = "Unknown"
    bootloader: str = "Unknown"
    verified_boot: str = "Unknown"
    root_status: str = "Not checked"

    @property
    def authorized(self) -> bool:
        return self.state == "device"


@dataclass
class FirmwareComponent:
    kind: str
    name: str
    size: int


@dataclass
class FirmwareInfo:
    path: Path
    components: list[FirmwareComponent] = field(default_factory=list)
    model_hint: str = "Unknown"
    build_hint: str = "Unknown"

    def component(self, kind: str) -> FirmwareComponent | None:
        return next((item for item in self.components if item.kind == kind), None)


@dataclass
class RootPlan:
    strategy: str
    patch_target: str
    notes: list[str]
    blockers: list[str]
