#!/usr/bin/env bash
# THKaguyaMod NeoForge port — 30-minute dedicated-server soak (release gate).
# Boots runServerSmoke in run-smoke/, injects a load-datapack that summons a
# representative entity roster at spawn (living AI/attributes/navigation tick
# all window long), then checks the log for ERRORs / OOM / "Can't keep up".
#
# soak-v2 (台账 P39): the old gate only counted "[... ERROR]" lines, so a summon
# that fails *during entity construction* (EntityType throws inside create()) is
# logged at WARN and was silently ignored — 18 consecutive 妖精 summon failures
# (I11) were reported as "roster summoned" with 0 errors.
# soak-v2 therefore:
#   [a] counts `Failed to summon` / `Unable to summon` / `IndexOutOfBounds` /
#       `Exception` lines in the fresh log and makes them part of the FAIL verdict;
#   [b] appends a per-summon aliveness receipt (`execute as @e[type=X] run say
#       [soak] alive: X`) so the expected receipt count can be asserted — this
#       catches "command succeeded but entity died straight away" too;
#   [c] keeps the SmokeRunVmArgs-scoped process kill semantics unchanged.
# Usage: bash staging/soak-v2/soak_test.sh [minutes]   (default 30)
set -u
# Self-locating: works both from porting/tools/ (canonical) and from
# porting/staging/soak-v2/ (delivery location) by walking up to the repo root.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
while [ "$ROOT_DIR" != "/" ] && [ ! -f "$ROOT_DIR/porting/tools/smoke_test.sh" ]; do
    ROOT_DIR="$(dirname "$ROOT_DIR")"
done
if [ ! -f "$ROOT_DIR/porting/tools/smoke_test.sh" ]; then
    echo "FAIL: repo root not found above $SCRIPT_DIR"; exit 1
fi
cd "$ROOT_DIR"
PROJ="porting/neoforge-1.21.1"
export JAVA_HOME="/c/Program Files/Java/zulu-21"
SOAK_MINUTES="${1:-30}"
# benign noise that must not fail the gate (kept identical to smoke_test.sh)
BENIGN="Yggdrasil\|sessionserver\|minecraftservices\|chat session\|Missing sound\|mcassetsroot\|Sampler2\|Failed to load properties from file"
# additionally benign for the Exception/IndexOutOfBounds counters (auth noise only)
BENIGN_EXC="api.minecraftservices.com\|PKIX path building failed\|SunCertPathBuilderException\|SSLHandshakeException\|com.mojang.authlib"
LOG="$PROJ/run-smoke/logs/latest.log"
DP="$PROJ/run-smoke/world/datapacks/thkaguya_soak"

