#!/usr/bin/env bash
# Downloads the Android SDK command-line tools and installs the packages
# needed to build this project. Safe to re-run.
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CLT_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  curl -fL -o /tmp/clt.zip "$CLT_URL"
  rm -rf /tmp/clt && unzip -q /tmp/clt.zip -d /tmp/clt
  mv /tmp/clt/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
  rm -f /tmp/clt.zip
fi

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
# `yes` dies of SIGPIPE when sdkmanager exits; don't let pipefail turn that into a failure
(yes || true) | "$SDKMANAGER" --licenses > /dev/null
"$SDKMANAGER" "platform-tools" "platforms;android-35" "build-tools;35.0.0"

echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_DIR/local.properties"
echo "Android SDK installed at $ANDROID_HOME"
