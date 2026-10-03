package thkaguyamod.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityNuclearShot;
import thkaguyamod.init.ModItems;

/**
 * 核弾（制御棒）の地形破壊 — 実測報告「核弾が地形を壊さない」の回帰テスト。
 *
 * <h2>为什么值得测</h2>
 * <p>1.7.10 の核弾は {@code EntityNuclearShot#onUpdate} の 4 箇所で
 * {@code createExplosion(..., true)} を呼ぶが、その到達条件は「チャージ中に手持ちを
 * 制御棒以外へ替えた」ときだけで、正常に撃つと {@code EntityTHShot#hitCheck} の
 * 軸判定 → {@code blockHitSpecialProcess()} が false を返して<b>無爆で消滅</b>していた
 * （shipped jar のバイトコードで確認済み）。移植版はその状態をそのまま引き継いでいたため、
 * 「爆発音はする（{@code releaseUsing} の {@code GENERIC_EXPLODE}）のに地形が壊れない」
 * という実測報告になった。
 *
 * <h2>测试口径</h2>
 * <p>着弾はフルチャージに依存する（爆発半径 = {@code getShotSize()}）。落下中の加速度で
 * 着弾位置が構造ごとに数ブロックずれるため、<b>構造内の石を数えて減ったこと</b>で判定する。
 * これは「壊したのはこの爆発だけ」という前提が構造の独立性で担保されるため成立する
 * （1 テスト 1 構造。正例・負例は別構造なので互いに干渉しない）。
 * グローバル設定は書き換えず、負の対照は {@code explodeOnBlockHit(..., false)} の
 * 直接呼び出しで取る（他テストへ設定を漏らさない）。
 *
 * <h2>隔离</h2>
 * <p>テンプレート {@code empty} = 7×7×7、構造の床はバリア（爆発耐性が極端に高く壊れない）なので、
 * 爆発が構造外へ漏れて後続テストを壊すことはない。プラットフォームはローカル y=1..3 の 7×7 石。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearShotTerrainTest {

    private static final String TEMPLATE = "empty";

    /** フルチャージ相当（{@code tick()} が 0.06/tick で size を育てる。上限 6.0）。 */
    private static final int CHARGE_TICKS = 90;

    /** 発射から判定までの tick 数（チャージ + 落下 + 着弾に十分な余裕）。 */
    private static final int RESULT_TICKS = CHARGE_TICKS + 25;

    /** プラットフォーム内の石の初期数（7×7×3）。 */
    private static final int PLATFORM_STONES = 7 * 7 * 3;

    private NuclearShotTerrainTest() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 2 * RESULT_TICKS)
    public static void nuclearShotBreaksTerrain(GameTestHelper helper) {
        buildPlatform(helper);
        chargeAndRelease(helper);

        helper.runAtTickTime(RESULT_TICKS, () -> {
            int remaining = countStones(helper);
            helper.assertTrue(remaining < PLATFORM_STONES,
                    "核弾が地形を壊さなかった（石 " + remaining + "/" + PLATFORM_STONES + " が残存）");
            helper.succeed();
        });
    }

    /**
     * 負の対照：{@code destroysBlocks=false}（= 設定 {@code Master Spark Destroys Blocks=false}）なら
     * 地形は残る。設定そのものは書き換えない（同一バッチの他テストへ漏らさないため）。
     */
    @GameTest(template = TEMPLATE)
    public static void nuclearShotRespectsConfigGate(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        EntityNuclearShot shot = new EntityNuclearShot(thkaguyamod.init.ModEntities.NUCLEAR_SHOT.get(),
                helper.getLevel(), user, user, helper.absoluteVec(new Vec3(3.5D, 5.0D, 3.5D)),
                THShotLib.angle(1.0D, 0.0D, 0.0D), 0.0F, THShotLib.rotate_Default(), 0.0F, 9999,
                0.0D, 0.8D, 0.0D, THShotLib.gravity_Zero(),
                DanmakuConstants.FORM_SMALL, 3.0F, 8.0F, 0, 200, 0);

        buildPlatform(helper);
        // プラットフォーム上面の中心で、設定 false 相当の爆発を起こす
        shot.explodeOnBlockHit(helper.absoluteVec(new Vec3(3.5D, 3.5D, 3.5D)), false);

        helper.runAtTickTime(5, () -> {
            helper.assertValueEqual(countStones(helper), PLATFORM_STONES,
                    "Master Spark Destroys Blocks=false 相当なのに地形が壊れた");
            shot.discard();
            helper.succeed();
        });
    }

    /* ------------------------------------------------------------------ */

    /** 構造ローカル y=1..3 に 7×7 の石プラットフォームを敷く。 */
    private static void buildPlatform(GameTestHelper helper) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    /** プラットフォーム範囲内に残っている石の数。 */
    private static int countStones(GameTestHelper helper) {
        int count = 0;
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                for (int y = 1; y <= 3; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * 制御棒で撃つ（{@code ItemNuclearControlRod.use()} 相当）→ {@link #CHARGE_TICKS} チャージ →
     * {@code releaseUsing} 相当で離す。
     */
    private static void chargeAndRelease(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setPos(helper.absoluteVec(new Vec3(3.5D, 5.0D, 3.5D)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NUCLEAR_CONTROL_ROD.get()));
        player.setXRot(90.0F);//真下
        player.setYRot(0.0F);

        player.startUsingItem(InteractionHand.MAIN_HAND);
        Vec3 direction = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), 1.0D);
        Vec3 spawnOffset = THShotLib.getVecFromAngle(player.getYRot() + 20F, player.getXRot(), 0.3D + 0.3D / 2.0D);
        ShotData data = ShotData.shot(0, DanmakuConstants.ORANGE, 0.3F, 10.0F, 0, 70);

        EntityNuclearShot shot = THShotLib.createNuclearShot(player, player,
                THShotLib.pos(player.getX() + spawnOffset.x,
                        THShotLib.getPosYFromEye(player, -0.1D) + spawnOffset.y,
                        player.getZ() + spawnOffset.z),
                direction, 0F, THShotLib.rotate_Default(), 0F, 9999,
                0.0D, 0.8D, 0.0D, THShotLib.gravity_Zero(), data);
        helper.assertFalse(shot.isRemoved(), "核弾が生成直後に消えている");

        // チャージ（離すと shootingFlag が立ち、以降は自由飛行）
        helper.runAtTickTime(CHARGE_TICKS, player::stopUsingItem);
    }
}