# ---- soak roster: single source of truth so receipts and summons stay in sync ----
# (type, x, y, z) — 36 summons. The old script hard-coded 35 `summon` lines while
# the ledger/regression gate describes the pack as a "36 实体 roster"
# (integration-notes.md §回归闸门); the 36th entry below reconciles the two.
# EXPECTED_* are derived from the array, so the counts can never drift again.
ROSTER=(
  "thkaguyamod:th_fairy 4 90 4"
  "thkaguyamod:th_fairy -6 90 3"
  "thkaguyamod:th_fairy 8 90 -5"
  "thkaguyamod:th_fairy -3 90 -8"
  "thkaguyamod:th_fairy 12 90 0"
  "thkaguyamod:th_fairy 0 90 12"
  "thkaguyamod:th_fairy -12 90 -12"
  "thkaguyamod:th_fairy 6 90 9"
  "thkaguyamod:sunflower_fairy 5 90 -10"
  "thkaguyamod:sunflower_fairy -9 90 7"
  "thkaguyamod:sunflower_fairy 10 90 6"
  "thkaguyamod:sunflower_fairy -4 90 11"
  "thkaguyamod:th_phantom 7 90 2"
  "thkaguyamod:th_phantom -8 90 -4"
  "thkaguyamod:th_phantom 2 90 -11"
  "thkaguyamod:th_phantom -11 90 9"
  "thkaguyamod:familiar 3 90 7"
  "thkaguyamod:familiar -7 90 -9"
  "thkaguyamod:cirno 9 90 8"
  "thkaguyamod:cirno -10 90 -6"
  "thkaguyamod:rumia 6 90 -7"
  "thkaguyamod:rumia -5 90 10"
  "thkaguyamod:toziko 11 90 -3"
  "thkaguyamod:toziko -2 90 -10"
  "thkaguyamod:wriggle 1 90 9"
  "thkaguyamod:wriggle -12 90 1"
  "thkaguyamod:danmaku_creeper 8 90 11"
  "thkaguyamod:danmaku_creeper -6 90 -12"
  "thkaguyamod:danmaku_creeper 12 90 -8"
  "thkaguyamod:reimu 5 90 1"
  "thkaguyamod:miko -1 90 6"
  "thkaguyamod:sanae 3 90 -3"
  "thkaguyamod:sakuya -4 90 -2"
  "thkaguyamod:rinnosuke 7 90 -1"
  "thkaguyamod:cursed_decoy_doll 2 90 3"
  "thkaguyamod:cirno -14 90 14"
)
EXPECTED_SUMMONS=${#ROSTER[@]}          # 36
EXPECTED_RECEIPTS=$EXPECTED_SUMMONS     # one aliveness receipt per summon

echo "== [0] soak datapack =="
mkdir -p "$DP/data/minecraft/tags/function" "$DP/data/thkaguya_soak/function"
cat > "$DP/pack.mcmeta" <<'EOF'
{
  "pack": {
    "pack_format": 48,
    "description": "THKaguyaMod soak-test entity spawner (dev only)"
  }
}
EOF
cat > "$DP/data/minecraft/tags/function/load.json" <<'EOF'
{
  "values": ["thkaguya_soak:boot"]
}
EOF

# boot.mcfunction: one summon + one immediate aliveness receipt per roster entry.
# The receipt is emitted only if the entity actually exists right after the
# summon, so a construction-time failure (e.g. EntityTHFairy.setPattern
# IndexOutOfBoundsException, I11) leaves the receipt missing instead of letting
# "roster summoned" look like success.
# The `unless` fallback keeps the receipt count at exactly one per summon: a
# failed summon (or a dead-on-arrival entity) yields an explicit dead: line,
# which the gate counts the same way but prints with a DEAD marker.
{
  echo "say [soak] boot: summoning soak entity roster (${EXPECTED_SUMMONS} entities)"
  for entry in "${ROSTER[@]}"; do
    set -- $entry
    type="$1"; x="$2"; y="$3"; z="$4"
    echo "summon $type $x $y $z"
    # `distance=..2` is measured from the selector's own x/y/z, and tolerates the
    # small position fix-up the game may apply on spawn (e.g. nudging a mob out of
    # a block); roster positions are >=3 blocks apart, so limit=1 can never latch
    # onto a neighbour.
    echo "execute as @e[type=$type,x=$x,y=$y,z=$z,distance=..2,limit=1] run say [soak] alive: $type"
    echo "execute unless entity @e[type=$type,x=$x,y=$y,z=$z,distance=..2] run say [soak] dead: $type"
  done
  echo "say [soak] roster summoned"
} > "$DP/data/thkaguya_soak/function/boot.mcfunction"
echo "datapack ready: $DP (${EXPECTED_SUMMONS} summons / ${EXPECTED_RECEIPTS} receipts)"

echo "== [1] launch dedicated server =="
rm -f "$LOG"
mkdir -p "$PROJ/run-smoke"
echo "eula=true" > "$PROJ/run-smoke/eula.txt"
(cd "$PROJ" && ./gradlew runServerSmoke >/dev/null 2>&1) &
GPID=$!
T0=$(date +%s)
BOOT_OK=0
for i in $(seq 1 72); do
    if [ -f "$LOG" ] && grep -qE 'Done \(.*\)!' "$LOG" 2>/dev/null; then BOOT_OK=1; break; fi
    sleep 5
done
T1=$(date +%s)
if [ "$BOOT_OK" != "1" ]; then
    echo "FAIL: server did not reach Done within 6 minutes"
    powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'SmokeRunVmArgs' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
    exit 1
fi
echo "boot: $((T1-T0))s; soaking ${SOAK_MINUTES}m (until $(date -d "+${SOAK_MINUTES} minutes" +%T 2>/dev/null || date +%T))"

sleep $((SOAK_MINUTES * 60))

echo "== [2] stop =="
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'SmokeRunVmArgs' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { \$_.CommandLine -match 'gradlew(.bat)?\"? runServerSmoke' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force -ErrorAction SilentlyContinue }" 2>/dev/null
kill $GPID 2>/dev/null
sleep 3

echo "== [3] result =="
ERR=999; WARN=0; OOM=0
SUMMON_FAIL=999; EXC=999; IOOB=999; RECEIPTS=0; DEAD=0; ALIVE=0; SUMMONED=0; RECEIPT_CMDS=0
if [ -f "$LOG" ]; then
    ERR=$(grep -E "\[.*ERROR\]" "$LOG" | grep -vc "$BENIGN" || true)
    WARN=$(grep -c "Can't keep up" "$LOG" || true)
    OOM=$(grep -ci "OutOfMemory" "$LOG" || true)
    # [a] summon-specific / exception counters (P39 的核心：这些行不走 [ERROR] 级)
    SUMMON_FAIL=$(grep -cE "Failed to summon|Unable to summon" "$LOG" || true)
    # 只数「异常头部」行：栈帧 "at ...Exception..." / "Caused by:" 不重复计数
    EXC=$(grep -E "Exception" "$LOG" | grep -v "$BENIGN_EXC" | grep -vc "^[[:space:]]*at " || true)
    IOOB=$(grep -c "IndexOutOfBounds" "$LOG" || true)
    # [b] per-summon aliveness receipts
    ALIVE=$(grep -c "\[soak\] alive:" "$LOG" || true)
    DEAD=$(grep -c "\[soak\] dead:" "$LOG" || true)
    SUMMONED=$(grep -c "\[soak\] roster summoned" "$LOG" || true)
    RECEIPT_CMDS=$(grep -cE "\[soak\] (alive|dead): " "$LOG" || true)
fi
echo "relevant_errors=$ERR cant_keep_up=$WARN oom=$OOM"
echo "summon_failures=$SUMMON_FAIL exceptions=$EXC index_out_of_bounds=$IOOB"
echo "receipts=$RECEIPT_CMDS/$EXPECTED_RECEIPTS (alive=$ALIVE dead=$DEAD) roster_summons=$SUMMONED/$EXPECTED_SUMMONS"
if [ -f "$LOG" ]; then
    grep -E "\[.*ERROR\]" "$LOG" | grep -v "$BENIGN" | head -10
    echo "-- exception lines (non-benign) --"
    grep -E "Exception" "$LOG" | grep -v "$BENIGN_EXC" | grep -v "^[[:space:]]*at " | head -8
    echo "-- non-alive receipts (failed summon / dead-on-arrival) --"
    grep -E "\[soak\] dead: " "$LOG" | head -20
    echo "-- missing receipts (no alive:/dead: line for a summoned type) --"
    for entry in "${ROSTER[@]}"; do
        set -- $entry
        type="$1"; x="$2"; y="$3"; z="$4"
        if ! grep -qE "\[soak\] (alive|dead): $type" "$LOG"; then echo "MISSING RECEIPT: $type @$x $y $z"; fi
    done
    echo "-- last 5 log lines --"
    tail -5 "$LOG"
fi
NEW_CRASHES=$(find "$PROJ/run-smoke/crash-reports" -name "crash-*server*.txt" -newermt "@$T0" 2>/dev/null | head -5)
if [ -n "$NEW_CRASHES" ]; then
    echo "CRASH REPORTS:"; echo "$NEW_CRASHES"
fi
if [ "$ERR" = "0" ] && [ "$OOM" = "0" ] && [ -z "$NEW_CRASHES" ] \
   && [ "$SUMMON_FAIL" = "0" ] && [ "$IOOB" = "0" ] && [ "$EXC" = "0" ] \
   && [ "$RECEIPT_CMDS" = "$EXPECTED_RECEIPTS" ] && [ "$DEAD" = "0" ] && [ "$SUMMONED" = "1" ]; then
    echo "SOAK PASS (${SOAK_MINUTES}m uptime)"
else
    echo "SOAK FAIL (errors=$ERR oom=$OOM summon_failures=$SUMMON_FAIL exceptions=$EXC index_out_of_bounds=$IOOB receipts=$RECEIPT_CMDS/$EXPECTED_RECEIPTS dead=$DEAD)"
    exit 1
fi