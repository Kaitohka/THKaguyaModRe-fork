package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModDataComponents;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_PEARL;
import static thkaguyamod.danmaku.DanmakuConstants.RAINBOW;
import static thkaguyamod.danmaku.THShotLib.angle;
import static thkaguyamod.danmaku.THShotLib.gravity_Zero;
import static thkaguyamod.danmaku.THShotLib.pos;

/**
 * 蓬莱の玉の枝 — ported from 1.7.10 {@code thKaguyaMod.item.ItemHouraiJeweledBranch}.
 *
 * <p>1.7.10 は {@link BowItem} を継承しつつ矢を撃たず、耐久値を「チャージ溜め」として使っていた。
 * 「Charge」NBT（short）の値は<b>消費量</b>を表し、0 が満タン、200 以下で発射可能、50 tick ごとに
 * 50 ずつ回復（Charge 値が減る）する。本移植は契約どおり
 * {@link ModDataComponents#CHARGE}（Integer）に読み替える。
 *
 * <p>1.7.10 は {@code getCharge()} が NBT なしで 0 を返し、{@code addCharge()} の初回呼び出しで
 * {@code -charge} を書き込むという回りくどい実装だったが、どちらの経路でも同じ値に収束するため、
 * 本移植では {@code charge += 加算量} として等価に実装している（{@code addCharge(stack, -50)} で
 * 消費量が 50 増える、{@code addCharge(stack, +50)} で 50 減る）。
 *
 * <p>移植しないもの: {@code getIconIndex}/{@code getIconFromDamage}/{@code registerIcons}/
 * {@code getIcon}（アイコンはチャージ量で 6 段階に切り替わっていた。1.21.1 ではモデル側の
 * {@code ItemProperties} predicate で {@code damage} を参照する形にする必要がある）。
 */
public class ItemHouraiJeweledBranch extends BowItem {

    public static final String houraiJeweledBranchIconName[] = {
            "houraiEda", "houraiEda_4", "houraiEda_3", "houraiEda_2", "houraiEda_1", "houraiEda_0"
    };

    public ItemHouraiJeweledBranch(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     *
     * <p>原作は矢の有無に関係なく使用を開始できたため、{@link BowItem} の矢チェックは行わない。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /**
     * 右クリックを終了したときに呼び出されるメソッド（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int shotNum;
        shotNum = 32 + getUseDuration(stack, player) - usedTime;//最低32発。継続時間に応じて増える

        if (shotNum > 72) {
            shotNum = 72;//最大でも72発まで
        }
        shotNum /= 3;

        if (getCharge(stack) <= 200) {
            ShotData shot = ShotData.shot(FORM_PEARL, RAINBOW, 0.3F, 5.0F, 1, 80);
            if (player.isShiftKeyDown())//スニーク状態なら
            {
                //弾速の違う３つをセットに全方位に放つ
                for (int i = 0; i < shotNum; i += 3) {
                    THShotLib.createCircleShot(player, player, THShotLib.pos_Living(player),
                            angle(player.getYRot(), player.getXRot()), 0.3D + (double) i * 0.2D, 2.0D, 0.03D, gravity_Zero(),
                            shot, shotNum, 0.2D, i * 3F);
                }
            } else//スニーク状態でないなら
            {
                Vec3 look = angle(player.getYRot(), player.getXRot());
                Vec3 shotPos = pos(player.getX() + look.x * 0.5D, THShotLib.getPosYFromEye(player) + look.y * 0.5D,
                        player.getZ() + look.z * 0.5D);
                THShotLib.createRingShot(player, player, shotPos, look, 0F, 9999, 0.6D, 2.0D, 0.03D, gravity_Zero(),
                        shot, shotNum, 0.0D, 15F, player.getRandom().nextFloat() * 360F);
                shot = ShotData.shot(FORM_PEARL, RAINBOW, 0.3F, 5.0F, 1, 80);
                THShotLib.createRingShot(player, player, shotPos, look, 0F, 9999, 0.6D, 2.0D, 0.03D, gravity_Zero(),
                        shot, shotNum / 2, 0.0D, 10F, player.getRandom().nextFloat() * 360F);
                shot = ShotData.shot(FORM_PEARL, RAINBOW, 0.3F, 5.0F, 1, 80);
                THShotLib.createRingShot(player, player, shotPos, look, 0F, 9999, 0.6D, 2.0D, 0.03D, gravity_Zero(),
                        shot, shotNum / 3, 0.0D, 5F, player.getRandom().nextFloat() * 360F);
            }

            // 1.7.10 の "random.orb"。1.21.1 では entity.experience_orb.pickup を近似として使用。
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

            addCharge(stack, -50);//消費量を50増やす（連射は6回まで）
        }
    }

    /**
     * インベントリにある限り常時呼び出されるメソッド（1.7.10 {@code onUpdate}）。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (getCharge(stack) > 0 && entity.tickCount % 50 == 0) {
            addCharge(stack, 50);//消費量を50減らす（＝回復）
            if (getCharge(stack) < 0) {
                setCharge(stack, 0);
            }
        }
    }

    /** 消費量を {@code charge} だけ増やす（負値なら減る）。 */
    private void addCharge(ItemStack stack, int charge) {
        setCharge(stack, getCharge(stack) + charge);
    }

    /** 消費量を設定する。 */
    private void setCharge(ItemStack stack, int charge) {
        stack.set(ModDataComponents.CHARGE.get(), Math.max(0, charge));
    }

    /** 消費量を返す（NBT なしは 0）。 */
    private int getCharge(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHARGE.get(), 0);
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
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