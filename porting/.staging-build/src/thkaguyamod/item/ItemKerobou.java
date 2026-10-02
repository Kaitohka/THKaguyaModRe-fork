package thkaguyamod.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * 諏訪子の帽子 (Suwako's Hat) — ported from 1.7.10 {@code thKaguyaMod.item.ItemKerobou}.
 *
 * <p>Self-applies Thorns X so physical attackers are always punished. The original applied this in
 * {@code onUpdate} (anywhere the stack is carried) *and* in {@code onArmorTick}; both collapse into
 * {@link #inventoryTick} here.
 *
 * <p>TODO K3-review: vanilla 1.21.1 caps Thorns at III; the enchant component still stores level 10,
 * but gameplay hooks may clamp it. Kept at 10 to preserve the original numbers.
 * <p>TODO K3-review: the custom {@code ModelSuwakoHat} armor model has no 1.21.1 item-side hook.
 */
public class ItemKerobou extends ArmorItem {

    public ItemKerobou(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) {
            return;
        }
        // onUpdate / onArmorTick: エンチャントが付いていないなら刺の鎧10を付与（救済処置）
        if (!stack.isEnchanted()) {
            stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.THORNS), 10);
        }
    }

    /** エンチャント不可。 */
    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    /** 1.7.10 の isBookEnchantable 相当。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}
