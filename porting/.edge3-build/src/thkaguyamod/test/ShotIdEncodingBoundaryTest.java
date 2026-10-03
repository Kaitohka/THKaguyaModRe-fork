package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

/**
 * 階段 5 品質基建（gametest-edge）：彈の形状 / 色 ID エンコードの往復テスト。
 *
 * <p>背景：{@code EntityTHShot} は弾 ID 1 個（同期槽 {@code DATA_SHOT_ID}）に
 * 「形状 × 8 + 色」を詰めている（1.7.10 の dataWatcher 19 と同じ）。
 * <ul>
 *   <li>{@code setShotForm(form)} = {@code setShotId(form * 8 + getShotColor())}</li>
 *   <li>{@code setShotColor(color)} = {@code setShotId(getShotForm() * 8 + color)}</li>
 *   <li>{@code getShotForm()} = {@code (id - getShotColor()) / 8}（整数除算）</li>
 *   <li>{@code getShotColor()} = {@code id % 8}</li>
 * </ul>
 * 形状を変えると色が、色を変えると形状が、それぞれ現在値から再計算される。
 * この「読み書きの相互参照」はコンパイルでは絶対に検出できず、レンダラ側では
 * {@code colorR[color]} / {@code switch(form)} の<b>配列添字</b>になる（色 8 以上 / 形状の取りこぼしで
 * クライアント即落ち）。ここでは全合法組み合わせの往復と、色の値域 0..7 を固定する。
 *
 * <p>{@code DanmakuConstants} の色は 8 = RANDOM / 9 = RAINBOW という<b>特殊値</b>で、
 * 弾 ID には決して入らない（{@code THShotLib.createShot} が 0..7 に振り直してから
 * {@code ShotData} を作る）。この「9 が入らない」境界も明示的に検査する。
 *
 * <p>テンプレートと mock player の約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShotIdEncodingBoundaryTest {

    private static final String TEMPLATE = "empty";

    /** 弾生成位置（構造中心）。 */
    private static final Vec3 CENTER = new Vec3(3.5D, 3.5D, 3.5D);

    /**
     * 1.7.10 から存在する形状 ID（{@link DanmakuConstants} の FORM_*）。
     * 18 と 19 の間、19..25 は欠番（1.7.10 も未使用）。欠番を入れないのは
     * 「形状 × 8 + 色」の空間に穴があることをテスト側でも正しく表現するため。
     */
    private static final int[] FORMS = {
            DanmakuConstants.FORM_SMALL, DanmakuConstants.FORM_TINY, DanmakuConstants.FORM_MEDIUM,
            DanmakuConstants.FORM_PEARL, DanmakuConstants.FORM_CIRCLE, DanmakuConstants.FORM_LIGHT,
            DanmakuConstants.FORM_SCALE, DanmakuConstants.FORM_BUTTERFLY, DanmakuConstants.FORM_SMALLSTAR,
            DanmakuConstants.FORM_STAR, DanmakuConstants.FORM_RICE, DanmakuConstants.FORM_CRYSTAL,
            DanmakuConstants.FORM_HEART, DanmakuConstants.FORM_KUNAI, DanmakuConstants.FORM_TALISMAN,
            DanmakuConstants.FORM_BIGLIGHT, DanmakuConstants.FORM_OVAL, DanmakuConstants.FORM_FAMILIAR,
            DanmakuConstants.FORM_ARROW, DanmakuConstants.FORM_PHANTOM, DanmakuConstants.FORM_AMULET,
            DanmakuConstants.FORM_KNIFE, DanmakuConstants.FORM_WIND, DanmakuConstants.FORM_BIG,
            DanmakuConstants.FORM_KISHITU};

    private ShotIdEncodingBoundaryTest() {}

    /**
     * 全合法 form × color 組み合わせ（25 × 8 = 200 通り）の往復。
     * <p>生成時に渡した組が {@code getShotForm()} / {@code getShotColor()} からそのまま戻ること、
     * および {@code getShotId() == form * 8 + color} であることを検査する。
     */
    @GameTest(template = TEMPLATE)
    public static void everyFormColorPairRoundTrips(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        List<String> failures = new ArrayList<>();

        for (int form : FORMS) {
            for (int color = 0; color < 8; color++) {
                EntityTHShot shot = newShot(helper, user, form * 8 + color);
                int actualForm = shot.getShotForm();
                int actualColor = shot.getShotColor();
                if (actualForm != form || actualColor != color) {
                    failures.add("form=" + form + " color=" + color + " -> form=" + actualForm
                            + " color=" + actualColor + " id=" + shot.getShotId());
                }
                if (shot.getShotId() != form * 8 + color) {
                    failures.add("form=" + form + " color=" + color + " -> id=" + shot.getShotId()
                            + " (expected " + (form * 8 + color) + ")");
                }
                shot.discard();
            }
        }

        helper.assertTrue(failures.isEmpty(),
                "弹 ID エンコードの往復に失敗した組が " + failures.size() + " 件ある: " + failures);
        helper.succeed();
    }

    /**
     * 生成後の {@code setShotForm} / {@code setShotColor} の相互参照往復。
     * <p>片方を変えるともう片方が現在値から再計算される（= 相手を壊さない）ことを、
     * 順序を変えながら検査する。
     */
    @GameTest(template = TEMPLATE)
    public static void settersPreserveTheOtherHalf(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        EntityTHShot shot = newShot(helper, user, DanmakuConstants.FORM_SMALL * 8 + DanmakuConstants.RED);

        // 形状だけ変える → 色は据え置き
        shot.setShotForm(DanmakuConstants.FORM_BIG);
        helper.assertValueEqual(shot.getShotForm(), DanmakuConstants.FORM_BIG, "setShotForm 後の形状");
        helper.assertValueEqual(shot.getShotColor(), DanmakuConstants.RED, "setShotForm は色を保つはず");

        // 色だけ変える → 形状は据え置き
        shot.setShotColor(DanmakuConstants.WHITE);
        helper.assertValueEqual(shot.getShotForm(), DanmakuConstants.FORM_BIG, "setShotColor は形状を保つはず");
        helper.assertValueEqual(shot.getShotColor(), DanmakuConstants.WHITE, "setShotColor 後の色");

        // 全色を順に通しても形状が動かない（= getShotForm() の整数除算が色の影響を受けない）
        for (int color = 0; color < 8; color++) {
            shot.setShotColor(color);
            helper.assertValueEqual(shot.getShotForm(), DanmakuConstants.FORM_BIG,
                    "setShotColor(" + color + ") 後に形状が変わった");
            helper.assertValueEqual(shot.getShotColor(), color, "setShotColor(" + color + ") の往復");
        }

        // 全形状を順に通しても色が動かない
        for (int form : FORMS) {
            shot.setShotForm(form);
            helper.assertValueEqual(shot.getShotColor(), DanmakuConstants.WHITE,
                    "setShotForm(" + form + ") 後に色が変わった");
            helper.assertValueEqual(shot.getShotForm(), form, "setShotForm(" + form + ") の往復");
        }

        shot.discard();
        helper.succeed();
    }

    /**
     * {@code getShotColor()} は常に 0..7。
     * <p>レンダラは {@code colorR[color]} のように配列添字として使うため、8 以上が返ると
     * 範囲外アクセスになる。RANDOM(8) / RAINBOW(9) を {@code setShotId} に直接入れた場合でも
     * 「{@code getShotColor()} は 0..7 に収まる」こと（{@code id % 8}）を固定する。
     */
    @GameTest(template = TEMPLATE)
    public static void shotColorIsAlwaysInPaletteRange(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        EntityTHShot shot = newShot(helper, user, 0);

        for (int id = 0; id < 8 * 32; id++) {
            shot.setShotId(id);
            int color = shot.getShotColor();
            helper.assertTrue(color >= 0 && color <= 7,
                    "getShotColor() は 0..7（レンダラの colorR[color] 添字）のはず: id=" + id + " color=" + color);
        }

        // 特殊値（RANDOM=8 / RAINBOW=9）を弾 ID に直接入れた場合の実挙動を記録
        shot.setShotId(DanmakuConstants.RANDOM);
        helper.assertValueEqual(shot.getShotColor(), DanmakuConstants.RANDOM % 8,
                "id=8(RANDOM) の色は 8%8=0 に正規化される（0..7 の外には出ない）");
        shot.setShotId(DanmakuConstants.RAINBOW);
        helper.assertValueEqual(shot.getShotColor(), DanmakuConstants.RAINBOW % 8,
                "id=9(RAINBOW) の色は 9%8=1 に正規化される（0..7 の外には出ない）");

        shot.discard();
        helper.succeed();
    }

    /**
     * 生成経路（{@code THShotLib.createShot}）でも色は 0..7 に収まる。
     * <p>RANDOM(8) / RAINBOW(9) を {@code ShotData} に渡すと、ライブラリ側が 0..7 に振り直してから
     * 実体を作る（1.7.10 と同じ）。呼び出し側の {@code ShotData} は RANDOM/RAINBOW のまま戻る。
     */
    @GameTest(template = TEMPLATE)
    public static void createdShotColorIsResolvedToPalette(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 spawnPos = helper.absoluteVec(CENTER);

        int[] specialColors = {DanmakuConstants.RANDOM, DanmakuConstants.RAINBOW};
        for (int specialColor : specialColors) {
            // ShotData.shot() は color % 16 するだけなので 8/9 はそのまま残る
            thkaguyamod.danmaku.ShotData data = thkaguyamod.danmaku.ShotData.shot(
                    DanmakuConstants.FORM_SMALL, specialColor, 0.3F, 2.0F, 0, 200);
            EntityTHShot shot = THShotLib.createShot(user, spawnPos, THShotLib.angle(0.0D, 0.0D, 1.0D),
                    0.02D, data);

            int color = shot.getShotColor();
            helper.assertTrue(color >= 0 && color <= 7,
                    "特殊色 " + specialColor + " の弾も色は 0..7 に振り直されるはず: color=" + color);
            helper.assertValueEqual(shot.getShotForm(), DanmakuConstants.FORM_SMALL,
                    "色を振り直しても形状は保たれるはず（特殊色 " + specialColor + "）");
            shot.discard();
        }

        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    /** 弾 ID を直接指定して弾を作る（他のフィールドは描画に影響しない固定値）。 */
    private static EntityTHShot newShot(GameTestHelper helper, Player user, int shotId) {
        Vec3 pos = helper.absoluteVec(CENTER);
        return new EntityTHShot(ModEntities.NORMAL_SHOT.get(), helper.getLevel(), user, user,
                pos, THShotLib.angle(1.0D, 0.0D, 0.0D), 0.0F,
                THShotLib.rotate_Default(), 0.0F, 9999,
                0.3D, 0.3D, 0.0D, THShotLib.gravity_Zero(),
                shotId, 0.3F, 2.0F, 0, 200, 0);
    }
}