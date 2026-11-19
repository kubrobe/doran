#!/usr/bin/env bash
# Installs the debug APK, launches it, and checks it is still running 20 s later.
set -u
APK=app/build/outputs/apk/debug/app-debug.apk
PKG=com.doranalley
adb install -r "$APK"
adb logcat -c
adb shell am start -W -n "$PKG/.ui.MainActivity"
sleep 20
adb logcat -d > logcat.txt
adb logcat -d -b crash > logcat-crash.txt 2>/dev/null || true
echo "===== crash buffer ====="
cat logcat-crash.txt
echo "===== AndroidRuntime / app errors ====="
grep -E "AndroidRuntime|FATAL|$PKG" logcat.txt | grep -E " E |FATAL|Exception|Caused by|at " | head -120
if adb shell pidof "$PKG" > /dev/null; then
  echo "RESULT: app is running"
  # Second launch after a cold restart (resume path).
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/.ui.MainActivity"
  sleep 10
  adb shell pidof "$PKG" > /dev/null && echo "RESULT: relaunch ok" || { echo "RESULT: crashed on relaunch"; adb logcat -d -b crash; exit 1; }
else
  echo "RESULT: app crashed"
  exit 1
fi
