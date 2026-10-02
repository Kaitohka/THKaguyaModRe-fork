package thkaguyamod.item;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 魔理沙の帽子 (Marisa's Hat) — ported from 1.7.10 {@code thKaguyaMod.item.ItemMarisaHat}.
 *
 * <p>Wearing it enlarges the wearer's item pickup reach: every tick all dropped
 * {@link ItemEntity}s within a 5-block box are forced to collide with (i.e. be picked up by)
 * the wearer.
 *
 * <p>TODO K3-review: the 1.7.10 custom {@code ModelMarisaHat} armor model / texture override has no
 * item-side hook in 1.21.1; armor rendering is client-side (see the port's client model code).
 */
public class ItemMarisaHat extends ArmorItem {

    public ItemMarisaHat(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // 頭スロットに着用されているときだけ発動
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }
        // 周囲5ブロックのEntityを取得
        AABB box = player.getBoundingBox().inflate(5.0D, 5.0D, 5.0D);
        List<ItemEntity> list = level.getEntitiesOfClass(ItemEntity.class, box, e -> true);
        for (ItemEntity item : list) {
            // 1.7.10 の onCollideWithPlayer → 1.21.1 は playerTouch
            item.playerTouch(player);
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
