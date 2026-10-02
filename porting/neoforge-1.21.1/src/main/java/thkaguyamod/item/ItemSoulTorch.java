package thkaguyamod.item;

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
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_PHANTOM;
import static thkaguyamod.danmaku.DanmakuConstants.PURPLE;
import static thkaguyamod.danmaku.THShotLib.gravity_Zero;
import static thkaguyamod.danmaku.THShotLib.pos_Living;

/**
 * 人魂灯 — ported from 1.7.10 {@code thKaguyaMod.item.ItemSoulTorch}.
 *
 * <p>幽霊を呼び出す。使用（{@code onEaten} → {@code finishUsingItem}）で
 * 使用者の真上からリング状に人魂弾を 3 発放ち、特殊弾 {@link #SPECIAL_SOULTORCH} で
 * ブロック衝突では消えない＋ホーミングする弾になる。クリエイティブでは
 * 右クリックで即座に発動する（原作どおり）。
 *
 * <p>TODO K3-review: 1.7.10 は使用完了時にのみ {@code onEaten} が呼ばれた。1.21.1 では
 * 使用時間 12 tick を最後まで使うと {@code finishUsingItem} が呼ばれる（原作と同じ）。
 * ただしクリエイティブ時の即時発動は原作では {@code onItemRightClick} から
 * {@code onEaten} を直接呼んでいた。本移植も同じく {@link #finishUsingItem} を直接呼ぶ
 * （サーバ側のみ）。クライアントでは使用開始だけを行う。
 */
public class ItemSoulTorch extends Item implements ISpecialShot {

    public static final int SPECIAL_SOULTORCH = 352;

    public ItemSoulTorch(Item.Properties properties) {
        super(properties);
        SpecialShotRegistry.registerSpecialShot(this, SPECIAL_SOULTORCH);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCreative() && !level.isClientSide) {
            finishUsingItem(stack, level, player);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    /**
     * 右クリックを使い切ったときの処理（1.7.10 {@code onEaten}）。
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return stack;
        }

        ShotData shot = ShotData.shot(FORM_PHANTOM, PURPLE, 0.4F, 7.0F, 0, 480, SPECIAL_SOULTORCH);
        Vec3 angle = THShotLib.angle(player.getYRot(), -90F);
        THShotLib.createRingShot(player, player, pos_Living(player), angle, 0F, 9999, 0.01D, 0.4D, 0.03D,
                gravity_Zero(), shot, 3, 0.3D, 30F, 0F);

        THShotLib.playShotSound(player);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 12;
    }

    /** 1.7.10 の {@code EnumAction.bow} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    //===================================================================================
    //	特殊弾定義
    //===================================================================================

    @Override
    public void specialShot_move(Level level, int id, EntityTHShot shot) {
        shot.homing(16.0F);
    }

    @Override
    public boolean specialShot_hitBlock(Level level, int id, EntityTHShot shot, BlockHitResult hitResult) {
        return true;//ブロック衝突では消えない
    }

    @Override
    public boolean specialShot_hitEntity(Level level, int id, EntityTHShot shot, Entity hitEntity) {
        // 原作は空実装（消滅させる）
        return false;
    }
}