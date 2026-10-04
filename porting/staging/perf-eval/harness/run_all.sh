#!/usr/bin/env bash
# perf-eval の全ベンチをまとめて実行する。
#   用法: bash porting/staging/perf-eval/harness/run_all.sh
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$(cd "$HERE/.." && pwd)/out"
LOGS="$(cd "$HERE/.." && pwd)/logs"
mkdir -p "$OUT" "$LOGS"

for b in a7 b1; do
  echo "########## $b ##########"
  bash "$HERE/run_$b.sh" 2>&1 | tee "$LOGS/$b.log"
  echo
done
echo "logs -> $LOGS"
