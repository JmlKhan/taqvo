#!/bin/bash
# Store screenshots on a booted simulator running the DEBUG build, one folder per language, driven
# entirely by the debug harness URLs (no taps). Sideload Alafasy surah 2 into the app's Application
# Support/quran/audio/ar.alafasy first, set the location (simctl location set 32.8872,13.1913 is
# Tripoli, Libya) and grant notifications once by hand.
#
#   tools/store/capture-ios.sh <udid> en ar fr tr id ur bn
#   ONLY="2-notifications" tools/store/capture-ios.sh <udid> en ar …   re-shoots just those
#
# Output: build/store-shots/ios/<lang>/<n>-<name>.png. The simulator has no compass, so 6-qibla is
# dropped by assemble.sh.
U=$1; shift; APP=com.taqvo.app; OUT=$(cd "$(dirname "$0")/../.." && pwd)/build/store-shots/ios
# iOS 27 asks "Open in Taqvo?" before simctl openurl reaches the app, so each shot relaunches it with its URLs in
# SIMCTL_CHILD_TAQVO_HARNESS_URLS, which the DEBUG build handles in order (RecitationHarness.runLaunchURLs): the
# first 2 s after launch, then every 1.5 s. run "<urls>" [extra seconds after the last one].
run() {
  xcrun simctl terminate $U $APP 2>/dev/null; sleep 1
  SIMCTL_CHILD_TZ=Africa/Tripoli SIMCTL_CHILD_TAQVO_HARNESS_URLS="$1" xcrun simctl launch $U $APP -AppleLanguages "($L)" -AppleLocale "$(locale_for $L)" >/dev/null
  local n; n=$(echo "$1" | wc -w | tr -d ' ')
  sleep $(( 5 + (3 * n + 1) / 2 + ${2:-2} ))
}
shot() { sleep ${2:-1}; xcrun simctl io $U screenshot "$D/$1.png" >/dev/null 2>&1; echo "  shot $1"; }
want() { [ -z "$ONLY" ] || [[ " $ONLY " == *" $1 "* ]]; }
tr_for() { case $1 in en) echo en.sahih;; ar) echo ar.muyassar;; fr) echo fr.hamidullah;; tr) echo tr.diyanet;; id) echo id.indonesian;; ur) echo ur.junagarhi;; bn) echo bn.bengali;; esac; }
locale_for() { case $1 in en) echo en_GB;; ar) echo ar_LY;; fr) echo fr_FR;; tr) echo tr_TR;; id) echo id_ID;; ur) echo ur_PK;; bn) echo bn_BD;; esac; }
for L in "$@"; do
  D=$OUT/$L; mkdir -p "$D"; echo "$L"
  BASE="taqvo://recite/theme?name=light taqvo://recite/mode?name=translation taqvo://recite/translation?id=$(tr_for $L)"
  want 1-prayer && { run "$BASE taqvo://recite/screen?name=prayer"; shot 1-prayer; }
  want 2-notifications && { run "$BASE taqvo://recite/tahajjud?name=on taqvo://recite/screen?name=notifications" 3; shot 2-notifications; run "taqvo://recite/tahajjud?name=off" 0; }
  want 3-reader && { run "$BASE taqvo://ayah/2/255" 4; shot 3-reader; }
  want 4-mushaf && { run "$BASE taqvo://recite/mode?name=mushaf taqvo://ayah/2/255" 4; shot 4-mushaf; }
  want 5-recitation && { run "$BASE taqvo://ayah/2/255 taqvo://recite/load?surah=2&ayah=255&reciter=ar.alafasy" 5; shot 5-recitation; run "taqvo://recite/stop" 0; }
  want 6-qibla && { run "$BASE taqvo://recite/screen?name=qibla" 4; shot 6-qibla; }
  want 7-tasbeeh && { run "$BASE taqvo://recite/screen?name=tasbeeh" 3; shot 7-tasbeeh; }
  want 9-adhkar && { run "$BASE taqvo://recite/screen?name=adhkar" 3; shot 9-adhkar; }
  want 8-dark && { run "taqvo://recite/theme?name=dark taqvo://recite/screen?name=prayer" 3; shot 8-dark; }
  run "taqvo://recite/theme?name=system" 0
  echo "  $(ls $D | wc -l | tr -d ' ') files"
done
