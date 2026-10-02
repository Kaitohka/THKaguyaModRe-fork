package thkaguyamod.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.entity.effect.EntityAjaRedStoneEffect;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;
import thkaguyamod.registry.SpecialShotRegistry;

/**
 * エイジャの赤石 — ported from 1.7.10 {@code thKaguyaMod.item.ItemAjaRedStone}.
 *
 * <p>光を溜めてアンデッド特攻のレーザーを発射する。右クリックで吸光エフェクト
 * （{@link EntityAjaRedStoneEffect}）を出現させる。特殊弾 {@link #SPECIAL_AJA_REDSTONE} は
 * アンデッドに当たったとき、その弾に着火時間と 2 倍のダメージ倍率を与える。
 *
 * <p>TODO K3-review: 1.7.10 のアンデッド判定は {@code EnumCreatureAttribute.UNDEAD} だった。
 * 1.21.1 に EnumCreatureAttribute は無いため {@code EntityTypeTags.UNDEAD} で代替している
 * （バニラのゾンビ・スケルトン系は一致するが、他 MOD が独自に UNDEAD 属性だけを付けた
 * エンティティは拾えない可能性がある）。
 *
 * <p>TODO K3-review: 原作は {@code shot.shotDamage} をそのまま着火時間（秒）に使っていた。
 * {@code EntityTHShot#shotDamage} は public float のためそのまま参照している。
 */
public class ItemAjaRedStone extends Item implements ISpecialShot {

    /** アンデッドだけを燃やすレーザーの特殊弾ID */
    public static final int SPECIAL_AJA_REDSTONE = 351;

    public ItemAjaRedStone(Item.Properties properties) {
        super(properties);
        SpecialShotRegistry.registerSpecialShot(this, SPECIAL_AJA_REDSTONE);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //アイテムの使用継続時間を記憶させる
        player.startUsingItem(hand);

        //エイジャの赤石の光吸収エフェクトを出現させる
        EntityAjaRedStoneEffect ajaRedStoneEffect = new EntityAjaRedStoneEffect(ModEntities.AJA_RED_STONE_EFFECT.get(), level, player);

        if (!level.isClientSide) {
            level.addFreshEntity(ajaRedStoneEffect);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** 1.7.10 の {@code EnumAction.block} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** 1.7.10 の {@code isBookEnchantable} 相当。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    //===================================================================================
    //	特殊弾定義
    //===================================================================================

    @Override
    public void specialShot_move(Level level, int id, EntityTHShot shot) {
        // 原作は空実装
    }

    @Override
    public boolean specialShot_hitBlock(Level level, int id, EntityTHShot shot, BlockHitResult hitResult) {
        // 原作は空実装（消滅させる）
        return false;
    }

    @Override
    public boolean specialShot_hitEntity(Level level, int id, EntityTHShot shot, Entity hitEntity) {
        //指定したダメージ分の魔法ダメージを与える
        if (hitEntity instanceof LivingEntity living) {
            if (living.getType().is(EntityTypeTags.UNDEAD))//アンデッドなら
            {
                //通常の2倍のダメージを与える
                living.setRemainingFireTicks((int) shot.shotDamage);
                shot.damageRate *= 2.0F;

                // 1.7.10 の "random.fizz"。1.21.1 では block.fire.extinguish を近似として使用。
                level.playSound(null, shot.getX(), shot.getY(), shot.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                        0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);

                // 1.7.10 は "largesmoke" を 8 個、速度 0 で散らしていた
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            shot.getX() + 0.5D, shot.getY() + 0.5D, shot.getZ() + 0.5D,
                            8, 0.5D, 0.5D, 0.5D, 0.0D);
                }
            }
        }
        return false;
    }
}