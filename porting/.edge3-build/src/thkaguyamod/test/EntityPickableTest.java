package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
 * I8 回归族（第二批）：{@code isPickable} の一括検証。
 *
 * <p>1.7.10 の {@code canBeCollidedWith} は
 * <b>①推挤 ②准星（クロスヘア）での選択 ③弹丸の命中</b> の三職を兼ねていた。
 * 1.21.1 では {@code canBeCollidedWith}（①）/ {@code isPickable}（②、基类は恒 false）/
 * {@code canBeHitByProjectile}（③、既定は {@code isAlive() && isPickable()}）に分離されている。
 * 移植で②が抜けると、弾丸命中（③）も {@code isPickable} 経由で一緒に死ぬ
 * （{@code canBeHitByProjectile} の既定実装が {@code isPickable()} を参照するため）。
 *
 * <p>本テストは修正で {@code isPickable} を補った 12 实体を<b>実際に spawn して</b>
 * {@code isPickable()} と {@code canBeHitByProjectile()} の両方を確かめる。
 * 12 個を 1 テストで回すのは「どの実体で漏れたか」をメッセージに集約するため
 * （1 実体 1 テストに割ると増殖して保守コストだけが上がる）。
 *
 * <p>弹（{@link EntityTHShot}）だけは遅延フェーズの境界を持つ
 * （{@code getAnimationCount() >= 1} のときだけ狙える）ので別テストに分ける。
 *
 * <p>テンプレートの約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityPickableTest {

    private static final String TEMPLATE = "empty";

    /** spawn 位置（相対座標。構造中心）。 */
    private static final Vec3 CENTER = new Vec3(3.5D, 3.0D, 3.5D);

    private EntityPickableTest() {}

    /**
     * 修正で {@code isPickable} を補った 12 实体を逐一 spawn して検査する。
     * <p>{@code canBeHitByProjectile()} も同時に検査するのは、これが
     * 命中扫描（7 处）が実際に見る述語であり、{@code isPickable} 漏れの<b>実害</b>そのものだから。
     */
    @GameTest(template = TEMPLATE)
    public static void allTwelvePickableEntitiesAreTargetable(GameTestHelper helper) {
        List<EntityType<?>> types = List.of(
                ModEntities.MARISA_BROOM.get(),        // 掃帚
                ModEntities.SUKIMA.get(),              // 隙間
                ModEntities.MINI_HAKKERO.get(),        // 迷你八卦炉
                ModEntities.SILVER_KNIFE.get(),        // 銀のナイフ
                ModEntities.DIVINE_SPIRIT.get(),       // 小神霊
                ModEntities.SHOT_MATERIAL_ENTITY.get(),// 弾の素
                ModEntities.KINKAKUZI.get(),           // 金閣寺
                ModEntities.CIRNO_ICE_BOX.get(),       // 氷棺
                ModEntities.HAKUREI_SHIELD.get(),      // 白霊盾
                ModEntities.HAKUROU_REFLECTER.get(),   // 白狼反射
                ModEntities.YUUKA_PARASOL.get(),       // 折りたたみ傘
                ModEntities.SANAE_WIND.get());         // SanaeWind（THShot 派生だが true 固定）

        helper.assertValueEqual(types.size(), 12, "isPickable を補った实体数");

        List<String> failures = new ArrayList<>();
        for (EntityType<?> type : types) {
            String name = EntityType.getKey(type).toString();
            Entity entity = helper.spawn(type, CENTER);
            if (!entity.isPickable()) {
                failures.add(name + ": isPickable()==false（准星で狙えず、弾丸命中も isPickable 経由で落ちる）");
            }
            if (!entity.canBeHitByProjectile()) {
                failures.add(name + ": canBeHitByProjectile()==false（弾幕の命中扫描から除外される）");
            }
            entity.discard();
        }

        helper.assertTrue(failures.isEmpty(),
                "isPickable 漏れが " + failures.size() + " 件ある: " + failures);
        helper.succeed();
    }

    /**
     * 破棄済みの实体は {@code isPickable()} が false に戻ること
     * （扫帚・金閣寺は {@code !isRemoved()} を返す実装なので、その分岐も固定する）。
     */
    @GameTest(template = TEMPLATE)
    public static void removedEntityIsNoLongerPickable(GameTestHelper helper) {
        Entity broom = helper.spawn(ModEntities.MARISA_BROOM.get(), CENTER);
        helper.assertTrue(broom.isPickable(), "spawn 直後の扫帚は狙えるはず（前提条件）");

        broom.discard();
        helper.assertFalse(broom.isPickable(),
                "discard() 後は isPickable()==false のはず（扫帚/金閣寺は !isRemoved() を返す実装）");
        helper.assertFalse(broom.canBeHitByProjectile(),
                "discard() 後は canBeHitByProjectile()==false のはず（isAlive() && isPickable()）");
        helper.succeed();
    }

    /**
     * 弹（{@link EntityTHShot}）の狙える条件の境界：
     * 遅延フェーズ（{@code getAnimationCount() < 0}）は false、実体化後（{@code >= 1}）は true。
     * <p>I8 の修正で {@code isPickable()} が
     * {@code getAnimationCount() >= 1} を返すようになった（遅延中は実体が無いので狙えない仕様）。
     * ここが恒 false / 恒 true のどちらに化けても、それぞれ「弹が打ち返せない」「遅延中の光を殴れる」になる。
     *
     * <p>注意（このテストで踏んだ落とし穴）：サーバー側の {@code getAnimationCount()} は
     * 同期槽 raw が 1000 未満の間だけ {@code raw - 1000} を返し、1000 以上では
     * {@code lastTime}（実体化後に進む tick）を返す。したがって<b>遅延中の弾に
     * {@code setAnimationCount(1)} しても読み戻しは {@code lastTime = 0}</b> になる。
     * {@code setAnimationCount} は遅延期の位相（= 出現までのカウントダウン）を決める API であり、
     * 実体化後のカウントを代入する API ではない。実体化側の境界は実 tick で待って確かめる。
     */
    @GameTest(template = TEMPLATE)
    public static void shotPickableFollowsAnimationCount(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);

        // 遅延 10 tick の弾：生成直後は getAnimationCount() == -10
        EntityTHShot shot = THShotLib.createShot(user, helper.absoluteVec(CENTER),
                THShotLib.angle(1.0D, 0.0D, 0.0D), 0.02D,
                ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.RED, 0.3F, 2.0F, 10, 200));

        helper.assertValueEqual(shot.getAnimationCount(), -10, "遅延 10 tick の弾の生成直後カウント");
        helper.assertFalse(shot.isPickable(),
                "遅延フェーズ（カウント < 0）の弹は狙えないはず（1.7.10 の canBeCollidedWith 相当）");
        helper.assertFalse(shot.canBeHitByProjectile(), "遅延フェーズの弹は弾丸命中の対象外のはず");

        // 境界：カウント 0 は「実体化した瞬間」でまだ狙えない（canBeCollidedWith と同じ >= 1 条件）
        shot.setAnimationCount(0);
        helper.assertValueEqual(shot.getAnimationCount(), 0, "setAnimationCount(0) 後のカウント");
        helper.assertFalse(shot.isPickable(), "getAnimationCount()==0 は境界で、まだ狙えない（>= 1 条件）");

        shot.setAnimationCount(-80);
        helper.assertValueEqual(shot.getAnimationCount(), -80, "setAnimationCount(-80) 後のカウント");
        helper.assertFalse(shot.isPickable(), "負値（遅延期）の弹は狙えないはず");
        shot.discard();

        // 「カウント >= 1 なら狙える」側は生きた弹で確認する。
        // サーバー側の getAnimationCount() は raw >= 1000 のとき lastTime（実体化後の経過 tick）を返すため、
        // 遅延中の弾に setAnimationCount(1) しても読み戻しは lastTime=0 になり 1 にはならない
        // （setAnimationCount は遅延期の位相を決める API で、実体化後の値を代入する API ではない）。
        // よって実体化は実 tick で待つ。
        EntityTHShot live = THShotLib.createShot(user, helper.absoluteVec(CENTER.add(0.0D, 0.0D, 1.0D)),
                THShotLib.angle(0.0D, 0.0D, 1.0D), 0.02D,
                ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.BLUE, 0.3F, 2.0F, 5, 200));
        helper.assertValueEqual(live.getAnimationCount(), -5, "遅延 5 tick の弾の生成直後カウント");
        helper.assertFalse(live.isPickable(), "遅延期の生きた弾は狙えないはず");

        helper.runAtTickTime(15, () -> {
            helper.assertFalse(live.isRemoved(), "弾が実体化前に消えた（endTime=200 のはず）");
            helper.assertTrue(live.getAnimationCount() >= 1,
                    "遅延期を過ぎたらカウントは 1 以上になるはず: count=" + live.getAnimationCount());
            helper.assertTrue(live.isPickable(),
                    "実体化後（カウント >= 1）の弹は狙えるはず（打ち返し可能）: count=" + live.getAnimationCount());
            helper.assertTrue(live.canBeHitByProjectile(),
                    "実体化後の弹は弾丸命中の対象のはず: count=" + live.getAnimationCount());
            live.discard();
            helper.succeed();
        });
    }
}