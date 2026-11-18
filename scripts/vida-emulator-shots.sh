#!/usr/bin/env bash
# Instala Salve y su APK de pruebas en el emulador, ejecuta VidaScreenshotDeviceTest en dos
# anchos de pantalla (≈411 dp y ≈360 dp) y descarga las capturas a build/vida-emulator/.
set -uo pipefail
out=build/vida-emulator
mkdir -p "$out"
adb shell getprop ro.product.cpu.abilist | tee "$out/abilist.txt"
adb install -r -g app/build/outputs/apk/debug/app-debug.apk 2>&1 | tee "$out/install.txt"
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk 2>&1 | tee -a "$out/install.txt"
status=0
for density in 420 480; do
  adb shell wm density "$density"
  sleep 3
  adb shell am instrument -w -e vidaShots true -e class salve.presentation.ui.VidaScreenshotDeviceTest \
    com.salve.app.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee "$out/instrument-$density.txt"
  grep -q "OK (1 test)" "$out/instrument-$density.txt" || status=1
done
adb shell wm density reset
for f in $(adb shell run-as com.salve.app ls files/vida-shots 2>/dev/null | tr -d '\r'); do
  adb exec-out run-as com.salve.app cat "files/vida-shots/$f" > "$out/$f"
done
adb logcat -d -t 2000 > "$out/logcat.txt" 2>/dev/null || true
ls -la "$out"
exit $status
