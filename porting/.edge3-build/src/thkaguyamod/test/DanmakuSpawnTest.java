package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
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
 * 阶段 5 质量基建：弹幕生成 / 运动 / 寿命的冒烟测试。
 *
 * <p>为什么值得测：{@link EntityTHShot} 的移动不是原版 {@code DeltaMovement}，而是自己维护
 * {@code motionX/Y/Z} 并在 {@code tick()} 里手动 {@code setPos}；同时 {@code tick()} 开头有
 * 三条「立刻 discard」的守卫（user == null / damage &lt;= 0 / 动画计数超过 endTime）。
 * 任何一条被改错，弹幕就会「生成即消失」或「永远不消失」——这在客户端很难肉眼定位，
 * 但服务端断言一次就能说清楚。
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #smallShotSpawnsAndMoves}：{@code THShotLib.createShot} 生成小弹 →
 *       实体确实进了世界（{@code getType() == normal_shot}）→ tick 若干次后仍存活且位置移动过。</li>
 *   <li>{@link #shotExpiresAfterEndTime}：{@code ShotData.end = 10} 的小弹，tick 到 25 后必须已被移除。</li>
 * </ol>
 *
 * <h2>环境假设（写死在这里，改结构尺寸时同步改）</h2>
 * <ul>
 *   <li>模板 {@code thkaguyamod:empty} = 7×7×7 空结构（{@code data/thkaguyamod/structure/empty.nbt}，
 *       由 tools/make_empty_structure.py 生成）。</li>
 *   <li>GameTest 会给结构四周（含 x/z 四个面）自动加屏障墙，所以弹幕不能飞太远：
 *       从中心 (3,3,3) 出发，速度 0.25/格每 tick、只 tick 8 次 = 移动 2 格，离墙（x=6）还有 1 格余量。</li>
 *   <li>发射者用 {@link GameTestHelper#makeMockPlayer}：它不会被加入世界，因此既不会被弹幕打到，
 *       也不会自己走动，弹幕的 {@code user == null} 守卫仍能通过（非 null 即可）。</li>
 * </ul>
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuSpawnTest {

    private static final String TEMPLATE = "empty";

    /** 结构中心（相对坐标）。 */
    private static final Vec3 CENTER = new Vec3(3.5D, 3.5D, 3.5D);

    /** 运动测试用速度（格/tick）。 */
    private static final double MOVE_SPEED = 0.25D;

    /** 运动测试 tick 数：8 tick × 0.25 = 2.0 格。 */
    private static final int MOVE_TEST_TICKS = 8;

    /** 寿命测试的 endTime。 */
    private static final int LIFETIME_END = 10;

    private DanmakuSpawnTest() {}

    @GameTest(template = TEMPLATE)
    public static void smallShotSpawnsAndMoves(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 spawnPos = helper.absoluteVec(CENTER);
        Vec3 direction = THShotLib.angle(1.0D, 0.0D, 0.0D);
        ShotData data = ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.RED, 0.3F, 2.0F, 0, 200);

        EntityTHShot shot = THShotLib.createShot(user, spawnPos, direction, MOVE_SPEED, data);

        helper.assertTrue(shot != null, "THShotLib.createShot returned null");
        helper.assertValueEqual(shot.getType(), ModEntities.NORMAL_SHOT.get(), "spawned shot entity type");
        helper.assertFalse(shot.isRemoved(), "shot was removed immediately after spawning");
        helper.assertValueEqual(shot.getShotEndTime(), 200, "shot end time taken from ShotData.end");

        helper.runAtTickTime(MOVE_TEST_TICKS, () -> {
            helper.assertFalse(shot.isRemoved(),
                    "shot died within " + MOVE_TEST_TICKS + " ticks although endTime=200 and damage>0");
            double moved = shot.position().distanceTo(spawnPos);
            helper.assertTrue(moved > 0.0D,
                    "shot never moved: motionX/Y/Z did not translate into a position change (moved=" + moved + ")");
            // 期望位移 = 8 tick × 0.25 = 2.0；只要求下界，避免被首 tick 时序或浮点误差干扰
            helper.assertTrue(moved >= MOVE_SPEED * 4,
                    "shot moved less than 4 ticks' worth of motion (moved=" + moved + ", expected >= "
                            + (MOVE_SPEED * 4) + ")");
            helper.assertTrue(shot.getX() > spawnPos.x,
                    "shot drifted in the wrong direction: x=" + shot.getX() + " startX=" + spawnPos.x);
            shot.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void shotExpiresAfterEndTime(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 spawnPos = helper.absoluteVec(CENTER);
        Vec3 direction = THShotLib.angle(0.0D, 0.0D, 1.0D);
        // 速度极小，保证在 endTime 之前撞不到屏障墙
        ShotData data = ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.BLUE, 0.3F, 2.0F, 0, LIFETIME_END);

        EntityTHShot shot = THShotLib.createShot(user, spawnPos, direction, 0.02D, data);
        helper.assertFalse(shot.isRemoved(), "shot was removed immediately after spawning");

        // endTime 判定为 getAnimationCount() > getShotEndTime()，即最快也要 11 tick 后才消失
        helper.runAtTickTime(LIFETIME_END * 2 + 5, () -> {
            helper.assertTrue(shot.isRemoved(),
                    "shot survived past its endTime=" + LIFETIME_END + " (animationCount="
                            + shot.getAnimationCount() + ")");
            helper.succeed();
        });
    }
}
