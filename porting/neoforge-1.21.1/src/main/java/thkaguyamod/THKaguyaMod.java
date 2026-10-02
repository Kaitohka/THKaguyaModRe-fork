package thkaguyamod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thkaguyamod.config.ModConfig;
import thkaguyamod.entity.living.EntityCirno;
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
import thkaguyamod.event.PlayerDeathEventHandler;
import thkaguyamod.init.ModArmorMaterials;
import thkaguyamod.init.ModBlockEntities;
import thkaguyamod.init.ModBlocks;
import thkaguyamod.init.ModCreativeTabs;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModDispenserBehaviors;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModMenus;
import thkaguyamod.init.ModNetworking;
import thkaguyamod.init.ModSounds;
import thkaguyamod.init.ModSpawns;
import thkaguyamod.init.ModSpellCards;

/**
 * 五つの難題MOD+ (Itutu no Nandai MOD+) — NeoForge 1.21.1 port.
 *
 * <p>Ported from the 1.7.10 Forge mod "THKaguyaMod" v2.90 (modid {@code THKaguyaMod}).
 * The modid is lowercased to {@code thkaguyamod} because modern FML requires lowercase
 * modids; this matches the resource domain already used by the original mod.
 */
@Mod(THKaguyaMod.MOD_ID)
public class THKaguyaMod {

    public static final String MOD_ID = "thkaguyamod";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public THKaguyaMod(IEventBus modEventBus, ModContainer modContainer) {
        ModArmorMaterials.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        ModSpellCards.registerSpellCards();

        modContainer.registerConfig(Type.SERVER, ModConfig.SPEC);
        modEventBus.addListener(ModConfigEvent.Loading.class, event -> ModConfig.sync());
        modEventBus.addListener(ModConfigEvent.Reloading.class, event -> ModConfig.sync());
        modEventBus.addListener(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class,
                ModNetworking::register);

        NeoForge.EVENT_BUS.register(new PlayerDeathEventHandler());

        modEventBus.addListener(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent.class, event -> {
            event.put(ModEntities.TH_FAIRY.get(), EntityTHFairy.createAttributes().build());
            event.put(ModEntities.SUNFLOWER_FAIRY.get(), EntitySunFlowerFairy.createAttributes().build());
            event.put(ModEntities.TH_PHANTOM.get(), EntityTHPhantom.createAttributes().build());
            event.put(ModEntities.FAMILIAR.get(), EntityFamiliar.createAttributes().build());
            event.put(ModEntities.CIRNO.get(), EntityCirno.createAttributes().build());
            event.put(ModEntities.RUMIA.get(), EntityRumia.createAttributes().build());
            event.put(ModEntities.TOZIKO.get(), EntityToziko.createAttributes().build());
            event.put(ModEntities.SANAE.get(), EntitySanae.createAttributes().build());
            event.put(ModEntities.REIMU.get(), EntityReimu.createAttributes().build());
            event.put(ModEntities.MIKO.get(), EntityMiko.createAttributes().build());
            event.put(ModEntities.WRIGGLE.get(), EntityWriggle.createAttributes().build());
            event.put(ModEntities.SAKUYA.get(), EntitySakuya.createAttributes().build());
            event.put(ModEntities.RINNOSUKE.get(), EntityRinnosuke.createAttributes().build());
            event.put(ModEntities.DANMAKU_CREEPER.get(), net.minecraft.world.entity.monster.Creeper.createAttributes().build());
            event.put(ModEntities.CURSED_DECOY_DOLL.get(), net.minecraft.world.entity.PathfinderMob.createMobAttributes().build());
        });

        ModSpawns.register(modEventBus);
        modEventBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class,
                event -> event.enqueueWork(ModDispenserBehaviors::register));

        LOGGER.info("Itutu no Nandai MOD+ (NeoForge port) loading.");
    }
}
