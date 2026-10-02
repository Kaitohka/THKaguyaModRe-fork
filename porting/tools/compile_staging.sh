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

# 暫定エンティティ登録（ModEntities への追記分）を「一時コピーにだけ」挿入する。
# 主ソースツリーは変更しない。挿入内容は porting/tools/staging_modentities_extra.txt。
EXTRA="$ROOT/porting/tools/staging_modentities_extra.txt"
if [ -f "$EXTRA" ]; then
  python - "$WORK/src/thkaguyamod/init/ModEntities.java" "$EXTRA" <<'PY'
import io, sys
target, extra = sys.argv[1], sys.argv[2]
src = io.open(target, encoding='utf-8').read()
add = io.open(extra, encoding='utf-8').read()
add = add.split('----\n', 1)[1].split('// ---- ここまで', 1)[0]
marker = '    public static void register(IEventBus modEventBus) {'
assert marker in src, 'ModEntities marker not found'
src = src.replace(marker, add.strip('\n') + '\n\n' + marker, 1)
io.open(target, 'w', encoding='utf-8').write(src)
PY
fi

# 注意: clientLegacyClasspath.txt は Windows 絶対パス（C:\...）の ';' 区切り。
# Git Bash から Windows 版 javac に渡すため、各要素を cygpath -w で Windows 形式に正規化し
# ';' 区切りのまま渡す（':' 区切りにすると javac が /c/... を相対パスと解釈する）。
MERGED=$(cygpath -w "$NF/build/moddev/artifacts/neoforge-21.1.252-merged.jar")
LEGACY=$(tr ';' '\n' < "$NF/build/moddev/clientLegacyClasspath.txt" | tr -d '\r' \
         | while IFS= read -r p; do [ -n "$p" ] && cygpath -w "$p"; done | paste -sd';' -)

javac -nowarn -proc:none -encoding UTF-8 \
  -cp "$MERGED;$LEGACY" \
  -d "$WORK/out" @"$WORK/files.txt" 2>&1
rc=$?

echo "--- javac exit: $rc   compiled: $(wc -l < "$WORK/files.txt") files"
rm -rf "$WORK"
exit $rc