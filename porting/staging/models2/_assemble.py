"""Assembler for the models2 batch (1.7.10 living entity models -> NeoForge 1.21.1).

Geometry (box coordinates, sizes, texture offsets, rotation points, mirror flags and
the part hierarchy) is extracted mechanically from the 1.7.10 sources so the numbers
cannot drift. The animation code (setupAnim) is hand-ported and lives in MODELS below.

Run:  python _assemble.py
Output: thkaguyamod/client/model/living/*.java
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _gen_geometry as G

SRC = G.SRC
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "thkaguyamod", "client", "model", "living")

BASE_FIELDS = ["bipedHead", "bipedBody", "bipedRightArm", "bipedLeftArm",
               "bipedRightLeg", "bipedLeftLeg", "skirtTop", "skirtBottom",
               "longHair", "rightBraid", "leftBraid"]

MODELS = []


def M(name, **kw):
    kw["name"] = name
    MODELS.append(kw)


IMPORTS_COMMON = """import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
"""


def build_source(cfg):
    info = G.parse(os.path.join(SRC, cfg["name"] + ".java"))
    for link in cfg.get("extra_links", []):
        info["links"].append(link)

    declared = [p["name"] for p in info["parts"]]
    fields = cfg.get("fields") or [n for n in declared if n not in cfg.get("skip_fields", [])]
    fields = [f for f in fields if f not in cfg.get("skip_fields", [])]

    nullable = cfg.get("nullable", False)
    getter = (lambda n: 'part(root, "%s")' % n) if nullable else (lambda n: 'root.getChild("%s")' % n)

    out = ["package thkaguyamod.client.model.living;", ""]
    out.append(IMPORTS_COMMON)
    if cfg.get("extra_imports"):
        out.append(cfg["extra_imports"].strip())
        out.append("")
    if "Mth." in cfg["setup"] or "Mth." in cfg.get("extra_methods", ""):
        out.append("import net.minecraft.util.Mth;")
    out.append("import thkaguyamod.entity.living.%s;" % cfg["entity"])
    out.append("")
    out.append(cfg["doc"].strip())
    out.append("public class %s extends %s" % (cfg["name"], cfg["extends"]))
    out.append("{")
    for f in fields:
        out.append("    public final ModelPart %s;" % f)
    if cfg.get("extra_fields"):
        out.append(cfg["extra_fields"].strip())
    out.append("    private final ModelPart root;")
    out.append("")
    out.append("    public %s(ModelPart root)" % cfg["name"])
    out.append("    {")
    out.append("        this.root = root;")
    if cfg.get("ctor_head"):
        out.append(cfg["ctor_head"].strip())
    for f in fields:
        out.append("        this.%s = %s;" % (f, getter(f)))
    if cfg.get("ctor_tail"):
        out.append(cfg["ctor_tail"].strip())
    out.append("    }")
    out.append("")
    if nullable:
        out.append("""    /** 1.7.10 は未生成パーツが null になり得たため、null 許容で取得する */
    protected static ModelPart part(ModelPart root, String name)
    {
        return root.hasChild(name) ? root.getChild(name) : null;
    }
""")
    if info["parts"]:
        out.append(G.emit(info))
        out.append("")
    out.append("""    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
""")
    out.append(cfg["setup"].strip("\n"))
    if cfg.get("extra_methods"):
        out.append("")
        out.append(cfg["extra_methods"].strip("\n"))
    out.append("}")
    return "\n".join(out) + "\n"