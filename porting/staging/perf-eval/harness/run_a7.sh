#!/usr/bin/env bash
# A7 (P29) fixed-buffer flush ベンチのビルド & 実行。
# 主樹は一切変更しない（javac の出力先は staging/perf-eval/out）。
#
#   用法: bash porting/staging/perf-eval/harness/run_a7.sh
set -u

ROOT="D:/AI构建插件源代码/THKaguyaModRe-fork-master"
NF="$ROOT/porting/neoforge-1.21.1"
SRC="$ROOT/porting/staging/perf-eval/harness"
OUT="$ROOT/porting/staging/perf-eval/out"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Java/zulu-21}"

MERGED=$(cygpath -w "$NF/build/moddev/artifacts/neoforge-21.1.252-merged.jar")
# 専用サーバ用 legacy classpath を使う（clientLegacyClasspath.txt は prepareClientRun 後にしか生成されない）
LEGACY=$(tr ';' '\n' < "$NF/build/moddev/gameTestServerLegacyClasspath.txt" | tr -d '\r' \
         | while IFS= read -r p; do [ -n "$p" ] && cygpath -w "$p"; done | paste -sd';' -)
CLS=$(cygpath -w "$NF/build/classes/java/main")   # 主樹のコンパイル済みクラス（読み取りのみ）
# lwjgl（MemoryUtil）は gameTestServerLegacyClasspath には入っていないので明示的に足す
LWJGL=$(cygpath -w "$HOME/.gradle/caches/modules-2/files-2.1/org.lwjgl/lwjgl/3.3.3/29589b5f87ed335a6c7e7ee6a5775f81f97ecb84/lwjgl-3.3.3.jar")
LWJGL_NAT=$(cygpath -w "$HOME/.gradle/caches/modules-2/files-2.1/org.lwjgl/lwjgl/3.3.3/a5ed18a2b82fc91b81f40d717cb1f64c9dcb0540/lwjgl-3.3.3-natives-windows.jar")
CP="$OUT;$MERGED;$LEGACY;$CLS;$LWJGL;$LWJGL_NAT"

rm -rf "$OUT"; mkdir -p "$OUT"

echo "== javac =="
"$JAVA_HOME/bin/javac" -nowarn -proc:none -encoding UTF-8 \
  -cp "$MERGED;$LEGACY;$CLS;$LWJGL" -d "$OUT" "$SRC/A7BufferFlushBench.java" || exit 1

echo "== run =="
"$JAVA_HOME/bin/java" -cp "$CP" perf_eval.A7BufferFlushBench
