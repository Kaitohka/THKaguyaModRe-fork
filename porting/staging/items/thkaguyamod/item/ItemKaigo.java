package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 悔悟の棒 (Rod of Remorse) — ported from 1.7.10 {@code thKaguyaMod.item.ItemKaigo}.
 *
 * <p>A sword that pins a target's health down to 75% when struck while above 75%. The "word"
 * state is one-shot: after triggering, the rod must be "recharged" by right-clicking with black
 * dye in the inventory.
 *
 * <p>1.7.10 kept the word state in the item's damage/subtype metadata (0 = word present,
 * 1 = used). This port maps that marker onto {@code ItemStack} damage value:
 * {@code getDamageValue() == 0} means usable, {@code == 1} means used. See also
 * {@link thkaguyamod.init.ModItems#ROD_OF_REMORSE}.
 *
 * <p><b>TODO K3-review (blocking):</b> {@code ItemStack.getDamageValue()}/{@code setDamageValue()}
 * clamp the stored value to {@code getMaxDamage()} (see NeoForge {@code IItemExtension.getDamage}).
 * {@code ROD_OF_REMORSE} is currently registered without {@code durability(...)}, so max damage is 0
 * and the 1-marker would clamp to 0 — the "used" state could never stick (and damage could not be
 * stored on an unbreakable item). To make this class work as-is, register the item with
 * {@code new Item.Properties().durability(1)} (no visible bar, still unbreakable in practice since
 * nothing calls hurtAndBreak), or store the marker in a dedicated data component and read it here.
 *
 * <p>TODO K3-review: the 1.7.10 icon swap used subtypes/damage for two textures. With the damage
 * value repurposed as the marker this still works only if the model predicates are driven by
 * {@code damage} 0/1; if the port instead drives the icon by a component, migrate the marker there.
 */
public class ItemKaigo extends SwordItem {

    public ItemKaigo(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 文字が書かれている状態（damage == 0）で、当てた相手の体力が75％以上なら
        if ((double) target.getMaxHealth() * 0.75D < (double) target.getHealth() && stack.getDamageValue() == 0) {
            // 当てた相手の体力を75％にする
            target.setHealth((float) ((double) target.getMaxHealth() * 0.75D));
            // 文字が消える
            stack.setDamageValue(1);
            return true;
        }
        return false;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 1.7.10 の原作は hitEntity で耐久を減らしていなかった（後述の TODO 参照）ため、
        // SwordItem 既定の hurtAndBreak(1) はここでは行わない。
        // TODO K3-review: 原作は「敵を75%にした時のみ」効果が発動し、耐久消費の記述が無い。
        //  バランス意図が「使用すると減る」なら postHurtEnemy で hurtAndBreak を呼ぶこと。
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // メインインベントリ（1.7.10 の mainInventory[36] 相当）から黒染料を1つ探す
        int ikachan = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (!inv.isEmpty() && inv.is(Items.BLACK_DYE)) {
                ikachan = i;
                break;
            }
        }
        // イカスミを消費して効果を回復
        if (ikachan >= 0 && stack.getDamageValue() == 1) {
            player.getInventory().getItem(ikachan).shrink(1);
            stack.setDamageValue(0);// 効果回復
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** エンチャント不可。 */
    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    /** 1.7.10 の isBookEnchantable 相当（エンチャント本を弾く）。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    /** 文字が書かれている（未使用）間は発光させる（1.7.10 の hasEffect 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getDamageValue() == 0;
    }
}
