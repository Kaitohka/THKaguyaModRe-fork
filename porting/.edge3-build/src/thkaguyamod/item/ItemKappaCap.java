package thkaguyamod.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 河童の帽子 (Kappa Cap) — ported from 1.7.10 {@code thKaguyaMod.item.ItemKappaCap}.
 *
 * <p>In water the wearer moves 1.2× faster (capped at 0.5 per axis) and the cap repairs itself;
 * out of water it slowly dries out, losing durability, unless the wearer is wet.
 */
public class ItemKappaCap extends ArmorItem {

    public ItemKappaCap(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        // 頭スロットに着用されているときだけ発動
        if (player.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }

        if (player.isInWater()) {// 水中にいるなら
            Vec3 motion = player.getDeltaMovement();
            double mx = motion.x * 1.2D;
            double my = motion.y > 0.0D ? motion.y * 1.2D : motion.y;
            double mz = motion.z * 1.2D;
            double maxSpeed = 0.5D;
            if (mx > maxSpeed) {
                mx = maxSpeed;
            } else if (mx < -maxSpeed) {
                mx = -maxSpeed;
            }
            if (my > maxSpeed) {
                my = maxSpeed;
            }
            if (mz > maxSpeed) {
                mz = maxSpeed;
            } else if (mz < -maxSpeed) {
                mz = -maxSpeed;
            }
            player.setDeltaMovement(mx, my, mz);
            if (stack.isDamaged()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 3));// 耐久を3増やす
            }
        } else {// 地上にいるなら
            if (player.isInWaterOrRain()) {// 濡れているなら
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));// 耐久を1増やす
            } else if (stack.getDamageValue() < 498) {
                stack.setDamageValue(stack.getDamageValue() + 1);// 耐久を1減らす
            }
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
