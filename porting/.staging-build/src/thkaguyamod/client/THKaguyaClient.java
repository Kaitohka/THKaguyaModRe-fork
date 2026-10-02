package thkaguyamod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.client.gui.screen.DanmakuCraftingScreen;
import thkaguyamod.client.gui.screen.DanmakuLaserCraftingScreen;
import thkaguyamod.client.renderer.NoopRenderer;
import thkaguyamod.client.renderer.shot.RenderTHLaser;
import thkaguyamod.client.renderer.shot.RenderTHShot;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModMenus;

/** Client-only entry: screen + entity renderer registration (replaces the 1.7.10 ClientProxy). */
@Mod(value = THKaguyaMod.MOD_ID, dist = Dist.CLIENT)
public class THKaguyaClient {

    public THKaguyaClient(IEventBus modEventBus) {
        modEventBus.addListener(RegisterMenuScreensEvent.class, event -> {
            event.register(ModMenus.DANMAKU_CRAFTING.get(), DanmakuCraftingScreen::new);
            event.register(ModMenus.DANMAKU_LASER_CRAFTING.get(), DanmakuLaserCraftingScreen::new);
        });

        modEventBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            // Danmaku core renderers (ported from RenderTHShot / RenderTHLaser)
            event.registerEntityRenderer(ModEntities.NORMAL_SHOT.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.NORMAL_LASER.get(), RenderTHLaser::new);
            event.registerEntityRenderer(ModEntities.SET_LASER.get(), RenderTHLaser::new);
            // Shot subclasses share the base renderer until their dedicated renderers are ported
            event.registerEntityRenderer(ModEntities.HOMING_AMULET.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.MUSOU_FUUIN.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.NUCLEAR_SHOT.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.ONMYOUDAMA.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.SANAE_WIND.get(), RenderTHShot::new);
            event.registerEntityRenderer(ModEntities.DRAGON_NECK_JEWEL.get(), RenderTHShot::new);
            // Entities whose renderers land in a later batch get a no-op placeholder
            event.registerEntityRenderer(ModEntities.SHOT_MATERIAL_ENTITY.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModEntities.TH_ITEM.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModEntities.DIVINE_SPIRIT.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModEntities.SPELL_CARD.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModEntities.SPELL_CARD_CIRCLE.get(), NoopRenderer::new);
        });
    }
}
