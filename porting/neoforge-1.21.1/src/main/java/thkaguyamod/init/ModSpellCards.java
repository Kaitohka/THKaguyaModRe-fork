package thkaguyamod.init;

import thkaguyamod.registry.SpellCardRegistry;
import thkaguyamod.spellcard.THSC_Catadioptric;
import thkaguyamod.spellcard.THSC_EternalMeek;
import thkaguyamod.spellcard.THSC_Fafurotskies_no_Kiseki;
import thkaguyamod.spellcard.THSC_FujiyamaVolcano;
import thkaguyamod.spellcard.THSC_GagoujiCyclone;
import thkaguyamod.spellcard.THSC_HikouchuuNest;
import thkaguyamod.spellcard.THSC_HoukaKenran;
import thkaguyamod.spellcard.THSC_IcicleFall;
import thkaguyamod.spellcard.THSC_Kachoufuugetu;
import thkaguyamod.spellcard.THSC_KappaPororoca;
import thkaguyamod.spellcard.THSC_Kasho_no_Eimin;
import thkaguyamod.spellcard.THSC_Kerochan_Fuuu_ni_Makezu;
import thkaguyamod.spellcard.THSC_LittleBugStorm;
import thkaguyamod.spellcard.THSC_MasterSpark;
import thkaguyamod.spellcard.THSC_MeteonicShower;
import thkaguyamod.spellcard.THSC_MeteorOnEarth;
import thkaguyamod.spellcard.THSC_MiracleFruit;
import thkaguyamod.spellcard.THSC_Mishagujisama;
import thkaguyamod.spellcard.THSC_MoonlightRay;
import thkaguyamod.spellcard.THSC_MosesMiracle;
import thkaguyamod.spellcard.THSC_MusouFuuin;
import thkaguyamod.spellcard.THSC_Nami_to_Tubu_no_Kyoukai;
import thkaguyamod.spellcard.THSC_NijuuKokushichou;
import thkaguyamod.spellcard.THSC_NonDirectionalLaser;
import thkaguyamod.spellcard.THSC_PerfectFreeze;
import thkaguyamod.spellcard.THSC_RedMagic;
import thkaguyamod.spellcard.THSC_SaikouRanbu;
import thkaguyamod.spellcard.THSC_SatuzinDoll;
import thkaguyamod.spellcard.THSC_ScarletShoot;
import thkaguyamod.spellcard.THSC_Spear_the_Gungnir;
import thkaguyamod.spellcard.THSC_StarbowBreak;
import thkaguyamod.spellcard.THSC_StardustReverie;
import thkaguyamod.spellcard.THSC_Yasaka_no_Kamikaze;
import thkaguyamod.spellcard.THSC_YouryokuSpoiler;
import thkaguyamod.spellcard.THSC_Zenzinrui_no_Hisouten;

/**
 * Spell card registration table — ported from the 1.7.10
 * {@code CommonProxy.registerSpellCards()} (35 cards; BrilliantDragonBullet was
 * commented out in 1.7.10 and stays unregistered).
 */
public final class ModSpellCards {

    private static final String DOMAIN = "thkaguyamod";

