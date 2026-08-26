from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).parent))

from srm.sync_ui import CarbonSyncApp


if __name__ == "__main__":
    CarbonSyncApp().mainloop()
