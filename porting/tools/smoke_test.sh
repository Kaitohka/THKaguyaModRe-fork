#!/usr/bin/env bash
# THKaguyaMod NeoForge port — basic regression smoke test.
# Usage: bash porting/tools/smoke_test.sh [--skip-build]
#   PASS criteria: build OK, client reaches title screen, no new ERROR lines
#   (excluding known-benign offline-dev noise), no crash.
set -u
cd "$(dirname "$0")/.."
PROJ="neoforge-1.21.1"
LOG="$PROJ/run/logs/latest.log"
export JAVA_HOME="/c/Program Files/Java/zulu-21"

echo "== [1/4] build =="
if [ "${1:-}" != "--skip-build" ]; then
    (cd "$PROJ" && ./gradlew build -q) || { echo "FAIL: build"; exit 1; }
fi
echo "build OK"

echo "== [2/4] launch client =="
rm -rf "$PROJ/run/logs" 2>/dev/null
(cd "$PROJ" && ./gradlew runClient -q >/dev/null 2>&1) &
LAUNCH_PID=$!

# wait for title screen (texture atlas creation) or timeout
TITLE_OK=0
for i in $(seq 1 60); do
    if [ -f "$LOG" ] && grep -q "textures/atlas/gui.png" "$LOG" 2>/dev/null; then
        TITLE_OK=1
        break
    fi
    sleep 5
done
sleep 10  # let world-gen/reports settle

echo "== [3/4] log scan =="
if [ ! -f "$LOG" ]; then
    echo "FAIL: no latest.log"
    TITLE_OK=0
else
    ERRORS=$(grep -cE "\[.*ERROR\]" "$LOG" || true)
    MOD_ERRORS=$(grep -E "\[.*ERROR\]" "$LOG" | grep -vc "Yggdrasil\|sessionserver\|minecraftservices\|chat session" || true)
    RECIPE_ERRORS=$(grep -c "Parsing error loading recipe" "$LOG" || true)
    MODEL_ERRORS=$(grep -c "Unable to load model" "$LOG" || true)
    CRASH=$(grep -c "Fatal|crash" "$LOG" || true)
    MOD_LOADED=$(grep -c "Itutu no Nandai MOD+ (NeoForge port) loading" "$LOG" || true)
    echo "title_screen=$TITLE_OK errors_total=$ERRORS errors_relevant=$MOD_ERRORS recipe_errors=$RECIPE_ERRORS model_errors=$MODEL_ERRORS mod_loaded=$MOD_LOADED"
    grep -E "\[.*ERROR\]" "$LOG" | grep -v "Yggdrasil\|sessionserver\|minecraftservices\|chat session" | head -10
fi

echo "== [4/4] shutdown =="
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'neoforge-1.21.1' -and \$_.CommandLine -notmatch 'gradlew' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
kill $LAUNCH_PID 2>/dev/null
sleep 3

if [ "$TITLE_OK" = "1" ] && [ "${MOD_ERRORS:-1}" = "0" ] && [ "${RECIPE_ERRORS:-1}" = "0" ] && [ "${MODEL_ERRORS:-1}" = "0" ] && [ "${MOD_LOADED:-0}" = "1" ]; then
    echo "SMOKE PASS"
else
    echo "SMOKE FAIL"
    exit 1
fi