    public static void registerSpellCards() {
        SpellCardRegistry.registerSpellCard(THSC_MusouFuuin::new, DOMAIN, "MusouFuuin", 0);
        SpellCardRegistry.registerSpellCard(THSC_MasterSpark::new, DOMAIN, "MasterSpark", 1);
        SpellCardRegistry.registerSpellCard(THSC_Kasho_no_Eimin::new, DOMAIN, "Kasho_no_Eimin", 2);
        SpellCardRegistry.registerSpellCard(THSC_MeteonicShower::new, DOMAIN, "MeteonicShower", 3);
        SpellCardRegistry.registerSpellCard(THSC_Nami_to_Tubu_no_Kyoukai::new, DOMAIN, "Nami_to_Tubu_no_Kyoukai", 4);
        SpellCardRegistry.registerSpellCard(THSC_NijuuKokushichou::new, DOMAIN, "NijuuKokushichou", 5);
        SpellCardRegistry.registerSpellCard(THSC_ScarletShoot::new, DOMAIN, "ScarletShoot", 6);
        SpellCardRegistry.registerSpellCard(THSC_Zenzinrui_no_Hisouten::new, DOMAIN, "Zenzinrui_no_Hisouten", 7);
        SpellCardRegistry.registerSpellCard(THSC_SatuzinDoll::new, DOMAIN, "SatuzinDoll", 8);
        SpellCardRegistry.registerSpellCard(THSC_PerfectFreeze::new, DOMAIN, "PerfectFreeze", 9);
        SpellCardRegistry.registerSpellCard(THSC_HikouchuuNest::new, DOMAIN, "HikouchuuNest", 10);
        SpellCardRegistry.registerSpellCard(THSC_KappaPororoca::new, DOMAIN, "Kappa'sPororoca", 11);
        SpellCardRegistry.registerSpellCard(THSC_StardustReverie::new, DOMAIN, "StardustReverie", 12);
        SpellCardRegistry.registerSpellCard(THSC_Kerochan_Fuuu_ni_Makezu::new, DOMAIN, "Kerochan_Fuuu_ni_Makezu", 13);
        SpellCardRegistry.registerSpellCard(THSC_MiracleFruit::new, DOMAIN, "MiracleFruit", 14);
        SpellCardRegistry.registerSpellCard(THSC_Fafurotskies_no_Kiseki::new, DOMAIN, "Fafrotskies_no_Kiseki", 15);
        SpellCardRegistry.registerSpellCard(THSC_YouryokuSpoiler::new, DOMAIN, "YouryokuSpoiler", 16);
        SpellCardRegistry.registerSpellCard(THSC_MosesMiracle::new, DOMAIN, "Moses_no_Kiseki", 17);
        SpellCardRegistry.registerSpellCard(THSC_Yasaka_no_Kamikaze::new, DOMAIN, "Yasaka_no_Kamikaze", 18);
        SpellCardRegistry.registerSpellCard(THSC_IcicleFall::new, DOMAIN, "IcicleFall", 19);
        SpellCardRegistry.registerSpellCard(THSC_StarbowBreak::new, DOMAIN, "StarbowBreak", 20);
        SpellCardRegistry.registerSpellCard(THSC_Catadioptric::new, DOMAIN, "Catadioptric", 21);
        SpellCardRegistry.registerSpellCard(THSC_Mishagujisama::new, DOMAIN, "Mishagujisama", 22);
        SpellCardRegistry.registerSpellCard(THSC_RedMagic::new, DOMAIN, "RedMagic", 23);
        SpellCardRegistry.registerSpellCard(THSC_EternalMeek::new, DOMAIN, "EternalMeek", 24);
        SpellCardRegistry.registerSpellCard(THSC_NonDirectionalLaser::new, DOMAIN, "Non-DirectionalLaser", 25);
        SpellCardRegistry.registerSpellCard(THSC_Kachoufuugetu::new, DOMAIN, "KachoufuugetuShoufuurougetu", 26);
        SpellCardRegistry.registerSpellCard(THSC_Spear_the_Gungnir::new, DOMAIN, "Spear_the_Gungnir", 27);
        SpellCardRegistry.registerSpellCard(THSC_MoonlightRay::new, DOMAIN, "MoonlightRay", 28);
        SpellCardRegistry.registerSpellCard(THSC_HoukaKenran::new, DOMAIN, "HoukaKenran", 29);
        SpellCardRegistry.registerSpellCard(THSC_SaikouRanbu::new, DOMAIN, "SaikouRanbu", 30);
        SpellCardRegistry.registerSpellCard(THSC_FujiyamaVolcano::new, DOMAIN, "FujiyamaVolcano", 33);
        SpellCardRegistry.registerSpellCard(THSC_LittleBugStorm::new, DOMAIN, "LittleBugStorm", 34);
        SpellCardRegistry.registerSpellCard(THSC_MeteorOnEarth::new, DOMAIN, "Meteor_on_Earth", 35);
        SpellCardRegistry.registerSpellCard(THSC_GagoujiCyclone::new, DOMAIN, "GagoujiCyclone", 40);
    }

    private ModSpellCards() {}
}
