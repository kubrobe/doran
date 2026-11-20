#!/usr/bin/env bash
# Runs on the emulator: instrumented tests (full=1), then a random-tap crash check.
set -u
FULL=${1:-1}
adb wait-for-device
adb root >/dev/null 2>&1 || true
sleep 3
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb logcat -v time > logcat.txt 2>&1 &
LOGPID=$!
status=0
if [ "$FULL" = "1" ]; then
  adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  adb shell am instrument -w -r com.doranalley.test/androidx.test.runner.AndroidJUnitRunner > instrument.txt 2>&1
  grep -E "INSTRUMENTATION_STATUS: (class|test)=|INSTRUMENTATION_STATUS_CODE: -2|^OK|FAILURES|Tests run|INSTRUMENTATION_FAILED" instrument.txt | tail -80
  if ! grep -q "^OK (" instrument.txt; then status=1; fi
  mkdir -p out/screens
  adb pull /sdcard/Android/data/com.doranalley/files/screens out/ >/dev/null 2>&1 || true
  adb pull /storage/emulated/0/Android/data/com.doranalley/files/screens out/ >/dev/null 2>&1 || true
fi
# Fresh start, open the app, then 2000 random taps.
adb shell pm clear com.doranalley
adb shell am start -W -n com.doranalley/.ui.MainActivity
sleep 6
adb shell pidof com.doranalley || { echo "APP NOT RUNNING AFTER LAUNCH"; status=1; }
adb shell pm grant com.doranalley android.permission.RECORD_AUDIO || true
adb shell monkey -p com.doranalley --pct-syskeys 0 --pct-appswitch 0 --throttle 60 -s 42 -v 2000 > monkey.txt 2>&1
tail -5 monkey.txt
if grep -qE "// CRASH|// NOT RESPONDING|Monkey aborted" monkey.txt; then echo "MONKEY FOUND A CRASH"; status=1; fi
kill $LOGPID 2>/dev/null || true
grep -E "FATAL EXCEPTION|AndroidRuntime: (at com\.doranalley|[a-z]+\.[A-Za-z.]+Exception)" logcat.txt | head -30
grep -E "AsrTest|AudioTest|RecorderTest|KoreanAsr|AudioPlayer|MicRecorder" logcat.txt | cut -c19- | head -80
mkdir -p out && cp logcat.txt instrument.txt monkey.txt out/ 2>/dev/null || true
echo "RESULT status=$status"
exit $status
