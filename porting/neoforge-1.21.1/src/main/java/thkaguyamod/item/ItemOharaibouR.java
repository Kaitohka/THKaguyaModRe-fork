package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityOnmyoudama;
import thkaguyamod.init.ModEntities;

import static thkaguyamod.danmaku.DanmakuConstants.BOUND04;
import static thkaguyamod.danmaku.DanmakuConstants.ORANGE;
import static thkaguyamod.danmaku.THShotLib.getPosYFromEye;
import static thkaguyamod.danmaku.THShotLib.gravity_Default;
import static thkaguyamod.danmaku.THShotLib.pos;
import static thkaguyamod.danmaku.THShotLib.rotate_Default;

/**
 * 博麗のお祓い棒 — ported from 1.7.10 {@code thKaguyaMod.item.ItemOharaibouR}.
 *
 * <p>右クリックで陰陽玉（{@link EntityOnmyoudama}）を目の前に出現させ、使用を開始する。
 * 使用を止めると音が鳴り耐久が 1 減る（旧実装は「チャージ時間」を計算するだけで効果に使っていない。
 * その挙動をそのまま維持している）。
 *
 * <p>移植しないもの: {@code isFull3D}（1.7.10 の手持ち描画フラグ。1.21.1 はモデル側で処理）。
 */
public class ItemOharaibouR extends Item {

    public ItemOharaibouR(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);// 1.7.10 の setItemInUse に相当

        Vec3 vec3 = THShotLib.getVecFromAngle(-player.getYRot(), -player.getXRot(), 0.6D);

        EntityOnmyoudama onmyoudama = new EntityOnmyoudama(ModEntities.ONMYOUDAMA.get(), level, player, player,
                pos(player.getX() + vec3.x, getPosYFromEye(player, -0.1D) + vec3.y, player.getZ() + vec3.z), vec3, 0F,
                rotate_Default(), 0F, 9999, 0.0D, 1.8D, 0.00D, gravity_Default(), ORANGE, 0.3F, 10.0F, 0, 180, BOUND04);

        if (!level.isClientSide) {
            level.addFreshEntity(onmyoudama);//陰陽玉を出現させる
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * 右クリックを終了したときに呼び出されるメソッド（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        int time = getUseDuration(stack, player) - usedTime;
        if (time > 100) {
            time = 100;
        }

        // 1.7.10 の "random.bow"。1.21.1 では entity.arrow.shoot を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 2.0F, 0.3F);

        //耐久を１減らす
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return 72000;
    }

    /** 1.7.10 の {@code EnumAction.block} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
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
