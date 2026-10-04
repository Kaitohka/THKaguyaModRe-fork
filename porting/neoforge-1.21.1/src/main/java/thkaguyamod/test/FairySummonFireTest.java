package thkaguyamod.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.entity.living.EntityTHFairy;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;
import thkaguyamod.registry.DanmakuPatternRegistry;

/**
 * diag-fairy-fire 回归：{@code /summon thkaguyamod:th_fairy} 后「弹卡在身体里 + 每 tick 连发」。
 *
 * <h2>根因</h2>
 * {@code /summon} 与存档加载一样走 {@code Entity.load(tag)} → {@code readAdditionalSaveData(tag)}，
 * 但 summon 的 tag 只含 {@code id}/{@code Pos}。1.21.1 的 {@code CompoundTag#getShort/getFloat/...}
 * 在键缺失时返回 <b>0</b>，于是 {@code EntityDanmakuMob#readAdditionalSaveData} 把构造函数
 * （{@code EntityTHFairy#setPattern} 从 {@link DanmakuPatternRegistry} 读出的）正确值全部覆盖成 0：
 * <ul>
 *   <li>{@code danmakuSpan = 0} → {@code EntityTHFairy.tick()} 的
 *       {@code attackCounter > danmakuSpan → attackCounter = 0} 每 tick 复位 → 每 tick 开火；</li>
 *   <li>{@code speedRate = 0.0F} → {@code createSphereShot(..., speed, ...)} 的
 *       firstSpeed/limitSpeed/acceleration 全为 0 → 弹速度恒为 0，停在生成点（= 妖精 {@code pos()}）。</li>
 * </ul>
 * 自然生成 / 刷怪蛋走 {@code EntityType.create(level)}，只跑构造函数、不读 NBT，因此不受影响
 * —— 这正是「只有 /summon 复现」的原因。
 *
 * <p>模板 {@code thkaguyamod:empty}（7×7×7 空结构）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FairySummonFireTest {

    private static final String TEMPLATE = "empty";

    /** 妖精位置（相对坐标）。 */
    private static final Vec3 FAIRY_POS = new Vec3(3.5D, 2.0D, 3.5D);

    /** 目标位置：落在 detectionDistance(8) / attackDistance(8) 之内，空结构内视线通畅。 */
    private static final Vec3 TARGET_POS = new Vec3(3.5D, 2.0D, 6.5D);

    /** 弹速判定阈值（speedRate 最小 0.1 时的每 tick 位移，留足余量）。 */
    private static final double MIN_SHOT_SPEED = 0.05D;

    private FairySummonFireTest() {}

    // ------------------------------------------------------------------ //
    // 1. 机制：部分 NBT 不得覆盖构造函数从样式表读到的值
    // ------------------------------------------------------------------ //

    /**
     * 用「只有 id 的 tag」（等价 /summon 的 NBT）走 {@code Entity.load}，断言弹幕样式参数
     * 没有被缺键的 0 覆盖；同时断言显式给出的键仍然生效、完整 NBT 往返仍然生效。
     *
     * <p>为什么不断言「等于某个具体数值」：样式表按 {@code danmakuLevel} 分档，且 patternId
     * 由 {@code random} 选，具体值不唯一。这里改用三个更强的性质：
     * <ol>
     *   <li>值必须 &gt; 0（覆盖后必然是 0，这是本 bug 的特征）；</li>
     *   <li>值必须是样式表里真实注册过的 (span, speed) 组合（证明来自构造函数）；</li>
     *   <li>显式写入的键必须被读回（证明 contains 守卫没有把「存档加载」一起废掉）。</li>
     * </ol>
     */
    @GameTest(template = TEMPLATE)
    public static void summonLikePartialNbtKeepsPatternStats(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // /summon 等价路径：只有 id 的 tag 走 load → readAdditionalSaveData。
        CompoundTag summonLike = new CompoundTag();
        summonLike.putString("id", "thkaguyamod:th_fairy");
        Entity loaded = EntityType.loadEntityRecursive(summonLike, level, e -> e);
        helper.assertTrue(loaded instanceof EntityTHFairy, "load 没有产出 th_fairy: " + loaded);
        EntityTHFairy fairy = (EntityTHFairy) loaded;

        helper.assertTrue(fairy.danmakuSpan > 0,
                "summon 后 danmakuSpan 被 NBT 缺键覆盖成 " + fairy.danmakuSpan
                        + "（0 表示每 tick 连发）——readAdditionalSaveData 未做 contains 守卫");
        helper.assertTrue(fairy.speedRate > 0.0F,
                "summon 后 speedRate 被 NBT 缺键覆盖成 " + fairy.speedRate
                        + "（0 会让弹停在生成点）——readAdditionalSaveData 未做 contains 守卫");
        helper.assertTrue(fairy.getAttackDistance() > 0.0D,
                "summon 后 attackDistance 被 NBT 缺键覆盖成 " + fairy.getAttackDistance());

        // 值必须来自样式表（构造函数路径），而不是别的兜底。
        helper.assertTrue(DanmakuPatternRegistry.span.containsValue(fairy.danmakuSpan),
                "danmakuSpan=" + fairy.danmakuSpan + " 不在样式表 span 值集合里: "
                        + DanmakuPatternRegistry.span.values());
        helper.assertTrue(DanmakuPatternRegistry.speed.containsValue(fairy.speedRate),
                "speedRate=" + fairy.speedRate + " 不在样式表 speed 值集合里: "
                        + DanmakuPatternRegistry.speed.values());

        // 反向：显式写入的键必须被读回（存档加载语义不能被破坏）。
        EntityTHFairy probe = new EntityTHFairy(ModEntities.TH_FAIRY.get(), level);
        CompoundTag explicit = new CompoundTag();
        probe.saveWithoutId(explicit);
        explicit.putString("id", "thkaguyamod:th_fairy");
        explicit.putShort("Span", (short) 77);
        explicit.putFloat("Speed", 0.42F);
        explicit.putByte("AttackRange", (byte) 5);
        Entity reloaded = EntityType.loadEntityRecursive(explicit, level, e -> e);
        helper.assertTrue(reloaded instanceof EntityTHFairy, "完整 NBT 重新加载失败");
        EntityTHFairy reloadedFairy = (EntityTHFairy) reloaded;
        helper.assertValueEqual(reloadedFairy.danmakuSpan, 77, "显式写入的 Span 必须被读回");
        helper.assertTrue(reloadedFairy.speedRate == 0.42F,
                "显式写入的 Speed 必须被读回: " + reloadedFairy.speedRate);
        helper.assertTrue(reloadedFairy.getAttackDistance() == 5.0D,
                "显式写入的 AttackRange 必须被读回: " + reloadedFairy.getAttackDistance());

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 2. 直调开火：弹必须「有速度」且「离开生成点」（= 截图里的「卡在身体里」）
    // ------------------------------------------------------------------ //

    /**
     * 绕过 AI 索敌直调 {@code danmakuPattern}，把「开火 → 弹速」与「索敌 → 开火」两段解耦。
     *
     * <p>修复前 {@code speedRate=0} ⇒ {@code getSpeed()==0} 且位置恒等于生成点（妖精胸口）。
     *
     * <h2>偶发红的真因（本用例的原始写法）</h2>
     * 原始断言「6 tick 后所有弹位移都必须达标」会偶发红，实测数据（诊断日志）显示：
     * 弹的 {@code getSpeed()} 是正常的 {@code speedRate}（0.15〜0.3），但 {@code tickCount==0}
     * 就被 {@code discard} 了 —— 也就是<b>在踏出第一 tick 之前</b>被外部消掉。
     * 消掉它们的是同批次其它用例触发的 {@link thkaguyamod.danmaku.THShotLib#danmakuRemove}：
     * <ul>
     *   <li>符卡宣言（{@code THKaguyaLib#checkSpellCardDeclaration}）→ {@code danmakuRemove(user, 40, "Enemy")}；</li>
     *   <li>{@code EntitySpellCard} 展开中 → {@code danmakuRemove(user, 40, "Other")}；</li>
     *   <li>{@code EntityDanmakuMob#moveDanmakuAttack} → {@code danmakuRemove(this, 40, "Enemy")}。</li>
     * </ul>
     * GameTest 的结构是按 12 格间距网格排列的（7×7 结构 + 5 格间隙），40 格半径会扫到邻接结构，
     * 而本测试的弹属于非 Player 的妖精，正好落进 "Enemy"/"Other" 的清除条件。
     * 因此位移判定必须容忍「弹在 tick 之前被外部清除」这一情形；
     * 而「{@code speedRate=0} ⇒ 弹速 0」这一回归由生成瞬间的确定性断言（下）负责捕获。
     */
    @GameTest(template = TEMPLATE)
    public static void directDanmakuPatternProducesMovingShot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // 必须显式设为非 PEACEFUL：EntityDanmakuMob.tick() 在 PEACEFUL 会 discard 妖精，
        // 妖精一死，它的弹（非 Player 持有）就会走 shotFinishBonus 变成道具而「消失」，
        // 让本测试变成随机失败（测试批次顺序不同 → 继承的难度不同）。
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);

        CompoundTag summonLike = new CompoundTag();
        summonLike.putString("id", "thkaguyamod:th_fairy");
        Entity loaded = EntityType.loadEntityRecursive(summonLike, level, e -> e);
        helper.assertTrue(loaded instanceof EntityTHFairy, "load 没有产出 th_fairy");
        EntityTHFairy fairy = (EntityTHFairy) loaded;

        Vec3 abs = helper.absoluteVec(FAIRY_POS);
        fairy.moveTo(abs.x, abs.y, abs.z, 0.0F, 0.0F);
        level.addFreshEntity(fairy);

        fairy.attackCounter = 0;
        fairy.danmakuPattern(DanmakuConstants.NORMAL);

        List<EntityTHShot> shots = level.getEntitiesOfClass(EntityTHShot.class,
                fairy.getBoundingBox().inflate(6.0D));
        helper.assertFalse(shots.isEmpty(),
                "danmakuPattern 没有产出弹（span=" + fairy.danmakuSpan + " speed=" + fairy.speedRate + "）");

        // 回归核心（确定性）：弹速必须来自 speedRate。speedRate=0 的变异在这里必红，
        // 不依赖任何 world tick，因此不受同批次其它用例的弹消し干扰。
        for (EntityTHShot s : shots) {
            helper.assertTrue(s.getSpeed() > MIN_SHOT_SPEED,
                    "直调路径的弹速度为 0（卡在生成点）: " + s.getSpeed()
                            + "（speedRate=" + fairy.speedRate + "）");
        }

        // 位移判定：只对「真正踏过 world tick 的弹」成立。
        // 若全部弹都在 tick 之前被外部 danmakuRemove 清掉（tickCount==0 で discard）、
        // 位移无从谈起，此时以生成瞬间的弹速断言为准（上面的循环）。
        List<Vec3> starts = shots.stream().map(Entity::position).toList();
        final double[] maxMoved = {0.0D};
        final boolean[] anyTicked = {false};
        helper.onEachTick(() -> {
            for (int i = 0; i < shots.size(); i++) {
                EntityTHShot s = shots.get(i);
                if (s.tickCount >= 1) {
                    anyTicked[0] = true;
                    maxMoved[0] = Math.max(maxMoved[0], s.position().distanceTo(starts.get(i)));
                }
            }
        });
        helper.runAtTickTime(6, () -> {
            int killedBeforeTick = 0;
            for (EntityTHShot s : shots) {
                if (s.isRemoved() && s.tickCount == 0) {
                    killedBeforeTick++;
                }
            }
            THKaguyaMod.LOGGER.info(
                    "DIAG-FAIRY difficulty={} fairyAlive={} speedRate={} span={} shots={} maxMoved={} "
                            + "anyTicked={} killedBeforeTick={}",
                    level.getDifficulty(), fairy.isAlive(), fairy.speedRate, fairy.danmakuSpan,
                    shots.size(), maxMoved[0], anyTicked[0], killedBeforeTick);

            if (killedBeforeTick == shots.size()) {
                // 全弹在踏出第一 tick 之前被同批次其它用例的 danmakuRemove 清除。
                // 弹速的确定性断言已通过 ⇒ 回归（speedRate=0）不存在，这里不再用位移判负。
                helper.assertTrue(true,
                        "全弹在 tick 前被外部弹消し清除（已由生成瞬间的弹速断言覆盖）");
            } else {
                helper.assertTrue(maxMoved[0] > MIN_SHOT_SPEED * 3.0D,
                        "存活到 tick 之后的弹没有位移（maxMoved=" + maxMoved[0] + " 格，shots=" + shots.size()
                                + "，killedBeforeTick=" + killedBeforeTick + "）——弹速链路疑被归零");
            }
            shots.forEach(Entity::discard);
            fairy.discard();
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ //
    // 3. 端到端：妖精 + 目标 + 真实 tick，弹必须移动
    // ------------------------------------------------------------------ //

    /**
     * 真实驱动：spawn 妖精 + 僵尸目标 + 真实 tick，断言妖精确实开火，且**发射瞬间**的弹
     * {@code speed > 0}，并且随后确实位移过。
     *
     * <p>目标必须放在结构**内部**：GameTest 会给结构四周加屏障墙，墙外目标会被
     * {@code hasLineOfSight} 判为不可见 → 妖精不开火（本测试第一版就踩了这个坑）。
     * 结构是 7×7×7，故目标放在妖精前方 2.5 格处。
     *
     * <p>修复前 {@code speedRate=0} ⇒ 弹的 {@code motionX/Y/Z} 恒为 0，弹停在妖精胸口。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void summonedFairyShotsMove(GameTestHelper helper) {
        // EntityDanmakuMob.tick() 在 PEACEFUL 会 discard（1.7.10 同），GameTest 默认 PEACEFUL。
        helper.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);

        EntityTHFairy fairy = helper.spawn(ModEntities.TH_FAIRY.get(), FAIRY_POS);

        Zombie target = helper.spawn(EntityType.ZOMBIE, TARGET_POS);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setInvulnerable(true);

        // mock player 不在世界里，getNearestPlayer 找不到它，因此显式指定目标
        // （等价于「玩家在旁边」的实测条件）。
        fairy.setTarget(target);
        helper.assertTrue(fairy.getTarget() == target, "前提: 妖精应锁定目标");

        final EntityTHShot[] firstShot = new EntityTHShot[1];
        final Vec3[] firstPos = new Vec3[1];
        final double[] speedAtFirstSeen = new double[]{-1.0D};
        final double[] maxMoved = new double[]{0.0D};
        final int[] firstSeenTick = new int[]{-1};

        helper.onEachTick(() -> {
            if (firstShot[0] == null) {
                List<EntityTHShot> shots = helper.getLevel().getEntitiesOfClass(EntityTHShot.class,
                        fairy.getBoundingBox().inflate(8.0D));
                if (shots.isEmpty()) {
                    return;
                }
                firstShot[0] = shots.get(0);
                firstPos[0] = firstShot[0].position();
                speedAtFirstSeen[0] = firstShot[0].getSpeed();
                firstSeenTick[0] = (int) helper.getTick();
                return;
            }
            if (!firstShot[0].isRemoved()) {
                maxMoved[0] = Math.max(maxMoved[0], firstShot[0].position().distanceTo(firstPos[0]));
            }
        });

        helper.runAtTickTime(20, () -> {
            helper.assertTrue(firstShot[0] != null,
                    "妖精在 20 tick 内没有发射任何弹（开火链路断了；span=" + fairy.danmakuSpan
                            + " speed=" + fairy.speedRate + " attack=" + fairy.getAttackDistance()
                            + " targetAlive=" + target.isAlive() + "）");
            helper.assertTrue(speedAtFirstSeen[0] > MIN_SHOT_SPEED,
                    "发射瞬间的弹速度为 " + speedAtFirstSeen[0] + "（应 > " + MIN_SHOT_SPEED
                            + "）—— speedRate=" + fairy.speedRate + "，弹会卡在妖精身体里");
            helper.assertTrue(maxMoved[0] > MIN_SHOT_SPEED * 3.0D,
                    "弹没有离开生成点: 观测到的最大位移仅 " + maxMoved[0] + " 格（生成于 " + firstPos[0]
                            + "，首见于 tick " + firstSeenTick[0] + "）");
            if (!firstShot[0].isRemoved()) {
                firstShot[0].discard();
            }
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ //
    // 4. 端到端：开火间隔与样式表 span 同量级（不是每 tick）
    // ------------------------------------------------------------------ //

    /**
     * 统计观察窗口内**首次出现新弹实体的 tick**（= 开火 tick），断言开火次数与
     * {@code window / span} 同量级（不是每 tick）。
     *
     * <p>注意不能直接数弹实体：同一发 pattern 可能一次射出多发（例如 d=400 ⇒ num=5、
     * way=1 时一次 5 发），实体数 ≠ 开火次数。
     *
     * <p>修复前 {@code span=0} → 每 tick 开火一次 → 开火 tick 数 ≈ window。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void summonedFairyFireIntervalMatchesSpan(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);

        EntityTHFairy fairy = helper.spawn(ModEntities.TH_FAIRY.get(), FAIRY_POS);
        Zombie target = helper.spawn(EntityType.ZOMBIE, TARGET_POS);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setInvulnerable(true);
        fairy.setTarget(target);

        final int span = fairy.danmakuSpan;
        helper.assertTrue(span > 0, "前提: span 应 > 0（实际 " + span + "）");

        final int window = Math.min(span * 3 + 20, 160);
        final Set<Integer> seenShotIds = new LinkedHashSet<>();
        final Set<Integer> fireTicks = new LinkedHashSet<>();

        helper.onEachTick(() -> {
            boolean sawNew = false;
            for (EntityTHShot s : helper.getLevel().getEntitiesOfClass(EntityTHShot.class,
                    fairy.getBoundingBox().inflate(8.0D))) {
                if (seenShotIds.add(s.getId())) {
                    sawNew = true;
                }
            }
            if (sawNew) {
                fireTicks.add((int) helper.getTick());
            }
        });

        helper.runAtTickTime(window, () -> {
            int maxExpected = window / span + 3;
            helper.assertTrue(fireTicks.size() >= 2,
                    "观察窗口(" + window + " tick)内只开火 " + fireTicks.size()
                            + " 次（span=" + span + " 时应 ≥2 次；妖精是否没开火？）");
            helper.assertTrue(fireTicks.size() <= maxExpected,
                    "开火次数过多（每 tick 连发）: " + fireTicks.size() + " 次 / " + window
                            + " tick，span=" + span + "，上限 " + maxExpected
                            + "；开火 tick=" + fireTicks + "；实体总数=" + seenShotIds.size());
            helper.succeed();
        });
    }
}
