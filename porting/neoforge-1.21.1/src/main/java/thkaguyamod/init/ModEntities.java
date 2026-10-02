package thkaguyamod.init;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.EntityDivineSpirit;
import thkaguyamod.entity.EntityShotMaterial;
import thkaguyamod.entity.EntityTHItem;
import thkaguyamod.entity.effect.EntityAjaRedStoneEffect;
import thkaguyamod.entity.effect.EntityCirnoIceBox;
import thkaguyamod.entity.effect.EntityHakureiShield;
import thkaguyamod.entity.effect.EntityHakurouReflecter;
import thkaguyamod.entity.effect.EntityMiracleCircle;
import thkaguyamod.entity.effect.EntitySpellCardCircle;
import thkaguyamod.entity.item.EntityCursedDecoyDoll;
import thkaguyamod.entity.item.EntityHisou;
import thkaguyamod.entity.item.EntityKinkakuzi;
import thkaguyamod.entity.item.EntityMarisaBroom;
import thkaguyamod.entity.item.EntityMazinkyoukan;
import thkaguyamod.entity.item.EntityMiniHakkero;
import thkaguyamod.entity.item.EntityPendulum;
import thkaguyamod.entity.item.EntitySakuyaStopWatch;
import thkaguyamod.entity.item.EntitySakuyaWatch;
import thkaguyamod.entity.item.EntitySilverKnife;
import thkaguyamod.entity.item.EntitySpiritualStrikeTalisman;
import thkaguyamod.entity.item.EntitySukima;
import thkaguyamod.entity.item.EntityYuukaParasol;
import thkaguyamod.entity.living.EntityCirno;
import thkaguyamod.entity.living.EntityDanmakuCreeper;
import thkaguyamod.entity.living.EntityFamiliar;
import thkaguyamod.entity.living.EntityMiko;
import thkaguyamod.entity.living.EntityReimu;
import thkaguyamod.entity.living.EntityRinnosuke;
import thkaguyamod.entity.living.EntityRumia;
import thkaguyamod.entity.living.EntitySakuya;
import thkaguyamod.entity.living.EntitySanae;
import thkaguyamod.entity.living.EntitySunFlowerFairy;
import thkaguyamod.entity.living.EntityTHFairy;
import thkaguyamod.entity.living.EntityTHPhantom;
import thkaguyamod.entity.living.EntityToziko;
import thkaguyamod.entity.living.EntityWriggle;
import thkaguyamod.entity.shot.EntityDragonNeckJewel;
import thkaguyamod.entity.shot.EntityHomingAmulet;
import thkaguyamod.entity.shot.EntityMusouFuuin;
import thkaguyamod.entity.shot.EntityNuclearShot;
import thkaguyamod.entity.shot.EntityOnmyoudama;
import thkaguyamod.entity.shot.EntitySanaeWind;
import thkaguyamod.entity.shot.EntityTHLaser;
import thkaguyamod.entity.shot.EntityTHSetLaser;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.spellcard.EntitySpellCard;

