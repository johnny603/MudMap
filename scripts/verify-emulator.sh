#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$SDK" ]] || { echo "Set ANDROID_HOME first." >&2; exit 1; }
ADB="$SDK/platform-tools/adb"
AVD="${AVD:-mudmap-api28}"
REPORT="$ROOT/app/build/reports/emulator"
mkdir -p "$REPORT"
# Install emulator + system-images;android-28;default;x86_64 and create this AVD.
"$SDK/emulator/emulator" -avd "$AVD" -no-window -no-audio -no-boot-anim \
    -no-snapshot -accel off -gpu swiftshader_indirect -memory 1536 -cores 2 \
    -camera-back none -camera-front none > "$REPORT/emulator.log" 2>&1 &
PID=$!
trap 'kill "$PID" 2>/dev/null || true' EXIT
BOOTED=false
for _ in $(seq 1 100); do
    if [[ "$(timeout 10 "$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
        BOOTED=true
        break
    fi
    kill -0 "$PID" 2>/dev/null || { tail -30 "$REPORT/emulator.log"; exit 1; }
    sleep 3
done
[[ "$BOOTED" == true ]] || { echo "Emulator did not boot."; exit 1; }
timeout 15 "$ADB" shell input keyevent 82
timeout 180 "$ADB" install --no-streaming -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
timeout 90 "$ADB" install --no-streaming -r "$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
"$ADB" shell pm grant com.mudmap android.permission.RECORD_AUDIO
"$ADB" logcat -c
timeout 300 "$ADB" shell am instrument -w \
    com.mudmap.test/androidx.test.runner.AndroidJUnitRunner | tee "$REPORT/instrumentation.txt"
"$ADB" shell am start -n com.mudmap/.MainActivity
sleep 15
"$ADB" exec-out screencap -p > "$REPORT/offline-map.png"
"$ADB" logcat -d > "$REPORT/logcat.txt"
rg -q 'OK \([0-9]+ tests?\)' "$REPORT/instrumentation.txt"
echo "Android runtime checks passed; see $REPORT."
