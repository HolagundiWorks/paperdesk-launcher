from __future__ import annotations

import threading
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox, ttk

from .config import load_config
from .device import AdbClient
from .firmware import inspect_firmware, inspect_patched_ap
from .flashing import flash_patched_ap
from .logging_utils import configure
from .models import DeviceInfo, FirmwareInfo
from .planner import make_plan
from .profiles import find_profile, load_profiles


ROOT = Path(__file__).resolve().parents[2]


class SamsungRootManagerApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Samsung Root Manager")
        self.geometry("1040x720")
        self.minsize(860, 600)
        self.configure(bg="#0b1220")
        self.config_data = load_config(ROOT)
        self.adb = AdbClient(self.config_data.get("adb_path", ""))
        self.log = configure(ROOT)
        self.profiles = load_profiles(ROOT / "profiles")
        self.device: DeviceInfo | None = None
        self.firmware: FirmwareInfo | None = None
        self.patched_ap: Path | None = None
        self.profile = None
        self._style()
        self._build()

    def _style(self):
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure("TFrame", background="#0b1220")
        style.configure("Card.TFrame", background="#121c2e")
        style.configure("TLabel", background="#0b1220", foreground="#dce7f7", font=("Segoe UI", 10))
        style.configure("Title.TLabel", font=("Segoe UI Semibold", 22), foreground="#f4f8ff")
        style.configure("Sub.TLabel", foreground="#8fa4bf")
        style.configure("Card.TLabel", background="#121c2e", foreground="#dce7f7")
        style.configure("Value.Card.TLabel", background="#121c2e", foreground="#68d5c4", font=("Segoe UI Semibold", 11))
        style.configure("TButton", font=("Segoe UI Semibold", 10), padding=(14, 9))
        style.configure("Accent.TButton", background="#2878ff", foreground="white")

    def _build(self):
        main = ttk.Frame(self, padding=24)
        main.pack(fill="both", expand=True)
        ttk.Label(main, text="Samsung Root Manager", style="Title.TLabel").pack(anchor="w")
        ttk.Label(main, text="Controlled firmware preparation and root verification • no exploits or FRP bypass", style="Sub.TLabel").pack(anchor="w", pady=(2, 18))

        actions = ttk.Frame(main)
        actions.pack(fill="x", pady=(0, 14))
        for text, command in [
            ("1  Scan ADB", self.scan), ("2  Load firmware", self.load_firmware),
            ("3  Select patched AP", self.load_patched), ("4  Enter Download Mode", self.enter_download),
            ("5  Flash (gated)", self.flash), ("6  Verify root", self.verify_root),
        ]:
            ttk.Button(actions, text=text, command=command, style="Accent.TButton" if text.startswith("1") else "TButton").pack(side="left", padx=(0, 8))

        cards = ttk.Frame(main)
        cards.pack(fill="x")
        self.device_text = tk.StringVar(value="No ADB device scanned")
        self.firmware_text = tk.StringVar(value="No firmware selected")
        self.plan_text = tk.StringVar(value="A strategy will appear after device and firmware inspection")
        for title, variable in [("DEVICE", self.device_text), ("FIRMWARE", self.firmware_text), ("ROOT PLAN", self.plan_text)]:
            card = ttk.Frame(cards, style="Card.TFrame", padding=16)
            card.pack(side="left", fill="both", expand=True, padx=(0, 10))
            ttk.Label(card, text=title, style="Card.TLabel", font=("Segoe UI Semibold", 9)).pack(anchor="w")
            ttk.Label(card, textvariable=variable, style="Value.Card.TLabel", justify="left", wraplength=285).pack(anchor="w", pady=(8, 0))

        ttk.Label(main, text="STATUS & SAFETY LOG", style="Sub.TLabel").pack(anchor="w", pady=(20, 7))
        self.console = tk.Text(main, bg="#08101d", fg="#c6d4e7", insertbackground="white", relief="flat", font=("Cascadia Mono", 9), padx=14, pady=12)
        self.console.pack(fill="both", expand=True)
        self.status("Ready. Start with an authorized ADB connection. Back up your data before any unlock or flash.")

    def status(self, message: str):
        self.console.insert("end", message.rstrip() + "\n")
        self.console.see("end")
        self.log.info(message.replace("\n", " | "))

    def work(self, label: str, operation, done):
        self.status(label)
        def worker():
            try:
                result = operation()
                self.after(0, lambda: done(result))
            except Exception as exc:
                self.after(0, lambda e=exc: self.fail(e))
        threading.Thread(target=worker, daemon=True).start()

    def fail(self, exc: Exception):
        self.status(f"ERROR: {exc}")
        messagebox.showerror("Samsung Root Manager", str(exc))

    def scan(self):
        self.work("Scanning ADB devices…", self.adb.devices, self._scanned)

    def _scanned(self, devices: list[DeviceInfo]):
        if not devices:
            return self.fail(RuntimeError("No ADB devices found"))
        authorized = [item for item in devices if item.authorized]
        self.device = authorized[0] if authorized else devices[0]
        d = self.device
        self.profile = find_profile(d.model, self.profiles)
        profile_name = self.profile.display_name if self.profile else "Generic Samsung profile"
        self.device_text.set(f"{profile_name}\n{d.model}\n{d.product} • Android {d.android}\nBL: {d.bootloader}\nCSC: {d.csc}\nADB: {d.state}")
        self.status(f"Detected {d.model} ({d.serial}), ADB state: {d.state}")
        self.update_plan()

    def load_firmware(self):
        selected = filedialog.askopenfilename(title="Select official Samsung firmware", filetypes=[("Samsung firmware", "*.zip *.tar *.md5"), ("All files", "*.*")])
        if not selected:
            return
        try:
            self.firmware = inspect_firmware(Path(selected))
            f = self.firmware
            components = ", ".join(item.kind for item in f.components) or "none"
            self.firmware_text.set(f"{f.path.name}\nModel hint: {f.model_hint}\nComponents: {components}")
            self.status(f"Firmware inspected read-only: {len(f.components)} components; model hint {f.model_hint}")
            self.update_plan()
        except Exception as exc:
            self.fail(exc)

    def update_plan(self):
        if not (self.device and self.firmware):
            return
        plan = make_plan(self.device, self.firmware, self.profile)
        state = "BLOCKED" if plan.blockers else "READY TO PATCH"
        details = plan.blockers or [plan.patch_target]
        self.plan_text.set(f"{state}\n{plan.strategy}\n" + "\n".join(details))
        self.status("Plan: " + state + ". " + " ".join(plan.blockers + plan.notes))

    def load_patched(self):
        selected = filedialog.askopenfilename(title="Select Magisk patched AP tar", filetypes=[("Tar archive", "*.tar *.md5"), ("All files", "*.*")])
        if not selected:
            return
        try:
            images = inspect_patched_ap(Path(selected))
            self.patched_ap = Path(selected)
            self.status(f"Patched AP validated: {self.patched_ap.name}; relevant images: {', '.join(images)}")
        except Exception as exc:
            self.fail(exc)

    def enter_download(self):
        if not self.device or not self.device.authorized:
            return self.fail(RuntimeError("Scan an authorized ADB device first"))
        if not messagebox.askyesno("Enter Download Mode", "The device will reboot into Samsung Download Mode. Continue?"):
            return
        self.work("Requesting Download Mode…", lambda: self.adb.reboot_download(self.device.serial), lambda _: self.status("Download Mode requested. Confirm the screen on the device."))

    def flash(self):
        if not (self.device and self.firmware and self.patched_ap):
            return self.fail(RuntimeError("Device, official firmware, and a validated patched AP are required"))
        plan = make_plan(self.device, self.firmware, self.profile)
        if plan.blockers:
            return self.fail(RuntimeError(" ".join(plan.blockers)))
        confirmation = self._ask_confirmation(self.device.model)
        if confirmation is None:
            return
        self.work("Starting configured flash backend…", lambda: flash_patched_ap(self.config_data, self.patched_ap, self.device.model, confirmation), self.status)

    def _ask_confirmation(self, model: str) -> str | None:
        dialog = tk.Toplevel(self)
        dialog.title("Destructive operation confirmation")
        dialog.transient(self); dialog.grab_set(); dialog.resizable(False, False)
        ttk.Label(dialog, text=f"Flashing can brick the device or erase data.\nType the exact model {model} to continue.", padding=16).pack()
        value = tk.StringVar(); ttk.Entry(dialog, textvariable=value, width=35).pack(padx=16)
        result = {"value": None}
        def accept(): result["value"] = value.get(); dialog.destroy()
        ttk.Button(dialog, text="Confirm flash", command=accept).pack(pady=16)
        self.wait_window(dialog)
        return result["value"]

    def verify_root(self):
        if not self.device or not self.device.authorized:
            return self.fail(RuntimeError("Scan an authorized ADB device first"))
        def done(result):
            rooted, output = result
            self.status(("ROOT VERIFIED: " if rooted else "Root not verified: ") + output)
            messagebox.showinfo("Root verification", "Root access verified." if rooted else "Root access was not granted. Check Magisk and its prompt.")
        self.work("Requesting root identity through ADB (approve the Magisk prompt)…", lambda: self.adb.verify_root(self.device.serial), done)
