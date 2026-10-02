#!/usr/bin/env python3
# P19 enforcement: strip entity-side checkSpawnRules duplicates (ModSpawns predicates own the logic).
import io, os

base = 'porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/living/'
targets = ['EntityCirno', 'EntityRumia', 'EntityRinnosuke', 'EntitySanae',
           'EntityTHFairy', 'EntityTHPhantom', 'EntityToziko', 'EntityWriggle']

STUB = """	public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		//P19: 自然生成の全条件（config・確率・地形・明るさ）は ModSpawns の SpawnPlacement 謂詞に一元化。
		//ここで再判定すると確率が二重に掛かる（妖精 50%→25% 等）ため、実体側は常に true。
		return true;
	}"""

for name in targets:
    p = base + name + '.java'
    s = io.open(p, encoding='utf-8').read()
    idx = s.find('public boolean checkSpawnRules')
    if idx < 0:
        print(name, 'no override, skip')
        continue
    # find the opening brace of the method
    brace = s.index('{', idx)
    depth = 0
    i = brace
    while i < len(s):
        c = s[i]
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                break
        i += 1
    old_block = s[idx:i + 1]
    s = s[:idx] + STUB + s[i + 1:]
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print(name, 'stripped', len(old_block), 'chars')
print('done')
