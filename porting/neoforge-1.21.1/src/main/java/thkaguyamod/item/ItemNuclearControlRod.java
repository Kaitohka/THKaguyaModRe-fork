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
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.ORANGE;
import static thkaguyamod.danmaku.THShotLib.gravity_Zero;
import static thkaguyamod.danmaku.THShotLib.pos;
import static thkaguyamod.danmaku.THShotLib.rotate_Default;

/**
 * お空の制御棒 — ported from 1.7.10 {@code thKaguyaMod.item.ItemNuclearControlRod}.
 *
 * <p>右クリックで核弾（{@code EntityNuclearShot}）を発射し、離した時点で使用時間に比例した
 * 反動（後方への押し出し）と爆発音、耐久 1 消費が発生する。
 */
public class ItemNuclearControlRod extends Item {

    public ItemNuclearControlRod(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);

        Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), 1.0D);
        Vec3 vec3_2 = THShotLib.getVecFromAngle(player.getYRot() + 20F, player.getXRot(), 0.3D + 0.3D / 2.0D);

        ShotData shot = ShotData.shot(0, ORANGE, 0.3F, 10.0F, 0, 70);
        THShotLib.createNuclearShot(player, player,
                pos(player.getX() + vec3_2.x, THShotLib.getPosYFromEye(player, -0.1D) + vec3_2.y, player.getZ() + vec3_2.z), vec3, 0F,
                rotate_Default(), 0F, 9999, 0.0D, 0.8D, 0.0D, gravity_Zero(), shot);

        return InteractionResultHolder.success(stack);
    }

    /**
     * 右クリックを終了したときに呼び出されるメソッド（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int time = getUseDuration(stack, player) - usedTime;
        if (time > 100) {
            time = 100;
        }

        //爆発音（1.7.10 の "random.explode"）
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL,
                4.0F, (1.0F + (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F) * 0.7F);

        //反動処理（1.7.10 の motionX/Y/Z -= は push() に相当）
        Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), time / 25.0D);
        player.push(-vec3.x, -vec3.y, -vec3.z);

        //耐久を１減らす
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** 1.7.10 の {@code EnumAction.bow} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
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