/**
 * Entity registry. Tracking ranges / update intervals follow the 1.7.10
 * {@code EntityRegistry.registerModEntity} calls (CommonProxy.java:158-216).
 *
 * <p>1.7.10 registered every mod entity with its own global id and all of them were
 * saved by default; only {@code miracle_circle} lacked NBT, so it gets {@code noSave()}.
 * Boss/fairy mobs use {@link MobCategory#MONSTER}; Sanae/Sakuya/Rinnosuke keep 1.7.10's
 * {@code EnumCreatureType.ambient} (their biome modifiers depend on it).
 *
 * <p># perf: A3 — {@code clientTrackingRange} の単位は 1.21.1 では「チャンク」で、
 * {@code ChunkMap.addEntity} が {@code clientTrackingRange() * 16} を追跡距離（ブロック）として使う
 * （{@code MinecraftServer.getScaledTrackingDistance} は本バージョンでは恒等）。
 * 1.7.10 の {@code registerModEntity(..., trackingRange, ...)} はブロック単位だったため、
 * 移植時にそのまま持ち込んだ 40 / 80 / 250 / 60 は 16 倍（640 / 1280 / 4000 / 960 ブロック）に
 * なっていた。ここでは 1.7.10 の値を 16 で割った 3 / 5 / 16 / 4 チャンクに補正して、
 * 「弾は 48 ブロック、ボスや大型弾は 80 ブロック、金閣寺は 256 ブロック」という元の意味に戻す。
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, THKaguyaMod.MOD_ID);

    /* ---------------- 弾幕（entity/shot） ---------------- */

    public static final Supplier<EntityType<EntityTHShot>> NORMAL_SHOT = ENTITY_TYPES.register(
            "normal_shot", () -> EntityType.Builder.<EntityTHShot>of(EntityTHShot::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(3).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityTHLaser>> NORMAL_LASER = ENTITY_TYPES.register(
            "normal_laser", () -> EntityType.Builder.<EntityTHLaser>of(EntityTHLaser::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(3).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityTHSetLaser>> SET_LASER = ENTITY_TYPES.register(
            "set_laser", () -> EntityType.Builder.<EntityTHSetLaser>of(EntityTHSetLaser::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(3).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityHomingAmulet>> HOMING_AMULET = ENTITY_TYPES.register(
            "homing_amulet", () -> EntityType.Builder.<EntityHomingAmulet>of(EntityHomingAmulet::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(5).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityMusouFuuin>> MUSOU_FUUIN = ENTITY_TYPES.register(
            "musou_fuuin", () -> EntityType.Builder.<EntityMusouFuuin>of(EntityMusouFuuin::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(5).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityNuclearShot>> NUCLEAR_SHOT = ENTITY_TYPES.register(
            "nuclear_shot", () -> EntityType.Builder.<EntityNuclearShot>of(EntityNuclearShot::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(5).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityOnmyoudama>> ONMYOUDAMA = ENTITY_TYPES.register(
            "onmyoudama", () -> EntityType.Builder.<EntityOnmyoudama>of(EntityOnmyoudama::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(5).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntitySanaeWind>> SANAE_WIND = ENTITY_TYPES.register(
            "sanae_wind", () -> EntityType.Builder.<EntitySanaeWind>of(EntitySanaeWind::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(5).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityDragonNeckJewel>> DRAGON_NECK_JEWEL = ENTITY_TYPES.register(
            "dragon_neck_jewel", () -> EntityType.Builder.<EntityDragonNeckJewel>of(EntityDragonNeckJewel::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(5).updateInterval(1).noSave().build(""));

    /* ---------------- ドロップ品（entity/） ---------------- */

    public static final Supplier<EntityType<EntityShotMaterial>> SHOT_MATERIAL_ENTITY = ENTITY_TYPES.register(
            "shot_material", () -> EntityType.Builder.<EntityShotMaterial>of(EntityShotMaterial::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityTHItem>> TH_ITEM = ENTITY_TYPES.register(
            "th_item", () -> EntityType.Builder.<EntityTHItem>of(EntityTHItem::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityDivineSpirit>> DIVINE_SPIRIT = ENTITY_TYPES.register(
            "divine_spirit", () -> EntityType.Builder.<EntityDivineSpirit>of(EntityDivineSpirit::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(5).updateInterval(1).build(""));

    /* ---------------- 符卡（spellcard/ + entity/effect/） ---------------- */

    public static final Supplier<EntityType<EntitySpellCard>> SPELL_CARD = ENTITY_TYPES.register(
            "spell_card", () -> EntityType.Builder.<EntitySpellCard>of(EntitySpellCard::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(3).updateInterval(5).noSave().build(""));

    public static final Supplier<EntityType<EntitySpellCardCircle>> SPELL_CARD_CIRCLE = ENTITY_TYPES.register(
            "spell_card_circle", () -> EntityType.Builder.<EntitySpellCardCircle>of(EntitySpellCardCircle::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(5).updateInterval(1).noSave().build(""));

    /* ---------------- 道具实体（entity/item） ---------------- */

    public static final Supplier<EntityType<EntitySakuyaWatch>> SAKUYA_WATCH = ENTITY_TYPES.register(
            "private_square", () -> EntityType.Builder.<EntitySakuyaWatch>of(EntitySakuyaWatch::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntitySakuyaStopWatch>> SAKUYA_STOP_WATCH = ENTITY_TYPES.register(
            "sakuya_stop_watch", () -> EntityType.Builder.<EntitySakuyaStopWatch>of(EntitySakuyaStopWatch::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntitySilverKnife>> SILVER_KNIFE = ENTITY_TYPES.register(
            "silver_knife", () -> EntityType.Builder.<EntitySilverKnife>of(EntitySilverKnife::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityKinkakuzi>> KINKAKUZI = ENTITY_TYPES.register(
            "kinkakuzi", () -> EntityType.Builder.<EntityKinkakuzi>of(EntityKinkakuzi::new, MobCategory.MISC)
                    .sized(6.0F, 0.3F).clientTrackingRange(16).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityMarisaBroom>> MARISA_BROOM = ENTITY_TYPES.register(
            "marisa_broom", () -> EntityType.Builder.<EntityMarisaBroom>of(EntityMarisaBroom::new, MobCategory.MISC)
                    .sized(0.9F, 0.5F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityMiniHakkero>> MINI_HAKKERO = ENTITY_TYPES.register(
            "mini_hakkero", () -> EntityType.Builder.<EntityMiniHakkero>of(EntityMiniHakkero::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(3).updateInterval(5).build(""));

    public static final Supplier<EntityType<EntityHisou>> HISOU = ENTITY_TYPES.register(
            "hisou", () -> EntityType.Builder.<EntityHisou>of(EntityHisou::new, MobCategory.MISC)
                    .sized(4.4F, 4.4F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityPendulum>> PENDULUM = ENTITY_TYPES.register(
            "nazrin_pendulum", () -> EntityType.Builder.<EntityPendulum>of(EntityPendulum::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntitySukima>> SUKIMA = ENTITY_TYPES.register(
            "sukima", () -> EntityType.Builder.<EntitySukima>of(EntitySukima::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(3).updateInterval(5).build(""));

    public static final Supplier<EntityType<EntityMazinkyoukan>> MAZINKYOUKAN = ENTITY_TYPES.register(
            "mazinkyoukan", () -> EntityType.Builder.<EntityMazinkyoukan>of(EntityMazinkyoukan::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntitySpiritualStrikeTalisman>> SPIRITUAL_STRIKE_TALISMAN = ENTITY_TYPES.register(
            "spiritual_strike_talisman", () -> EntityType.Builder.<EntitySpiritualStrikeTalisman>of(EntitySpiritualStrikeTalisman::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).clientTrackingRange(5).updateInterval(10).build(""));

    public static final Supplier<EntityType<EntityYuukaParasol>> YUUKA_PARASOL = ENTITY_TYPES.register(
            "yuuka_parasol", () -> EntityType.Builder.<EntityYuukaParasol>of(EntityYuukaParasol::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityCursedDecoyDoll>> CURSED_DECOY_DOLL = ENTITY_TYPES.register(
            "cursed_decoy_doll", () -> EntityType.Builder.<EntityCursedDecoyDoll>of(EntityCursedDecoyDoll::new, MobCategory.MISC)
                    .sized(1.0F, 1.8F).clientTrackingRange(4).updateInterval(5).build(""));

    /* ---------------- 效果实体（entity/effect） ---------------- */

    public static final Supplier<EntityType<EntityAjaRedStoneEffect>> AJA_RED_STONE_EFFECT = ENTITY_TYPES.register(
            "aja_red_stone_effect", () -> EntityType.Builder.<EntityAjaRedStoneEffect>of(EntityAjaRedStoneEffect::new, MobCategory.MISC)
                    .sized(2.0F, 0.0F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityCirnoIceBox>> CIRNO_ICE_BOX = ENTITY_TYPES.register(
            "cirno_ice_box", () -> EntityType.Builder.<EntityCirnoIceBox>of(EntityCirnoIceBox::new, MobCategory.MISC)
                    .sized(1.8F, 1.8F).clientTrackingRange(5).updateInterval(10).build(""));

    public static final Supplier<EntityType<EntityHakureiShield>> HAKUREI_SHIELD = ENTITY_TYPES.register(
            "hakurei_shield", () -> EntityType.Builder.<EntityHakureiShield>of(EntityHakureiShield::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityHakurouReflecter>> HAKUROU_REFLECTER = ENTITY_TYPES.register(
            "hakurou_reflecter", () -> EntityType.Builder.<EntityHakurouReflecter>of(EntityHakurouReflecter::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F).clientTrackingRange(3).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityMiracleCircle>> MIRACLE_CIRCLE = ENTITY_TYPES.register(
            "miracle_circle", () -> EntityType.Builder.<EntityMiracleCircle>of(EntityMiracleCircle::new, MobCategory.MISC)
                    .sized(2.0F, 0.0F).clientTrackingRange(3).updateInterval(1).noSave().build(""));

    /* ---------------- 生物（entity/living） ---------------- */

    public static final Supplier<EntityType<EntityTHFairy>> TH_FAIRY = ENTITY_TYPES.register(
            "th_fairy", () -> EntityType.Builder.<EntityTHFairy>of(EntityTHFairy::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.5F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntitySunFlowerFairy>> SUNFLOWER_FAIRY = ENTITY_TYPES.register(
            "sunflower_fairy", () -> EntityType.Builder.<EntitySunFlowerFairy>of(EntitySunFlowerFairy::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.7F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityTHPhantom>> TH_PHANTOM = ENTITY_TYPES.register(
            "th_phantom", () -> EntityType.Builder.<EntityTHPhantom>of(EntityTHPhantom::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityFamiliar>> FAMILIAR = ENTITY_TYPES.register(
            "familiar", () -> EntityType.Builder.<EntityFamiliar>of(EntityFamiliar::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.6F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityCirno>> CIRNO = ENTITY_TYPES.register(
            "cirno", () -> EntityType.Builder.<EntityCirno>of(EntityCirno::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.8F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityRumia>> RUMIA = ENTITY_TYPES.register(
            "rumia", () -> EntityType.Builder.<EntityRumia>of(EntityRumia::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.8F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityToziko>> TOZIKO = ENTITY_TYPES.register(
            "toziko", () -> EntityType.Builder.<EntityToziko>of(EntityToziko::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.8F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntitySanae>> SANAE = ENTITY_TYPES.register(
            "sanae", () -> EntityType.Builder.<EntitySanae>of(EntitySanae::new, MobCategory.AMBIENT)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityReimu>> REIMU = ENTITY_TYPES.register(
            "reimu", () -> EntityType.Builder.<EntityReimu>of(EntityReimu::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.8F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityMiko>> MIKO = ENTITY_TYPES.register(
            "miko", () -> EntityType.Builder.<EntityMiko>of(EntityMiko::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.8F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityWriggle>> WRIGGLE = ENTITY_TYPES.register(
            "wriggle", () -> EntityType.Builder.<EntityWriggle>of(EntityWriggle::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.8F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntitySakuya>> SAKUYA = ENTITY_TYPES.register(
            "sakuya", () -> EntityType.Builder.<EntitySakuya>of(EntitySakuya::new, MobCategory.AMBIENT)
                    .sized(1.0F, 1.8F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityRinnosuke>> RINNOSUKE = ENTITY_TYPES.register(
            "rinnosuke", () -> EntityType.Builder.<EntityRinnosuke>of(EntityRinnosuke::new, MobCategory.AMBIENT)
                    .sized(0.6F, 1.95F).clientTrackingRange(5).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityDanmakuCreeper>> DANMAKU_CREEPER = ENTITY_TYPES.register(
            "danmaku_creeper", () -> EntityType.Builder.<EntityDanmakuCreeper>of(EntityDanmakuCreeper::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).updateInterval(1).build(""));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private ModEntities() {}
}
