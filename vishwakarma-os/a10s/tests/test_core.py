import tempfile
import unittest
import zipfile
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from srm.firmware import classify, inspect_firmware
from srm.models import DeviceInfo
from srm.planner import make_plan, normalize_model
from srm.process import redact
from srm.profiles import canonical_model, find_profile, load_profiles


class CoreTests(unittest.TestCase):
    def test_classification(self):
        self.assertEqual(classify("folder/AP_A107FXXU8.tar.md5"), "AP")
        self.assertEqual(classify("HOME_CSC_OMC.tar.md5"), "HOME_CSC")
        self.assertIsNone(classify("readme.txt"))

    def test_firmware_and_plan(self):
        with tempfile.TemporaryDirectory() as folder:
            archive = Path(folder) / "firmware.zip"
            with zipfile.ZipFile(archive, "w") as zf:
                zf.writestr("AP_A107FXXU8_TEST.tar.md5", "ap")
                zf.writestr("BL_A107FXXU8_TEST.tar.md5", "bl")
                zf.writestr("CSC_A107F_TEST.tar.md5", "csc")
            firmware = inspect_firmware(archive)
            device = DeviceInfo("123", "device", model="SM-A107F", sdk="30")
            plan = make_plan(device, firmware)
            self.assertEqual(len(firmware.components), 3)
            self.assertFalse(plan.blockers)
            self.assertIn("full AP", plan.patch_target)

    def test_mismatch_blocks(self):
        with tempfile.TemporaryDirectory() as folder:
            archive = Path(folder) / "firmware.zip"
            with zipfile.ZipFile(archive, "w") as zf:
                zf.writestr("AP_A217FXX_TEST.tar.md5", "ap")
            plan = make_plan(DeviceInfo("1", "device", model="SM-A107F", sdk="30"), inspect_firmware(archive))
            self.assertTrue(plan.blockers)

    def test_helpers(self):
        self.assertEqual(normalize_model("SM-A107F"), "A107F")
        self.assertEqual(redact("token=abcdef"), "token=<redacted>")

    def test_a10s_dual_sim_profile(self):
        profiles = load_profiles(Path(__file__).resolve().parents[1] / "profiles")
        profile = find_profile("SM-A107F/DS", profiles)
        self.assertIsNotNone(profile)
        self.assertEqual(profile.display_name, "Samsung Galaxy A10s")
        self.assertEqual(canonical_model("SM-A107F/DS"), "A107F")


if __name__ == "__main__":
    unittest.main()
