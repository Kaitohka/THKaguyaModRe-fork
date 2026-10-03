package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import thkaguyamod.init.ModDataComponents;

/**
 * 悔悟の棒 (Rod of Remorse) — ported from 1.7.10 {@code thKaguyaMod.item.ItemKaigo}.
 *
 * <p>A sword that pins a target's health down to 75% when struck while above 75%. The "word"
 * state is one-shot: after triggering, the rod must be "recharged" by right-clicking with black
 * dye in the inventory.
 *
 * <p>1.7.10 kept the word state in the item's damage/subtype metadata (0 = word present,
 * 1 = used). This port stores it in the dedicated {@link ModDataComponents#ROD_OF_REMORSE_USED}
 * boolean component (absent = word present / usable, present = used), which is what the
 * {@code thkaguyamod:remorse_used} item-model predicate reads for the two-texture swap
 * ({@code kaigoStick_0} → {@code kaigoStick_1} in 1.7.10).
 *
 * <p>The {@code durability(2)} on {@link thkaguyamod.init.ModItems#ROD_OF_REMORSE} is no longer
 * load-bearing for this class; see the note there before removing it.
 */
public class ItemKaigo extends SwordItem {

    public ItemKaigo(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    /** 文字が書かれている（未使用）か。コンポーネント不在は未使用。 */
    private static boolean hasWord(ItemStack stack) {
        return !stack.getOrDefault(ModDataComponents.ROD_OF_REMORSE_USED.get(), false);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 文字が書かれている状態で、当てた相手の体力が75％以上なら
        if ((double) target.getMaxHealth() * 0.75D < (double) target.getHealth() && hasWord(stack)) {
            // 当てた相手の体力を75％にする
            target.setHealth((float) ((double) target.getMaxHealth() * 0.75D));
            // 文字が消える（1.7.10 の itemStack.setItemDamage(1) 相当）
            stack.set(ModDataComponents.ROD_OF_REMORSE_USED.get(), true);
            return true;
        }
        return false;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 1.7.10 の原作は hitEntity で耐久を減らしていなかったため、
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
        // イカスミを消費して効果を回復（1.7.10 の itemStack.setItemDamage(0) 相当）
        if (ikachan >= 0 && !hasWord(stack)) {
            player.getInventory().getItem(ikachan).shrink(1);
            stack.remove(ModDataComponents.ROD_OF_REMORSE_USED.get());
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
        return hasWord(stack);
    }
}
