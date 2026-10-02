import re, glob, os

SRC = r"D:/AI构建插件源代码/THKaguyaModRe-fork-master/src/main/java/thKaguyaMod/client/model/living"

decl = re.compile(r'^\s*(?:this\.)?textureWidth\s*=\s*(\d+)\s*;')
declh = re.compile(r'^\s*(?:this\.)?textureHeight\s*=\s*(\d+)\s*;')

for path in sorted(glob.glob(os.path.join(SRC, "*.java"))):
    name = os.path.basename(path)
    lines = open(path, encoding="utf-8").read().split("\n")
    tw = th = None
    events = {}     # var -> ordered event list
    children = {}   # parent -> [child]
    parented = set()
    for i, ln in enumerate(lines):
        s = ln.strip()
        if s.startswith("//"):
            continue
        m = decl.search(ln)
        if m and "getTextureWidth" not in ln:
            tw = m.group(1)
        m = declh.search(ln)
        if m:
            th = m.group(1)
        m = re.search(r'(\w+)\.(addBox|mirror\s*=\s*(true|false)|setTextureSize|setRotationPoint)\s*\(', ln)
        if m:
            events.setdefault(m.group(1), []).append((i + 1, s, m.group(2)))
        m = re.search(r'(\w+)\.mirror\s*=\s*(true|false)', ln)
        if m:
            events.setdefault(m.group(1), []).append((i + 1, s, "mirror=" + m.group(2)))
        m = re.search(r'(\w+)\.addChild\s*\(\s*(?:this\.)?(\w+)\s*\)', ln)
        if m:
            children.setdefault(m.group(1), []).append(m.group(2))
            parented.add(m.group(2))
    print("=" * 70)
    print(name, "declared tex", tw, "x", th)
    for var, evs in events.items():
        tss = [e for e in evs if e[2] == "setTextureSize"]
        sizes = set(re.search(r'\(([^)]*)\)', e[1]).group(1) for e in tss)
        order = []
        for e in evs:
            if e[2] == "addBox":
                order.append("BOX@%d" % e[0])
            elif e[2].startswith("mirror"):
                order.append("%s@%d" % (e[2], e[0]))
        if tss and sizes != {"%s, %s" % (tw, th)}:
            print("   TEXSIZE-MISMATCH", var, sizes)
        # mirror occurring before a later addBox => effective
        boxes = [e[0] for e in evs if e[2] == "addBox"]
        for e in evs:
            if e[2].startswith("mirror") and any(b > e[0] for b in boxes):
                print("   EFFECTIVE MIRROR", var, e)
    allvars = set(events)
    roots = sorted(v for v in allvars if v not in parented and re.search(r'\b\w+\s*=', "") is None)
    print("   tree roots(no parent):", sorted(v for v in children.keys() | allvars if v not in parented))
    print("   declared but unused-ish:", sorted(v for v in allvars if v not in children and v not in parented))