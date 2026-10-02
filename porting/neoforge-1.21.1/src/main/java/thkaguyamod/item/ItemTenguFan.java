package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.AQUA;
import static thkaguyamod.danmaku.DanmakuConstants.FORM_WIND;
import static thkaguyamod.danmaku.DanmakuConstants.WIND01;

/**
 * 風を起こす天狗団扇 — ported from 1.7.10 {@code thKaguyaMod.item.ItemTenguFan}.
 *
 * <p>右クリックの継続時間に比例した速度で風弾（特殊弾 {@link WIND01}）を 1 発発射する。
 * 最大使用時間は 24 tick（= 最速 24/7 ≈ 3.4）。
 */
public class ItemTenguFan extends Item {

    public ItemTenguFan(Item.Properties properties) {
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
     * 右クリックを離したときの処理（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        ShotData shot = ShotData.shot(FORM_WIND, AQUA, 0, 20, WIND01);
        double speed = (double) (getUseDuration(stack, player) - usedTime) / 7.0D;
        THShotLib.createShot(player, THShotLib.pos_Living(player),
                THShotLib.angle(player.getYRot(), player.getXRot()), speed, shot);

        player.swing(InteractionHand.MAIN_HAND);//投げる動作をさせる

        // 1.7.10 の "random.bow"。1.21.1 では entity.arrow.shoot を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    /** 1.7.10 の {@code EnumAction.bow} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
