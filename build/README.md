# AOSP build host

The physical device currently runs Android 11/vendor API 30, so the first compatibility baseline is pinned to `android-11.0.0_r48`. This is a bring-up starting point, not yet a bootable A10s product: AOSP alone lacks Samsung/MediaTek device configuration and proprietary vendor libraries.

Official current AOSP guidance requires a 64-bit Linux host, at least 400 GB free disk, and 64 GB RAM for current full builds. This workstation currently lacks Linux/WSL and has at most about 168.5 GB free on one drive, so source sync is intentionally blocked.

After providing a Linux filesystem with 400+ GB free:

```bash
sudo apt-get install git-core gnupg flex bison build-essential zip curl \
  zlib1g-dev libc6-dev-i386 x11proto-core-dev libx11-dev lib32z1-dev \
  libgl1-mesa-dev libxml2-utils xsltproc unzip fontconfig python3
AOSP_ROOT=$HOME/aosp/a10s ./build/bootstrap-aosp.sh
cd $HOME/aosp/a10s
$HOME/bin/repo sync -c -j8
source build/envsetup.sh
lunch aosp_arm-eng
m -j2
```

The generic target only validates the host/toolchain. Do not flash generic output to the A10s. A device product is added only after exact kernel, device tree, vendor blobs, image headers, and partition rules are established.
