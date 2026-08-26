from __future__ import annotations

import re
import subprocess
from pathlib import Path


SECRET = re.compile(r"(?i)(token|password|secret|authorization)(\s*[:=]\s*)(\S+)")


def redact(value: str) -> str:
    return SECRET.sub(r"\1\2<redacted>", value)


def run(binary: Path | str, args: list[str], timeout: int = 30) -> tuple[int, str]:
    command = [str(binary), *args]
    try:
        result = subprocess.run(
            command, capture_output=True, text=True, timeout=timeout,
            encoding="utf-8", errors="replace", shell=False,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
        output = "\n".join(part.strip() for part in (result.stdout, result.stderr) if part.strip())
        return result.returncode, redact(output)
    except subprocess.TimeoutExpired:
        return 124, f"Command timed out after {timeout} seconds"
    except OSError as exc:
        return 127, str(exc)
