# Samsung Galaxy A10s Device Port Plan

Status: discovery baseline  
Owner-reported target: `SM-A107F/DS`  
Allowed firmware family: `A107F`

## Purpose and safety boundary

This plan establishes a reproducible AOSP-derived baseline before removing hardware features. It separates observed facts from assumptions. The project contains no exploit, FRP bypass, anti-rollback bypass, or remote unlock. Unlock availability is observed on the physical regional device; an owner-supported unlock, if available, is manual and normally erases data.

`SM-A107F/DS` normalizes to `A107F` only for firmware matching. A107M/G/N packages are not interchangeable. A model match alone is insufficient: CSC, bootloader revision, partition layout, and hashes are also gates. Samsung Download Mode/Odin-family transport is used, not assumed fastboot.

## Evidence ledger

| Item | Current state | Evidence source |
|---|---|---|
| Model | owner reports `SM-A107F/DS`; verify | label, Download Mode, properties |
| Product/device/board | `a10s` / `S96116RA1` observed | [baseline](evidence/A107F_BASELINE_2026-08-22.md) |
| CSC and build | `INS` / `A107FXXS8CWD3` observed | baseline |
| Bootloader | `A107FXXS8CWD3`, locked | baseline; revision interpretation pending firmware intake |
| SoC/kernel | `mt6765` platform, `mt6762` hardware, kernel 4.19.127 observed | baseline; config/source pending |
| Partition/dynamic layout | dedicated partitions observed; dynamic property empty | baseline; fstab/images pending |
| A/B update model | A/B property empty | baseline; metadata confirmation pending |
| Treble/VNDK/vendor HALs | Treble true / VNDK 30 | baseline; manifests pending |
| Encryption/metadata layout | file-based encryption observed | baseline; fstab pending |
| Display/touch/USB components | unknown | DT, kernel logs, manifests |
| Unlock availability/state | unknown | Developer options, Download Mode |

Unknown values are gates, not permission to copy data from another A10s variant.

## Read-only device capture

With stock firmware booted and ADB authorized, capture raw output in a dated evidence directory:

```powershell
adb devices -l
adb shell getprop
adb shell uname -a
adb shell cat /proc/cpuinfo
adb shell cat /proc/meminfo
adb shell cat /proc/mounts
adb shell ls -la /dev/block/by-name
adb shell dumpsys SurfaceFlinger
adb shell dumpsys input
adb shell dumpsys usb
adb shell dumpsys battery
adb shell lshal
```

Permission denials are evidence; do not weaken security to avoid them. Shared reports redact serials, accounts, network identifiers, and unique device IDs.

## Stock firmware intake

Retain the untouched exact official package. Record source, original filename, byte length, SHA-256, model/CSC/bootloader metadata, component contents and sizes, extraction tool/version, and timestamp for the outer archive plus BL/AP/CP/CSC.

Extract only into a new workspace. Reject traversal, links, duplicate normalized paths, and decompression over configured limits. Then inventory boot/recovery headers and ramdisks, DTB/DTBO, vbmeta descriptors/rollback indexes, fstab/encryption, dynamic-partition metadata, actual partition images, SELinux policy, vendor manifests, and USB init/configfs setup. Do not disable verified boot as an analysis shortcut.

## Source selection

Evaluate and pin:

1. Samsung-published kernel source corresponding to the captured build;
2. a known-working tree for the exact A107F family with commit/test evidence;
3. matching proprietary files extracted from the owner's stock firmware where licensing permits;
4. an AOSP/Lineage-derived branch compatible with the recorded vendor interface.

For each, record URL, commit, license, variants, blobs, toolchain, partition assumptions, defects, and boot evidence. Another regional variant is reference material until every difference is reconciled.

## Bring-up gates

### Gate 0 — Recovery readiness

- stock firmware retained and hashed;
- Download Mode/recovery key sequences physically tested;
- host USB/tool detection verified non-destructively;
- model and bootloader-revision checks enforced;
- data-loss and recovery expectations documented.

### Gate 1 — Reproducible build

- pinned manifest/toolchain and host requirements;
- two clean successful builds;
- output hashes recorded or nondeterminism explained;
- image structure validated before writing.

### Gate 2 — First boot

- boot evidence and persistent logs;
- no repeated writes or thermal emergency;
- recovery and Download Mode remain reachable.

### Gate 3 — Essential hardware

- display/brightness, touch/multitouch/wake;
- storage and encryption;
- charging, USB enumeration, development ADB;
- suspend/resume, power button, battery and thermal reporting.

Every item needs a repeatable test and log.

## Hardware reduction

Begin only after Gate 3. Disable one capability class per reversible commit: Wi-Fi, Bluetooth, telephony/SIM/data, location, camera/microphone, then sensors. First map dependent init services, HAL clients, SELinux domains, shared libraries, and wakelocks. Prefer product packages, framework feature declarations, and service policy before kernel driver removal. After each change retest boot, USB, suspend, thermal, battery, display, and touch.

Do not blindly delete vendor binaries. Kernel removal is a late measured optimization after user-space disablement is stable.

## Production policy

Production defaults ADB off, never preauthorizes keys, exposes no network ADB, restricts USB functions, and requires visible consent for project sync. SELinux remains enforcing; no blanket allow rules or broad capability workarounds are accepted.

Before every device write, record normalized model, hashed serial, current and target bootloader revisions, build, image hashes, partition targets, tested recovery route, data-loss expectation, and operator confirmation. Block unknown targets, mismatches, downgrades, missing recovery media, and structurally invalid images.

## Milestone 01 exit criteria

- [ ] Label, Android properties, and Download Mode agree on model family.
- [ ] CSC, bootloader, fingerprint, kernel, board, and partition layout captured.
- [ ] Exact stock package/components archived with hashes.
- [ ] Unlock state observed rather than assumed.
- [ ] Recovery and Download Mode procedures tested.
- [ ] Kernel/device/vendor candidates pinned and licensed.
- [ ] Variant-tree differences documented.
- [ ] Build manifest/toolchain reproducible.
- [ ] No custom image flashed before all preceding checks pass.

The next artifact is a versioned evidence bundle from the physical handset and exact firmware. It will support the ADR selecting the ROM baseline and replace each applicable `unknown` above with a local evidence path and hash.
