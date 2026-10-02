"""Verification: parse the staged 1.21.1 files and the 1.7.10 originals, then compare
per-part texOffs / addBox geometry / rotation points / hierarchy. Throwaway check."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _gen_geometry as G

NEW = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "thkaguyamod", "client", "model", "living")

OLD_PARSE = G.parse


def parse_new(path):
    """Parse a generated file: texOffs/texOffs + addBox + PartPose + addOrReplaceChild."""
    src = G.strip_comments(open(path, encoding='utf-8').read())
    parts = {}        # name -> dict
    order = []
    links = []
    cur = [None]
    decl = re.compile(r'\bPartDefinition\s+(\w+)\s*=\s*(\w+)\.addOrReplaceChild\("(\w+)",')
    tex = re.compile(r'texOffs\((\d+),\s*(\d+)\)(\.mirror\(\))?')
    box = re.compile(r'\.addBox\(\s*([^)]*)\)')
    pose = re.compile(r'PartPose\.offsetAndRotation\(([^;]*)\)')
    for ln in src.split('\n'):
        m = decl.search(ln)
        if m:
            cur[0] = m.group(3)
            parts[cur[0]] = dict(name=cur[0], holder=m.group(2), tex=None, box=None, pose=None)
            order.append(cur[0])
            continue
        if cur[0] is None:
            continue
        m = tex.search(ln)
        if m and parts[cur[0]]['tex'] is None:
            parts[cur[0]]['tex'] = (int(m.group(1)), int(m.group(2)))
        m = box.search(ln)
        if m and parts[cur[0]]['box'] is None:
            parts[cur[0]]['box'] = m.group(1)
        m = pose.search(ln)
        if m and parts[cur[0]]['pose'] is None:
            parts[cur[0]]['pose'] = m.group(1)
        if 'addOrReplaceChild' in ln and decl.search(ln) is None:
            pass
    for n, p in parts.items():
        if n != 'root' and p['holder'] != 'root':
            links.append((p['holder'], n))
    return dict(parts=parts, order=order, links=links)


def norm_float(s):
    vals = []
    for tok in s.split(','):
        tok = tok.strip().rstrip(')').strip()
        tok = re.sub(r'[FfDd]$', '', tok)
        if tok.startswith('+'):
            tok = tok[1:]
        if re.fullmatch(r'-?\d+', tok):
            tok = tok + '.0'
        vals.append(tok)
    return vals


def old_geometry(path):
    """Re-derive from the 1.7.10 file: (name, tex, box[6 floats], pose 6 floats, holder)."""
    info = OLD_PARSE(path)
    # ModelWriggle inherits defaultAddChildSetting() from ModelTouhouDefault, which wires
    # body/head/limbs/braids; the subclass only adds its own extras. Fold the inherited
    # links in so the comparison sees the real 1.7.10 hierarchy.
    if os.path.basename(path) == "ModelWriggle.java":
        info['links'] = [("bipedBody", "bipedHead"), ("bipedBody", "bipedRightArm"),
                         ("bipedBody", "bipedLeftArm"), ("bipedBody", "bipedRightLeg"),
                         ("bipedBody", "bipedLeftLeg"), ("bipedHead", "rightBraid"),
                         ("bipedHead", "leftBraid"), ("bipedHead", "longHair")] + info['links']
    parent = {}
    for par, ch in info['links']:
        parent[ch] = par
    declared = {p['name'] for p in info['parts']}
    out = {}
    for p in info['parts']:
        if p['box'] is None:
            continue
        args = norm_float(p['box'])
        boxs = args[:3] + args[3:6]
        pose = p['pose'] or ('0, 0, 0', '0, 0, 0')
        off = norm_float(pose[0] or '0, 0, 0')
        rot = norm_float(pose[1] or '0, 0, 0')
        holder = parent.get(p['name'])
        if holder not in declared:
            holder = 'root'
        out[p['name']] = dict(tex=p['tex'], box=boxs, pose=off + rot, holder=holder,
                              mirror=p['mirror'])
    return out


def new_geometry(path):
    info = parse_new(path)
    out = {}
    for name, p in info['parts'].items():
        if name == 'root' or p['box'] is None:
            continue
        args = norm_float(p['box'])
        boxs = args[:3] + args[3:6]
        pose = norm_float(p['pose'] or '0,0,0,0,0,0')
        out[name] = dict(tex=p['tex'], box=boxs, pose=pose, holder=p['holder'])
    return out


def num_eq(a, b):
    try:
        return abs(float(a.replace('(float)Math.PI', '3.1415927')) - float(b.replace('(float)Math.PI', '3.1415927'))) < 1e-5
    except ValueError:
        return a.strip() == b.strip()


bad = 0
for fn in sorted(os.listdir(G.SRC)):
    if not fn.endswith('.java'):
        continue
    name = fn[:-5]
    newpath = os.path.join(NEW, fn)
    if not os.path.exists(newpath):
        print("MISSING STAGED FILE:", fn)
        bad += 1
        continue
    old = old_geometry(os.path.join(G.SRC, fn))
    new = new_geometry(newpath)
    problems = []
    if set(old) != set(new):
        problems.append("part set differs: only-old=%s only-new=%s" %
                        (sorted(set(old) - set(new)), sorted(set(new) - set(old))))
    for k in sorted(set(old) & set(new)):
        o, n = old[k], new[k]
        if o['tex'] != n['tex']:
            problems.append("%s texOffs %s != %s" % (k, n['tex'], o['tex']))
        for i, (a, b) in enumerate(zip(o['box'], n['box'])):
            if not num_eq(a, b):
                problems.append("%s box[%d] %s != %s" % (k, i, b, a))
        for i, (a, b) in enumerate(zip(o['pose'], n['pose'])):
            if not num_eq(a, b):
                problems.append("%s pose[%d] %s != %s" % (k, i, b, a))
        if o['holder'] != n['holder']:
            problems.append("%s holder %s != %s" % (k, n['holder'], o['holder']))
    print("%-28s parts old=%2d new=%2d  %s" % (fn, len(old), len(new), "OK" if not problems else "PROBLEMS"))
    for p in problems:
        print("    -", p)
    bad += len(problems)

print()
print("total problems:", bad)