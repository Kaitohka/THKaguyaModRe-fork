import re, glob, os, sys

SRC = r"D:/AI构建插件源代码/THKaguyaModRe-fork-master/src/main/java/thKaguyaMod/client/model/living"

for path in sorted(glob.glob(os.path.join(SRC, "*.java"))):
    src = open(path, encoding="utf-8").read()
    lines = src.split("\n")
    events = {}
    for i, ln in enumerate(lines):
        m = re.search(r'(\w+)\.(addBox|mirror\s*=|setTextureSize|setRotationPoint)\b', ln)
        if m:
            events.setdefault(m.group(1), []).append((i + 1, m.group(2).strip()))
    bad = []
    for var, evs in events.items():
        adds = [e for e in evs if e[1] == 'addBox']
        mirs = [e for e in evs if e[1].startswith('mirror')]
        for ml in mirs:
            if not any(a[0] < ml[0] for a in adds):
                bad.append((var, ml))
    print(os.path.basename(path), "| mirror-before-addBox:", bad if bad else "none")