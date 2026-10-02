package thkaguyamod.init;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.menu.DanmakuCraftingMenu;
import thkaguyamod.menu.DanmakuLaserCraftingMenu;
import thkaguyamod.menu.MerchantSanaeMenu;
import thkaguyamod.menu.MiracleMalletMenu;

/** Menu registry (1.7.10: numeric GUI ids via IGuiHandler). */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, THKaguyaMod.MOD_ID);

    public static final Supplier<MenuType<DanmakuCraftingMenu>> DANMAKU_CRAFTING = MENU_TYPES.register(
            "danmaku_crafting", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new DanmakuCraftingMenu(containerId, inventory)));

    public static final Supplier<MenuType<DanmakuLaserCraftingMenu>> DANMAKU_LASER_CRAFTING = MENU_TYPES.register(
            "danmaku_laser_crafting", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new DanmakuLaserCraftingMenu(containerId, inventory)));

    public static final Supplier<MenuType<MerchantSanaeMenu>> MERCHANT_SANAE = MENU_TYPES.register(
            "merchant_sanae", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new MerchantSanaeMenu(containerId, inventory)));

    public static final Supplier<MenuType<MiracleMalletMenu>> MIRACLE_MALLET = MENU_TYPES.register(
            "miracle_mallet", () -> IMenuTypeExtension.create(
                    (containerId, inventory, data) -> new MiracleMalletMenu(containerId, inventory)));

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }

    private ModMenus() {}
}
