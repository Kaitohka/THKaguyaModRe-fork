#!/usr/bin/env bash
# compile_staging.sh — 暂存区（staging）编译校验
#
# 把 porting/neoforge-1.21.1/src 与指定暂存子目录合并到仓库内临时目录，用 javac 对
# 已构建的 neoforge merged jar + Gradle legacy classpath 编译。
# 不修改主源码树、不运行 gradlew，结束后删除临时目录。
#
# 用法: bash porting/tools/compile_staging.sh [staging-subdir]   # 默认 entities2
#
# 依赖尚未落地（并行移植中）的文件列在 porting/tools/staging_blocked.txt，会被跳过，
# 以便先验证其余类；清单清空后即为完整编译。
set -u

ROOT="D:/AI构建插件源代码/THKaguyaModRe-fork-master"
NF="$ROOT/porting/neoforge-1.21.1"
SUB="${1:-entities2}"
WORK="$ROOT/porting/.staging-build"
BLOCKED="$ROOT/porting/tools/staging_blocked.txt"

rm -rf "$WORK"
mkdir -p "$WORK/src" "$WORK/staging" "$WORK/out"

cp -r "$NF/src/main/java/." "$WORK/src/"
cp -r "$ROOT/porting/staging/$SUB/." "$WORK/staging/"

find "$WORK/src" -name '*.java' > "$WORK/files.txt"
find "$WORK/staging" -name '*.java' >> "$WORK/files.txt"

if [ -f "$BLOCKED" ]; then
  while IFS= read -r line; do
    case "$line" in
      ''|'#'*) continue ;;
    esac
    rel=$(echo "$line" | sed 's/#.*//' | tr -d '[:space:]')
    grep -v -F "$rel" "$WORK/files.txt" > "$WORK/files.tmp" && mv "$WORK/files.tmp" "$WORK/files.txt"
  done < "$BLOCKED"
fi

MERGED="$NF/build/moddev/artifacts/neoforge-21.1.252-merged.jar"
LEGACY=$(tr -d '\r\n' < "$NF/build/moddev/clientLegacyClasspath.txt")

javac -nowarn -proc:none -encoding UTF-8 \
  -cp "$MERGED;$LEGACY" \
  -d "$WORK/out" @"$WORK/files.txt" 2>&1
rc=$?

echo "--- javac exit: $rc   compiled: $(wc -l < "$WORK/files.txt") files"
rm -rf "$WORK"
exit $rc