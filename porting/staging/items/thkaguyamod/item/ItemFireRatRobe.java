package thkaguyamod.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * 火鼠の皮衣 (Robe of the Fire Rat) — ported from 1.7.10 {@code thKaguyaMod.item.ItemFireRatRobe}.
 *
 * <p>An unbreakable chestplate that keeps its wearer fire-safe: it self-applies Fire Protection V,
 * extinguishes the wearer every tick while worn, can be right-clicked to block/extinguish, and
 * sets struck entities on fire (when used as a weapon).
 *
 * <p>TODO K3-review: 1.7.10 applied {@code Enchantment.fireProtection} level 5. Vanilla 1.21.1 caps
 * Fire Protection at IV; the enchant component still stores level 5, but gameplay/max-level checks
 * may clamp it. Kept at 5 to preserve the original numbers.
 * <p>TODO K3-review: 1.7.10's {@code getArmorTexture}/{@code isFull3D}/{@code hasEffect(pass)} have no
 * 1.21.1 item-side equivalents — armor textures now come from the ArmorMaterial layers
 * ({@code hinezumi}) and the foil flag from {@link #isFoil(ItemStack)}.
 */
public class ItemFireRatRobe extends ArmorItem {

    public ItemFireRatRobe(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    /** 1.7.10 の hitEntity: 当たったEntityに着火。耐久無限のため postHurtEnemy は何もしない。 */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.igniteForSeconds(1.0F);// 1.7.10: setFire(20) == 20 tick
        return true;
    }

    /** 右クリックでガードの構え＋使用者の炎を消す。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        player.clearFire();// 1.7.10 の extinguish 相当
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        // onUpdate: エンチャントが付いていないなら炎耐性5を付与（救済処置）
        if (!stack.isEnchanted()) {
            stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FIRE_PROTECTION), 5);
        }
        // onArmorTick: 着用者の炎を消す（胴スロットにこのスタックを着ている場合）
        if (player.getItemBySlot(EquipmentSlot.CHEST) == stack) {
            player.clearFire();
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

    /** アイテムを常時発光させる（1.7.10 の hasEffect 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
