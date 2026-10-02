package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.BLUE;
import static thkaguyamod.danmaku.DanmakuConstants.FORM_BUTTERFLY;
import static thkaguyamod.danmaku.THShotLib.SIZE;
import static thkaguyamod.danmaku.THShotLib.angle;

/**
 * 幽々子の扇 — ported from 1.7.10 {@code thKaguyaMod.item.ItemYuyukoFan}.
 *
 * <p>蝶の弾幕をいくらでも出せるが、使うたびに満腹度が 1.5 減る。
 * 近接攻撃では、相手の体力がハート 10 未満かつ無敵時間中のとき、5% の確率で
 * 相手の残り体力を吸収して 4444 ダメージ（即死級）を与える。
 *
 * <p>TODO K3-review: 1.7.10 の {@code hitEntity} は「相手を攻撃した瞬間」に呼ばれたが、
 * 1.21.1 は {@code hurtEnemy} がtrueを返した場合のみ {@code postHurtEnemy}（= 耐久消費）が続く。
 * 原作は {@code hitEntity} 内で耐久を 1 消費し true を返していたため、
 * 本移植では {@link #hurtEnemy} で耐久消費まで行い true を返す、
 * {@link #postHurtEnemy} は何もしない（二重消費を避ける）形にしている。
 * この噛み合わせで「攻撃が無効化された攻撃でも耐久が減るか」の差が出る可能性がある。
 */
public class ItemYuyukoFan extends Item {

    public ItemYuyukoFan(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /**
     * 右クリックを終了したときに呼ばれる（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int color, color2, shotNum, shotNum3;
        shotNum = 32 + getUseDuration(stack, player) - usedTime;//最低32発。継続時間に応じて増える
        if (shotNum > 72) {
            shotNum = 72;//最大でも72発まで
        } else if (shotNum % 3 != 0) {
            shotNum = (int) (shotNum / 3) * 3;
        }
        shotNum3 = shotNum / 3;

        if (player.isShiftKeyDown())//スニーク状態なら
        {
            int color3;
            color = BLUE + player.getRandom().nextInt(4);//弾の色をランダムで決める
            color2 = color + 1;
            if (color2 >= BLUE + 4) {
                color2 = BLUE;
            }

            //弾速の違う３つをセットに全方位に放つ（左右の２方向）
            for (int i = 0; i < 3; i++) {
                color3 = color;
                if (i == 1) {
                    color3 = color2;
                }
                double speed = 0.1D + (double) (i * 0.1D);
                THShotLib.createCircleShot(player, player, THShotLib.pos_Living(player), player.getLookAngle(), 12F, 10,
                        speed, speed, 0.0D, THShotLib.gravity_Zero(),
                        ShotData.shot(FORM_BUTTERFLY, color3, SIZE[FORM_BUTTERFLY], 4.0F, 1, 80), shotNum3, 0.5D, 0F);
                THShotLib.createCircleShot(player, player, THShotLib.pos_Living(player), player.getLookAngle(), -12F, 10,
                        speed, speed, 0.0D, THShotLib.gravity_Zero(),
                        ShotData.shot(FORM_BUTTERFLY, color3, SIZE[FORM_BUTTERFLY], 4.0F, 1, 80), shotNum3, 0.5D, 0F);
            }
        } else//スニーク状態でないなら
        {
            color = BLUE + player.getRandom().nextInt(4);//弾の色をランダムで決める
            color2 = color + 1;
            if (color2 >= BLUE + 4) {
                color2 = BLUE;
            }
            Vec3 look = angle(player.getYRot(), player.getXRot());
            Vec3 shotPos = THShotLib.pos(player.getX() + look.x * 0.5D, THShotLib.getPosYFromEye(player) + look.y * 0.5D,
                    player.getZ() + look.z * 0.5D);

            THShotLib.createWideShot(player, player, shotPos, look, 14.4F, 8, 0.0D, 0.45D, 0.02D, THShotLib.gravity_Zero(),
                    ShotData.shot(FORM_BUTTERFLY, color, SIZE[FORM_BUTTERFLY], 4.0F, 1, 80), shotNum3 + 1, 90F, 0.2D, -120F);
            THShotLib.createWideShot(player, player, shotPos, look, -14.4F, 8, 0.0D, 0.45D, 0.02D, THShotLib.gravity_Zero(),
                    ShotData.shot(FORM_BUTTERFLY, color2, SIZE[FORM_BUTTERFLY], 4.0F, 1, 80), shotNum3 + 1, 90F, 0.2D, 120F);
        }

        if (!player.isCreative())//クリエイティブでないなら
        {
            player.getFoodData().addExhaustion(1.5F);//使うたびに少し腹が減る
        }
        // 1.7.10 の "random.orb"。1.21.1 では entity.experience_orb.pickup を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    /**
     * Entityに当てたときに呼ばれる（1.7.10 {@code hitEntity}）。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        //体力がハート１０以下で、無敵時間でないなら
        if (target.getHealth() > 0 && target.getHealth() < 20 && target.hurtTime > 0) {
            //５％の確立で
            if (attacker.getRandom().nextInt(100) < 5) {
                //使用者は当てた相手の体力を全て吸収する
                attacker.heal(target.getHealth());
                //当てた相手に4444ダメージ（即死級）
                DamageSource source = attacker.damageSources().mobAttack(attacker);
                target.hurt(source, 4444);
                //村人ゾンビが回復するときの音を出す（1.7.10 の "mob.zombie.unfect"）
                attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                        SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.0F, 40.0F);
            }
        }
        //耐久を1減らす
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    /** 耐久消費は {@link #hurtEnemy} 側で行うため、既定の消費は行わない。 */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
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

    /** エンチャント不可。 */
    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    /** 1.7.10 の {@code isBookEnchantable} 相当。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}