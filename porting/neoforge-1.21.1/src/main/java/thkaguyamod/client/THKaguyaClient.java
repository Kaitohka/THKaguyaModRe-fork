package thkaguyamod.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.client.gui.screen.DanmakuCraftingScreen;
import thkaguyamod.client.gui.screen.DanmakuLaserCraftingScreen;
import thkaguyamod.client.gui.screen.MerchantSanaeScreen;
import thkaguyamod.client.gui.screen.MiracleMalletScreen;
import thkaguyamod.client.model.ModelHisou;
import thkaguyamod.client.model.ModelKinkakuzi;
import thkaguyamod.client.model.ModelMarisaBroom;
import thkaguyamod.client.model.ModelMarisaHat;
import thkaguyamod.client.model.ModelMiniHakkero;
import thkaguyamod.client.model.ModelMiniHakkero2;
import thkaguyamod.client.model.ModelPrivateSquare;
import thkaguyamod.client.model.ModelSilverKnife;
import thkaguyamod.client.model.ModelSuwakoHat;
import thkaguyamod.client.model.ModelYuukaParasol;
import thkaguyamod.client.model.living.ModelCirno;
import thkaguyamod.client.model.living.ModelCursedDecoyDoll;
import thkaguyamod.client.model.living.ModelMiko;
import thkaguyamod.client.model.living.ModelReimu;
import thkaguyamod.client.model.living.ModelRinnosuke;
import thkaguyamod.client.model.living.ModelRumia;
import thkaguyamod.client.model.living.ModelSakuya;
import thkaguyamod.client.model.living.ModelSanae;
import thkaguyamod.client.model.living.ModelSunFlowerFairy;
import thkaguyamod.client.model.living.ModelTHFairy;
import thkaguyamod.client.model.living.ModelTHFairyCirno2;
import thkaguyamod.client.model.living.ModelToziko;
import thkaguyamod.client.model.living.ModelWriggle;
import thkaguyamod.client.renderer.RenderAjaRedStoneEffect;
import thkaguyamod.client.renderer.RenderCirnoIceBox;
import thkaguyamod.client.renderer.RenderDivineSpirit;
import thkaguyamod.client.renderer.RenderHisou;
import thkaguyamod.client.renderer.RenderKinkakuzi;
import thkaguyamod.client.renderer.RenderMarisaBroom;
import thkaguyamod.client.renderer.RenderMazinkyoukan;
import thkaguyamod.client.renderer.RenderMiniHakkero;
import thkaguyamod.client.renderer.RenderPendulum;
import thkaguyamod.client.renderer.RenderPrivateSquare;
import thkaguyamod.client.renderer.RenderSakuyaStopWatch;
import thkaguyamod.client.renderer.RenderShotMaterial;
import thkaguyamod.client.renderer.RenderSilverKnife;
import thkaguyamod.client.renderer.RenderSpellCard;
import thkaguyamod.client.renderer.RenderSpiritualStrikeTalisman;
import thkaguyamod.client.renderer.RenderSukima;
import thkaguyamod.client.renderer.RenderTHItem;
import thkaguyamod.client.renderer.RenderYuukaParasol;
import thkaguyamod.client.renderer.effect.RenderHakureiShield;
import thkaguyamod.client.renderer.effect.RenderHakurouReflecter;
import thkaguyamod.client.renderer.effect.RenderMiracleCircle;
import thkaguyamod.client.renderer.effect.RenderSpellCardCircle;
import thkaguyamod.client.renderer.living.RenderCirno;
import thkaguyamod.client.renderer.living.RenderCursedDecoyDoll;
import thkaguyamod.client.renderer.living.RenderFamiliar;
import thkaguyamod.client.renderer.living.RenderMiko;
import thkaguyamod.client.renderer.living.RenderReimu;
import thkaguyamod.client.renderer.living.RenderRinnosuke;
import thkaguyamod.client.renderer.living.RenderRumia;
import thkaguyamod.client.renderer.living.RenderSakuya;
import thkaguyamod.client.renderer.living.RenderSanae;
import thkaguyamod.client.renderer.living.RenderSunFlowerFairy;
import thkaguyamod.client.renderer.living.RenderTHFairy;
import thkaguyamod.client.renderer.living.RenderTHPhantom;
import thkaguyamod.client.renderer.living.RenderToziko;
import thkaguyamod.client.renderer.living.RenderWriggle;
import thkaguyamod.client.renderer.shot.RenderTHLaser;
import thkaguyamod.client.renderer.shot.RenderTHShot;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModMenus;

/** Client-only entry: screens, entity renderers, model layer definitions. */
@Mod(value = THKaguyaMod.MOD_ID, dist = Dist.CLIENT)
public class THKaguyaClient {

