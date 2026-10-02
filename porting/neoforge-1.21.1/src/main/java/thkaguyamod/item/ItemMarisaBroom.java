package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntityMarisaBroom;
import thkaguyamod.init.ModEntities;

/**
 * 魔理沙の魔法の箒 — ported from 1.7.10 {@code thKaguyaMod.item.ItemMarisaBroom}.
 *
 * <p>そのまま武器としても使え、空をとぶこともできる。
 * 右クリックで箒（{@link EntityMarisaBroom}）を出現させて 1 個消費し、
 * 乗っていない＆スニークでないならそれに騎乗する。近接攻撃で 2.0 ダメージ。
 *
 * <p>TODO K3-review: 1.7.10 の {@code hitEntity} は {@code attackEntityFrom(DamageSource.causeMobDamage(...), 2.0F)}
 * のみを行い耐久は消費していなかった。1.21.1 では {@code hurtEnemy} が true を返すと
 * {@code postHurtEnemy}（= SwordItem 等の既定耐久消費）が続く。本クラスは {@link Item} 直系で
 * 既定の {@code postHurtEnemy} が空のため実害はないが、明示的に空実装で固定している。
 */
public class ItemMarisaBroom extends Item {

    public ItemMarisaBroom(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //使用者の位置に発射
        EntityMarisaBroom broom = new EntityMarisaBroom(ModEntities.MARISA_BROOM.get(), level,
                player.getX(), player.getY(), player.getZ());

        // 1.7.10 の "random.drr"。1.21.1 に対応する音が無いため、block.wood.break を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

        if (!level.isClientSide) {
            level.addFreshEntity(broom);//魔理沙の箒を出現させる
        }

        if (!player.isPassenger() && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.startRiding(broom);
                broom.setYRot(player.getYRot());
                broom.setXRot(player.getXRot());
            }
        }

        if (!level.isClientSide) {
            stack.shrink(1);//スタックから消滅させる
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * Entityに当たったときの処理（1.7.10 {@code hitEntity}）。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.hurt(target.damageSources().mobAttack(attacker), 2.0F);
        return true;
    }

    /** 原作の {@code hitEntity} は耐久を消費しなかったため、既定の消費は行わない。 */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    /** 1.7.10 の {@code EnumAction.block} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }
}