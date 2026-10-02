#!/usr/bin/env bash
# compile_staging.sh — 暂存区（staging）编译校验
#
# 把 porting/neoforge-1.21.1/src 与 porting/staging/entities2 合并到临时目录，
# 用 javac 对已构建的 neoforge merged jar + Gradle legacy classpath 编译。
# 不修改主源码树、不运行 gradlew。
#
# 用法: bash porting/tools/compile_staging.sh [--staging-subdir entities2]
set -u

ROOT="D:/AI构建插件源代码/THKaguyaModRe-fork-master"
NF="$ROOT/porting/neoforge-1.21.1"
SUB="${1:-entities2}"
WORK="${TMPDIR:-/tmp}/thk_staging_build"

rm -rf "$WORK"
mkdir -p "$WORK/src" "$WORK/out"

cp -r "$NF/src/main/java/." "$WORK/src/"
mkdir -p "$WORK/staging"
cp -r "$ROOT/porting/staging/$SUB/." "$WORK/staging/"

# 只编译主源码树 + 指定暂存子目录
find "$WORK/src" -name '*.java' > "$WORK/files.txt"
find "$WORK/staging" -name '*.java' >> "$WORK/files.txt"

MERGED="$NF/build/moddev/artifacts/neoforge-21.1.252-merged.jar"
LEGACY=$(tr -d '\r\n' < "$NF/build/moddev/clientLegacyClasspath.txt")

javac -nowarn -proc:none -encoding UTF-8 \
  -cp "$MERGED;$LEGACY" \
  -d "$WORK/out" @"$WORK/files.txt" 2>&1

echo "--- exit: $?"
echo "--- workdir: $WORK"