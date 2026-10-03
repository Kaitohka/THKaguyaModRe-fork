package thkaguyamod.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntityYuukaParasol;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_SCALE;
import static thkaguyamod.danmaku.DanmakuConstants.RED;
import static thkaguyamod.danmaku.DanmakuConstants.YELLOW;
import static thkaguyamod.danmaku.THShotLib.angle;
import static thkaguyamod.danmaku.THShotLib.gravity;
import static thkaguyamod.danmaku.THShotLib.pos;

/**
 * 幽香の傘 — ported from 1.7.10 {@code thKaguyaMod.item.ItemYuukaParasol}.
 *
 * <p>スニーク右クリックで日傘（{@link EntityYuukaParasol}）を出現させる（同一使用者の日傘が
 * 周囲にあるときは出せない）。離すと弾速の異なる 2 段の鱗弾を全方位に放つ。
 * 特殊弾 {@link #SPECIAL_FLOWER_LAND} により、草ブロックに着弾すると花を咲かせる。
 *
 * <p>特殊弾インターフェースは 1.21.1 版（{@link ISpecialShot}、{@code Level}/{@code BlockHitResult}）。
 * 登録は旧版と同じく本クラスのコンストラクタで行う（
 * {@link SpecialShotRegistry#registerSpecialShot(ISpecialShot, int)}）。
 *
 * <p>TODO K3-review: 1.7.10 の {@code onPlayerStoppedUsing} は着弾点の草ブロックを
 * {@code Block.getBlockById(2)} で判定していた。1.21.1 では {@link Blocks#GRASS_BLOCK} に置換済み。
 * また開花先は 37=黄色い花→{@link Blocks#DANDELION}、38=赤い花→{@link Blocks#POPPY} に置換済み。
 * 原作は {@code BlockFlower} の meta 1（設置向き）を渡していたが、1.21.1 の花は
 * 設置状態を持たないため既定状態で設置する。
 *
 * <p>TODO K3-review: 原作の {@code SPECIAL_FLOWER_LAND} 発射コードには
 * 未使用変数 {@code int color = THShotLib.SCALE[RED] + random.nextInt(2);} が残っている。
 * 挙動に影響しないため移植側では削除している。
 */
public class ItemYuukaParasol extends Item implements ISpecialShot {

    /** 耐久値（原作の登録側 properties に渡す値。クラス内では使用しない）。 */
    public static final int PARASOL_MAX_DAMAGE = 60;
    public static final int SPECIAL_FLOWER_LAND = 350;

    public ItemYuukaParasol(Item.Properties properties) {
        super(properties);
        //特殊弾の登録はコンストラクタ内で行う（旧版の自登録パターンを維持）
        SpecialShotRegistry.registerSpecialShot(this, SPECIAL_FLOWER_LAND);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);//アイテムの使用継続時間を記憶させる

