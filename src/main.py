from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).parent))

from srm.ui import SamsungRootManagerApp


if __name__ == "__main__":
    SamsungRootManagerApp().mainloop()
