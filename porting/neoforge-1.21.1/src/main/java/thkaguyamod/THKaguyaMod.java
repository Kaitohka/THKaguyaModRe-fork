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
import thkaguyamod.event.PlayerDeathEventHandler;
import thkaguyamod.init.ModArmorMaterials;
import thkaguyamod.init.ModBlockEntities;
import thkaguyamod.init.ModBlocks;
import thkaguyamod.init.ModCreativeTabs;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModMenus;
import thkaguyamod.init.ModNetworking;
import thkaguyamod.init.ModSounds;
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

        LOGGER.info("Itutu no Nandai MOD+ (NeoForge port) loading.");
    }
}
