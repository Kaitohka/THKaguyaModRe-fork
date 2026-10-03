package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 伊吹瓢 (Ibuki Gourd) — ported from 1.7.10 {@code thKaguyaMod.item.ItemIbukihyou}.
 *
 * <p>Drinking grants Strength (+ Nausea). Right-clicking a water source refills one point of
 * durability. Like the original, drinking is only possible while the gourd still holds liquid
 * (damage &lt; 6) and is a 16-tick drink action. The "drunk" effect is applied when the drink
 * completes (1.7.10 {@code onEaten} → 1.21.1 {@link #finishUsingItem}).
 */
public class ItemIbukihyou extends Item {

    public ItemIbukihyou(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 視線先（水源）を取得。1.7.10 の getMovingObjectPositionFromPlayer(world, player, true) 相当。
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        boolean onWater = level.getBlockState(hit.getBlockPos()).getBlock() == Blocks.WATER;

        if (onWater) {
            if (stack.isDamaged()) {
                player.swing(hand);
                stack.setDamageValue(stack.getDamageValue() - 1);
                return InteractionResultHolder.success(stack);
            }
            // 満タンなら何もしない
            return InteractionResultHolder.fail(stack);
        }

        if (stack.getDamageValue() < 6) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player) || player.isShiftKeyDown()) {
            return stack;
        }
        // 液体が残っている間だけ飲める（damage < 6）
        if (stack.getDamageValue() < 6) {
            stack.hurtAndBreak(5, player, EquipmentSlot.MAINHAND);
            player.getFoodData().eat(0, 0.0F);// 満腹度回復（原作は 0, 0.0F）
            if (!level.isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 15 * 20, 0));//攻撃力アップ
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 20, 2));//ゆらゆら
            }
        }
        return stack;
    }

    /** 飲み干すまでにかかる時間。 */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 16;
    }

    /** 液体が残っていれば drink、空なら none。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (stack.getDamageValue() >= 6) {
            return UseAnim.NONE;
        }
        return UseAnim.DRINK;
    }
}
