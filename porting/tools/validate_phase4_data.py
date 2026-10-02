import json, glob, os, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))          # repo root
STAGING = os.path.join(ROOT, 'porting', 'staging', 'data')
ART = os.path.join(ROOT, 'porting', 'neoforge-1.21.1', 'build', 'moddev', 'artifacts')
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
bm = sorted(glob.glob(os.path.join('thkaguyamod', 'biome_modifier', '*.json')))
ents = {}
for p in bm:
    d = docs[p]
    errs = []
    if d.get('type') != 'neoforge:add_spawns':
        errs.append('type')
    b = d.get('biomes')
    if not isinstance(b, list) or not b:
        errs.append('biomes(not non-empty list)')
    else:
        for x in b:
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
    for b in docs[p]['biomes']:
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
print("RESULT:", "ALL CHECKS PASS" if not bad and not unresolved else "PROBLEMS FOUND")
