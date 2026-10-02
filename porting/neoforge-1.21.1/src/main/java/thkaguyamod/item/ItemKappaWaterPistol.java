package thkaguyamod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.BLUE;
import static thkaguyamod.danmaku.DanmakuConstants.FORM_LIGHT;

/**
 * 河童の水鉄砲 — ported from 1.7.10 {@code thKaguyaMod.item.ItemKappaWaterPistol}.
 *
 * <p>スニーク時に水源を向いて右クリックすると水を補充する（耐久を 2 回復）。
 * それ以外では水の弾を 3 発（±7 度）発射し、耐久を 1 消費する。
 * 耐久が最大の直前（{@code damage == maxDamage - 1}）では壊れないように発射できない。
 *
 * <p>TODO K3-review: 原作は最後の 1 点を「壊れないための残り」として温存していた
 * （修理不可・アイテム消滅回避）。1.21.1 の耐久管理は登録側 properties に移っているため、
 * {@code getMaxDamage()} は登録値（原作 39）に依存する。登録値が変わると残量 1 の挙動も変わる。
 */
public class ItemKappaWaterPistol extends Item {

    public ItemKappaWaterPistol(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //水を補充する処理
        if (player.isShiftKeyDown()) {
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            BlockPos hitPos = hit.getBlockPos();
            // 1.7.10 は「水源（water, meta 0）」を判定していた
            if (level.getFluidState(hitPos).is(Fluids.WATER) && level.getFluidState(hitPos).isSource()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 2));
                player.swing(InteractionHand.MAIN_HAND);//投げる動作をさせる
                return InteractionResultHolder.success(stack);//補充ができたなら弾幕は出さずに終了
            }
        }

        //壊れないようにする処理
        if (stack.getDamageValue() == stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }

        //水弾幕を出す処理
        double speed = 0.3D;
        Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot() + 40F, player.getXRot(), 0.4D);
        double xPos = player.getX() + vec3.x;
        double yPos = THShotLib.getPosYFromEye(player, -0.1D) + vec3.y;
        double zPos = player.getZ() + vec3.z;
        Vec3 shotPos = THShotLib.pos(xPos, yPos, zPos);
        ShotData shot = ShotData.shot(FORM_LIGHT, BLUE, 0.2F, 4.0F, 0, 120);

        THShotLib.createShot(player, player, shotPos, THShotLib.angle(player.getYRot(), player.getXRot() - 7F), 0F,
                1.0D - speed, 0.0D, 0.4D, THShotLib.gravity_Default(), shot);
        THShotLib.createShot(player, player, shotPos, THShotLib.angle(player.getYRot(), player.getXRot()), 0F,
                1.1D - speed, 0.0D, 0.4D, THShotLib.gravity_Default(), shot);
        THShotLib.createShot(player, player, shotPos, THShotLib.angle(player.getYRot(), player.getXRot() + 7F), 0F,
                1.2D - speed, 0.0D, 0.4D, THShotLib.gravity_Default(), shot);
        // 原作どおり（この代入は以降で使われない）
        speed += 0.07D;

        // 1.7.10 の "liquid.splash"。1.21.1 では entity.generic.splash を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.5F, 4.0F);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        return InteractionResultHolder.success(stack);
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