    /* -------- model layers -------- */
    public static final ModelLayerLocation HISOU = layer("hisou");
    public static final ModelLayerLocation KINKAKUZI = layer("kinkakuzi");
    public static final ModelLayerLocation MARISA_BROOM = layer("marisa_broom");
    public static final ModelLayerLocation MINI_HAKKERO = layer("mini_hakkero");
    public static final ModelLayerLocation MINI_HAKKERO_2 = layer("mini_hakkero2");
    public static final ModelLayerLocation PRIVATE_SQUARE = layer("private_square");
    public static final ModelLayerLocation SILVER_KNIFE = layer("silver_knife");
    public static final ModelLayerLocation YUUKA_PARASOL = layer("yuuka_parasol");
    public static final ModelLayerLocation MARISA_HAT = layer("marisa_hat");
    public static final ModelLayerLocation SUWAKO_HAT = layer("suwako_hat");

    public static final ModelLayerLocation CIRNO = layer("cirno");
    public static final ModelLayerLocation RUMIA = layer("rumia");
    public static final ModelLayerLocation REIMU = layer("reimu");
    public static final ModelLayerLocation SAKUYA = layer("sakuya");
    public static final ModelLayerLocation MIKO = layer("miko");
    public static final ModelLayerLocation SANAE = layer("sanae");
    public static final ModelLayerLocation TOZIKO = layer("toziko");
    public static final ModelLayerLocation RINNOSUKE = layer("rinnosuke");
    public static final ModelLayerLocation TH_FAIRY = layer("th_fairy");
    public static final ModelLayerLocation SUNFLOWER_FAIRY = layer("sun_flower_fairy");
    public static final ModelLayerLocation WRIGGLE = layer("wriggle");
    public static final ModelLayerLocation CURSED_DECOY_DOLL = layer("cursed_decoy_doll");
    public static final ModelLayerLocation TH_FAIRY_CIRNO2 = layer("th_fairy_cirno2");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, name), "main");
    }

    public THKaguyaClient(IEventBus modEventBus) {
        modEventBus.addListener(RegisterMenuScreensEvent.class, event -> {
            event.register(ModMenus.DANMAKU_CRAFTING.get(), DanmakuCraftingScreen::new);
            event.register(ModMenus.DANMAKU_LASER_CRAFTING.get(), DanmakuLaserCraftingScreen::new);
            event.register(ModMenus.MERCHANT_SANAE.get(), MerchantSanaeScreen::new);
            event.register(ModMenus.MIRACLE_MALLET.get(), MiracleMalletScreen::new);
        });

        modEventBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
            event.registerLayerDefinition(HISOU, ModelHisou::createBodyLayer);
            event.registerLayerDefinition(KINKAKUZI, ModelKinkakuzi::createBodyLayer);
            event.registerLayerDefinition(MARISA_BROOM, ModelMarisaBroom::createBodyLayer);
            event.registerLayerDefinition(MINI_HAKKERO, ModelMiniHakkero::createBodyLayer);
            event.registerLayerDefinition(MINI_HAKKERO_2, ModelMiniHakkero2::createBodyLayer);
            event.registerLayerDefinition(PRIVATE_SQUARE, ModelPrivateSquare::createBodyLayer);
            event.registerLayerDefinition(SILVER_KNIFE, ModelSilverKnife::createBodyLayer);
            event.registerLayerDefinition(YUUKA_PARASOL, ModelYuukaParasol::createBodyLayer);
            event.registerLayerDefinition(MARISA_HAT, ModelMarisaHat::createBodyLayer);
            event.registerLayerDefinition(SUWAKO_HAT, ModelSuwakoHat::createBodyLayer);
            event.registerLayerDefinition(CIRNO, ModelCirno::createBodyLayer);
            event.registerLayerDefinition(RUMIA, ModelRumia::createBodyLayer);
            event.registerLayerDefinition(REIMU, ModelReimu::createBodyLayer);
            event.registerLayerDefinition(SAKUYA, ModelSakuya::createBodyLayer);
            event.registerLayerDefinition(MIKO, ModelMiko::createBodyLayer);
            event.registerLayerDefinition(SANAE, ModelSanae::createBodyLayer);
            event.registerLayerDefinition(TOZIKO, ModelToziko::createBodyLayer);
            event.registerLayerDefinition(RINNOSUKE, ModelRinnosuke::createBodyLayer);
            event.registerLayerDefinition(TH_FAIRY, ModelTHFairy::createBodyLayer);
            event.registerLayerDefinition(SUNFLOWER_FAIRY, ModelSunFlowerFairy::createBodyLayer);
            event.registerLayerDefinition(WRIGGLE, ModelWriggle::createBodyLayer);
            event.registerLayerDefinition(CURSED_DECOY_DOLL, ModelCursedDecoyDoll::createBodyLayer);
            event.registerLayerDefinition(TH_FAIRY_CIRNO2, ModelTHFairyCirno2::createBodyLayer);
        });

        modEventBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            // 弾幕
            event.registerEntityRenderer(ModEntities.NORMAL_SHOT.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.NORMAL_LASER.get(), RenderTHLaser::new);
            event.registerEntityRenderer(ModEntities.SET_LASER.get(), RenderTHLaser::new);
            event.registerEntityRenderer(ModEntities.HOMING_AMULET.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.MUSOU_FUUIN.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.NUCLEAR_SHOT.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.ONMYOUDAMA.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.SANAE_WIND.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.DRAGON_NECK_JEWEL.get(), RenderTHShot::new);
            // ドロップ品
            event.registerEntityRenderer(ModEntities.SHOT_MATERIAL_ENTITY.get(), RenderShotMaterial::new);
            event.registerEntityRenderer(ModEntities.TH_ITEM.get(), RenderTHItem::new);
            event.registerEntityRenderer(ModEntities.DIVINE_SPIRIT.get(), RenderDivineSpirit::new);
            // 符卡
            event.registerEntityRenderer(ModEntities.SPELL_CARD.get(), RenderSpellCard::new);
            event.registerEntityRenderer(ModEntities.SPELL_CARD_CIRCLE.get(), RenderSpellCardCircle::new);
            // 道具实体
            event.registerEntityRenderer(ModEntities.SAKUYA_WATCH.get(), RenderPrivateSquare::new);
            event.registerEntityRenderer(ModEntities.SAKUYA_STOP_WATCH.get(), RenderSakuyaStopWatch::new);
            event.registerEntityRenderer(ModEntities.SILVER_KNIFE.get(), RenderSilverKnife::new);
            event.registerEntityRenderer(ModEntities.KINKAKUZI.get(), RenderKinkakuzi::new);
            event.registerEntityRenderer(ModEntities.MARISA_BROOM.get(), RenderMarisaBroom::new);
            event.registerEntityRenderer(ModEntities.MINI_HAKKERO.get(), RenderMiniHakkero::new);
            event.registerEntityRenderer(ModEntities.HISOU.get(), RenderHisou::new);
            event.registerEntityRenderer(ModEntities.PENDULUM.get(), RenderPendulum::new);
            event.registerEntityRenderer(ModEntities.SUKIMA.get(), RenderSukima::new);
            event.registerEntityRenderer(ModEntities.MAZINKYOUKAN.get(), RenderMazinkyoukan::new);
            event.registerEntityRenderer(ModEntities.SPIRITUAL_STRIKE_TALISMAN.get(), RenderSpiritualStrikeTalisman::new);
            event.registerEntityRenderer(ModEntities.YUUKA_PARASOL.get(), RenderYuukaParasol::new);
            // 效果实体
            event.registerEntityRenderer(ModEntities.AJA_RED_STONE_EFFECT.get(), RenderAjaRedStoneEffect::new);
            event.registerEntityRenderer(ModEntities.CIRNO_ICE_BOX.get(), RenderCirnoIceBox::new);
            event.registerEntityRenderer(ModEntities.HAKUREI_SHIELD.get(), RenderHakureiShield::new);
            event.registerEntityRenderer(ModEntities.HAKUROU_REFLECTER.get(), RenderHakurouReflecter::new);
            event.registerEntityRenderer(ModEntities.MIRACLE_CIRCLE.get(), RenderMiracleCircle::new);
            // 生物
            event.registerEntityRenderer(ModEntities.TH_FAIRY.get(), RenderTHFairy::new);
            event.registerEntityRenderer(ModEntities.SUNFLOWER_FAIRY.get(), RenderSunFlowerFairy::new);
            event.registerEntityRenderer(ModEntities.TH_PHANTOM.get(), RenderTHPhantom::new);
            event.registerEntityRenderer(ModEntities.FAMILIAR.get(), RenderFamiliar::new);
            event.registerEntityRenderer(ModEntities.CIRNO.get(), RenderCirno::new);
            event.registerEntityRenderer(ModEntities.RUMIA.get(), RenderRumia::new);
            event.registerEntityRenderer(ModEntities.TOZIKO.get(), RenderToziko::new);
            event.registerEntityRenderer(ModEntities.SANAE.get(), RenderSanae::new);
            event.registerEntityRenderer(ModEntities.REIMU.get(), RenderReimu::new);
            event.registerEntityRenderer(ModEntities.MIKO.get(), RenderMiko::new);
            event.registerEntityRenderer(ModEntities.WRIGGLE.get(), RenderWriggle::new);
            event.registerEntityRenderer(ModEntities.SAKUYA.get(), RenderSakuya::new);
            event.registerEntityRenderer(ModEntities.RINNOSUKE.get(), RenderRinnosuke::new);
            event.registerEntityRenderer(ModEntities.CURSED_DECOY_DOLL.get(), RenderCursedDecoyDoll::new);
            // 弹幕苦力怕沿用原版渲染器（1.7.10 无自定义渲染器）
            event.registerEntityRenderer(ModEntities.DANMAKU_CREEPER.get(), CreeperRenderer::new);
        });
    }
}
