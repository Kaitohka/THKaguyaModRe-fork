package thkaguyamod.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModItems;

/**
 * 血に餓えた陰陽玉 — ported from 1.7.10 {@code thKaguyaMod.item.ItemBloodthirstyOnmyoudama}.
 *
 * <p>右クリックを離したとき、視線 64 ブロック先までの最も近い生物を探し、
 * その手前 2 ブロックの位置へ使用者を瞬間移動させる。耐久を 1 消費し、
 * 耐久が尽きると「陰陽玉」({@link ModItems#YIN_YANG_ORB}) に戻る
 * （旧 damage は {@link ModDataComponents#ORIGINAL_DAMAGE} から復元）。
 *
 * <p>TODO K3-review: 1.7.10 は {@code player.posX/Y/Z} を直接書き換えたうえで
 * {@code playerNetServerHandler.setPlayerLocation(...)} を呼んでいた。1.21.1 では
 * {@link ServerPlayer#connection} の {@code teleport(...)} に読み替えている
 * （クライアント側では何もしない。原作も位置変更は {@code !world.isRemote} の中でのみ行っていた）。
 *
 * <p>TODO K3-review: 1.7.10 の {@code func_147447_a(vec3d, vec3d1, false, true, true)} は
 * {@code ClipContext.Block.COLLIDER} / {@code Fluid.NONE} で近似している
 * （{@code THKaguyaLib} は同じ呼び出しに {@code Fluid.ANY} を使っており、液体の扱いが両者で異なる）。
 *
 * <p>TODO K3-review: 原作の {@code if(entity.posY &gt; player.posY) {...} else {...}} は
 * 両分岐が同一の式だった（死コード）。本移植は式を 1 つにまとめている（挙動は同じ）。
 *
 * <p>TODO K3-review: アイテム差し替えは原作では戻り値の ItemStack 経由だったが、
 * 1.21.1 では {@code player.setItemInHand} で明示的に書き戻している
 * （{@link ItemGapFoldingUmbrella} と同じ扱い）。
 */
public class ItemBloodthirstyOnmyoudama extends Item {

    public ItemBloodthirstyOnmyoudama(Item.Properties properties) {
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

        Vec3 look = player.getLookAngle();
        //始点を登録
        Vec3 vec3d = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        //終点を登録
        Vec3 vec3d1 = new Vec3(player.getX() + look.x * 64.0D, player.getY() + look.y * 64.0D, player.getZ() + look.z * 64.0D);

        //始点と終点からブロックとの衝突を取得
        BlockHitResult blockHit = level.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        //ブロックと衝突していたなら
        if (blockHit.getType() != HitResult.Type.MISS) {
            //終点を衝突した点に変更
            vec3d1 = blockHit.getLocation();
        }

        Entity entity = null;
        Vec3 hitVec = null;

        //このEntityから移動後までの線分に、指定分の範囲を追加した直方体と衝突するEntityのリストを取得
        List<Entity> list = level.getEntities(player,
                player.getBoundingBox().expandTowards(look.x * 64.0D, look.y * 64.0D, look.z * 64.0D).inflate(1.0D, 1.0D, 1.0D));
        double d = 0.0D;
        for (Entity candidate : list) {
            if (candidate instanceof LivingEntity) {
                double f2 = 0.3F;
                Optional<Vec3> intercept = candidate.getBoundingBox().inflate(f2, f2, f2).clip(vec3d, vec3d1);
                //Entityとの衝突がないなら、このEntityはパスする
                if (intercept.isPresent()) {
                    //始点からEntityに衝突した点までの距離を取得
                    double d1 = vec3d.distanceTo(intercept.get());
                    //今までで一番近いEntityなら、Entityと距離を記憶する
                    if (d1 < d || d == 0.0D) {
                        entity = candidate;
                        hitVec = intercept.get();
                        d = d1;
                    }
                }
            }
        }

        //Entityに当たっていたなら
        if (entity != null && hitVec != null) {
            if (player instanceof ServerPlayer playerMP && !level.isClientSide) {
                Vec3 posPlayer = THShotLib.pos_Living(player);
                Vec3 toVec = THShotLib.pos(hitVec.x - posPlayer.x, hitVec.y - posPlayer.y, hitVec.z - posPlayer.z);
                Vec3 vec = THShotLib.getVectorNomalize(toVec);
                double newX = player.getX() + toVec.x - vec.x * 2.0D;
                // 原作は y をいったん int 化してから +0.1D していた
                double newY = (int) (player.getY() + toVec.y - vec.y * 2.0D + 1.0D) + 0.1D;
                double newZ = player.getZ() + toVec.z - vec.z * 2.0D;

                playerMP.connection.teleport(newX, newY, newZ, player.getYRot(), player.getXRot());
            }
        }

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            int originalDamage = stack.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), 0);

            ItemStack yinYangOrb = new ItemStack(ModItems.YIN_YANG_ORB.get());
            yinYangOrb.setDamageValue(originalDamage);
            player.setItemInHand(InteractionHand.MAIN_HAND, yinYangOrb);
        }
    }

    /** 1.7.10 の {@code EnumAction.bow} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}