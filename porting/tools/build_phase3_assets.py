#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Phase-3 content generator for the THKaguyaMod NeoForge 1.21.1 port.

Single source of truth for: item/block registry ids, texture mapping,
lang key mapping (1.7.10 .lang -> 1.21.1 .json), item model JSONs,
blockstate/block-model JSONs, and the generated ModItems.java entries.

Run from anywhere:  python porting/tools/build_phase3_assets.py
"""
import json
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
SRC_TEX = os.path.join(ROOT, 'porting', 'assets-1.7.10', 'assets', 'thkaguyamod', 'textures')
SRC_LANG = os.path.join(ROOT, 'porting', 'assets-1.7.10', 'assets', 'thkaguyamod', 'lang')
PROJ = os.path.join(ROOT, 'porting', 'neoforge-1.21.1')
RES = os.path.join(PROJ, 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'thkaguyamod')
DATA = os.path.join(RES, 'data', 'thkaguyamod')
JAVA_INIT = os.path.join(PROJ, 'src', 'main', 'java', 'thkaguyamod', 'init')

# ---------------------------------------------------------------------------
# Master item table
#   (CONST, registry_id, old_lang_base, texture_source, kind, props)
# texture_source: path under 1.7.10 textures/items/
# kind: item | sword:TIER | bow | armor:MATERIAL:helm|chest | food:NUTRITION:always?
# props: {'stack': n, 'dur': n}
# ---------------------------------------------------------------------------
ITEMS = [
    # ---- 五つの難題 + 新難題 ----
    ('JEWELED_BRANCH_OF_HOURAI', 'jeweled_branch_of_hourai', 'houraiJewelBranch', 'houraiEda.png', 'bow', {'dur': 301}, 'ItemHouraiJeweledBranch'),
    ('JEWEL_FROM_THE_DRAGONS_NECK', 'jewel_from_the_dragons_neck', 'dragonNeckJewel', 'ryuuTama.png', 'item', {'dur': 300, 'stack': 1}, 'ItemDragonNeckJewel'),
    ('BUDDHAS_STONE_BOWL', 'buddhas_stone_bowl', 'buddhaStoneBowl', 'hotokeHachi.png', 'item', {'dur': 131}, 'ItemBuddhaStoneBowl'),
    ('ROBE_OF_THE_FIRE_RAT', 'robe_of_the_fire_rat', 'fireRatRobe', 'hinezumi.png', 'armor:HINEZUMI_ROBE:chest', {}, 'ItemFireRatRobe'),
    ('SWALLOWS_COWRIE_SHELL', 'swallows_cowrie_shell', 'swallowCowrieShell', 'koyasugai.png', 'food:0:always', {}, 'ItemSwallowCowrieShell'),
    ('SEAMLESS_CEILING_OF_KINKAKUJI', 'seamless_ceiling_of_kinkakuji', 'kinkaku-ji', 'kinkakuzi.png', 'item', {'stack': 1}, 'ItemKinkakuzi'),
    ('RED_STONE_OF_AJA', 'red_stone_of_aja', 'ajaRedStone', 'AjaRedStone.png', 'item', {'stack': 1}, 'ItemAjaRedStone'),# 8 pearls
    ('RED_PEARL',    'red_pearl',    'houraiPearl.red',    'material/RedPearl.png',    'item', {}),
    ('BLUE_PEARL',   'blue_pearl',   'houraiPearl.blue',   'material/BluePearl.png',   'item', {}),
    ('GREEN_PEARL',  'green_pearl',  'houraiPearl.green',  'material/GreenPearl.png',  'item', {}),
    ('YELLOW_PEARL', 'yellow_pearl', 'houraiPearl.yellow', 'material/YellowPearl.png', 'item', {}),
    ('PURPLE_PEARL', 'purple_pearl', 'houraiPearl.purple', 'material/PurplePearl.png', 'item', {}),
    ('AQUA_PEARL',   'aqua_pearl',   'houraiPearl.aqua',   'material/AquaPearl.png',   'item', {}),
    ('ORANGE_PEARL', 'orange_pearl', 'houraiPearl.orange', 'material/OrangePearl.png', 'item', {}),
    ('WHITE_PEARL',  'white_pearl',  'houraiPearl.white',  'material/WhitePearl.png',  'item', {}),
    # ---- 霊夢・魔理沙 ----
    ('HAKUREI_SHRINE_MAIDEN_STICK', 'hakurei_shrine_maiden_stick', 'hakureiMikoStick', 'HakureiOharaibou.png', 'item', {'dur': 89}, 'ItemOharaibouR'),
    ('HOMING_AMULET', 'homing_amulet', 'homingAmulet.homing', 'homingAmulet.png', 'item', {}, 'ItemHomingAmulet'),
    ('DIFFUSION_AMULET', 'diffusion_amulet', 'homingAmulet.diffusion', 'diffusionAmulet.png', 'item', {}, 'ItemHomingAmulet'),
    ('YIN_YANG_ORB', 'yin_yang_orb', 'yinYangOrb', 'material/Onmyoudama.png', 'item', {}, 'ItemOnmyoudama'),
    ('BLOODTHIRSTY_YIN_YANG_ORB', 'bloodthirsty_yin_yang_orb', 'bloodthirstyYinYangOrb', 'material/Onmyoudama.png', 'item', {'dur': 150}, 'ItemBloodthirstyOnmyoudama'),
    ('MAGIC_BROOM', 'magic_broom', 'magicBroom', 'magicBroom.png', 'item', {'stack': 1}, 'ItemMarisaBroom'),
    ('MINI_HAKKERO', 'mini_hakkero', 'miniHakkero', 'miniHakkero.png', 'item', {'stack': 1}, 'ItemMiniHakkero'),
    ('MARISAS_HAT', 'marisas_hat', 'marisaHat', 'MarisaHat.png', 'armor:MARISA:helm', {'dur': 40}, 'ItemMarisaHat'),
    # ---- 紅魔郷 ----
    ('ICICLE_SWORD', 'icicle_sword', 'icicleSword', 'IcicleSword.png', 'sword:ICICLE', {}),
    ('SILVER_KNIFE_BLUE', 'silver_knife_blue', 'silverKnife.blue', 'shot/SilverKnife_Blue.png', 'item', {}, 'ItemSilverKnife'),
    ('SILVER_KNIFE_RED', 'silver_knife_red', 'silverKnife.red', 'shot/SilverKnife_Red.png', 'item', {}, 'ItemSilverKnife'),
    ('SILVER_KNIFE_GREEN', 'silver_knife_green', 'silverKnife.green', 'shot/SilverKnife_Green.png', 'item', {}, 'ItemSilverKnife'),
    ('SILVER_KNIFE_WHITE', 'silver_knife_white', 'silverKnife.white', 'shot/SilverKnife_White.png', 'item', {'stack': 1}, 'ItemSilverKnife'),
    ('SAKUYAS_POCKET_WATCH', 'sakuyas_pocket_watch', 'sakuyaWatch', 'sakuyaWatch.png', 'item', {'stack': 1, 'dur': 2}, 'ItemSakuyaWatch'),
    ('SAKUYAS_STOPWATCH', 'sakuyas_stopwatch', 'sakuyaStopwatch', 'SakuyaStopWatch.png', 'item', {'stack': 1}, 'ItemSakuyaStopWatch'),
    ('SPEAR_THE_GUNGNIR', 'spear_the_gungnir', 'spearTheGungnir', 'Gungnir.png', 'item', {'dur': 95}, 'ItemSpearTheGungnir'),
    ('LAEVATEINN', 'laevateinn', 'laevateinn', 'Laevateinn.png', 'sword:LAEVATEINN', {}),
    # ---- 妖々夢 ----
    ('CURSED_DECOY_DOLL', 'cursed_decoy_doll', 'cursedDecoyDoll', 'cursedDecoyDoll.png', 'item', {'dur': 30}, 'ItemCursedDecoyDoll'),
    ('HAKUROUKEN', 'hakurouken', 'hakurouken', 'hakurouken.png', 'sword:HAKUROUKEN', {}),
    ('ROUKANKEN', 'roukanken', 'roukanken', 'roukanSword.png', 'sword:ROUKANKEN', {}),
    ('YUYUKOS_FAN', 'yuyukos_fan', 'yuyukoFan', 'yuyukoOugi.png', 'item', {'dur': 444, 'stack': 1}, 'ItemYuyukoFan'),
    ('SOUL_TORCH', 'soul_torch', 'soulTorch', 'soulTorch.png', 'item', {}, 'ItemSoulTorch'),
    ('GAP', 'gap', 'gap', 'sukima.png', 'item', {}, 'ItemSukima'),
    ('GAP_FOLDING_UMBRELLA', 'gap_folding_umbrella', 'gapFoldingUmbrella', 'GapFoldingUmbrella_base.png|GapFoldingUmbrella_ribbon.png', 'item', {'dur': 30}, 'ItemGapFoldingUmbrella'),
    # ---- 萃夢想 ----
    ('IBUKI_GOURD', 'ibuki_gourd', 'ibukiGourd', 'ibukihyou.png', 'item', {'dur': 11, 'stack': 1}, 'ItemIbukihyou'),
    # ---- 花映塚 ----
    ('YUUKAS_PARASOL', 'yuukas_parasol', 'yuukaParasol', 'yuukaParasol.png', 'item', {'dur': 60}, 'ItemYuukaParasol'),
    ('SCYTHE_OF_DEATH', 'scythe_of_death', 'deathScythe', 'deathScythe.png', 'sword:IRON:444', {}, 'ItemDeathScythe'),
    ('ROD_OF_REMORSE', 'rod_of_remorse', 'remorseRod', 'kaigoStick_0.png', 'sword:KAIGO', {'dur': 2}, 'ItemKaigo'),
    # ---- 文花帖 ----
    ('TENGUS_FAN', 'tengus_fan', 'tenguFan', 'tenguFan.png', 'item', {'dur': 40}, 'ItemTenguFan'),
    # ---- 風神録 ----
    ('KAPPA_CAP', 'kappa_cap', 'kappaCap', 'kappaCap.png', 'armor:KAPPA:helm', {'dur': 500}, 'ItemKappaCap'),
    ('KAPPAS_WATER_PISTOL', 'kappas_water_pistol', 'kappaWaterPistol', 'kappaWaterPistol.png', 'item', {'dur': 39}, 'ItemKappaWaterPistol'),
    ('WIND_SHRINE_MAIDEN_STICK', 'wind_shrine_maiden_stick', 'windMikoStick', 'mikoStick_S.png', 'item', {'dur': 50}, 'ItemOharaibouS'),
    ('ILLNESS_RECOVERY_CHARM', 'illness_recovery_charm', 'byoukiheiyuMamori', 'ByoukiheiyuMamori.png', 'item', {}, 'ItemByoukiheiyuMamori'),
    ('ONBASHIRA', 'onbashira', 'onbashira', 'onbashira.png', 'sword:WOOD', {}, 'ItemOnbashira'),
    ('SUWAKOS_HAT', 'suwakos_hat', 'suwakoHat', 'Kerobou.png', 'armor:SUWAKO:helm', {'dur': 620}, 'ItemKerobou'),
    # ---- 緋想天 ----
    ('HEAVENLY_PEACH', 'heavenly_peach', 'heavenlyPeach', 'heavenlyPeach.png', 'food:2:always', {}, 'ItemHeavenlyPeach'),
    ('SWORD_OF_SCARLET_PERCEPTION', 'sword_of_scarlet_perception', 'scarletPerceptionSword', 'hisousword.png', 'sword:HISOU', {}),
    # ---- 地霊殿 ----
    ('THIRD_EYE', 'third_eye', 'thirdEye', 'thirdEye_0.png', 'item', {'dur': 150, 'stack': 1}, 'ItemThirdEye'),
    ('NUCLEAR_CONTROL_ROD', 'nuclear_control_rod', 'nuclearControlRod', 'nuclearControlRod.png', 'item', {'dur': 36}, 'ItemNuclearControlRod'),
    ('CLOSED_THIRD_EYE', 'closed_third_eye', 'closedThirdEye', 'closedThirdEye.png', 'item', {'stack': 1}, 'ItemClosedThirdEye'),
    # ---- 星蓮船 ----
    ('NAZRIN_PENDULUM', 'nazrin_pendulum', 'nazrinPendulum', 'pendulum.png', 'item', {'stack': 1}, 'ItemPendulum'),
    ('SHIP_GHOSTS_DIPPER_EMPTY', 'ship_ghosts_dipper_empty', 'shipGhostDipperEmpty', 'hisyaku_1.png', 'item', {'dur': 60, 'stack': 1}, 'ItemHisyaku:EMPTY'),
    ('SHIP_GHOSTS_DIPPER', 'ship_ghosts_dipper', 'shipGhostDipper', 'hisyaku_0.png', 'item', {'dur': 60, 'stack': 1}, 'ItemHisyaku:FILL'),
    ('SORCERERS_SUTRA_SCROLL', 'sorcerers_sutra_scroll', 'sorcererSutraScroll', 'mazinKyoukan.png', 'item', {'stack': 1}, 'ItemMazinKyoukan'),
    # ---- 神霊廟 ----
    ('WALL_PASSING_CHISEL', 'wall_passing_chisel', 'wallPassingChisel', 'kabenuke.png', 'item', {'dur': 122}, 'ItemKabenuke'),
    ('SACRED_SWORD_OF_TOYOSATOMIMI', 'sacred_sword_of_toyosatomimi', 'toyosatomimiSword', 'toyosatomimiSword.png', 'sword:IRON', {}, 'ItemMikoSword'),
    # ---- 輝針城 ----
    ('MIRACLE_MALLET', 'miracle_mallet', 'miracleMallet', 'UchidenoKoduchi.png', 'item', {'dur': 100}, 'ItemUchide'),
    # ---- 分類外 ----
    ('SPIRITUAL_STRIKE_TALISMAN', 'spiritual_strike_talisman', 'spiritualStrikeTalisman', 'SpiritualStrikeTalisman.png', 'item', {}, 'ItemSpiritualStrikeTalisman'),
    # ---- 基本アイテム ----
    ('SHOT_MATERIAL', 'shot_material', 'shotMaterial', 'material/ShotMaterial.png', 'item', {}, 'ItemShotMaterial'),
    ('POWER_UP_ITEM_SMALL', 'power_up_item_small', 'thPowerItem.small', 'material/SmallPowerUpItem.png', 'item', {}),
    ('POWER_UP_ITEM_BIG', 'power_up_item_big', 'thPowerItem.big', 'material/BigPowerUpItem.png', 'item', {}),
    ('POINT_ITEM', 'point_item', 'thPointItem', 'material/THPointItem.png', 'item', {}),
    ('BOMB_ITEM', 'bomb_item', 'thBombItem', 'material/BombItem.png', 'item', {}),
    ('EXTEND_ITEM', 'extend_item', 'thExtendItem', 'material/ExtendItem.png', 'item', {}),
]

# 19 danmaku shots: (CONST, id_suffix, old_variant_key, icon_file)
SHOTS = [
    ('SMALL_SHOT', 'small_shot', 'smallShot', 'SmallShot.png'),
    ('TINY_SHOT', 'tiny_shot', 'tinyShot', 'TinyShot.png'),
    ('MEDIUM_SHOT', 'medium_shot', 'mediumShot', 'MediumShot.png'),
    ('BIG_SHOT', 'big_shot', 'bigShot', 'BigShot.png'),
    ('STAR_SHOT', 'star_shot', 'starShot', 'StarShot.png'),
    ('SMALL_STAR_SHOT', 'small_star_shot', 'smallStarShot', 'SmallStarShot.png'),
    ('CIRCLE_SHOT', 'circle_shot', 'circleShot', 'CircleShot.png'),
    ('SCALE_SHOT', 'scale_shot', 'scaleShot', 'ScaleShot.png'),
    ('BUTTERFLY_SHOT', 'butterfly_shot', 'butterflyShot', 'ButterflyShot.png'),
    ('LIGHT_SHOT', 'light_shot', 'lightShot', 'LightShot.png'),
    ('KNIFE_SHOT', 'knife_shot', 'knifeShot', 'KnifeShot.png'),
    ('HEART_SHOT', 'heart_shot', 'heartShot', 'HeartShot.png'),
    ('KUNAI_SHOT', 'kunai_shot', 'kunaiShot', 'KunaiShot.png'),
    ('TALISMAN_SHOT', 'talisman_shot', 'talismanShot', 'TalismanShot.png'),
    ('BIG_LIGHT_SHOT', 'big_light_shot', 'bigLightShot', 'BigLightShot.png'),
    ('RICE_SHOT', 'rice_shot', 'riceShot', 'RiceShot.png'),
    ('OVAL_SHOT', 'oval_shot', 'ovalShot', 'OvalShot.png'),
    ('CRYSTAL_SHOT', 'crystal_shot', 'crystalShot', 'CrystalShot.png'),
    ('ARROW_SHOT', 'arrow_shot', 'arrowShot', 'ArrowShot.png'),
]
for c, sid, vk, icon in SHOTS:
    ITEMS.append((c, sid, 'thShot.' + vk, 'shot/' + icon, 'shotitem', {}))

LASERS = [
    ('MIDDLE_LASER', 'middle_laser', 'thLaser.middle', 'shot/MiddleLaser.png', 'laseritem', {}),
    ('SHORT_LASER', 'short_laser', 'thLaser.short', 'shot/ShortLaser.png', 'laseritem', {}),
    ('LONG_LASER', 'long_laser', 'thLaser.long', 'shot/LongLaser.png', 'laseritem', {}),
]
ITEMS.extend(LASERS)

# 35 spell cards: (old_numeric_id, registry_name_1_7_10, level)
SPELL_CARDS = [
    (0, 'MusouFuuin', 5), (1, 'MasterSpark', 5), (2, 'Kasho_no_Eimin', 3),
    (3, 'MeteonicShower', 1), (4, 'Nami_to_Tubu_no_Kyoukai', 5),
    (5, 'NijuuKokushichou', 3), (6, 'ScarletShoot', 4),
    (7, 'Zenzinrui_no_Hisouten', 5), (8, 'SatuzinDoll', 4),
    (9, 'PerfectFreeze', 1), (10, 'HikouchuuNest', 4), (11, "Kappa'sPororoca", 3),
    (12, 'StardustReverie', 4), (13, 'Kerochan_Fuuu_ni_Makezu', 2),
    (14, 'MiracleFruit', 1), (15, 'Fafrotskies_no_Kiseki', 1),
    (16, 'YouryokuSpoiler', 2), (17, 'Moses_no_Kiseki', 2),
    (18, 'Yasaka_no_Kamikaze', 3), (19, 'IcicleFall', 1),
    (20, 'StarbowBreak', 3), (21, 'Catadioptric', 3), (22, 'Mishagujisama', 3),
    (23, 'RedMagic', 4), (24, 'EternalMeek', 2), (25, 'Non-DirectionalLaser', 3),
    (26, 'KachoufuugetuShoufuurougetu', 5), (27, 'Spear_the_Gungnir', 3),
    (28, 'MoonlightRay', 1), (29, 'HoukaKenran', 2), (30, 'SaikouRanbu', 2),
    (33, 'FujiyamaVolcano', 5), (34, 'LittleBugStorm', 2),
    (35, 'Meteor_on_Earth', 1), (40, 'GagoujiCyclone', 2),
]


def snake(name):
    """Registry-name string from 1.7.10 -> clean snake_case."""
    s = name.replace("'", "").replace('-', '_')
    s = re_sub_camel(s)
    return s.lower()


def re_sub_camel(s):
    out = []
    prev = ''
    for ch in s:
        if ch.isupper() and prev and (prev.islower() or prev.isdigit()):
            out.append('_')
        elif ch.isupper() and prev.isupper():
            pass
        out.append(ch)
        prev = ch
    return ''.join(out)


# Blocks: (CONST, id, old_lang_key, texture_src(new name), extra)
BLOCKS = [
    ('DANMAKU_CRAFTING_TABLE', 'danmaku_crafting_table', 'tile.danmakuCraftingTable.shot.name', 'danmaku_crafting_table.png'),
    ('DANMAKU_LASER_CRAFTING_TABLE', 'danmaku_laser_crafting_table', 'tile.danmakuCraftingTable.laser.name', 'danmaku_crafting_table2.png'),
]
DIVINE_COLORS = ['red', 'blue', 'green', 'yellow', 'purple', 'aqua', 'orange', 'white']

LANG_FILES = {
    'en_us': 'en_US.lang', 'ja_jp': 'ja_JP.lang', 'ko_kr': 'ko_KR.lang',
    'zh_cn': 'zh_CN.lang', 'zh_tw': 'zh_TW.lang',
}

TAB_LANG = {
    'en_us': ('Itutu no Nandai MOD+', 'Spell Card'),
    'ja_jp': ('五つの難題MOD+', 'スペルカード'),
    'ko_kr': ('Itutu no Nandai MOD+', '스펠 카드'),
    'zh_cn': ('五难题MOD+', '符卡'),
    'zh_tw': ('五難題MOD+', '符卡'),
}

warnings = []


def load_lang(path):
    d = {}
    with open(path, encoding='utf-8-sig') as f:
        for line in f:
            line = line.rstrip('\n').rstrip('\r')
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                k, v = line.split('=', 1)
                d[k.strip()] = v
    return d


def tex_out_name(reg_id):
    return reg_id + '.png'


def copy_texture(src_rel, reg_id, sub=''):
    src = os.path.join(SRC_TEX, 'items', src_rel)
    dst_dir = os.path.join(ASSETS, 'textures', 'item', sub)
    os.makedirs(dst_dir, exist_ok=True)
    dst = os.path.join(dst_dir, tex_out_name(reg_id))
    if not os.path.exists(src):
        warnings.append('MISSING TEXTURE: %s (for %s)' % (src_rel, reg_id))
        return None
    shutil.copyfile(src, dst)
    # companion .mcmeta (animated texture)
    if os.path.exists(src + '.mcmeta'):
        shutil.copyfile(src + '.mcmeta', dst + '.mcmeta')
    return ('thkaguyamod:item/' + sub + reg_id) if sub else ('thkaguyamod:item/' + reg_id)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2, sort_keys=True)
        f.write('\n')


def item_model(reg_id, tex, kind, tex2=None):
    parent = 'minecraft:item/handheld' if kind.startswith('sword') else 'minecraft:item/generated'
    textures = {'layer0': tex}
    if tex2:
        textures['layer1'] = tex2
    write_json(os.path.join(ASSETS, 'models', 'item', reg_id + '.json'),
               {'parent': parent, 'textures': textures})


def main():
    # ---------- collect spell card item entries ----------
    card_entries = []
    for old_id, reg_name, level in SPELL_CARDS:
        cid = 'spell_card_' + snake(reg_name)
        card_entries.append((old_id, reg_name, cid, level))
        ITEMS.append(('SPELL_CARD_' + snake(reg_name).upper(), cid,
                      'thSpellCard.%d' % old_id,
                      'spellCard/%s.png' % reg_name, 'spellcard', {'stack': 1, 'level': level},
                      'ItemTHSpellCard:%d' % old_id))

    # ---------- textures + item models ----------
    for entry in ITEMS:
        const, rid, old_key, tex_src, kind, props = entry[:6]
        parts = tex_src.split('|')
        sub = 'spell_card/' if kind == 'spellcard' else ''
        if tex_src.startswith('shot/') or tex_src.startswith('material/'):
            sub = tex_src.split('/')[0] + '/'
        tex = copy_texture(parts[0], rid, sub)
        tex2 = None
        if len(parts) > 1:
            # second layer (umbrella ribbon) keeps its own name
            src2 = os.path.join(SRC_TEX, 'items', parts[1])
            if os.path.exists(src2):
                dst2 = os.path.join(ASSETS, 'textures', 'item', rid + '_ribbon.png')
                shutil.copyfile(src2, dst2)
                tex2 = 'thkaguyamod:item/' + rid + '_ribbon'
            else:
                warnings.append('MISSING TEXTURE layer1: %s' % parts[1])
        if kind == 'spellcard':
            star = os.path.join(SRC_TEX, 'items', 'spellCard', 'spellcard_lv%d.png' % props['level'])
            if os.path.exists(star):
                dst = os.path.join(ASSETS, 'textures', 'item', 'spell_card', 'spellcard_lv%d.png' % props['level'])
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                shutil.copyfile(star, dst)
                tex2 = 'thkaguyamod:item/spell_card/spellcard_lv%d' % props['level']
        if tex:
            item_model(rid, tex, kind, tex2)

    # ---------- block textures + models ----------
    for const, bid, old_key, tex_src in BLOCKS:
        src = os.path.join(SRC_TEX, 'blocks', tex_src)
        dst = os.path.join(ASSETS, 'textures', 'block', bid + '.png')
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        if os.path.exists(src):
            shutil.copyfile(src, dst)
        else:
            warnings.append('MISSING BLOCK TEXTURE: %s' % tex_src)
        write_json(os.path.join(ASSETS, 'blockstates', bid + '.json'),
                   {'variants': {'': {'model': 'thkaguyamod:block/' + bid}}})
        write_json(os.path.join(ASSETS, 'models', 'block', bid + '.json'),
                   {'parent': 'minecraft:block/cube_all',
                    'textures': {'all': 'thkaguyamod:block/' + bid}})
        write_json(os.path.join(ASSETS, 'models', 'item', bid + '.json'),
                   {'parent': 'thkaguyamod:block/' + bid})
    for color in DIVINE_COLORS:
        bid = 'divine_spirit_' + color
        src = os.path.join(SRC_TEX, 'blocks', 'divinespirit_%s.png' % color)
        dst = os.path.join(ASSETS, 'textures', 'block', bid + '.png')
        if os.path.exists(src):
            shutil.copyfile(src, dst)
        else:
            warnings.append('MISSING BLOCK TEXTURE: divinespirit_%s.png' % color)
        write_json(os.path.join(ASSETS, 'blockstates', bid + '.json'),
                   {'variants': {'': {'model': 'thkaguyamod:block/' + bid}}})
        write_json(os.path.join(ASSETS, 'models', 'block', bid + '.json'),
                   {'parent': 'minecraft:block/cube_all',
                    'textures': {'all': 'thkaguyamod:block/' + bid}})
        write_json(os.path.join(ASSETS, 'models', 'item', bid + '.json'),
                   {'parent': 'thkaguyamod:block/' + bid})

    # armor layer textures (custom biped models come in phase 4)
    armor_map = {'hinezumi_1.png': 'hinezumi_layer_1.png',
                 'MarisaHatTexture.png': 'marisa_layer_1.png',
                 'SuwakoHatTexture.png': 'suwako_layer_1.png'}
    for src_name, dst_name in armor_map.items():
        src = os.path.join(SRC_TEX, 'armor', src_name)
        dst = os.path.join(ASSETS, 'textures', 'models', 'armor', dst_name)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        if os.path.exists(src):
            shutil.copyfile(src, dst)
        else:
            warnings.append('MISSING ARMOR TEXTURE: %s' % src_name)

    # ---------- lang conversion ----------
    old_langs = {k: load_lang(os.path.join(SRC_LANG, v)) for k, v in LANG_FILES.items()}
    for lang, old in old_langs.items():
        new = {}
        main_tab, spell_tab = TAB_LANG[lang]
        new['itemGroup.thkaguyamod'] = main_tab
        new['itemGroup.thkaguyamod.spellcard'] = spell_tab
        for entry in ITEMS:
            const, rid, old_key, tex_src, kind, props = entry[:6]
            ok = 'item.%s.name' % old_key
            if ok in old:
                new['item.thkaguyamod.%s' % rid] = old[ok]
            elif ok in old_langs['en_us']:
                # e.g. zh_TW has a corrupted line missing '=' for thirdEye
                new['item.thkaguyamod.%s' % rid] = old_langs['en_us'][ok]
                warnings.append('LANG FALLBACK to en_us %s: %s (for %s)' % (lang, ok, rid))
            else:
                warnings.append('MISSING LANG %s: %s (for %s)' % (lang, ok, rid))
                new['item.thkaguyamod.%s' % rid] = rid
        for const, bid, old_key, tex_src in BLOCKS:
            if old_key in old:
                new['block.thkaguyamod.%s' % bid] = old[old_key]
            else:
                warnings.append('MISSING LANG %s: %s' % (lang, old_key))
                new['block.thkaguyamod.%s' % bid] = bid
        for color in DIVINE_COLORS:
            ok = 'tile.divineSpirit.%s.name' % color
            new['block.thkaguyamod.divine_spirit_%s' % color] = old.get(ok, old.get('tile.divineSpirit.name', 'divine_spirit_' + color))
        # entity names: entity.THKaguyaMod.<Camel>.name -> entity.thkaguyamod.<snake>
        ENTITY_LANG = {
            'THFairy': 'th_fairy', 'SunflowerFairy': 'sunflower_fairy', 'THPhantom': 'th_phantom',
            'Familiar': 'familiar', 'Cirno': 'cirno', 'Rumia': 'rumia', 'Toziko': 'toziko',
            'Sanae': 'sanae', 'Reimu': 'reimu', 'Miko': 'miko', 'Wriggle': 'wriggle',
            'Sakuya': 'sakuya', 'Rinnosuke': 'rinnosuke', 'Hanabeeper': 'danmaku_creeper',
            'CursedDecoyDoll': 'cursed_decoy_doll',
        }
        for camel, nid in ENTITY_LANG.items():
            ok = 'entity.THKaguyaMod.%s.name' % camel
            if ok in old:
                new['entity.thkaguyamod.%s' % nid] = old[ok]
        # GUI labels and misc message keys pass through unchanged
        for k, v in old.items():
            if k.startswith(('danmakuCrafting.', 'laserCrafting.', 'thKaguya.')):
                new[k] = v
        out_path = os.path.join(ASSETS, 'lang', lang + '.json')
        if os.path.exists(out_path):
            # lang 所有权已移交 check_lang.py --apply（审计补齐批次）；生成器不再覆盖
            print('lang %s exists, skip (owned by check_lang.py)' % lang)
        else:
            write_json(out_path, new)

    # ---------- ModItems.java ----------
    body = []
    body.append('/** Item registry. GENERATED by porting/tools/build_phase3_assets.py - do not edit by hand. */')
    body.append('public final class ModItems {')
    body.append('')
    body.append('    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(THKaguyaMod.MOD_ID);')
    body.append('')
    body.append('    private ModItems() {}')
    body.append('')
    shot_idx = 0
    laser_idx = 0
    extra_imports = set()
    for entry in ITEMS:
        const, rid, old_key, tex_src, kind, props = entry[:6]
        cls = entry[6] if len(entry) > 6 else None
        p = ['new Item.Properties()']
        if props.get('stack'):
            p.append('.stacksTo(%d)' % props['stack'])
        if props.get('dur'):
            p.append('.durability(%d)' % props['dur'])
        prop = ''.join(p)
        if cls:
            extra_imports.add(cls.split(':')[0])
        if kind == 'shotitem':
            body.append('    public static final DeferredItem<ItemTHShot> %s = ITEMS.registerItem("%s", props -> new ItemTHShot(props, %d), %s);' % (const, rid, shot_idx, prop))
            extra_imports.add('ItemTHShot')
            shot_idx += 1
        elif kind == 'laseritem':
            body.append('    public static final DeferredItem<ItemTHLaser> %s = ITEMS.registerItem("%s", props -> new ItemTHLaser(props, %d), %s);' % (const, rid, laser_idx, prop))
            extra_imports.add('ItemTHLaser')
            laser_idx += 1
        elif kind.startswith('sword:'):
            tier = kind.split(':')[1]
            if len(kind.split(':')) > 2:
                prop = 'new Item.Properties().durability(%s)' % kind.split(':')[2]
            if cls:
                body.append('    public static final DeferredItem<%s> %s = ITEMS.registerItem("%s", props -> new %s(ModTiers.%s, props), %s);' % (cls, const, rid, cls, tier, prop))
            else:
                body.append('    public static final DeferredItem<SwordItem> %s = ITEMS.registerItem("%s", props -> new SwordItem(ModTiers.%s, props), %s);' % (const, rid, tier, prop))
        elif kind == 'bow':
            if cls:
                body.append('    public static final DeferredItem<%s> %s = ITEMS.registerItem("%s", %s::new, %s);' % (cls, const, rid, cls, prop))
            else:
                body.append('    public static final DeferredItem<BowItem> %s = ITEMS.registerItem("%s", BowItem::new, %s);' % (const, rid, prop))
        elif kind.startswith('armor:'):
            _, mat, slot = kind.split(':')
            atype = 'ArmorItem.Type.HELMET' if slot == 'helm' else 'ArmorItem.Type.CHESTPLATE'
            if cls:
                body.append('    public static final DeferredItem<%s> %s = ITEMS.registerItem("%s", props -> new %s(ModArmorMaterials.%s, %s, props), %s);' % (cls, const, rid, cls, mat, atype, prop))
            else:
                body.append('    public static final DeferredItem<ArmorItem> %s = ITEMS.registerItem("%s", props -> new ArmorItem(ModArmorMaterials.%s, %s, props), %s);' % (const, rid, mat, atype, prop))
        elif kind.startswith('food:'):
            _, nut, always = kind.split(':')
            food = 'new FoodProperties.Builder().nutrition(%s)%s.build()' % (nut, '.alwaysEdible()' if always == 'always' else '')
            factory = (cls + '::new') if cls else 'Item::new'
            rettype = cls if cls else 'Item'
            body.append('    public static final DeferredItem<%s> %s = ITEMS.registerItem("%s", %s, %s.food(%s));' % (rettype, const, rid, factory, prop, food))
        elif kind == 'spellcard':
            clsname, arg = cls.split(':')
            extra_imports.add(clsname)
            body.append('    public static final DeferredItem<%s> %s = ITEMS.registerItem("%s", props -> new %s(props, %s), %s);' % (clsname, const, rid, clsname, arg, prop))
        else:
            if cls and ':' in cls:
                clsname, arg = cls.split(':')
                factory = 'props -> new %s(props, %s.%s)' % (clsname, clsname, arg)
                extra_imports.add(clsname)
                rettype = clsname
            else:
                factory = (cls + '::new') if cls else 'Item::new'
                rettype = cls if cls else 'Item'
            body.append('    public static final DeferredItem<%s> %s = ITEMS.registerItem("%s", %s, %s);' % (rettype, const, rid, factory, prop))
    body.append('')

    lines = ['package thkaguyamod.init;', '', 'import java.util.List;', '',
             'import net.minecraft.world.food.FoodProperties;',
             'import net.minecraft.world.item.ArmorItem;',
             'import net.minecraft.world.item.BowItem;',
             'import net.minecraft.world.item.Item;',
             'import net.minecraft.world.item.SwordItem;',
             'import net.neoforged.bus.api.IEventBus;',
             'import net.neoforged.neoforge.registries.DeferredItem;',
             'import net.neoforged.neoforge.registries.DeferredRegister;', '',
             'import thkaguyamod.THKaguyaMod;']
    for imp in sorted(extra_imports):
        lines.append('import thkaguyamod.item.%s;' % imp)
    lines.append('')
    lines.extend(body)

    main_consts = [e[0] for e in ITEMS if e[4] != 'spellcard']
    card_consts = [e[0] for e in ITEMS if e[4] == 'spellcard']
    lines.append('    /** Contents of the main creative tab (registration order). */')
    lines.append('    public static final List<DeferredItem<? extends Item>> MAIN_TAB = List.of(')
    for i, c in enumerate(main_consts):
        lines.append('            %s%s' % (c, ',' if i < len(main_consts) - 1 else ''))
    lines.append('    );')
    lines.append('')
    lines.append('    /** Contents of the spell card creative tab (registration order). */')
    lines.append('    public static final List<DeferredItem<? extends Item>> SPELLCARD_TAB = List.of(')
    for i, c in enumerate(card_consts):
        lines.append('            %s%s' % (c, ',' if i < len(card_consts) - 1 else ''))
    lines.append('    );')
    lines.append('')
    lines.append('    public static void register(IEventBus modEventBus) {')
    lines.append('        ITEMS.register(modEventBus);')
    lines.append('    }')
    lines.append('}')
    os.makedirs(JAVA_INIT, exist_ok=True)
    with open(os.path.join(JAVA_INIT, 'ModItems.java'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')

    # ---------- id manifest for the recipe migration ----------
    manifest = {
        'items': {e[1]: {'old_lang_key': e[2], 'kind': e[4]} for e in ITEMS},
        'blocks': [b[1] for b in BLOCKS] + ['divine_spirit_' + c for c in DIVINE_COLORS],
        'spell_cards': {str(old): cid for old, name, cid, lvl in card_entries},
    }
    with open(os.path.join(ROOT, 'porting', 'items_phase3.json'), 'w', encoding='utf-8') as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    # ---------- report ----------
    print('items registered : %d' % len(ITEMS))
    print('  of which cards : %d' % len(card_entries))
    print('blocks           : %d' % (len(BLOCKS) + len(DIVINE_COLORS)))
    print('warnings         : %d' % len(warnings))
    for w in warnings:
        print('  ! ' + w)


if __name__ == '__main__':
    main()
