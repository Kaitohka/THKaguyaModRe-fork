package thkaguyamod.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 陰陽玉 — 1.7.10 の {@code onArmorTick} 移植：防具スロット装着中は移動速度 ×1.2。
 * （旧版は素材アイテムだが防具スロットに置くと効果が出る仕様）
 */
public class ItemOnmyoudama extends Item {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ItemOnmyoudama(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) {
            return;
        }
        boolean worn = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot) == stack) {
                worn = true;
                break;
            }
        }
        if (!worn) {
            return;
        }
        player.setDeltaMovement(player.getDeltaMovement().multiply(1.2D, 1.0D, 1.2D));
    }
}
