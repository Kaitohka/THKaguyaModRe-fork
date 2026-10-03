package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntitySilverKnife;
import thkaguyamod.entity.shot.EntityTHLaser;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

/**
 * I8 回归族（第二批）：{@link thkaguyamod.test.DanmakuDamageTest} が押さえていない
 * <b>他の命中経路</b>のテスト。
 *
 * <p>I8 の根因は 1.7.10 の {@code canBeCollidedWith} が兼ねていた三職
 * （推挤 / 准星 / 弹丸命中）を 1.21.1 で分離したのに命中扫描侧だけ旧 API のままだったこと。
 * 修正は 7 处命中扫描（THShot / THLaser / THSetLaser / NuclearShot / HenyoriLaser /
 * SilverKnife / EntityDanmakuMob）を {@code canBeHitByProjectile} に置き換えた。
 * 既存の {@code DanmakuDamageTest} は THShot 経路しか通っていないので、
 * ここではレーザーと银刀という<b>別クラス・別扫描コード</b>の 2 経路を実弾で検証する。
 *
 * <h2>靶的选择：ZombifiedPiglin（火免疫）</h2>
 * <p>ゾンビは昼間の日照で毎 20 tick 1 ダメージを受け、その 1 ダメージが
 * 「命中した」判定を偽陽性にしてしまう（前批で踏んだ罠）。ここでは火免疫の
 * {@link ZombifiedPiglin} を使い、さらに「生成直前の体力が満タン」を
 * 負の対照として固定する（この時点では弾/刀はまだ存在しないので、後段の減少量は
 * 命中由来だと確定できる）。
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #laserDamagesMob}：{@code THShotLib.createLaserA} で生成した
 *       {@link EntityTHLaser} を直射 → 掉血（レーザー固有の hitCheck を通る）；</li>
 *   <li>{@link #silverKnifeDamagesMob}：{@link EntitySilverKnife} を投擲 → 掉血。
 *       銀刀は「{@code getKnifeCount() > 2} になるまで前進しない」「{@code ticksInAir >= 5}」
 *       の門を持つため、初期位置を命中 tick の幾何から逆算して置く（下のコメント参照）；</li>
 *   <li>{@link #shotsFromDifferentUsersCancelOut}：使用者の異なる弾同士の相殺（0.1 係数）。</li>
 * </ol>
 *
 * <p>テンプレート・mock player の約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuDamagePathTest {

    private static final String TEMPLATE = "empty";

    /** 命中判定を待つ tick（余量充足）。 */
    private static final int HIT_WAIT_TICKS = 40;

    /** 弹同士が交差し終わるまでに十分な tick。 */
    private static final int CANCEL_WAIT_TICKS = 10;

    private DanmakuDamagePathTest() {}

    /**
     * レーザー（{@link EntityTHLaser}）の命中路径。
     * <p>レーザーは {@code EntityTHShot} を継承しつつ {@code hitCheck()} を丸ごと上書きしており、
     * 命中扫描のコードが弹とは別物（{@code expandTowards(xLength, yLength, zLength)}）。
     * 修正漏れがあると「レーザーだけ素通りする」状態になるため、ここは独立した回帰锚点になる。
     */
    @GameTest(template = TEMPLATE)
    public static void laserDamagesMob(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        ZombifiedPiglin target = spawnTarget(helper, 4, 2, 3);

        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 spawnPos = center.add(-2.5D, 0.0D, 0.0D);
        Vec3 direction = THShotLib.angle(1.0D, 0.0D, 0.0D);
        LaserData laserData = LaserData.laser(DanmakuConstants.LASER_NORMAL, DanmakuConstants.RED,
                0.3F, 3.0F, 4.0F, 0, 200);

        // 負の対照：対象は満タンから始まる（この時点でレーザーはまだ存在しない）
        helper.assertValueEqual(target.getHealth(), target.getMaxHealth(),
                "対象の初期体力が最大値でない（spawn 直後に別要因のダメージを受けている）");

        EntityTHLaser laser = THShotLib.createLaserA(user, spawnPos, direction, 0.5D, laserData);
        helper.assertTrue(laser != null, "THShotLib.createLaserA returned null");
        helper.assertFalse(laser.isRemoved(), "laser was removed immediately after spawning");

        helper.runAtTickTime(HIT_WAIT_TICKS, () -> {
            helper.assertTrue(target.isRemoved() || target.getHealth() < target.getMaxHealth(),
                    "レーザーが対象に当たらなかった（旧フィルタ canBeCollidedWith の回帰）: health="
                            + target.getHealth() + "/" + target.getMaxHealth()
                            + " laserPos=" + laser.position() + " removed=" + laser.isRemoved());
            laser.discard();
            helper.succeed();
        });
    }

    /**
     * 银のナイフ（{@link EntitySilverKnife}）の命中路径。
     *
     * <p>银刀は他の弹类と共通コードを持たない独自の {@code tick()} / 命中扫描を回す。
     * 幾何は実測して逆算した（このテストを書く際に {@code EntitySilverKnife} を実際に走らせて確認）：
     * <ul>
     *   <li>銀刀は {@code tickCount > 1} のときだけ移動量を更新し、加速はその更新の中で行われる。
     *       したがって 1 tick 目の前進は初速そのまま（速度 0.5 → 0.5 格）、2 tick 目は
     *       加速度 {@code motion/2} が乗って 0.71 格、以降も増えていく；</li>
     *   <li>命中扫描は「現在地 → 現在地+移動量」の線分を相手判定箱に膨張させた箱で clip する。
     *       1.7.10 由来のこの実装は<b>始点が相手の箱の内側だと clip が null</b> になるので、
     *       始点は箱の外に置く必要がある；</li>
     *   <li>{@code ticksInAir} は非 {@code inGround} なら常に 5 にセットされる（門は実質無効）。</li>
     * </ul>
     * そこで刀の初期中心を「対象の {@code minX} から 0.7 格手前」に置く：1 tick 目は 0.5 格しか
     * 進まず届かない（= 負の対照が成立し、始点も常に箱の外）、2 tick 目は 0.71 格進んで
     * 交点が区間内（0.7 < 0.71）に入り命中する。CENTER 系の座標を使う限りこの位置は
     * 対象の判定箱から算出するので、テンプレートや構造座標に依存しない。
     */
    @GameTest(template = TEMPLATE)
    public static void silverKnifeDamagesMob(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        ZombifiedPiglin target = spawnTarget(helper, 4, 2, 3);

        AABB box = target.getBoundingBox();
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 direction = THShotLib.angle(1.0D, 0.0D, 0.0D);
        double speed = 0.5D;
        // 生成位置は「渡した pos + angle*speed」なので、中心を minX-0.7 に置くには 0.5 手前を渡す
        Vec3 spawnPos = new Vec3(box.minX - 0.7D - speed * direction.x, center.y, center.z);

        // 負の対照：対象は満タンから始まる（この時点で銀刀はまだ存在しない）
        helper.assertValueEqual(target.getHealth(), target.getMaxHealth(),
                "対象の初期体力が最大値でない（spawn 直後に別要因のダメージを受けている）");

        // 青ナイフ（color=0）。1.7.10 の ItemSilverKnife#onItemRightClick と同じ生成経路
        EntitySilverKnife knife = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), helper.getLevel(),
                user, spawnPos, direction, speed, EntitySilverKnife.KNIFE_COLOR_BLUE);
        helper.getLevel().addFreshEntity(knife);
        helper.assertFalse(knife.isRemoved(), "knife was removed immediately after spawning");

        helper.runAtTickTime(HIT_WAIT_TICKS, () -> {
            helper.assertTrue(target.isRemoved() || target.getHealth() < target.getMaxHealth(),
                    "银刀が対象に当たらなかった: health=" + target.getHealth() + "/" + target.getMaxHealth()
                            + " knifePos=" + knife.position() + " knifeCount=" + knife.getKnifeCount()
                            + " removed=" + knife.isRemoved());
            knife.discard();
            helper.succeed();
        });
    }

    /**
     * 使用者の異なる弾同士の相殺（1.7.10 の {@code EntityTHShot.onImpact} の THShot×THShot 分岐）。
     *
     * <p>係数はレーザー版（全額）と異なり <b>0.1</b>（使用者が Player の場合）。
     *
     * <h2>幾何の設計（1 回だけ交差させる）</h2>
     * <ul>
     *   <li>弹の {@code isPickable()} は {@code getAnimationCount() >= 1}、実体化直後の
     *       {@code tick 1} はカウント 0 なので<b>互いを検出できない</b>（ライブラリが送る
     *       {@code DATA_ANIMATION_COUNT} の位相）。よって初めて命中し得るのは {@code tick 2}；</li>
     *   <li>{@code tick 2} の開始時に 2 発が 0.3 格差で並ぶよう初期位置を 4.3 格ずらし、
     *       速度を両方 2.0 格/tick にする。交差の 1 tick で 1.7 格すれ違うので、
     *       <b>どちらが先に tick しても</b>次の tick には互いの走査箱（前後 1.4 格）から抜ける
     *       ＝ 相殺はちょうど 1 回だけ起きる（値が確定する）。</li>
     * </ul>
     */
    @GameTest(template = TEMPLATE)
    public static void shotsFromDifferentUsersCancelOut(GameTestHelper helper) {
        Player userA = helper.makeMockPlayer(GameType.CREATIVE);
        Player userB = helper.makeMockPlayer(GameType.CREATIVE);

        ShotData dataA = ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.RED, 0.3F, 10.0F, 0, 200);
        ShotData dataB = ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.BLUE, 0.3F, 6.0F, 0, 200);

        EntityTHShot shotA = THShotLib.createShot(userA, helper.absoluteVec(new Vec3(1.2D, 3.5D, 3.5D)),
                THShotLib.angle(1.0D, 0.0D, 0.0D), 2.0D, dataA);
        EntityTHShot shotB = THShotLib.createShot(userB, helper.absoluteVec(new Vec3(5.5D, 3.5D, 3.5D)),
                THShotLib.angle(-1.0D, 0.0D, 0.0D), 2.0D, dataB);

        helper.assertValueEqual(shotA.shotDamage, 10.0F, "shot A initial damage");
        helper.assertValueEqual(shotB.shotDamage, 6.0F, "shot B initial damage");

        helper.runAtTickTime(CANCEL_WAIT_TICKS, () -> {
            // 回帰锚点：旧フィルタ（canBeCollidedWith）では弾同士が互いを検出せず、値が 10/6 のまま残る
            helper.assertTrue(shotA.shotDamage < 10.0F && shotB.shotDamage < 6.0F,
                    "弾同士の相殺が起きていない（旧フィルタの回帰）: A=" + shotA.shotDamage + " B=" + shotB.shotDamage);
            helper.assertTrue(Math.abs(shotA.shotDamage - 9.4F) < 1.0E-3F,
                    "1.7.10 の THShot×THShot 相殺は 0.1 係数: A は 10 - 6×0.1 = 9.4 のはず（実際 "
                            + shotA.shotDamage + "）");
            helper.assertTrue(Math.abs(shotB.shotDamage - 5.0F) < 1.0E-3F,
                    "1.7.10 の THShot×THShot 相殺は 0.1 係数: B は 6 - 10×0.1 = 5.0 のはず（実際 "
                            + shotB.shotDamage + "）");
            helper.assertTrue(shotA.shotDamage > 0.0F && shotB.shotDamage > 0.0F,
                    "相殺後の両弹のダメージは正のはず（負なら係数の取り違え）: A=" + shotA.shotDamage
                            + " B=" + shotB.shotDamage);
            shotA.discard();
            shotB.discard();
            helper.succeed();
        });
    }

    /* ------------------------------------------------------------------ */

    /** 火免疫の的（日照ダメージが混ざらない）。 */
    private static ZombifiedPiglin spawnTarget(GameTestHelper helper, int x, int y, int z) {
        ZombifiedPiglin target = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, x, y, z);
        target.setNoAi(true);
        target.setNoGravity(true);
        return target;
    }
}