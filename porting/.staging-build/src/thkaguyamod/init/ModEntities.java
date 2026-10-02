package thkaguyamod.init;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.EntityDivineSpirit;
import thkaguyamod.entity.EntityShotMaterial;
import thkaguyamod.entity.EntityTHItem;
import thkaguyamod.entity.shot.EntityDragonNeckJewel;
import thkaguyamod.entity.shot.EntityHomingAmulet;
import thkaguyamod.entity.shot.EntityMusouFuuin;
import thkaguyamod.entity.shot.EntityNuclearShot;
import thkaguyamod.entity.shot.EntityOnmyoudama;
import thkaguyamod.entity.shot.EntitySanaeWind;
import thkaguyamod.entity.shot.EntityTHLaser;
import thkaguyamod.entity.shot.EntityTHSetLaser;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.entity.effect.EntitySpellCardCircle;
import thkaguyamod.spellcard.EntitySpellCard;

/**
 * Entity registry. Tracking ranges / update intervals follow the 1.7.10
 * {@code EntityRegistry.registerModEntity} calls. Danmaku entities are marked
 * {@code noSave()} — in 1.7.10 their NBT methods were empty (bullets vanish on
 * chunk reload by design).
 *
 * <p>Renamed: 1.7.10 "BrilliantDragonBullet" entity → {@code dragon_neck_jewel}
 * (matching the item that spawns it; the actual BrilliantDragonBullet spell card
 * was commented out in 1.7.10).
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, THKaguyaMod.MOD_ID);

    /* ---------------- 弾幕（entity/shot） ---------------- */

    public static final Supplier<EntityType<EntityTHShot>> NORMAL_SHOT = ENTITY_TYPES.register(
            "normal_shot", () -> EntityType.Builder.<EntityTHShot>of(EntityTHShot::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(40).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityTHLaser>> NORMAL_LASER = ENTITY_TYPES.register(
            "normal_laser", () -> EntityType.Builder.<EntityTHLaser>of(EntityTHLaser::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(40).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityTHSetLaser>> SET_LASER = ENTITY_TYPES.register(
            "set_laser", () -> EntityType.Builder.<EntityTHSetLaser>of(EntityTHSetLaser::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(40).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityHomingAmulet>> HOMING_AMULET = ENTITY_TYPES.register(
            "homing_amulet", () -> EntityType.Builder.<EntityHomingAmulet>of(EntityHomingAmulet::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(80).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityMusouFuuin>> MUSOU_FUUIN = ENTITY_TYPES.register(
            "musou_fuuin", () -> EntityType.Builder.<EntityMusouFuuin>of(EntityMusouFuuin::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(80).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityNuclearShot>> NUCLEAR_SHOT = ENTITY_TYPES.register(
            "nuclear_shot", () -> EntityType.Builder.<EntityNuclearShot>of(EntityNuclearShot::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(80).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityOnmyoudama>> ONMYOUDAMA = ENTITY_TYPES.register(
            "onmyoudama", () -> EntityType.Builder.<EntityOnmyoudama>of(EntityOnmyoudama::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(80).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntitySanaeWind>> SANAE_WIND = ENTITY_TYPES.register(
            "sanae_wind", () -> EntityType.Builder.<EntitySanaeWind>of(EntitySanaeWind::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(80).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<EntityDragonNeckJewel>> DRAGON_NECK_JEWEL = ENTITY_TYPES.register(
            "dragon_neck_jewel", () -> EntityType.Builder.<EntityDragonNeckJewel>of(EntityDragonNeckJewel::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(80).updateInterval(1).noSave().build(""));

    /* ---------------- 弾消しボーナス・ドロップ品（entity/） ----------------
     * これらはドロップアイテム相当のため永続化が必要。1.7.10 の registerModEntity と同様に
     * noSave() を付けない（位置・回転は Entity 基底の save/load に任せる）。
     */

    public static final Supplier<EntityType<EntityShotMaterial>> SHOT_MATERIAL_ENTITY = ENTITY_TYPES.register(
            "shot_material", () -> EntityType.Builder.<EntityShotMaterial>of(EntityShotMaterial::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(40).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityTHItem>> TH_ITEM = ENTITY_TYPES.register(
            "th_item", () -> EntityType.Builder.<EntityTHItem>of(EntityTHItem::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(40).updateInterval(1).build(""));

    public static final Supplier<EntityType<EntityDivineSpirit>> DIVINE_SPIRIT = ENTITY_TYPES.register(
            "divine_spirit", () -> EntityType.Builder.<EntityDivineSpirit>of(EntityDivineSpirit::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(80).updateInterval(1).build(""));

    /* ---------------- 符卡（spellcard/ + entity/effect/） ---------------- */

    public static final Supplier<EntityType<EntitySpellCard>> SPELL_CARD = ENTITY_TYPES.register(
            "spell_card", () -> EntityType.Builder.<EntitySpellCard>of(EntitySpellCard::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(40).updateInterval(5).noSave().build(""));

    public static final Supplier<EntityType<EntitySpellCardCircle>> SPELL_CARD_CIRCLE = ENTITY_TYPES.register(
            "spell_card_circle", () -> EntityType.Builder.<EntitySpellCardCircle>of(EntitySpellCardCircle::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(80).updateInterval(1).noSave().build(""));

    public static final Supplier<EntityType<thkaguyamod.entity.item.EntitySilverKnife>> SILVER_KNIFE = ENTITY_TYPES.register(
            "silver_knife", () -> EntityType.Builder.<thkaguyamod.entity.item.EntitySilverKnife>of(thkaguyamod.entity.item.EntitySilverKnife::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(40).updateInterval(1).build(""));

    public static final Supplier<EntityType<thkaguyamod.entity.effect.EntityMiracleCircle>> MIRACLE_CIRCLE = ENTITY_TYPES.register(
            "miracle_circle", () -> EntityType.Builder.<thkaguyamod.entity.effect.EntityMiracleCircle>of(thkaguyamod.entity.effect.EntityMiracleCircle::new, MobCategory.MISC)
                    .sized(2.0F, 0.0F).clientTrackingRange(40).updateInterval(1).noSave().build(""));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    /** Convenience for subclasses sharing the shot base registration shape. */
    static <T extends Entity> EntityType.Builder<T> shotBuilder(EntityType.EntityFactory<T> factory, float size) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(size, size).clientTrackingRange(80).updateInterval(1).noSave();
    }

    private ModEntities() {}
}
