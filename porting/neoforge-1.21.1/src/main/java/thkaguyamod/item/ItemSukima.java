package thkaguyamod.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntitySukima;
import thkaguyamod.init.ModEntities;

/**
 * スキマを設置する — ported from 1.7.10 {@code thKaguyaMod.item.ItemSukima}.
 *
 * <p>通常使用では見ているブロック面にスキマを 1 つ開き、スニーク使用では
 * 128 ブロック先までの視線上に「入口」と「出口」の 2 つを開く。
 * 同一色のスキマ同士を行き来でき、スニーク版は 15% の確率でアイテムを消費する
 * （通常版は原作どおり常に 1 個消費する）。
 *
 * <p>TODO K3-review: 1.7.10 の {@code isBlockNormalCubeDefault(x, y, z, true)} は
 * 「その座標が固体ブロックか」の判定だった。1.21.1 に対応 API が無いため
 * {@code BlockState#isSolidRender} で近似している（スラブ・階段・ガラス等の扱いが原作とずれる可能性）。
 *
 * <p>TODO K3-review: {@code ModConfig.useDefaultGapSE} が false のときの
 * {@code THKaguyaConfig.sukimaWarpSE}（ユーザー指定の任意音效名）は、1.21.1 の音效登録体系に
 * 対応先が無いため移植していない（{@code ModConfig} に該当フィールドが存在しない）。
 * 現状は常に既定音（enderman.teleport）を鳴らす。
 */
public class ItemSukima extends Item {

    public ItemSukima(Item.Properties properties) {
        super(properties);
    }

