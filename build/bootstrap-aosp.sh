#!/usr/bin/env bash
set -euo pipefail

# Run inside a native Linux filesystem, never /mnt/c or another Windows-mounted tree.
AOSP_ROOT="${AOSP_ROOT:-$HOME/aosp/a10s}"
AOSP_BRANCH="${AOSP_BRANCH:-android-11.0.0_r48}"
MIN_FREE_GB="${MIN_FREE_GB:-400}"

[[ "$(uname -s)" == "Linux" ]] || { echo "A 64-bit Linux host is required." >&2; exit 2; }
[[ "$(uname -m)" == "x86_64" ]] || { echo "An x86_64 host is required." >&2; exit 2; }
case "$AOSP_ROOT" in /mnt/*) echo "Use the Linux filesystem, not /mnt/*, for AOSP." >&2; exit 2;; esac

available_kb="$(df -Pk "$(dirname "$AOSP_ROOT")" | awk 'NR==2 {print $4}')"
required_kb="$((MIN_FREE_GB * 1024 * 1024))"
(( available_kb >= required_kb )) || { echo "Need at least ${MIN_FREE_GB} GB free; found $((available_kb / 1024 / 1024)) GB." >&2; exit 3; }

for command in git curl python3; do command -v "$command" >/dev/null || { echo "Missing host command: $command" >&2; exit 4; }; done
mkdir -p "$HOME/bin" "$AOSP_ROOT"
if [[ ! -x "$HOME/bin/repo" ]]; then
  curl --fail --location https://storage.googleapis.com/git-repo-downloads/repo -o "$HOME/bin/repo"
  chmod 0755 "$HOME/bin/repo"
fi
cd "$AOSP_ROOT"
"$HOME/bin/repo" init --partial-clone --no-use-superproject \
  -u https://android.googlesource.com/platform/manifest -b "$AOSP_BRANCH"
echo "Initialized $AOSP_BRANCH at $AOSP_ROOT"
echo "Review .repo/manifests before running: repo sync -c -j8"
