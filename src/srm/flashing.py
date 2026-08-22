from pathlib import Path

from .process import run


def flash_patched_ap(config: dict, patched_ap: Path, model: str, confirmation: str) -> str:
    if not config.get("flash_enabled"):
        raise PermissionError("Flashing is disabled in config.json")
    if confirmation.strip().upper() != model.strip().upper():
        raise PermissionError("Typed model confirmation does not match")
    backend = Path(config.get("flash_backend", ""))
    if not backend.is_file():
        raise ValueError("Configured flash backend does not exist")
    args = [str(value).replace("{patched_ap}", str(patched_ap)) for value in config.get("flash_arguments", [])]
    code, output = run(backend, args, timeout=600)
    if code:
        raise RuntimeError(output or f"Flash backend exited with code {code}")
    return output or "Flash backend completed successfully"
