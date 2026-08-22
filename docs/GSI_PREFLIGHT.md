# A10s Treble GSI preflight

Date: 2026-08-22  
Device: Samsung `SM-A107F`, stock `A107FXXS8CWD3`, CSC `INS`

## Candidate

PHH AOSP 12.1 v416:

`system-squeak-arm32_binder64-ab-vndklite-vanilla.img.xz`

Source: <https://github.com/phhusson/treble_experimentations/releases/tag/v416>

| Check | Result |
|---|---|
| Device ABI | `armeabi-v7a`, 32-bit zygote |
| Treble/VNDK | enabled / 30 |
| System-as-root | true |
| Required GSI variant | `arm32_binder64-ab` |
| Bootloader | locked; OEM unlock supported and allowed |
| Verified Boot | green/enforcing |
| Compressed bytes | 470,059,992 |
| Compressed SHA-256 | `2edcf24b6fa1098b570719fe5690c739add2a9fc42e87e9a1199c30f0f53b42b` |
| Expanded bytes | 1,271,009,280 |
| Expanded SHA-256 | `dfd8193df1345534717a6c6591e7e61b8fc3e27ba1be61016b844b82c1269578` |
| Expanded filesystem | ext4 magic verified |
| System partition | 3,559,362,560 bytes |
| Size headroom | 2,288,353,280 bytes |
| Size verdict | pass |

The upstream archived release does not provide a separately signed checksum. These hashes establish local repeatability, not publisher authenticity.

## Destructive-operation gates

- [ ] User confirms all personal files and app data are backed up elsewhere.
- [ ] Exact stock `A107FXXS8CWD3` BL/AP/CP/CSC package is retained and hashed.
- [ ] Samsung USB/Odin recovery detection is tested.
- [ ] Recovery/vbmeta files are sourced for the exact A107F Android 11/binary-8 baseline and hashed.
- [ ] Recovery path to stock is documented and understood.
- [ ] User accepts full data wipe and permanent Knox trip.
- [ ] Bootloader is unlocked manually and its state is re-verified.
- [ ] Only then is a write procedure prepared.

## Appliance hardening order

First boot the unchanged vanilla GSI and validate display, touch, storage, USB, charging, suspend, and custom APK compatibility. Then disable radios and unused capabilities one class at a time, with a reboot/regression check after each change. Do not remove vendor HALs during the initial GSI installation.
