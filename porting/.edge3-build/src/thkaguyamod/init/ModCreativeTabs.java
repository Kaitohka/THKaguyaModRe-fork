package thkaguyamod.init;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;

/**
 * Creative tabs. The 1.7.10 mod had a single custom tab ("SpellCard") and scattered the
 * rest across vanilla tabs; the port uses a main tab plus the spell card tab.
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, THKaguyaMod.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thkaguyamod"))
                    .icon(() -> new ItemStack(ModItems.JEWELED_BRANCH_OF_HOURAI.get()))
                    .displayItems((params, output) -> {
                        ModItems.MAIN_TAB.forEach(item -> output.accept(item.get()));
                        output.accept(ModBlocks.DANMAKU_CRAFTING_TABLE.get());
                        output.accept(ModBlocks.DANMAKU_LASER_CRAFTING_TABLE.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_RED.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_BLUE.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_GREEN.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_YELLOW.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_PURPLE.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_AQUA.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_ORANGE.get());
                        output.accept(ModBlocks.DIVINE_SPIRIT_WHITE.get());
                    })
                    .build());

    public static final Supplier<CreativeModeTab> SPELLCARD = CREATIVE_TABS.register("spellcard",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thkaguyamod.spellcard"))
                    .icon(() -> new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get()))
                    .displayItems((params, output) ->
                            ModItems.SPELLCARD_TAB.forEach(item -> output.accept(item.get())))
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }

    private ModCreativeTabs() {}
}
