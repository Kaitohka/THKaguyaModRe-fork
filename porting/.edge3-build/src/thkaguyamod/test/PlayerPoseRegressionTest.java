package thkaguyamod.test;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

/**
 * diag-pose 回归族：右键符卡/物品后玩家**自动趴下**（{@link Pose#SWIMMING}）的机理与修复。
 *
 * <h2>机理（1.21.1 独有，1.7.10 没有）</h2>
 * <p>{@code Player#updatePlayerPose()} 每 tick 调用
 * {@code canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING)} 与
 * {@code ...(Pose.CROUCHING)}；两者都不成立时，兜底就是 {@code Pose.SWIMMING}
 * （0.6×0.6、眼高 0.4 → 第三人称趴平 + 视角下移，正是用户看到的现象）。
 *
 * <p>而这两个判定用的是 {@code CollisionGetter#noCollision(entity, box)}，
 * 它**把实体边界也算作障碍**（{@code getEntityCollisions} → {@code Entity#canCollideWith} →
 * 对方实体的 {@code canBeCollidedWith()}）。于是只要有一个
 * {@code canBeCollidedWith() == true} 的实体与玩家的站姿盒（0..1.8）重叠，
 * 玩家就会被按成匍匐 —— 1.7.10 没有这套「姿态拟合」，所以原作即使 box 重叠也不会趴下。
 *
 * <p>模组的弹/效果实体正好生成在**使用者自己的坐标**上（{@code THShotLib.pos_Living} = 脚底 + 1.12 格），
 * 因此右键发射/宣言后立刻就会踩中这条规则。
 *
 * <h2>本测试的两个作用</h2>
 * <ol>
 *   <li>{@link #boatAtEyeHeightDisplacesStandingPose}：用**原版船**（{@code canBeCollidedWith()==true}）
 *       放在玩家眼高，证明上述机理真实存在（这是修复前模组实体所处的位置，属"红证"级的机制证据，
 *       且不依赖模组代码，故不会被以后的模组改动掩盖）；</li>
 *   <li>{@link #modEntitiesAtEyeHeightDoNotDisplaceStandingPose} 与
 *       {@link #shotFiredFromPlayerKeepsStandingPose}：钉死修复结果——
 *       diag-pose 把 5 个「在玩家身上生成」的实体改成 {@code canBeCollidedWith()==false} 后，
 *       即使 box 依旧与站姿盒重叠，站姿仍然放得下（= 玩家不会趴下），
 *       同时 {@code isPickable()}/{@code canBeHitByProjectile()} 依旧为 true（I8 的战果不丢）。</li>
 * </ol>
 *
 * <p>之所以直接断言 {@code noCollision(...)} 而不用「tick 玩家再看 {@code getPose()}」：
 * mock player 不在世界的 tick 列表里，而且 {@code Player#updatePlayerPose()} 是 protected。
 * 直接调用判定所依赖的同一个公开谓词，等价且更稳（该谓词的参数组合与
 * {@code makeBoundingBox(position())} 一一对应）。
 *
 * <p>模板与 mock player 的约定见 {@link RegistryCompletenessTest}（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerPoseRegressionTest {

    private static final String TEMPLATE = "empty";

    /** 玩家站位（相对坐标，浮空以免方块碰撞干扰判定）。 */
    private static final Vec3 USER_POS = new Vec3(3.5D, 3.0D, 3.5D);

    private PlayerPoseRegressionTest() {}

    /**
     * 机理证据：把 {@code canBeCollidedWith()==true} 的原版船放在玩家眼高，
     * 玩家的站姿与蹲姿就都放不下了（→ 原版兜底 = 匍匐）。移开后又恢复。
     *
     * <p>这正是修复前模组弹/效果实体所处的位置（弹生成在脚底 +1.12 格）。
     * 用原版船而不是模组实体，是因为它天然保留 {@code canBeCollidedWith()==true}，
     * 所以这条断言不会随模组的修复一起变绿——它证明的是**平台机理**本身。
     */
    @GameTest(template = TEMPLATE)
    public static void boatAtEyeHeightDisplacesStandingPose(GameTestHelper helper) {
        Player player = placeUser(helper);

        helper.assertTrue(standingFits(helper, player),
                "前提：玩家周围没有任何实体时，站姿必须放得下");
        helper.assertTrue(crouchingFits(helper, player),
                "前提：玩家周围没有任何实体时，蹲姿必须放得下");

        // 船的原版尺寸 1.375×0.5625、canBeCollidedWith()==true。放在眼高（脚底 +1.12）就会
        // 与站姿盒(0..1.8)与蹲姿盒(0..1.5)相交，但与匍匐盒(0..0.6)不相交。
        Vec3 eyePos = helper.absoluteVec(USER_POS.add(0.0D, 1.12D, 0.0D));
        Entity boat = helper.spawn(EntityType.BOAT, USER_POS.add(0.0D, 1.12D, 0.0D));

        helper.assertTrue(boat.canBeCollidedWith(),
                "前提：原版船必须 canBeCollidedWith()==true（本测试靠它演示平台机理）");
        AABB standing = player.getDimensions(Pose.STANDING).makeBoundingBox(player.position());
        helper.assertTrue(boat.getBoundingBox().intersects(standing),
                "前提：眼高的船必须与玩家站姿盒相交（否则演示不到机理）: boat=" + boat.getBoundingBox()
                        + " standing=" + standing);

        helper.assertFalse(standingFits(helper, player),
                "眼高有 canBeCollidedWith 实体时，站姿应当放不下（→ 原版会落到 Pose.SWIMMING）");
        helper.assertFalse(crouchingFits(helper, player),
                "同一位置蹲姿也应放不下（原版先试站姿、再试蹲姿，最后兜底才是匍匐）");

        boat.discard();
        helper.assertTrue(standingFits(helper, player),
                "移开实体后站姿应当重新放得下（判定的输入确实是实体边界）");
        helper.succeed();
    }

    /**
     * 修复结果：会在玩家身上生成的模组实体全部 {@code canBeCollidedWith()==false}，
     * 因此即使它们与玩家站姿盒重叠，玩家也不会被按成匍匐。
     *
     * <p>同时钉死 I8 的战果：{@code isPickable()}/{@code canBeHitByProjectile()} 仍按各自语义生效
     * （瞄准/交互与弹丸命中都不依赖 {@code canBeCollidedWith()}）。
     *
     * <p><b>弹（{@code normal_shot}）是特例</b>：{@code isPickable()} 为
     * {@code getAnimationCount() >= 1}，{@code helper.spawn} 出来的是<b>延迟相位</b>（false）。
     * 但 {@code EntitySanaeWind} 等 {@code EntityTHShot} 的子类覆写了 {@code isPickable() == true}
     * （1.7.10 同样如此），所以只对<b>裸的 {@code normal_shot}</b> 断言「延迟相位不可瞄」，
     * 其余实体一律要求 {@code isPickable() == true}。
     * 相位相关的完整契约见 {@link ShotAnimationCountBoundaryTest} / {@link EntityPickableTest}。
     */
    @GameTest(template = TEMPLATE)
    public static void modEntitiesAtEyeHeightDoNotDisplaceStandingPose(GameTestHelper helper) {
        Player player = placeUser(helper);

        List<EntityType<?>> types = List.of(
                ModEntities.NORMAL_SHOT.get(),         // 弾（1.7.10 canBeCollidedWith = animCount>=1）
                ModEntities.SANAE_WIND.get(),          // 早苗の風起し（true 固定）
                ModEntities.SILVER_KNIFE.get(),        // 銀のナイフ（true 固定）
                ModEntities.DIVINE_SPIRIT.get(),       // 小神霊（true 固定）
                ModEntities.SHOT_MATERIAL_ENTITY.get());// 弾の素（true 固定）

        List<String> failures = new java.util.ArrayList<>();
        for (EntityType<?> type : types) {
            Entity entity = helper.spawn(type, USER_POS.add(0.0D, 1.12D, 0.0D));
            String name = EntityType.getKey(type).toString();

            if (entity.canBeCollidedWith()) {
                failures.add(name + ": canBeCollidedWith()==true（眼高でプレイヤーの姿勢を弾く）");
            }
            if (entity instanceof EntityTHShot shot && type == ModEntities.NORMAL_SHOT.get()) {
                // 素の弾だけ isPickable() = getAnimationCount() >= 1。spawn 直後は遅延相位なので false。
                if (shot.isPickable()) {
                    failures.add(name + ": 遅延相位の弾が狙える（isPickable は getAnimationCount() >= 1 の窓口）");
                }
            } else if (!entity.isPickable()) {
                failures.add(name + ": isPickable()==false（I8 の照準/打ち返しが死ぬ）");
            }
            entity.discard();
        }

        helper.assertTrue(failures.isEmpty(), "姿势/命中の前提が崩れている: " + failures);
        helper.assertTrue(standingFits(helper, player),
                "模组实体在眼高しても玩家的站姿必须放得下（＝不会趴下）");
        helper.succeed();
    }

    /**
     * 真实路径回归：用 {@code THShotLib.createShot} 在**玩家自身坐标**（= 符卡/射击物品的实际生成点）
     * 造一颗 1.0 格的弹，弹盒与玩家站姿盒确实重叠，但玩家站姿仍放得下。
     *
     * <p>修复前这里会是 false（→ {@code Pose.SWIMMING}），修复后为 true。
     * 顺带把「弹盒的确与站姿盒相交」写死，避免以后有人把生成高度改到别处后本测试变成空转。
     */
    @GameTest(template = TEMPLATE)
    public static void shotFiredFromPlayerKeepsStandingPose(GameTestHelper helper) {
        Player user = placeUser(helper);

        Vec3 spawnPos = THShotLib.pos_Living(user);
        EntityTHShot shot = THShotLib.createShot(user, spawnPos, THShotLib.angle(0.0D, 0.0D, 1.0D), 0.02D,
                ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.RED, 1.0F, 2.0F, 0, 200));

        AABB standing = user.getDimensions(Pose.STANDING).makeBoundingBox(user.position());
        helper.assertTrue(shot.getBoundingBox().intersects(standing),
                "前提：弹生成在玩家自身坐标（脚底+1.12）时，弹盒必须与站姿盒重叠: shot="
                        + shot.getBoundingBox() + " standing=" + standing);

        helper.assertFalse(shot.canBeCollidedWith(),
                "弹在玩家身上生成，绝不能参与玩家的姿势拟合（diag-pose 修复）");
        helper.assertTrue(standingFits(helper, user),
                "发射后玩家的站姿必须放得下（＝不会自动趴下）");
        helper.assertTrue(crouchingFits(helper, user), "蹲姿同样应当放得下");

        shot.discard();
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    private static Player placeUser(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // mock player 不在实体 tick 列表里，构造后必须显式 moveTo 才会把包围盒刷到目标位置
        // （与 SpellCardDeclarationTest 同一约定；不刷的话判定还停在 (0,0,0)）。
        Vec3 pos = helper.absoluteVec(USER_POS);
        player.moveTo(pos, 0.0F, 0.0F);
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    /** {@code Player#updatePlayerPose()} 里 {@code canPlayerFitWithinBlocksAndEntitiesWhen(STANDING)} 的等价判定。 */
    private static boolean standingFits(GameTestHelper helper, Player player) {
        return fits(helper, player, Pose.STANDING);
    }

    private static boolean crouchingFits(GameTestHelper helper, Player player) {
        return fits(helper, player, Pose.CROUCHING);
    }

    private static boolean fits(GameTestHelper helper, Player player, Pose pose) {
        return helper.getLevel().noCollision(player,
                player.getDimensions(pose).makeBoundingBox(player.position()).deflate(1.0E-7D));
    }
}