from __future__ import annotations

import threading
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox, ttk

from .config import load_config
from .sync import AdbSync, COLLECTIONS


ROOT = Path(__file__).resolve().parents[2]
BG, LAYER, BORDER = "#161616", "#262626", "#525252"
TEXT, MUTED, BLUE = "#f4f4f4", "#c6c6c6", "#0f62fe"


class CarbonSyncApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Carbon Device Sync")
        self.geometry("900x610")
        self.minsize(760, 520)
        self.configure(bg=BG)
        config = load_config(ROOT)
        library = Path(config.get("sync_library", ROOT / "library"))
        self.sync = AdbSync(config.get("adb_path", "adb"), library)
        self.serial = ""
        self._style()
        self._build()

    def _style(self):
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure("TFrame", background=BG)
        style.configure("Layer.TFrame", background=LAYER, relief="flat")
        style.configure("TLabel", background=BG, foreground=TEXT, font=("Segoe UI", 10))
        style.configure("Layer.TLabel", background=LAYER, foreground=TEXT)
        style.configure("Muted.TLabel", background=BG, foreground=MUTED)
        style.configure("TButton", background=LAYER, foreground=TEXT, bordercolor=BORDER, focusthickness=0, padding=(16, 11))
        style.map("TButton", background=[("active", "#393939")])
        style.configure("Primary.TButton", background=BLUE, foreground="white", bordercolor=BLUE)
        style.map("Primary.TButton", background=[("active", "#0353e9")])

    def _build(self):
        shell = ttk.Frame(self, padding=24)
        shell.pack(fill="both", expand=True)
        ttk.Label(shell, text="CARBON DEVICE SYNC", font=("Segoe UI Semibold", 23)).pack(anchor="w")
        ttk.Label(shell, text="Offline USB library · Gallery · Notes · PDF · EPUB", style="Muted.TLabel").pack(anchor="w", pady=(2, 22))
        toolbar = ttk.Frame(shell)
        toolbar.pack(fill="x", pady=(0, 16))
        ttk.Button(toolbar, text="CONNECT DEVICE", style="Primary.TButton", command=self.connect).pack(side="left")
        ttk.Button(toolbar, text="OPEN DESKTOP LIBRARY", command=self.sync.open_library).pack(side="left", padx=(8, 0))
        self.device_text = tk.StringVar(value="No device connected")
        ttk.Label(toolbar, textvariable=self.device_text, style="Muted.TLabel").pack(side="right")

        for collection in COLLECTIONS:
            row = ttk.Frame(shell, style="Layer.TFrame", padding=16)
            row.pack(fill="x", pady=(0, 1))
            ttk.Label(row, text=collection.label.upper(), style="Layer.TLabel", font=("Segoe UI Semibold", 12), width=12).pack(side="left")
            ttk.Label(row, text=str(collection.phone_path), style="Layer.TLabel", foreground=MUTED).pack(side="left", fill="x", expand=True)
            ttk.Button(row, text="FROM PHONE", command=lambda c=collection: self.transfer("pull", c)).pack(side="right")
            ttk.Button(row, text="TO PHONE", command=lambda c=collection: self.transfer("push", c)).pack(side="right", padx=(0, 8))

        ttk.Label(shell, text="ACTIVITY", style="Muted.TLabel").pack(anchor="w", pady=(22, 7))
        self.log = tk.Text(shell, height=9, bg="#0b0b0b", fg=TEXT, insertbackground=TEXT, relief="flat", padx=12, pady=10, font=("Cascadia Mono", 9))
        self.log.pack(fill="both", expand=True)
        self.status("Ready. Transfers are additive and never delete files on either side.")

    def status(self, value: str):
        self.log.insert("end", value + "\n")
        self.log.see("end")

    def work(self, operation):
        def worker():
            try:
                result = operation()
                self.after(0, lambda: self.status(result))
            except Exception as exc:
                self.after(0, lambda e=exc: (self.status(f"ERROR: {e}"), messagebox.showerror("Carbon Device Sync", str(e))))
        threading.Thread(target=worker, daemon=True).start()

    def connect(self):
        def operation():
            self.serial = self.sync.connected_serial()
            self.sync.prepare(self.serial)
            self.after(0, lambda: self.device_text.set(f"USB · {self.serial}"))
            return f"Connected {self.serial}; phone library prepared."
        self.work(operation)

    def transfer(self, direction, collection):
        if not self.serial:
            messagebox.showwarning("Carbon Device Sync", "Connect the phone first.")
            return
        self.status(f"{collection.label}: {direction} started…")
        self.work(lambda: getattr(self.sync, direction)(self.serial, collection))
