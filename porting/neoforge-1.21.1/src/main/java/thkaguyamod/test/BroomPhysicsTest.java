package thkaguyamod.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.item.EntityMarisaBroom;
import thkaguyamod.init.ModEntities;

/**
 * I9 回归族（第二批）+ diag-broom 診断批：{@link EntityMarisaBroom} の無人時物理。
 *
 * <p>修正内容（1.7.10 との差分を戻したもの）：
 * <ol>
 *   <li><b>無人時の減速が実体に届いていなかった</b>（「下骑不停」）。1.7.10 の
 *       {@code motionX *= 0.9D; motionY -= 0.03D; motionZ *= 0.9D;} に相当する処理が
 *       ローカル変数にしか書かれず {@code setDeltaMovement()} に届かない版があり、
 *       降りた後も箒が等速で飞び続けていた。修正後は {@code move()} の<b>前</b>に
 *       移動量へ反映される；</li>
 *   <li><b>着地時の垂直補償</b>。1.7.10 は地面上で毎 tick {@code motionY += 0.06D} を足して
 *       {@code -0.03} の沈降と釣り合わせていた。ただし 1.21.1 の {@code Entity#move()} は
 *       着地時に垂直成分を 0 にしないため、そのまま移植すると 2 tick 周期の上下動
 *       （実測 振幅 0.0141 ブロック）と運動量パケットの連続発火が起きる。
 *       接地中は {@code motionY = -0.03} に固定する（{@link #groundedBroomStaysStill} が回帰）。</li>
 * </ol>
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #unriddenBroomDeceleratesAfterDismount}：ゾンビを乗せてから降ろし、
 *       その直後に速度を与えて数 tick 進める → 速度模長が確実に減少していること；</li>
 *   <li>{@link #groundedBroomDoesNotSink}：石の上に置いた無人箒が 30 tick 後も
 *       地面の高さを保ち、下向き移動量が積み上がっていないこと；</li>
 *   <li>{@link #groundedBroomStaysStill}：着地後に上下振動せず、垂直移動量も毎 tick 一定；</li>
 *   <li>{@link #unriddenBroomGlideDistanceMatchesLegacy}：水平減衰が 0.9 × 0.99 倍/tick；</li>
 *   <li>{@link #unriddenBroomTravelDistanceMatchesLegacy}：滑走距離が理論部分和の 5% 以内。</li>
 * </ol>
 *
 * <p>テンプレートの約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BroomPhysicsTest {

    private static final String TEMPLATE = "empty";

    /** 箒の初期位置（構造中心、地面より浮かせて衝突を避ける）。 */
    private static final Vec3 BROOM_POS = new Vec3(3.5D, 3.0D, 3.5D);

    /** 与える水平速度（格/tick）。小さいので 6 tick でも壁に届かない。 */
    private static final double INITIAL_SPEED = 0.3D;

    /** 降車後の観測 tick 数（0.9^6 ≒ 0.53 まで落ちる）。 */
    private static final int POST_DISMOUNT_TICKS = 6;

    /** 1 tick あたりの無人時水平減衰（無人ブロックの 0.9 × 移動後抵抗 0.99）。 */
    private static final double DECAY_PER_TICK = 0.9D * 0.99D;

    private BroomPhysicsTest() {}

    /**
     * 下骑（降車）後に箒が減速すること — レガシー「下骑不停」の回帰テスト。
     * <p>騎乗中は騎手の視線方向へ加速する分岐、無人時は減速する分岐に分かれる。
     * 無人時の減速が {@code setDeltaMovement()} に届かない版では、このテストの
     * 速度模長が初期値のまま（または増加）になる。
     */
    @GameTest(template = TEMPLATE)
    public static void unriddenBroomDeceleratesAfterDismount(GameTestHelper helper) {
        EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), BROOM_POS);
        Zombie rider = helper.spawn(EntityType.ZOMBIE, BROOM_POS.add(0.0D, 1.0D, 0.0D));
        rider.setNoAi(true);
        rider.setNoGravity(true);

        rider.startRiding(broom, true);
        helper.assertTrue(broom.getFirstPassenger() == rider,
                "扫帚に騎手を乗せられなかった（前提条件。乗車可否の実装を確認）");

        // 降車 → 無人状態で水平速度を与える
        rider.stopRiding();
        helper.assertTrue(broom.getFirstPassenger() == null, "stopRiding 後は無人のはず（前提条件）");
        broom.setDeltaMovement(INITIAL_SPEED, 0.0D, 0.0D);
        double startSpeed = broom.getDeltaMovement().length();

        helper.runAtTickTime(POST_DISMOUNT_TICKS, () -> {
            helper.assertFalse(broom.isRemoved(), "扫帚が消えた（減速ではなく消滅している）");
            double endSpeed = broom.getDeltaMovement().length();
            helper.assertTrue(endSpeed < startSpeed,
                    "無人になった扫帚が減速していない（下骑不停の回帰）: start=" + startSpeed + " end=" + endSpeed);
            helper.assertTrue(endSpeed < startSpeed * 0.8D,
                    "無人時の減速率が 1.7.10 の 0.9/tick 相当に届いていない（" + POST_DISMOUNT_TICKS
                            + " tick 後）: start=" + startSpeed + " end=" + endSpeed);

            // 水平速度が十分に落ちていること（下向き成分は着地補償の影響を受けるので見ない）
            double horizontal = Math.sqrt(broom.getDeltaMovement().x * broom.getDeltaMovement().x
                    + broom.getDeltaMovement().z * broom.getDeltaMovement().z);
            helper.assertTrue(horizontal < INITIAL_SPEED * 0.8D,
                    "水平速度が残りすぎている（下骑後も前進し続けている）: horizontal=" + horizontal);

            broom.discard();
            rider.discard();
            helper.succeed();
        });
    }

    /**
     * 着地した無人箒が地面の高さを保つこと（{@code motionY += 0.06} 補償の回帰）。
     * <p>石ブロック上面（y=2.0）の少し上に置いた箒を 30 tick 観測し、
     * ① 地面より下へ沈んでいない、② 下向き移動量が積み上がっていない、を検査する。
     * 補償（{@code onGround → motionY += 0.06}）が抜けると、毎 tick の {@code -0.03} が
     * 打ち消されず移動量が下限へ向かって積み上がる。
     */
    @GameTest(template = TEMPLATE)
    public static void groundedBroomDoesNotSink(GameTestHelper helper) {
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.STONE);

        // 構造の原点は絶対座標で y=-60 にあるため、「相対 y=1 の石の上面」= 絶対 y=-58.0。
        // helper.spawn は<b>相対座標</b>を取り内部で absoluteVec するのに対し、
        // 後から読む broom.getY() は<b>絶対座標</b>。期待値を相対値 2.0 で書くと必ず落ちる（このテストが踏んだ罠）。
        double groundTop = helper.absolutePos(ground).getY() + 1.0D;   // 絶対座標（= -58.0）
        // 少し浮かせて spawn する（BB が石にめり込んだ状態から始めない）。相対座標で 2.4 を渡す。
        EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), new Vec3(3.5D, 2.4D, 3.5D));
        helper.assertFalse(broom.isRemoved(), "扫帚の spawn に失敗した");

        helper.runAtTickTime(30, () -> {
            helper.assertFalse(broom.isRemoved(), "着地後の扫帚が消えた");
            helper.assertTrue(broom.getY() > groundTop - 0.5D,
                    "着地した扫帚が地面へ沈み込んでいる（motionY += 0.06 の補償漏れ）: y=" + broom.getY()
                            + " groundTop=" + groundTop);
            helper.assertTrue(broom.getDeltaMovement().y > -0.15D,
                    "下向き移動量が積み上がっている（着地補償が効いていない）: motionY="
                            + broom.getDeltaMovement().y);
            helper.assertTrue(broom.getY() < groundTop + 1.5D,
                    "着地した扫帚が浮き上がり続けている: y=" + broom.getY());

            broom.discard();
            helper.succeed();
        });
    }

    /**
     * 着地した無人箒が<b>静止</b>すること（上下振動しない・垂直速度が振動しない）。
     *
     * <p>根因（実測で確定）: 1.21.1 の {@code Entity#move()} は着地時に
     * {@code deltaMovement} の垂直成分を 0 にしない（{@code setOnGroundWithMovement} は
     * {@code onGround} を立てるだけ）。そのため 1.7.10 の {@code motionY += 0.06} を
     * そのまま移植すると、接地中も {@code -0.03}（沈降）に {@code +0.06}（補償）が乗って
     * 差の <b>+0.03/0.95</b> が毎 tick 上向きに残り、「浮き上がる → 落ちる → 着地」の
     * 2 tick 周期の上下動（実測 振幅 0.0141 ブロック、{@code motionY} は -0.022 / +0.007 を往復）が
     * 永久に続く。さらに {@code deltaMovement} が毎 tick 変化するため運動量パケットと
     * 補間が絶えず発火し、静止した箒が細かく震えて見える（「乱窜」の一部）。
     *
     * <p>修正（{@code EntityMarisaBroom} の無人ブロック）: 接地中は {@code motionY} を
     * 沈降分の {@code -0.03} に固定する。下降が毎 tick 地面に阻まれるので位置は一切動かず
     * （振幅 0）、摩擦後も値が常に一定（-0.0285）になるため運動量パケットも出ない。
     * なお {@code 0} に固定すると衝突判定が「動いていない」と判定して {@code onGround} が
     * 落ち、1 tick おきの振動に変わる（実測で確認）。
     */
    @GameTest(template = TEMPLATE)
    public static void groundedBroomStaysStill(GameTestHelper helper) {
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.STONE);

        EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), new Vec3(3.5D, 2.4D, 3.5D));
        double groundTop = helper.absolutePos(ground).getY() + 1.0D;

        java.util.List<Double> samples = new java.util.ArrayList<>();
        java.util.List<Double> motionYs = new java.util.ArrayList<>();
        helper.onEachTick(() -> {
            samples.add(broom.getY());
            motionYs.add(broom.getDeltaMovement().y);
        });

        helper.runAtTickTime(59, () -> {
            helper.assertTrue(samples.size() > 40, "サンプルが取れていない: " + samples.size());

            // 着地後の窓（tick 30-59）だけを見る
            java.util.List<Double> settled = samples.subList(30, samples.size());
            double min = settled.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            double max = settled.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
            double amplitude = max - min;

            // 接地後の垂直移動量は一定（振動しない）であること
            java.util.List<Double> mySettled = motionYs.subList(30, motionYs.size());
            double myMin = mySettled.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            double myMax = mySettled.stream().mapToDouble(Double::doubleValue).max().orElseThrow();

            String detail = "amplitude=" + amplitude + " min=" + min + " max=" + max
                    + " myMin=" + myMin + " myMax=" + myMax
                    + " yLast=" + samples.get(samples.size() - 1) + " groundTop=" + groundTop;

            helper.assertTrue(amplitude < 0.001D, "着地した掃帚が上下振動している: " + detail);
            helper.assertTrue(min > groundTop - 0.05D, "地面へ沈み込んでいる: " + detail);
            helper.assertTrue(max < groundTop + 0.05D, "地面から浮き上がっている: " + detail);
            helper.assertTrue(myMax - myMin < 1.0E-9D,
                    "接地中の垂直移動量が毎 tick 変化している（運動量パケットが止まらない）: " + detail);
            helper.assertTrue(Math.abs(myMax) < 0.05D, "接地中の垂直移動量が大きすぎる: " + detail);

            broom.discard();
            helper.succeed();
        });
    }

    /**
     * 【診断 diag-broom §C】無人箒の水平速度が 0.891 倍/tick（0.9 × 0.99）で減衰すること。
     *
     * <p>無人ブロックの {@code motionX *= 0.9} と移動後抵抗 {@code *0.99} がどちらも
     * 1 tick に 1 回入っていれば、水平速度は {@code 0.9 × 0.99 = 0.891} 倍/tick で
     * 単調減少する。0.8 格/tick（原作の上限）から出発した場合、
     * {@code Σ 0.8 × 0.891^n = 0.8 / (1 - 0.891) ≈ 7.34 格} で停止する。
     *
     * <p>断言は<b>サンプル数に依存しない</b>形にしてある: 最終 tick では
     * {@link GameTestHelper#onEachTick} のサンプラと {@link GameTestHelper#runAtTickTime} の
     * アサートが同じ {@code tickCount} で発火し、その順序が
     * {@code GameTestInfo} 内 {@code Object2LongOpenHashMap} の反復順に依存して
     * 非決定（サンプルが 6 個 or 7 個）になるため、期待値を固定指数で書くと偶発的に赤くなる。
     */
    @GameTest(template = TEMPLATE)
    public static void unriddenBroomGlideDistanceMatchesLegacy(GameTestHelper helper) {
        double v0 = 0.8D;
        EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), new Vec3(0.5D, 3.0D, 3.5D));
        broom.setDeltaMovement(v0, 0.0D, 0.0D);

        java.util.List<Double> speeds = new java.util.ArrayList<>();
        helper.onEachTick(() -> {
            Vec3 m = broom.getDeltaMovement();
            speeds.add(Math.sqrt(m.x * m.x + m.z * m.z));
        });

        helper.runAtTickTime(POST_DISMOUNT_TICKS, () -> {
            helper.assertTrue(speeds.size() >= 6, "サンプルが取れていない: " + speeds.size());

            StringBuilder detail = new StringBuilder("speeds=");
            for (double s : speeds) {
                detail.append(String.format("%.6f ", s));
            }

            for (int i = 1; i < speeds.size(); i++) {
                helper.assertTrue(speeds.get(i) <= speeds.get(i - 1) + 1.0E-9D,
                        "無人の水平速度が増加した tick がある（index " + i + "）: " + detail);
            }

            double first = speeds.get(0);
            double last = speeds.get(speeds.size() - 1);
            helper.assertTrue(last < first, "水平速度が落ちていない: " + detail);

            // 減衰率: 0.9 × 0.99 = 0.891 倍/tick。
            // サンプル数は 6 に固定できない: 最終 tick では「サンプラ（onEachTick）」と
            // 「このアサート（runAtTickTime）」が<b>同じ tickCount</b> で発火し、その順序は
            // GameTestInfo の runAtTickTimeMap（Object2LongOpenHashMap）の反復順に依存して
            // 非決定（サンプラが先なら 7 個、後なら 6 個）。旧実装は期待値の指数を
            // POST_DISMOUNT_TICKS = 6 に固定していたため、6 個しか取れなかった実行では
            // observed = 0.891^5 と expected = 0.891^6 を比べてしまい偶発的に赤くなっていた
            // （差分 0.061 > 許容 0.02）。
            //
            // ⇒ 期待値は「実際に取れたサンプル数」から導き、さらに<b>隣接サンプルの比</b>
            //    （= 1 tick ぶんの減衰そのもの）を直接見る。これでサンプル数の揺れに依存せず、
            //    0.9 と 0.99 の両方が 1 tick に 1 回ずつ入っていることだけを厳密に固定できる。
            final int steps = speeds.size() - 1;
            for (int i = 1; i < speeds.size(); i++) {
                double ratio = speeds.get(i) / speeds.get(i - 1);
                helper.assertTrue(Math.abs(ratio - DECAY_PER_TICK) < 5.0E-3D,
                        "無人時の水平減衰率が 0.9×0.99 と一致しない（step " + i + "）: ratio=" + ratio
                                + " expected=" + DECAY_PER_TICK + " " + detail);
            }

            // 端点でも確認する。許容帯は ±1 tick ぶん（サンプル数の揺れ幅）に限定。
            double observed = last / first;
            double expectedLo = Math.pow(DECAY_PER_TICK, steps + 1);
            double expectedHi = Math.pow(DECAY_PER_TICK, Math.max(0, steps - 1));
            helper.assertTrue(observed <= expectedHi + 0.02D && observed >= expectedLo - 0.02D,
                    "無人時の水平減衰率が 0.9×0.99 の ±1 tick 帯から外れている: observed=" + observed
                            + " band=[" + expectedLo + ", " + expectedHi + "] steps=" + steps + " " + detail);

            broom.discard();
            helper.succeed();
        });
    }

    /**
     * 【診断 diag-broom §C】無人箒の滑走距離が 1.7.10 相当であること。
     *
     * <p>理論値: tick k の位置変位は {@code v0 × 0.9 × 0.891^k}（初回 tick で
     * 無人減速 0.9 と移動後抵抗 0.99 が両方入るため）。30 tick 分の部分和は
     * {@code v0 × 0.9 × (1 - 0.891^30) / 0.109 ≈ 1.999} 格（v0 = 0.25）。
     * 実測がこの近傍から外れる（大きい＝減衰漏れ／小さい＝二重適用）なら回帰。
     */
    @GameTest(template = TEMPLATE)
    public static void unriddenBroomTravelDistanceMatchesLegacy(GameTestHelper helper) {
        final double v0 = 0.25D;
        final int ticks = 30;
        EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), new Vec3(0.5D, 3.0D, 3.5D));
        broom.setDeltaMovement(v0, 0.0D, 0.0D);
        final double x0 = broom.getX();

        // 理論部分和（実装と同じ式を独立に回す）
        double sum = 0.0D;
        double v = v0;
        for (int i = 0; i < ticks; i++) {
            v *= 0.9D;              // 無人減速
            sum += v;               // この tick の変位
            v *= 0.99D;             // 移動後抵抗
        }
        final double expected = sum;

        helper.runAtTickTime(ticks, () -> {
            double travelled = broom.getX() - x0;
            String detail = "travelled=" + travelled + " expected=" + expected
                    + " v0=" + v0 + " x0=" + x0 + " x=" + broom.getX();

            helper.assertTrue(Math.abs(travelled - expected) < expected * 0.05D,
                    "滑走距離が 1.7.10 相当（0.9×0.99 減衰）から外れている: " + detail);

            broom.discard();
            helper.succeed();
        });
    }
}