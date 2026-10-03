package thkaguyamod.item;

import java.util.Random;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityDragonNeckJewel;
import thkaguyamod.init.ModEntities;

import static thkaguyamod.danmaku.THShotLib.pos;

/**
 * 龍の頸の玉 — ported from 1.7.10 {@code thKaguyaMod.item.ItemDragonNeckJewel}.
 *
 * <p>投げつけて落下地点に弾幕を発生させる。耐久値を「チャージ」として扱い、
 * 通常使用で 70、スニークの 5 連発で 299 消費する。所持中は 20 tick ごとに 20 回復する。
 *
 * <p>1.7.10 の {@code getItemDamageForDisplay()} はダメージ表示値（= 耐久消費量）であり、
 * 1.21.1 の {@code getDamageValue()} に対応する。
 */
public class ItemDragonNeckJewel extends Item {

    public ItemDragonNeckJewel(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        float[] r = {1.0F, 0.1F, 0.1F, 1.0F, 1.0F};
        float[] g = {0.1F, 1.0F, 0.1F, 1.0F, 0.1F};
        float[] b = {0.1F, 0.1F, 1.0F, 0.1F, 1.0F};
        Vec3 vec3;

        //受けているダメージが229よりちいさいなら　つまり耐久値が70/300より大きいなら
        if (!player.isShiftKeyDown() && stack.getDamageValue() < 229) {
            vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), 1.0D);
            Random random = new Random();
            int rand = random.nextInt(5);
            EntityDragonNeckJewel dragonNeckJewel = new EntityDragonNeckJewel(ModEntities.DRAGON_NECK_JEWEL.get(), level,
                    player, player, pos(player.getX(), THShotLib.getPosYFromEye(player, -0.2D), player.getZ()), vec3, r[rand], g[rand], b[rand]);

            // 1.7.10 の "random.orb"。1.21.1 では entity.experience_orb.pickup を近似として使用。
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
            if (!level.isClientSide) {
                level.addFreshEntity(dragonNeckJewel);//龍の頸の玉を発射
            }

            stack.hurtAndBreak(70, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);//耐久を70減らす
        }
        //スニーク時は５つ同時発射　5色の雷を落とす
        else if (player.isShiftKeyDown() && stack.getDamageValue() < 1) {
            float angle = -40F;
            for (int i = 0; i < 5; i++) {
                vec3 = THShotLib.getRotationVectorFromAngle(player.getYRot(), player.getXRot(), angle, 1.0D);
                EntityDragonNeckJewel dragonNeckJewel = new EntityDragonNeckJewel(ModEntities.DRAGON_NECK_JEWEL.get(), level,
                        player, player, pos(player.getX(), THShotLib.getPosYFromEye(player, -0.2D), player.getZ()), vec3, r[i], g[i], b[i]);
                if (!level.isClientSide) {
                    level.addFreshEntity(dragonNeckJewel);//龍の頸の玉を発射
                }
                angle += 20F;
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

            stack.hurtAndBreak(299, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);//耐久を最大まで減らす
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * インベントリにあるかぎり呼び出されるメソッド（1.7.10 {@code onUpdate}）。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        //耐久が減っていたら徐々に回復
        if (entity.tickCount % 20 == 0 && stack.isDamaged()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 20));//20 tickごとに耐久を20回復
        }
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
}
