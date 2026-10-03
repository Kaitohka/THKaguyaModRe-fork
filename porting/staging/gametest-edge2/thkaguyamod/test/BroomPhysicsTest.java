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
 * I9 回归族（第二批）：{@link EntityMarisaBroom} の無人時物理。
 *
 * <p>修正内容（1.7.10 との差分を戻したもの）：
 * <ol>
 *   <li><b>無人時の減速が実体に届いていなかった</b>（「下骑不停」）。1.7.10 の
 *       {@code motionX *= 0.9D; motionY -= 0.03D; motionZ *= 0.9D;} に相当する処理が
 *       ローカル変数にしか書かれず {@code setDeltaMovement()} に届かない版があり、
 *       降りた後も箒が等速で飞び続けていた。修正後は {@code move()} の<b>前</b>に
 *       移動量へ反映される；</li>
 *   <li><b>着地時の {@code motionY += 0.06D} 補償</b>。1.7.10 は地面上で毎 tick
 *       {@code +0.06} を足して {@code -0.03} の沈降と釣り合わせ、箒が地面にめり込まず
 *       滑らかに静止していた。これが無いと着地後も下向きの移動量が積み上がる。</li>
 * </ol>
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #unriddenBroomDeceleratesAfterDismount}：ゾンビを乗せてから降ろし、
 *       その直後に速度を与えて数 tick 進める → 速度模長が確実に減少していること；</li>
 *   <li>{@link #groundedBroomDoesNotSink}：石の上に置いた無人箒が 20 tick 後も
 *       地面の高さを保ち、下向き移動量が積み上がっていないこと。</li>
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
}