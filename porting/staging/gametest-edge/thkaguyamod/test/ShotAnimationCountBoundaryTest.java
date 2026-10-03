package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
 * 階段 5 品質基建（gametest-edge）：彈のアニメーションカウントの負値境界テスト。
 *
 * <p>背景（なぜこのファイルが必要か）：{@code EntityTHShot} の遅延フェーズ
 * （{@code delayTime} の間、弹未实体的「光の縮小アニメ」期間）では
 * {@code getAnimationCount()} が <b>負の値</b> になる。この値を
 * {@code count % 2} で配列添字に使っていたため、クライアントで
 * {@code colorR[-1]} 相当の範囲外アクセス（= クライアント即落ち）が発生していた（I7）。
 *
 * <p>本テストはその回帰の「最も安い一次防波堤」として、描画側の前提条件を
 * <b>サーバー側の数値だけ</b>で固定する：
 * <ol>
 *   <li>{@code setAnimationCount(-n)} した直後の {@code getAnimationCount()} が {@code -n} を返す
 *       （= 遅延期に負値が漏れ出る経路そのもの。ここが 0 以上に化けると描画の分岐が壊れる）；</li>
 *   <li>遅延期の代表値（0 / 1 / 5 / 80 / 200）で「負値 → 0 方向に単調」が保たれる；</li>
 *   <li>遅延期は当たり判定なし（{@code canBeCollidedWith() == false}）；</li>
 *   <li>NBT 保存/読み込みを経ても例外を出さず、同期槽の値（オフセット 1000）は
 *       {@code EntityType#noSave + addAdditionalSaveData() 空実装} の既存仕様どおり NBT に載らない、を明示的に記録する。</li>
 * </ol>
 *
 * <h2>存储方式（1.7.10 からの移植仕様）</h2>
 * <p>{@code setAnimationCount(count)} は {@code entityData.set(DATA_ANIMATION_COUNT, count + 1000)}、
 * {@code getAnimationCount()} はサーバー側で「raw &lt; 1000 なら raw - 1000（遅延期の負値）、
 * raw ≥ 1000 なら lastTime（実体化後の経過 tick）」を返す（{@code # perf: A2}）。
 * したがって <b>負値の往復だけ</b>が同期槽を通る厳密な往復になる。0 以上の値を
 * {@code setAnimationCount()} しても {@code getAnimationCount()} にその値は現れない
 * （1.7.10 も遅延期以外は {@code setAnimationCount(lastTime)} で、値を読み戻していなかった）。
 *
 * <p>テンプレートと mock player の約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShotAnimationCountBoundaryTest {

    private static final String TEMPLATE = "empty";

    /** 弾生成位置（構造中心）。 */
    private static final Vec3 CENTER = new Vec3(3.5D, 3.5D, 3.5D);

    /** 遅延期の境界サンプル（1.7.10 の delay は弾の種類ごとに 0〜数十）。 */
    private static final int[] DELAYS = {0, 1, 5, 80, 200};

    private ShotAnimationCountBoundaryTest() {}

    /**
     * 核心：遅延期の負値がそのまま {@code getAnimationCount()} に現れること。
     * <p>遅延 80 tick の弾は生成直後 {@code getAnimationCount() == -80} でなければならない
     * （{@code THShotLib.createShot} → コンストラクタの {@code setAnimationCount(-delay)}）。
     */
    @GameTest(template = TEMPLATE)
    public static void delayPhaseAnimationCountIsNegative(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        EntityTHShot shot = newShot(helper, user, 80, 200);

        helper.assertValueEqual(shot.getAnimationCount(), -80,
                "遅延 80 tick の弾は生成直後 -80 のはず（setAnimationCount(-delay) → stored count+1000 → 920-1000）");
        helper.assertFalse(shot.canBeCollidedWith(),
                "遅延期（getAnimationCount() < 0）は当たり判定を持たない（1.7.10 と同じ）");

        for (int expected : new int[] {-1, -2, -80, -1000, -1001}) {
            shot.setAnimationCount(expected);
            helper.assertValueEqual(shot.getAnimationCount(), expected,
                    "setAnimationCount(" + expected + ") の往復（負値 = 遅延期は同期槽のオフセット 1000 を外して復元される）");
        }

        shot.discard();
        helper.succeed();
    }

    /**
     * 遅延時間の各境界で「-delay」が返り、0 以下であること。
     * <p>{@code delay == 0} の弾は生成直後から実体化済みなので 0（= {@code -0}）になる。
     */
    @GameTest(template = TEMPLATE)
    public static void delayWindowKeepsNonPositiveCount(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);

        for (int delay : DELAYS) {
            EntityTHShot shot = newShot(helper, user, delay, 200);
            int count = shot.getAnimationCount();
            helper.assertValueEqual(count, -delay,
                    "delay=" + delay + " の弾の生成直後カウントは -delay のはず");
            helper.assertTrue(count <= 0,
                    "遅延フェーズのカウントは 0 以下でなければ描画の遅延分岐（count < 0）が壊れる: delay=" + delay
                            + " count=" + count);
            // 遅延期の代表的な描画前提：|count| が 10 を超えても破綻しない（RenderTHShot は Math.min(10) で丸める）
            shot.discard();
        }

        helper.succeed();
    }

    /**
     * 実体化（カウント 0 到達）後は負値から抜けること。
     * <p>{@code THShotLib.createShot} で実世界に入れた弾を遅延 tick 数だけ進め、
     * {@code getAnimationCount() >= 0} になり {@code canBeCollidedWith()} が true へ変わることを見る
     * （I7 の修理対象そのもの：負値のまま固定されると弾が永久に当たらない／描画も遅延のまま）。
     */
    @GameTest(template = TEMPLATE)
    public static void delayPhaseEndsAndCollisionTurnsOn(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 spawnPos = helper.absoluteVec(CENTER);
        // 速度 0.02 で endTime まで壁に当たらない（DanmakuSpawnTest と同じ設計）
        EntityTHShot shot = THShotLib.createShot(user, spawnPos, THShotLib.angle(0.0D, 0.0D, 1.0D), 0.02D,
                ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.BLUE, 0.3F, 2.0F, 5, 200));

        helper.assertValueEqual(shot.getAnimationCount(), -5, "遅延 5 tick の弾の生成直後カウント");

        // tick は「tickCount > lastTime」のときだけ進むため、delay + 余裕 tick を待つ
        helper.runAtTickTime(5 + 5, () -> {
            int count = shot.getAnimationCount();
            helper.assertTrue(count >= 0,
                    "遅延期を過ぎたらカウントは 0 以上になるはず（負値のまま = 弾が実体化しない）: count=" + count);
            helper.assertTrue(shot.canBeCollidedWith(),
                    "実体化後（getAnimationCount() >= 1）は当たり判定が有効になるはず: count=" + count);
            shot.discard();
            helper.succeed();
        });
    }

    /**
     * 弾の NBT 保存 → 読み込み往復（異常を出さないこと + 既存仕様の記録）。
     *
     * <p>{@code EntityTHShot#addAdditionalSaveData()} / {@code readAdditionalSaveData()} は 1.7.10 同様の
     * <b>空実装</b>で、{@code ModEntities.NORMAL_SHOT} も {@code .noSave()} で登録されている。
     * つまり同期槽（アニメーションカウントを含む）は NBT を経由して復元される設計ではない
     * （チャンクロードで弾は消える設計をそのまま保っている）。本テストはその境界を固定する：
     * 往復自体が例外を出さず、UUID/POS が保たれ、カウントは「NBT からは戻らない」ことを明示する。
     * 将来 {@code addAdditionalSaveData} に永続化を足すなら、このアサーションが先に落ちて気付ける。
     *
     * <p>NBT 往復の辅助メソッドは既存テストに無いため、{@code saveWithoutId}/{@code load} を直接使う
     * （{@code EntityType.create(tag, level)} は {@code saveWithoutId} が書かない "id" を要求するため使えない）。
     */
    @GameTest(template = TEMPLATE)
    public static void nbtSaveLoadRoundTripDoesNotCarryAnimationCount(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        EntityTHShot shot = newShot(helper, user, 80, 200);
        shot.setAnimationCount(-80);

        CompoundTag saved = shot.saveWithoutId(new CompoundTag());
        helper.assertTrue(saved.contains("Pos"), "saveWithoutId は最低限 Pos を書くはず（往復の前提）");
        helper.assertTrue(saved.contains("UUID"), "saveWithoutId は UUID を書くはず（同一性の検証に使う）");

        EntityTHShot loaded = new EntityTHShot(ModEntities.NORMAL_SHOT.get(), helper.getLevel());
        loaded.load(saved);

        helper.assertValueEqual(loaded.getUUID(), shot.getUUID(), "NBT 往復後の UUID");
        helper.assertTrue(loaded.position().distanceTo(shot.position()) < 1.0E-6D,
                "NBT 往復後の位置（saveWithoutId → load の Pos は clamp されるが通常座標では一致するはず）: "
                        + loaded.position() + " != " + shot.position());
        helper.assertValueEqual(loaded.getAnimationCount(), 0,
                "同期槽の値は NBT に載らない（addAdditionalSaveData は空実装 + EntityType#noSave）: 読み込み直後は既定の 0");

        // 読み込み後も負値の設定/取得が壊れていないこと（load が同期槽のオフセットを汚していない）
        loaded.setAnimationCount(-80);
        helper.assertValueEqual(loaded.getAnimationCount(), -80, "load 後の setAnimationCount(-80) 往復");

        shot.discard();
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    /**
     * 実世界に登録せずに弾を作る（遅延フェーズの数値だけを見たいので tick の影響を避ける）。
     * 引数順は {@code EntityTHShot} の 20 引数コンストラクタ（1.7.10 の生成処理と同じ並び）。
     */
    private static EntityTHShot newShot(GameTestHelper helper, Player user, int delay, int end) {
        Vec3 pos = helper.absoluteVec(CENTER);
        return new EntityTHShot(ModEntities.NORMAL_SHOT.get(), helper.getLevel(), user, user,
                pos, THShotLib.angle(1.0D, 0.0D, 0.0D), 0.0F,
                THShotLib.rotate_Default(), 0.0F, 9999,
                0.3D, 0.3D, 0.0D, THShotLib.gravity_Zero(),
                DanmakuConstants.FORM_SMALL, 0.3F, 2.0F, delay, end, 0);
    }
}