        if (player.isShiftKeyDown()) {
            //周囲のEntityを取得し、同一使用者の日傘があるなら出せない
            List<EntityYuukaParasol> list = level.getEntitiesOfClass(EntityYuukaParasol.class,
                    player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
            for (EntityYuukaParasol existing : list) {
                if (existing.user == player) {
                    return InteractionResultHolder.pass(stack);//日傘は出せない
                }
            }

            EntityYuukaParasol entityYuukaParasol = new EntityYuukaParasol(ModEntities.YUUKA_PARASOL.get(), level, player, stack.getDamageValue());
            if (!level.isClientSide) {
                level.addFreshEntity(entityYuukaParasol);
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * 右クリックを離したときに呼び出される（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int shotPower = 10 + getUseDuration(stack, player) - usedTime;
        double shotPower2 = (float) shotPower / 10.0D;
        if (shotPower2 > 4.0D) {
            shotPower2 = 5.0D;
        }
        float transDegree = 3.141593F / 180F;
        double ax, az;
        float shotAngle = (float) player.getRandom().nextInt(36000) / 100F;
        float angle2 = 0.0F;
        int shotNum = 45;
        float angleSpan = (360F / (float) shotNum) * transDegree;
        float angle2Span = angleSpan * 20F;
        ShotData shot = ShotData.shot(FORM_SCALE, RED, 0.3F, 7.0F, 3, 80, SPECIAL_FLOWER_LAND);
        if (player.getRandom().nextInt(2) == 0) {
            shot.color = YELLOW;
        }

        for (int i = 0; i < shotNum; i++) {
            ax = -(double) Mth.sin(shotAngle);//X方向　水平方向
            az = Mth.cos(shotAngle);//Z方向　水平方向
            double speed = 0.23D - (double) Mth.sin(angle2) * 0.02D;

            THShotLib.createShot(player, player,
                    pos(player.getX(), THShotLib.getPosYFromEye(player, 0.2D), player.getZ()),
                    angle(ax * shotPower2, 1.4F, az * shotPower2), 0F,
                    speed, speed, 0.0D, gravity(-0.02), shot);
            shotAngle += angleSpan;
            angle2 += angle2Span;
        }

        shotNum = (int) ((float) shotNum * 0.25F);
        angleSpan = (360F / (float) shotNum) * transDegree;
        shotPower2 *= 0.66D;
        for (int i = 0; i < shotNum; i++) {
            ax = -(double) Mth.sin(shotAngle);//X方向　水平方向
            az = Mth.cos(shotAngle);//Z方向　水平方向
            THShotLib.createShot(player, player,
                    pos(player.getX(), THShotLib.getPosYFromEye(player, 0.2D), player.getZ()),
                    angle(ax * shotPower2, 1.1F, az * shotPower2), 0F,
                    0.23D, 0.23D, 0.0D, gravity(-0.02), shot);
            shotAngle += angleSpan;
        }

        // 1.7.10 の "random.bow"。1.21.1 では entity.arrow.shoot を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    /**
     * Entityに当たった時に呼び出される（1.7.10 {@code hitEntity}）。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        //Entityを吹っ飛ばす
        double disX = target.getX() - attacker.getX();
        double disY = target.getY() - attacker.getY();
        double disZ = target.getZ() - attacker.getZ();
        float toEntityYaw = (float) Math.atan2(disX, disZ);
        float toEntityPitch = (float) Math.atan2(disY, Math.sqrt(disX * disX + disZ * disZ));
        double power = 3.0D;

        // 1.7.10 の motionX/Y/Z 直代入は 1.21.1 では setDeltaMovement に相当
        target.setDeltaMovement(Mth.sin(toEntityYaw) * Mth.cos(toEntityPitch) * power,
                Mth.sin(toEntityPitch) * power + 1.0D,
                Mth.cos(toEntityYaw) * Mth.cos(toEntityPitch) * power);

        target.hurt(target.damageSources().mobAttack(attacker), 7);

        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);

        return true;
    }

    /** 耐久消費は {@link #hurtEnemy} 側で行うため、既定の消費は行わない。 */
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

    //===================================================================================
    //	特殊弾定義
    //===================================================================================

    @Override
    public void specialShot_move(Level level, int id, EntityTHShot shot) {
        // 原作は空実装
    }

    @Override
    public boolean specialShot_hitBlock(Level level, int id, EntityTHShot shot, BlockHitResult hitResult) {
        int hitPointX = Mth.floor(shot.getX());
        int hitPointY = Mth.floor(shot.getY() - 0.3D);
        int hitPointZ = Mth.floor(shot.getZ());

        // 1.7.10 の Block.getBlockById(2)（草ブロック）
        BlockPos hitPos = new BlockPos(hitPointX, hitPointY, hitPointZ);
        if (level.getBlockState(hitPos).is(Blocks.GRASS_BLOCK)) {
            BlockPos setPos = new BlockPos(hitPointX, hitPointY + 1, hitPointZ);
            if (level.isEmptyBlock(setPos)) {
                if (shot.getShotColor() == YELLOW)//黄色の弾幕なら
                {
                    if (!level.isClientSide) {
                        //黄色い花を咲かす（1.7.10: BlockFlower id 37）
                        level.setBlockAndUpdate(setPos, Blocks.DANDELION.defaultBlockState());
                    }
                } else if (shot.getShotColor() == RED)//赤色の弾幕なら
                {
                    if (!level.isClientSide) {
                        //赤い花を咲かす（1.7.10: BlockFlower id 38）
                        level.setBlockAndUpdate(setPos, Blocks.POPPY.defaultBlockState());
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean specialShot_hitEntity(Level level, int id, EntityTHShot shot, Entity hitEntity) {
        // 原作は空実装（消滅させる）
        return false;
    }
}