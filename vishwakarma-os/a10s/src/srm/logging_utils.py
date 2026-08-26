import logging
from pathlib import Path


def configure(root: Path) -> logging.Logger:
    folder = root / "logs"
    folder.mkdir(exist_ok=True)
    logger = logging.getLogger("srm")
    if not logger.handlers:
        logger.setLevel(logging.INFO)
        handler = logging.FileHandler(folder / "srm.log", encoding="utf-8")
        handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(message)s"))
        logger.addHandler(handler)
    return logger
