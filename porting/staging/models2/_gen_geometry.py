"""Throwaway helper: extract part geometry from the 1.7.10 living models and emit
modern (1.21.1) createBodyLayer() snippets. Not part of the build.

Usage:  python _gen_geometry.py            -> prints snippets for every model
"""
import re
import os
import sys

SRC = r"D:/AI构建插件源代码/THKaguyaModRe-fork-master/src/main/java/thKaguyaMod/client/model/living"

PART = re.compile(r'^\s*(\w+)\s*=\s*new ModelRenderer\(this,\s*(\d+)\s*,\s*(\d+)\)\s*;')
ADDBOX = re.compile(r'^\s*(\w+)\.addBox\(([^;]*)\)\s*;')
ROTPT = re.compile(r'^\s*(\w+)\.setRotationPoint\(([^;]*)\)\s*;')
SETROT = re.compile(r'^\s*setRotation\((\w+),\s*([^;]*)\)\s*;')
ADCHILD = re.compile(r'^\s*(\w+)\.addChild\(this\.(\w+)\)\s*;|^\s*(\w+)\.addChild\((\w+)\)\s*;')
MIRROR = re.compile(r'^\s*(\w+)\.mirror\s*=\s*(true|false)\s*;')
TEXSIZE = re.compile(r'^\s*(\w+)\.setTextureSize\(([^;]*)\)\s*;')
TEXW = re.compile(r'^\s*(?:this\.)?textureWidth\s*=\s*(\d+)\s*;')
TEXH = re.compile(r'^\s*(?:this\.)?textureHeight\s*=\s*(\d+)\s*;')


def strip_comments(text):
    text = re.sub(r'/\*.*?\*/', lambda m: '\n' * m.group(0).count('\n'), text, flags=re.S)
    out = []
    for ln in text.split('\n'):
        out.append(ln.split('//')[0])
    return '\n'.join(out)


def fnum(lit):
    lit = lit.strip()
    lit = re.sub(r'[FfDd]$', '', lit)
    if lit.startswith('+'):
        lit = lit[1:]
    if re.fullmatch(r'-?\d+', lit):
        lit += '.0'
    return lit + 'F'


def parse(path, mirror_pre=None):
    raw = open(path, encoding='utf-8').read()
    src = strip_comments(raw)
    tw, th = None, None
    parts = []          # dicts: name, tex, pose, box, mirror_effective
    order = []
    links = []          # (parent, child)
    for ln in src.split('\n'):
        m = TEXW.match(ln)
        if m:
            tw = int(m.group(1))
        m = TEXH.match(ln)
        if m:
            th = int(m.group(1))
        m = PART.match(ln)
        if m:
            parts.append(dict(name=m.group(1), tex=(int(m.group(2)), int(m.group(3))),
                              box=None, pose=None, mirror=False))
            order.append(m.group(1))
            continue
        if not parts:
            continue
        cur = parts[-1]
        m = MIRROR.match(ln)
        if m and m.group(1) == cur['name']:
            if cur['box'] is None:
                cur['mirror'] = (m.group(2) == 'true')
            continue
        m = TEXSIZE.match(ln)
        if m:
            continue
        m = ADDBOX.match(ln)
        if m and m.group(1) == cur['name']:
            cur['box'] = m.group(2)
            continue
        m = ROTPT.match(ln)
        if m and m.group(1) == cur['name']:
            cur['pose'] = (m.group(2), None)
            continue
        m = SETROT.match(ln)
        if m and m.group(1) == cur['name']:
            if cur['pose'] is None:
                cur['pose'] = ('0F, 0F, 0F', m.group(2))
            else:
                cur['pose'] = (cur['pose'][0], m.group(2))
            continue
        m = ADCHILD.match(ln)
        if m:
            if m.group(1):
                links.append((m.group(1), m.group(2)))
            else:
                links.append((m.group(3), m.group(4)))
    for p in parts:
        # ModelRenderer.addBox() snapshots renderer.mirror into every ModelBox it
        # creates, so only a `mirror = true` that precedes the addBox call counts.
        p['mirror'] = bool(mirror_pre) and p['name'] in mirror_pre
    return dict(name=os.path.basename(path)[:-5], tex=(tw, th), parts=parts, order=order, links=links)


def emit(info):
    names = [p['name'] for p in info['parts']]
    parent = {}
    for par, ch in info['links']:
        parent[ch] = par
    byname = {p['name']: p for p in info['parts']}
    # topological emit order (stable w.r.t. declaration)
    emitted = set()
    out_order = []

    def visit(n):
        if n in emitted:
            return
        p = parent.get(n)
        if p and p in byname:
            visit(p)
        emitted.add(n)
        out_order.append(n)

    for n in names:
        visit(n)
    # orphan links (child not declared in this file) -> ignore with warning
    lines = []
    lines.append("    public static LayerDefinition createBodyLayer()")
    lines.append("    {")
    lines.append("        MeshDefinition mesh = new MeshDefinition();")
    lines.append("        PartDefinition root = mesh.getRoot();")
    lines.append("")
    for n in out_order:
        p = byname[n]
        par = parent.get(n)
        holder = par if (par in byname and par in emitted) else 'root'
        depth = 0
        q = n
        while parent.get(q) in byname:
            q = parent[q]
            depth += 1
        ind = "        " + "    " * depth
        box = p['box']
        if box is None:
            lines.append("%s// %s: 1.7.10 では箱なし（ノードのみ）" % (ind, n))
            continue
        args = [a.strip() for a in box.split(',')]
        boxs = ", ".join([fnum(args[0]), fnum(args[1]), fnum(args[2]),
                          args[3].rstrip('FfDd'), args[4].rstrip('FfDd'), args[5].rstrip('FfDd')])
        tex = "texOffs(%d, %d)" % p['tex']
        if p['mirror']:
            tex += ".mirror()"
        pose = p['pose']
        if pose is None:
            poseargs = "0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F"
        else:
            off = [fnum(a) for a in pose[0].split(',')]
            rot = [fnum(a) for a in (pose[1] or '0F, 0F, 0F').split(',')]
            poseargs = ", ".join(off + rot)
        lines.append('%sPartDefinition %s = %s.addOrReplaceChild("%s",' % (ind, n, holder, n))
        lines.append('%s        CubeListBuilder.create().%s' % (ind, tex))
        lines.append('%s                .addBox(%s),' % (ind, boxs))
        lines.append('%s        PartPose.offsetAndRotation(%s));' % (ind, poseargs))
    lines.append("")
    tw, th = info['tex']
    tw = tw or 64
    th = th or 64
    lines.append("        return LayerDefinition.create(mesh, %d, %d);" % (tw, th))
    lines.append("    }")
    return "\n".join(lines)


def main():
    only = sys.argv[1:] or None
    for fn in sorted(os.listdir(SRC)):
        if not fn.endswith('.java'):
            continue
        if only and fn[:-5] not in only:
            continue
        info = parse(os.path.join(SRC, fn))
        print("// " + "=" * 76)
        print("// %s   texture=%sx%s" % (info['name'], info['tex'][0], info['tex'][1]))
        for par, ch in info['links']:
            if ch not in info['order']:
                print("//   (link to part not declared here) %s -> %s" % (par, ch))
        mir = [p['name'] for p in info['parts'] if p['mirror']]
        print("//   effective mirror (mirror=true BEFORE addBox): %s" % (mir or "none"))
        print("// " + "=" * 76)
        print(emit(info))
        print()


if __name__ == '__main__':
    main()