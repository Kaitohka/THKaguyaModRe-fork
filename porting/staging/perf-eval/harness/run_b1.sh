#!/usr/bin/env bash
# B1（ホーミング追尾スロットル）ベンチのビルド & 実行。
# 主樹は一切変更しない（javac の出力先は staging/perf-eval/out）。
#
#   用法: bash porting/staging/perf-eval/harness/run_b1.sh
set -u

ROOT="D:/AI构建插件源代码/THKaguyaModRe-fork-master"
NF="$ROOT/porting/neoforge-1.21.1"
SRC="$ROOT/porting/staging/perf-eval/harness"
OUT="$ROOT/porting/staging/perf-eval/out"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Java/zulu-21}"

MERGED=$(cygpath -w "$NF/build/moddev/artifacts/neoforge-21.1.252-merged.jar")
LEGACY=$(tr ';' '\n' < "$NF/build/moddev/gameTestServerLegacyClasspath.txt" | tr -d '\r' \
         | while IFS= read -r p; do [ -n "$p" ] && cygpath -w "$p"; done | paste -sd';' -)
CLS=$(cygpath -w "$NF/build/classes/java/main")   # 主樹のコンパイル済みクラス（読み取りのみ）

mkdir -p "$OUT"

echo "== javac =="
"$JAVA_HOME/bin/javac" -nowarn -proc:none -encoding UTF-8 \
  -cp "$MERGED;$LEGACY;$CLS" -d "$OUT" "$SRC/B1HomingScanBench.java" || exit 1

echo "== run =="
"$JAVA_HOME/bin/java" -cp "$OUT;$MERGED;$LEGACY;$CLS" perf_eval.B1HomingScanBench
