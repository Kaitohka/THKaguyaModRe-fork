#!/usr/bin/env bash
# THKaguyaMod NeoForge port — regression smoke test (client + dedicated server).
# Usage: bash porting/tools/smoke_test.sh [--skip-build] [--client-only]
# R8 验收: 客户端与专用服务端均能启动; 无 common->client-only 引用; 无新增 ERROR.
set -u
cd "$(dirname "$0")/.."
PROJ="neoforge-1.21.1"
export JAVA_HOME="/c/Program Files/Java/zulu-21"
BENIGN="Yggdrasil\|sessionserver\|minecraftservices\|chat session\|Missing sound\|mcassetsroot\|Sampler2\|Failed to load properties from file"
FAILED=0

echo "== [0] R3 静态闸: common 代码不得引用 client-only 类 =="
# 只允许 thkaguyamod/client/** 引用 net.minecraft.client.*
VIOLATIONS=$(grep -rl "import net\.minecraft\.client" "$PROJ/src/main/java/thkaguyamod" --include="*.java" | grep -v "/client/" || true)
if [ -n "$VIOLATIONS" ]; then
    echo "R3 VIOLATION:"; echo "$VIOLATIONS"; FAILED=1
else
    echo "R3 OK"
fi

if [ "${1:-}" != "--skip-build" ]; then
    echo "== [1] build =="
    (cd "$PROJ" && ./gradlew build -q) || { echo "FAIL: build"; exit 1; }
    echo "build OK"
fi

run_side() {  # $1=client|server
    local SIDE="$1" LOG="$PROJ/run-smoke/logs/latest.log" MARKER GRADLE_TASK
    if [ "$SIDE" = "client" ]; then
        GRADLE_TASK="runClientSmoke"; MARKER="textures/atlas/gui.png"
    else
        GRADLE_TASK="runServerSmoke"; MARKER='Done (.*)!'
        mkdir -p "$PROJ/run-smoke"; echo "eula=true" > "$PROJ/run-smoke/eula.txt"
    fi
    rm -f "$PROJ/run-smoke/logs/latest.log" 2>/dev/null
    (cd "$PROJ" && ./gradlew "$GRADLE_TASK" -q >/dev/null 2>&1) &
    local LAUNCH_PID=$!
    local OK=0
    for i in $(seq 1 60); do
        if [ -f "$LOG" ] && grep -qE "$MARKER" "$LOG" 2>/dev/null; then OK=1; break; fi
        sleep 5
    done
    sleep 8
    if [ ! -f "$LOG" ]; then OK=0; fi
    local ERR=999 RECIPE=999 MODEL=999 MODOK=0
    if [ -f "$LOG" ]; then
        ERR=$(grep -E "\[.*ERROR\]" "$LOG" | grep -vc "$BENIGN" || true)
        RECIPE=$(grep -c "Parsing error loading recipe" "$LOG" || true)
        MODEL=$(grep -c "Unable to load model" "$LOG" || true)
        MODOK=$(grep -c "Itutu no Nandai MOD+ (NeoForge port) loading" "$LOG" || true)
    fi
    echo "[$SIDE] started=$OK relevant_errors=$ERR recipe_errors=$RECIPE model_errors=$MODEL mod_loaded=$MODOK"
    if [ -f "$LOG" ]; then
        grep -E "\[.*ERROR\]" "$LOG" | grep -v "$BENIGN" | head -8
    fi
    powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'SmokeRunVmArgs' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
    powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { \$_.CommandLine -match 'gradlew(.bat)?\"? run(Client|Server)Smoke' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force -ErrorAction SilentlyContinue }" 2>/dev/null
    kill $LAUNCH_PID 2>/dev/null
    sleep 3
    if [ "$OK" != "1" ] || [ "$ERR" != "0" ] || [ "$RECIPE" != "0" ] || [ "$MODEL" != "0" ] || [ "$MODOK" != "1" ]; then
        echo "[$SIDE] FAIL"; FAILED=1
    else
        echo "[$SIDE] PASS"
    fi
}

echo "== [2] client =="
run_side client

if [ "${1:-}" != "--client-only" ] && [ "${2:-}" != "--client-only" ]; then
    echo "== [3] dedicated server =="
    run_side server
fi

if [ "$FAILED" = "0" ]; then echo "SMOKE PASS"; else echo "SMOKE FAIL"; exit 1; fi
