import json, glob, os, subprocess, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))          # repo root
STAGING = os.path.join(ROOT, 'porting', 'staging', 'data')
ART = os.path.join(ROOT, 'porting', 'neoforge-1.21.1', 'build', 'moddev', 'artifacts')
# biome modifier 是 NeoForge 数据包注册表：目录必须带 neoforge/ 一层，
# 否则 RegistryDataLoader 静默读不到（不报错），自然生成整体失效。
BM_DIR = os.path.join('thkaguyamod', 'neoforge', 'biome_modifier')
os.chdir(STAGING)

files = sorted(glob.glob('**/*.json', recursive=True))
print("=== 1) JSON syntax (%d files) ===" % len(files))
bad = []
docs = {}
for p in files:
    try:
        docs[p] = json.load(open(p, encoding='utf-8'))
        print("  OK   " + p.replace(os.sep, '/'))
    except Exception as e:
        bad.append((p, e))
        print("  FAIL " + p.replace(os.sep, '/') + " -> %s" % e)

print()
print("=== 2) biome_modifier structure (neoforge:add_spawns) ===")
# 路径闸门：旧路径（无 neoforge/ 层）一旦重新出现，必须立刻报错而不是静默通过。
legacy = sorted(glob.glob(os.path.join('thkaguyamod', 'biome_modifier', '*.json')))
if legacy:
    print("  FAIL 检测到旧路径 data/thkaguyamod/biome_modifier/（缺 neoforge/ 层）：")
    for p in legacy:
        print("        " + p.replace(os.sep, '/'))
else:
    print("  OK   路径带 neoforge/ 层，未发现旧路径残留")

bm = sorted(glob.glob(os.path.join(BM_DIR, '*.json')))
if not bm:
    print("  FAIL %s 下没有任何 JSON" % BM_DIR.replace(os.sep, '/'))
ents = {}
for p in bm:
    d = docs[p]
    errs = []
    if d.get('type') != 'neoforge:add_spawns':
        errs.append('type')
    b = d.get('biomes')
    # 必须是 HolderSet 对象形式：{"type": "...", "values": [...]}
    # （字符串数组 ["#tagA","#tagB"] 不被 HolderSetCodec 接受；单个字符串虽合法但这里统一要求对象）
    if not isinstance(b, dict):
        errs.append('biomes(not holderset object; 字符串数组不被 HolderSetCodec 接受)')
    else:
        if not isinstance(b.get('type'), str) or ':' not in b.get('type', ''):
            errs.append('biomes.type')
        vals = b.get('values')
        if not isinstance(vals, list) or not vals:
            errs.append('biomes.values(not non-empty list)')
        else:
            for x in vals:
                if not (isinstance(x, str) and ':' in x):
                    errs.append('biome-entry:%r' % x)
    s = d.get('spawners')
    if not isinstance(s, dict):
        errs.append('spawners(not object)')
    else:
        for k in ('type', 'weight', 'minCount', 'maxCount'):
            if k not in s:
                errs.append('spawners.%s missing' % k)
        t = s.get('type')
        if isinstance(t, str):
            ns = t.split(':')[0]
            if ns != 'thkaguyamod':
                errs.append('namespace=%s' % ns)
            ents[t] = ents.get(t, 0) + 1
        if isinstance(s.get('weight'), int) and s['weight'] < 0:
            errs.append('weight<0')
        if isinstance(s.get('minCount'), int) and s['minCount'] < 1:
            errs.append('minCount<1')
        if isinstance(s.get('minCount'), int) and isinstance(s.get('maxCount'), int) \
                and s['minCount'] > s['maxCount']:
            errs.append('minCount>maxCount')
    print("  %-22s %s" % (os.path.basename(p), 'OK' if not errs else 'FAIL ' + '; '.join(errs)))

print()
print("=== 3) loot files ===")
glm = docs[os.path.join('neoforge', 'loot_modifiers', 'global_loot_modifiers.json')]
print("  global_loot_modifiers: replace=%r entries=%r" % (glm.get('replace'), glm.get('entries')))
lm = docs[os.path.join('thkaguyamod', 'loot_modifiers', 'miracle_mallet_in_dungeon.json')]
print("  loot_modifier: type=%r table=%r" % (lm.get('type'), lm.get('table')))
print("  loot_modifier.conditions=%r" % (lm.get('conditions'),))
lt = docs[os.path.join('thkaguyamod', 'loot_table', 'miracle_mallet_dungeon_loot.json')]
print("  subtable: type=%r items=%r" % (
    lt.get('type'), [e.get('name') for pool in lt['pools'] for e in pool['entries']]))

print()
print("=== 4) entity coverage ===")
for k in sorted(ents):
    print("  %-30s %d modifier(s)" % (k, ents[k]))

print()
print("=== 5) biome tag/id resolution against NeoForge 21.1.252 ===")
MC_JAR = os.path.join(ART, 'neoforge-21.1.252-client-extra-aka-minecraft-resources.jar')
NF_JAR = os.path.join(ART, 'neoforge-21.1.252.jar')
mc = zipfile.ZipFile(MC_JAR)
nf = zipfile.ZipFile(NF_JAR)
mcnames = set(mc.namelist())
nfnames = set(nf.namelist())
unresolved = []
for p in bm:
    for b in docs[p]['biomes']['values']:
        if b.startswith('#'):
            ns, path = b[1:].split(':', 1)
            if ns == 'minecraft':
                f = 'data/minecraft/tags/worldgen/biome/%s.json' % path
            elif ns == 'c':
                f = 'data/c/tags/worldgen/biome/%s.json' % path
            else:
                unresolved.append((os.path.basename(p), b, 'unknown namespace'))
                continue
            pool = mcnames if ns == 'minecraft' else nfnames
            if f not in pool:
                unresolved.append((os.path.basename(p), b, 'tag missing'))
        else:
            ns, path = b.split(':', 1)
            f = 'data/%s/worldgen/biome/%s.json' % (ns, path)
            if f not in mcnames:
                unresolved.append((os.path.basename(p), b, 'biome missing'))
print("  unresolved: %s" % (unresolved if unresolved else 'none'))

print()
print("=== 6) 生成器 ↔ 主树 逐字节一致性（单一事实源自检） ===")
gen = os.path.join(HERE, 'build_phase4_data.py')
try:
    r = subprocess.run([sys.executable, gen, '--verify'], cwd=ROOT,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    tail = r.stdout.decode('utf-8', 'replace').strip().splitlines()[-3:]
    for line in tail:
        print("  " + line)
    gen_ok = (r.returncode == 0)
except Exception as e:                                  # noqa: BLE001
    print("  FAIL 无法运行生成器自检: %s" % e)
    gen_ok = False

print()
print("RESULT:", "ALL CHECKS PASS" if not bad and not unresolved and not legacy and gen_ok
      else "PROBLEMS FOUND")