    /**
     * 見ているブロック面の向きからスキマの設置角度を求める。
     */
    protected float getSetAngle(Player player, double setX, double setZ) {
        float rotationYaw;
        rotationYaw = (float) Math.atan2(setX - player.getX(), setZ - player.getZ()) / 3.141593F * 180F;
        if (rotationYaw < 0F) {
            rotationYaw += 360F;
        }
        if (rotationYaw % 90F < 45F) {
            rotationYaw = rotationYaw - (rotationYaw % 45F);
        } else {
            rotationYaw = rotationYaw - (rotationYaw % 45F) + 45F;
        }
        return -rotationYaw;
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        double setX = player.getX() - Math.sin(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 2.0D;
        double setY = player.getY() - Math.sin(player.getXRot() / 180F * (float) Math.PI) * 2.0D + player.getEyeHeight() / 2.0D;
        double setZ = player.getZ() + Math.cos(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 2.0D;
        boolean lost = true;

        // 1.7.10 の isBlockNormalCubeDefault((int)setX, (int)setY, (int)setZ, true) を反転した条件
        BlockPos frontPos = BlockPos.containing(setX, setY, setZ);
        BlockState frontState = level.getBlockState(frontPos);
        if (!frontState.isSolidRender(level, frontPos)) {
            EntitySukima entitySukima;
            EntitySukima entitySukima2;
            double hitX = player.getX();
            double hitY = player.getY();
            double hitZ = player.getZ();
            float rotationYaw = 0F;

            if (!player.isShiftKeyDown()) {
                double i = (int) setX;
                double j = (int) setY;
                double k = (int) setZ;

                // 1.7.10 の getMovingObjectPositionFromPlayer(world, player, true)
                BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

                if (hit.getType() == HitResult.Type.MISS) {
                    Vec3 look = player.getLookAngle();
                    i = (player.getX() + look.x * 2.0D - 0.5D);
                    j = (player.getY() + look.y * 2.0D + 1.0D);
                    k = (player.getZ() + look.z * 2.0D - 0.5D);

                    rotationYaw = player.getYRot();
                } else {
                    i = hit.getBlockPos().getX();
                    j = hit.getBlockPos().getY();
                    k = hit.getBlockPos().getZ();

                    // 1.7.10 の sideHit 0..5 は 1.21.1 の Direction.get3DDataValue() と同じ並び
                    // (0=下, 1=上, 2=-Z, 3=+Z, 4=-X, 5=+X)
                    switch (hit.getDirection()) {
                        case DOWN -> {
                            j--;
                            rotationYaw = getSetAngle(player, i, k);
                        }
                        case UP -> {
                            j++;
                            rotationYaw = getSetAngle(player, i, k);
                        }
                        case NORTH -> {
                            k--;
                            rotationYaw = 0F;
                        }
                        case SOUTH -> {
                            k++;
                            rotationYaw = 180F;
                        }
                        case WEST -> {
                            i--;
                            rotationYaw = 270F;
                        }
                        case EAST -> {
                            i++;
                            rotationYaw = 90F;
                        }
                    }
                }

                entitySukima = new EntitySukima(ModEntities.SUKIMA.get(), level, player, i + 0.5D, j, k + 0.5D, rotationYaw);
            } else {
                double xVecMax = -Math.sin(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 128.0D;
                double yVecMax = -Math.sin(player.getXRot() / 180F * (float) Math.PI) * 128.0D;
                double zVecMax = Math.cos(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 128.0D;
                //始点（現在地）
                Vec3 vec3d = THShotLib.pos(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
                //終点（現在地に移動量を足した点）
                Vec3 vec3d1 = THShotLib.pos(player.getX() + xVecMax, player.getY() + yVecMax, player.getZ() + zVecMax);

                // 1.7.10 の world.func_147447_a(vec3d, vec3d1, false, true, false)
                //   stopOnLiquid=false → Fluid.NONE / ignoreBlockWithoutBoundingBox=true → Block.COLLIDER
                BlockHitResult blockHit = level.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

                //何らかのブロックに当たっているなら
                if (blockHit.getType() != HitResult.Type.MISS) {
                    //終点を当たった点に変更
                    vec3d1 = blockHit.getLocation();
                    hitX = blockHit.getLocation().x;
                    hitY = blockHit.getLocation().y;
                    hitZ = blockHit.getLocation().z;
                    double xz_sq = (hitX - setX) * (hitX - setX) + (hitZ - setZ) * (hitZ - setZ);//平面方向の平方
                    //５m以下の距離なら無効
                    if (Math.sqrt(xz_sq + (hitY - setY) * (hitY - setY)) <= 5.0D) {
                        return InteractionResultHolder.pass(stack);
                    }
                } else {
                    return InteractionResultHolder.pass(stack);
                }

                Entity hitEntity = null;

                //このEntityから移動後までの線分に、指定分の範囲を追加した直方体と衝突するEntityのリストを取得
                List<Entity> list = level.getEntities(player,
                        player.getBoundingBox().expandTowards(xVecMax, yVecMax, zVecMax).inflate(1.0D, 1.0D, 1.0D));
                double d = 0.0D;
                for (Entity candidate : list) {
                    if (candidate instanceof LivingEntity) {
                        double f2 = 0.3F;
                        java.util.Optional<Vec3> intercept = candidate.getBoundingBox().inflate(f2, f2, f2).clip(vec3d, vec3d1);
                        //Entityとの衝突がないなら、このEntityはパスする
                        if (intercept.isPresent()) {
                            //始点からEntityに衝突した点までの距離を取得
                            double d1 = vec3d.distanceTo(intercept.get());
                            //今までで一番近いEntity、かつ、５m以上離れているなら、Entityと距離を記憶する
                            if ((d1 < d || d == 0.0D) && d1 > 5.0D) {
                                hitEntity = candidate;
                                d = d1;
                            }
                        }
                    }
                }

                //Entityに当たっていたなら
                if (hitEntity != null) {
                    if (hitEntity instanceof LivingEntity hitLiving) {
                        hitX = hitLiving.getX();
                        // TODO K3-review: 1.7.10 の yOffset（当たり判定の高さオフセット）は
                        //   1.21.1 では getBbHeight() が相当。
                        hitY = hitLiving.getY() + hitLiving.getBbHeight() / 2.0D;
                        hitZ = hitLiving.getZ();
                        setY += 1.0D;
                    } else {
                        hitX = hitEntity.getX();
                        hitY = hitEntity.getY() + 0.75D;
                        hitZ = hitEntity.getZ();
                    }
                }

                entitySukima = new EntitySukima(ModEntities.SUKIMA.get(), level, player, setX, setY, setZ, 16);
                entitySukima2 = new EntitySukima(ModEntities.SUKIMA.get(), level, player, hitX, hitY, hitZ, 16);
                lost = player.getRandom().nextInt(100) < 15;//ランダムで消失

                if (!level.isClientSide) {
                    level.addFreshEntity(entitySukima2);//スキマを生み出す
                }
            }

            // 1.7.10 の "mob.endermen.portal"
            if (ModConfig.useDefaultGapSE) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                        0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
            } else {
                // TODO K3-review: 原作は ModConfig.sukimaWarpSE（ユーザー指定音效名）を鳴らしていたが、
                //   1.21.1 の音效は登録制のため任意名を再生できない。既定音で代替する。
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            if (!level.isClientSide) {
                level.addFreshEntity(entitySukima);//スキマを生み出す
                if (lost) {
                    stack.shrink(1);//スタックを一つ減らす
                }
            }
        }
        return InteractionResultHolder.success(stack);
    }
}