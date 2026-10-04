package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntitySilverKnife;
import thkaguyamod.init.ModEntities;

/**
 * diag-knife：銀のナイフの向き（yaw）が投擲方向に従うことの回帰テスト。
 *
 * <p>背景：クライアント側の {@link EntitySilverKnife} は投擲コンストラクタを通らない
 * （{@code ClientPacketListener} は {@code EntityType#create(level)} = 無引数コンストラクタで作る）ため
 * {@code motionX/Y/Z} が常に 0 になる。旧実装は毎 tick
 * {@code atan2(motionX, -motionZ)} で yaw を再計算していたため、{@code atan2(+0.0, -0.0)} が
 * Java 仕様どおり <b>180°</b> を返し、サーバーから同期された正しい向きを 180° 固定で上書きしていた。
 * 1.7.10 は {@code EntityRegistry.registerModEntity(..., sendsVelocityUpdates=true)} で
 * motion を同期していたためこの退化は起きなかった（移植で落ちた同期）。
 *
 * <p>本テストは「速度が観測できないときは現在の向き（= サーバー同期値 / 生成直後の投擲方向）を保つ」
 * という修正の不変条件を、<b>実サーバー tick を回して</b>固定する。
 * 描画（PoseStack の回転）は GameTest では観測できないので、描画が参照する
 * {@code getYRot()} の方を固定する（{@code RenderSilverKnife} は
 * {@code Axis.YP.rotationDegrees(180F - entityYaw)} で entityYaw を使う）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SilverKnifeOrientationTest {

    private static final String TEMPLATE = "empty";

    /** 投擲位置（構造中心）。 */
    private static final Vec3 CENTER = new Vec3(3.5D, 3.0D, 3.5D);

    /** 投擲速度（{@code ItemSilverKnife#use} と同じ）。 */
    private static final double SPEED = 0.28D;

    private SilverKnifeOrientationTest() {}

    /**
     * 投擲直後の向きが投擲方向と一致すること。
     *
     * <p>期待値は 1.7.10 原作の定義そのもの
     * （{@code rotationYaw = atan2(motionX, -motionZ)}、{@code rotationPitch = atan2(motionY, |motion_xz|)}）。
     * ここで {@code motion = THShotLib.angle(player.getYRot(), player.getXRot()) * speed}。
     * 「プレイヤーの yaw と等しい」ではない点に注意（原作の角度規約が 1 つ噛んでいる）。
     */
    @GameTest(template = TEMPLATE)
    public static void thrownKnifeKeepsThrowDirection(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);

        checkThrow(helper, user, 90.0F, 0.0F);
        checkThrow(helper, user, 0.0F, 0.0F);
        checkThrow(helper, user, -90.0F, 0.0F);
        checkThrow(helper, user, 180.0F, 0.0F);
        checkThrow(helper, user, 45.0F, 30.0F);

        helper.succeed();
    }

    /**
     * クライアント相当（motion が 0）でも向きが 180° に潰れず、与えられた向きを保つこと。
     *
     * <p>これが本バグの直接の回帰テストである：修正前は {@code motionX == 0 && motionZ == 0} の
     * とき {@code atan2(+0.0, -0.0) == π} により yaw が常に 180° になっていた。
     */
    @GameTest(template = TEMPLATE)
    public static void zeroMotionDoesNotCollapseYawTo180(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);

        for (float expected : new float[] {0.0F, 45.0F, 90.0F, -135.0F, 137.0F}) {
            // クライアントの生成経路（無引数コンストラクタ）をそのまま再現する。
            // この経路では motionX/Y/Z と lastShotMotionX/Y/Z が既定値 0 のまま。
            EntitySilverKnife knife = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), helper.getLevel());
            knife.setPos(helper.absoluteVec(CENTER));
            // サーバーから同期された向き（ClientboundAddEntityPacket → recreateFromPacket）
            knife.setYRot(expected);
            knife.setXRot(0.0F);
            knife.setKnifeCount(3); // tick の早期 return（count < 0）を避ける
            helper.getLevel().addFreshEntity(knife);

            final float want = expected;
            helper.runAtTickTime(2, () -> {
                float got = knife.getYRot();
                helper.assertTrue(Math.abs(got - want) < 0.01F,
                        "motion が 0（クライアント相当）でも yaw は 180° に潰れず " + want
                                + " を保つはず（diag-knife の回帰）。実際: " + got);
                knife.discard();
            });
        }

        helper.runAtTickTime(6, helper::succeed);
    }

    /* ------------------------------------------------------------------ */

    /** 1 回の投擲を再現して、生成直後の yaw/pitch が原作の定義どおりであることを確かめる。 */
    private static void checkThrow(GameTestHelper helper, Player user, float yaw, float pitch) {
        user.setYRot(yaw);
        user.setXRot(pitch);
        Vec3 angle = THShotLib.angle(user.getYRot(), user.getXRot());
        Vec3 pos = THShotLib.pos_Distance(THShotLib.pos_Living(user), angle, SPEED);

        EntitySilverKnife knife = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), helper.getLevel(),
                user, pos, angle, SPEED, 0);
        helper.getLevel().addFreshEntity(knife);

        // 1.7.10 原作 EntitySilverKnife.java:97-98 の定義（motion から yaw/pitch を導く）
        double mx = angle.x * SPEED;
        double my = angle.y * SPEED;
        double mz = angle.z * SPEED;
        float expectedYaw = (float) Math.atan2(mx, -mz) / 3.141593F * 180F;
        float expectedPitch = (float) Math.atan2(my, Math.sqrt(mx * mx + mz * mz)) / 3.141593F * 180F;

        helper.assertTrue(Math.abs(knife.getYRot() - expectedYaw) < 0.01F,
                "投擲直後の yaw は原作定義 atan2(motionX,-motionZ) = " + expectedYaw
                        + " のはず（player yaw=" + yaw + "）。実際: " + knife.getYRot());
        helper.assertTrue(Math.abs(knife.getXRot() - expectedPitch) < 0.01F,
                "投擲直後の pitch は原作定義 = " + expectedPitch + " のはず。実際: " + knife.getXRot());

        // 数 tick 進めても（= クライアントが受信後に tick しても）向きが壊れないこと
        final EntitySilverKnife k = knife;
        final float wantYaw = expectedYaw;
        helper.runAtTickTime(2, () -> {
            helper.assertTrue(Math.abs(k.getYRot() - wantYaw) < 0.01F,
                    "数 tick 後も yaw は " + wantYaw + " を保つはず（180° 固定の再発検知）。実際: " + k.getYRot());
            k.discard();
        });
    }
}
