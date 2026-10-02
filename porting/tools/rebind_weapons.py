#!/usr/bin/env python3
# One-off patch: rebind weapon/item classes in the phase3 generator table.
import io, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
p = os.path.join(HERE, 'build_phase3_assets.py')
s = io.open(p, encoding='utf-8').read()

rebind = [
 ("('HOMING_AMULET', 'homing_amulet', 'homingAmulet.homing', 'homingAmulet.png', 'item', {})", "('HOMING_AMULET', 'homing_amulet', 'homingAmulet.homing', 'homingAmulet.png', 'item', {}, 'ItemHomingAmulet')"),
 ("('DIFFUSION_AMULET', 'diffusion_amulet', 'homingAmulet.diffusion', 'diffusionAmulet.png', 'item', {})", "('DIFFUSION_AMULET', 'diffusion_amulet', 'homingAmulet.diffusion', 'diffusionAmulet.png', 'item', {}, 'ItemHomingAmulet')"),
 ("('MINI_HAKKERO', 'mini_hakkero', 'miniHakkero', 'miniHakkero.png', 'item', {'stack': 1})", "('MINI_HAKKERO', 'mini_hakkero', 'miniHakkero', 'miniHakkero.png', 'item', {'stack': 1}, 'ItemMiniHakkero')"),
 ("('HAKUREI_SHRINE_MAIDEN_STICK', 'hakurei_shrine_maiden_stick', 'hakureiMikoStick', 'HakureiOharaibou.png', 'item', {'dur': 89})", "('HAKUREI_SHRINE_MAIDEN_STICK', 'hakurei_shrine_maiden_stick', 'hakureiMikoStick', 'HakureiOharaibou.png', 'item', {'dur': 89}, 'ItemOharaibouR')"),
 ("('WIND_SHRINE_MAIDEN_STICK', 'wind_shrine_maiden_stick', 'windMikoStick', 'mikoStick_S.png', 'item', {'dur': 50})", "('WIND_SHRINE_MAIDEN_STICK', 'wind_shrine_maiden_stick', 'windMikoStick', 'mikoStick_S.png', 'item', {'dur': 50}, 'ItemOharaibouS')"),
 ("('TENGUS_FAN', 'tengus_fan', 'tenguFan', 'tenguFan.png', 'item', {'dur': 40})", "('TENGUS_FAN', 'tengus_fan', 'tenguFan', 'tenguFan.png', 'item', {'dur': 40}, 'ItemTenguFan')"),
 ("('NUCLEAR_CONTROL_ROD', 'nuclear_control_rod', 'nuclearControlRod', 'nuclearControlRod.png', 'item', {'dur': 36})", "('NUCLEAR_CONTROL_ROD', 'nuclear_control_rod', 'nuclearControlRod', 'nuclearControlRod.png', 'item', {'dur': 36}, 'ItemNuclearControlRod')"),
 ("('KAPPAS_WATER_PISTOL', 'kappas_water_pistol', 'kappaWaterPistol', 'kappaWaterPistol.png', 'item', {'dur': 39})", "('KAPPAS_WATER_PISTOL', 'kappas_water_pistol', 'kappaWaterPistol', 'kappaWaterPistol.png', 'item', {'dur': 39}, 'ItemKappaWaterPistol')"),
 ("('JEWEL_FROM_THE_DRAGONS_NECK', 'jewel_from_the_dragons_neck', 'dragonNeckJewel', 'ryuuTama.png', 'item', {'dur': 300, 'stack': 1})", "('JEWEL_FROM_THE_DRAGONS_NECK', 'jewel_from_the_dragons_neck', 'dragonNeckJewel', 'ryuuTama.png', 'item', {'dur': 300, 'stack': 1}, 'ItemDragonNeckJewel')"),
 ("('JEWELED_BRANCH_OF_HOURAI', 'jeweled_branch_of_hourai', 'houraiJewelBranch', 'houraiEda.png', 'bow', {'dur': 301})", "('JEWELED_BRANCH_OF_HOURAI', 'jeweled_branch_of_hourai', 'houraiJewelBranch', 'houraiEda.png', 'bow', {'dur': 301}, 'ItemHouraiJeweledBranch')"),
 ("('YUYUKOS_FAN', 'yuyukos_fan', 'yuyukoFan', 'yuyukoOugi.png', 'item', {'dur': 444, 'stack': 1})", "('YUYUKOS_FAN', 'yuyukos_fan', 'yuyukoFan', 'yuyukoOugi.png', 'item', {'dur': 444, 'stack': 1}, 'ItemYuyukoFan')"),
 ("('YUUKAS_PARASOL', 'yuukas_parasol', 'yuukaParasol', 'yuukaParasol.png', 'item', {'dur': 60})", "('YUUKAS_PARASOL', 'yuukas_parasol', 'yuukaParasol', 'yuukaParasol.png', 'item', {'dur': 60}, 'ItemYuukaParasol')"),
 ("('RED_STONE_OF_AJA', 'red_stone_of_aja', 'ajaRedStone', 'AjaRedStone.png', 'item', {'stack': 1})", "('RED_STONE_OF_AJA', 'red_stone_of_aja', 'ajaRedStone', 'AjaRedStone.png', 'item', {'stack': 1}, 'ItemAjaRedStone')"),
 ("('SOUL_TORCH', 'soul_torch', 'soulTorch', 'soulTorch.png', 'item', {})", "('SOUL_TORCH', 'soul_torch', 'soulTorch', 'soulTorch.png', 'item', {}, 'ItemSoulTorch')"),
 ("('SPEAR_THE_GUNGNIR', 'spear_the_gungnir', 'spearTheGungnir', 'Gungnir.png', 'item', {'dur': 95})", "('SPEAR_THE_GUNGNIR', 'spear_the_gungnir', 'spearTheGungnir', 'Gungnir.png', 'item', {'dur': 95}, 'ItemSpearTheGungnir')"),
 ("('GAP', 'gap', 'gap', 'sukima.png', 'item', {})", "('GAP', 'gap', 'gap', 'sukima.png', 'item', {}, 'ItemSukima')"),
 ("('GAP_FOLDING_UMBRELLA', 'gap_folding_umbrella', 'gapFoldingUmbrella', 'GapFoldingUmbrella_base.png|GapFoldingUmbrella_ribbon.png', 'item', {'dur': 30})", "('GAP_FOLDING_UMBRELLA', 'gap_folding_umbrella', 'gapFoldingUmbrella', 'GapFoldingUmbrella_base.png|GapFoldingUmbrella_ribbon.png', 'item', {'dur': 30}, 'ItemGapFoldingUmbrella')"),
 ("('BLOODTHIRSTY_YIN_YANG_ORB', 'bloodthirsty_yin_yang_orb', 'bloodthirstyYinYangOrb', 'material/Onmyoudama.png', 'item', {'dur': 150})", "('BLOODTHIRSTY_YIN_YANG_ORB', 'bloodthirsty_yin_yang_orb', 'bloodthirstyYinYangOrb', 'material/Onmyoudama.png', 'item', {'dur': 150}, 'ItemBloodthirstyOnmyoudama')"),
 ("('CURSED_DECOY_DOLL', 'cursed_decoy_doll', 'cursedDecoyDoll', 'cursedDecoyDoll.png', 'item', {'dur': 30})", "('CURSED_DECOY_DOLL', 'cursed_decoy_doll', 'cursedDecoyDoll', 'cursedDecoyDoll.png', 'item', {'dur': 30}, 'ItemCursedDecoyDoll')"),
 ("('SEAMLESS_CEILING_OF_KINKAKUJI', 'seamless_ceiling_of_kinkakuji', 'kinkaku-ji', 'kinkakuzi.png', 'item', {'stack': 1})", "('SEAMLESS_CEILING_OF_KINKAKUJI', 'seamless_ceiling_of_kinkakuji', 'kinkaku-ji', 'kinkakuzi.png', 'item', {'stack': 1}, 'ItemKinkakuzi')"),
 ("('SORCERERS_SUTRA_SCROLL', 'sorcerers_sutra_scroll', 'sorcererSutraScroll', 'mazinKyoukan.png', 'item', {'stack': 1})", "('SORCERERS_SUTRA_SCROLL', 'sorcerers_sutra_scroll', 'sorcererSutraScroll', 'mazinKyoukan.png', 'item', {'stack': 1}, 'ItemMazinKyoukan')"),
 ("('MAGIC_BROOM', 'magic_broom', 'magicBroom', 'magicBroom.png', 'item', {'stack': 1})", "('MAGIC_BROOM', 'magic_broom', 'magicBroom', 'magicBroom.png', 'item', {'stack': 1}, 'ItemMarisaBroom')"),
 ("('MIRACLE_MALLET', 'miracle_mallet', 'miracleMallet', 'UchidenoKoduchi.png', 'item', {'dur': 100})", "('MIRACLE_MALLET', 'miracle_mallet', 'miracleMallet', 'UchidenoKoduchi.png', 'item', {'dur': 100}, 'ItemUchide')"),
 ("('SPIRITUAL_STRIKE_TALISMAN', 'spiritual_strike_talisman', 'spiritualStrikeTalisman', 'SpiritualStrikeTalisman.png', 'item', {})", "('SPIRITUAL_STRIKE_TALISMAN', 'spiritual_strike_talisman', 'spiritualStrikeTalisman', 'SpiritualStrikeTalisman.png', 'item', {}, 'ItemSpiritualStrikeTalisman')"),
 ("('NAZRIN_PENDULUM', 'nazrin_pendulum', 'nazrinPendulum', 'pendulum.png', 'item', {'stack': 1})", "('NAZRIN_PENDULUM', 'nazrin_pendulum', 'nazrinPendulum', 'pendulum.png', 'item', {'stack': 1}, 'ItemPendulum')"),
 ("('SACRED_SWORD_OF_TOYOSATOMIMI', 'sacred_sword_of_toyosatomimi', 'toyosatomimiSword', 'toyosatomimiSword.png', 'sword:IRON', {})", "('SACRED_SWORD_OF_TOYOSATOMIMI', 'sacred_sword_of_toyosatomimi', 'toyosatomimiSword', 'toyosatomimiSword.png', 'sword:IRON', {}, 'ItemMikoSword')"),
 ("('SILVER_KNIFE_BLUE', 'silver_knife_blue', 'silverKnife.blue', 'shot/SilverKnife_Blue.png', 'item', {})", "('SILVER_KNIFE_BLUE', 'silver_knife_blue', 'silverKnife.blue', 'shot/SilverKnife_Blue.png', 'item', {}, 'ItemSilverKnife')"),
 ("('SILVER_KNIFE_RED', 'silver_knife_red', 'silverKnife.red', 'shot/SilverKnife_Red.png', 'item', {})", "('SILVER_KNIFE_RED', 'silver_knife_red', 'silverKnife.red', 'shot/SilverKnife_Red.png', 'item', {}, 'ItemSilverKnife')"),
 ("('SILVER_KNIFE_GREEN', 'silver_knife_green', 'silverKnife.green', 'shot/SilverKnife_Green.png', 'item', {})", "('SILVER_KNIFE_GREEN', 'silver_knife_green', 'silverKnife.green', 'shot/SilverKnife_Green.png', 'item', {}, 'ItemSilverKnife')"),
 ("('SILVER_KNIFE_WHITE', 'silver_knife_white', 'silverKnife.white', 'shot/SilverKnife_White.png', 'item', {'stack': 1})", "('SILVER_KNIFE_WHITE', 'silver_knife_white', 'silverKnife.white', 'shot/SilverKnife_White.png', 'item', {'stack': 1}, 'ItemSilverKnife')"),
 ("('SAKUYAS_POCKET_WATCH', 'sakuyas_pocket_watch', 'sakuyaWatch', 'sakuyaWatch.png', 'item', {'stack': 1})", "('SAKUYAS_POCKET_WATCH', 'sakuyas_pocket_watch', 'sakuyaWatch', 'sakuyaWatch.png', 'item', {'stack': 1, 'dur': 2}, 'ItemSakuyaWatch')"),
 ("('SAKUYAS_STOPWATCH', 'sakuyas_stopwatch', 'sakuyaStopwatch', 'SakuyaStopWatch.png', 'item', {'stack': 1})", "('SAKUYAS_STOPWATCH', 'sakuyas_stopwatch', 'sakuyaStopwatch', 'SakuyaStopWatch.png', 'item', {'stack': 1}, 'ItemSakuyaStopWatch')"),
]
missing = [old[:60] for old, new in rebind if old not in s]
for old, new in rebind:
    s = s.replace(old, new)
print('missing:', missing if missing else 'none')

old_bow = """        elif kind == 'bow':
            body.append('    public static final DeferredItem<BowItem> %s = ITEMS.registerItem("%s", BowItem::new, %s);' % (const, rid, prop))"""
new_bow = """        elif kind == 'bow':
            if cls:
                body.append('    public static final DeferredItem<%s> %s = ITEMS.registerItem("%s", %s::new, %s);' % (cls, const, rid, cls, prop))
            else:
                body.append('    public static final DeferredItem<BowItem> %s = ITEMS.registerItem("%s", BowItem::new, %s);' % (const, rid, prop))"""
if old_bow in s:
    s = s.replace(old_bow, new_bow)
    print('bow cls support ok')
else:
    print('WARN: bow block not found')

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('done')
