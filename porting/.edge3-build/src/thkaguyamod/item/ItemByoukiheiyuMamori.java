package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 病気平癒守 (Illness Recovery Charm) — ported from 1.7.10 {@code thKaguyaMod.item.ItemByoukiheiyuMamori}.
 *
 * <p>Clears every active potion effect, then grants Regeneration for 3 s at amplifier 3
 * (== Regeneration IV). 24-tick bow-style use; fails while the user's {@code hurtTime} is active.
 * The effect triggers on drink completion (1.7.10 {@code onEaten} → 1.21.1 {@link #finishUsingItem}).
 */
public class ItemByoukiheiyuMamori extends Item {

    public ItemByoukiheiyuMamori(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // クリエイティブ時は原作同様に即時発動（消費はされない）
        if (player.isCreative()) {
            applyEffect(level, player);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return super.finishUsingItem(stack, level, livingEntity);
        }
        // ダメージを受けている状態だと失敗する
        if (player.hurtTime > 0) {
            return stack;
        }
        applyEffect(level, player);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return stack;
    }

    private void applyEffect(Level level, Player player) {
        // 全てのポーション効果を消す
        player.removeAllEffects();
        if (!level.isClientSide) {
            int effectTime = 3 * 20;// 3秒
            int potionLevel = 3;// 0 がレベル1 なので 3 == レベル4
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, effectTime, potionLevel));
        }
    }

    /** 右クリックを押している時間の最大値。 */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    /** 使用時のアクション（bow 動作）。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
