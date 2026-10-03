package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.living.EntityDanmakuMob;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

/**
 * I8 回归族（第三批）：{@link EntityDanmakuMob} の自前命中扫描（{@code canBeHitByProjectile} 修正点 #7）。
 *
 * <h2>この扫描は何をするものか（実装を読んだ結論）</h2>
 * <p>{@code EntityDanmakuMob} は 1.7.10 の弾幕モブで、{@code hitEntityCheck(HitResult, Vec3, Vec3)} /
 * {@code hitCheck(float)} / {@code onImpact(HitResult, float)} の 3 点セットを自前で持つ。
 * <ul>
 *   <li>{@code hitEntityCheck} は<b>使用者の概念を持たない</b>接触扫描で、
 *       {@code getBoundingBox().inflate(0.9)} 内の {@code canBeHitByProjectile()} な实体のうち、
 *       移動線分に最も近く当たった 1 体を {@link EntityHitResult} として返す（＝攻撃判定・体当たり判定）；</li>
 *   <li>{@code hitCheck(float)} はその結果を {@code onImpact} に渡し、
 *       {@code damageSources().mobAttack(this)} で {@code hurt(damage)} する；</li>
 *   <li>ただし {@code hitCheck(float)} は<b>移植ツリー内に呼び出し元が無い</b>
 *       （1.7.10 でも呼び出し元が無く、サブクラス側の実装待ちのまま移植された死にコード）。
 *       このテストは「呼ばれたときに正しく動くか」を固定し、死にコードである事実は報告に書く。</li>
 * </ul>
 *
 * <h2>幾何</h2>
 * <p>扫描箱はモブの判定箱を {@code 0.9} 膨らませたもの、判定は対象の判定箱を {@code 0.9} 膨らませた
 * ものへの線分 clip なので、始点が「膨らませた対象箱」の外にあり、かつ対象箱が扫描箱に入る
 * 位置に置く必要がある。モブ中心から <b>+1.35 格</b> がその条件を満たす唯一の帯
 * （1.2 &lt; dx ≤ 1.5）なので、座標はハードコードせずモブの位置から算出する。
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #contactScanPicksPickableLivingTarget}：線分上の ZombifiedPiglin（火免疫靶）が
 *       選ばれ、{@code hitCheck(damage)} で実際に掉血する；</li>
 *   <li>{@link #contactScanIgnoresUnpickableShot}（負の対照）：同じ位置に「狙えない弾」
 *       （遅延フェーズの {@link EntityTHShot}）だけを置くと選ばれない
 *       ＝ 選択を決めているのが {@code canBeHitByProjectile} であることの裏取り。</li>
 * </ol>
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuMobContactDamageTest {

    private static final String TEMPLATE = "empty";

    /** モブの位置（相対座標）。 */
    private static final Vec3 MOB_POS = new Vec3(2.5D, 2.0D, 3.5D);

    /** 接触扫描の移動量（+X へ 2.0 格。線分が対象箱を貫く）。 */
    private static final double SCAN_MOTION = 2.0D;

    /** 対象までの距離（1.2 < dx <= 1.5 の帯）。 */
    private static final double TARGET_OFFSET = 1.35D;

    private DanmakuMobContactDamageTest() {}

    @GameTest(template = TEMPLATE)
    public static void contactScanPicksPickableLivingTarget(GameTestHelper helper) {
        EntityDanmakuMob mob = helper.spawn(ModEntities.TH_FAIRY.get(), MOB_POS);
        ZombifiedPiglin target = helper.spawn(EntityType.ZOMBIFIED_PIGLIN,
                MOB_POS.add(TARGET_OFFSET, 0.0D, 0.0D));
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.assertValueEqual(target.getHealth(), target.getMaxHealth(), "対象の初期体力");

        Vec3 start = mob.position();
        Vec3 end = start.add(SCAN_MOTION, 0.0D, 0.0D);

        // 扫描そのもの（フィルタの検証）
        HitResult result = mob.hitEntityCheck(null, start, end);
        helper.assertTrue(result instanceof EntityHitResult,
                "接触扫描が線分上の ZombifiedPiglin を拾わなかった（旧フィルタ canBeCollidedWith の回帰）: "
                        + "result=" + result + " mobPos=" + mob.position() + " targetPos=" + target.position());
        EntityHitResult hit = (EntityHitResult) result;
        helper.assertTrue(hit.getEntity() == target,
                "接触扫描が別の实体を選んだ: " + hit.getEntity());

        // ダメージ適用まで（hitCheck → onImpact）
        mob.setDeltaMovement(SCAN_MOTION, 0.0D, 0.0D);
        mob.hitCheck(4.0F);
        helper.assertTrue(target.isRemoved() || target.getHealth() < target.getMaxHealth(),
                "接触扫描の onImpact でダメージが入らなかった: health=" + target.getHealth() + "/"
                        + target.getMaxHealth());

        mob.discard();
        target.discard();
        helper.succeed();
    }

    /**
     * 負の対照：狙えない弾（遅延フェーズの {@link EntityTHShot}）だけが線分上にいる場合、
     * 接触扫描は何も拾わない（＝選択は {@code canBeHitByProjectile} が決めている）。
     */
    @GameTest(template = TEMPLATE)
    public static void contactScanIgnoresUnpickableShot(GameTestHelper helper) {
        EntityDanmakuMob mob = helper.spawn(ModEntities.TH_FAIRY.get(), MOB_POS);
        Player user = helper.makeMockPlayer(GameType.CREATIVE);

        Vec3 start = mob.position();
        Vec3 end = start.add(SCAN_MOTION, 0.0D, 0.0D);

        EntityTHShot delayed = THShotLib.createShot(user, start.add(TARGET_OFFSET, 0.0D, 0.0D),
                THShotLib.angle(1.0D, 0.0D, 0.0D), 0.02D,
                ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.BLUE, 0.3F, 1.0F, 10, 200));
        helper.assertFalse(delayed.isPickable(),
                "前提が崩れている: 遅延フェーズの弾 isPickable() が true になっている");

        HitResult result = mob.hitEntityCheck(null, start, end);
        helper.assertTrue(result == null,
                "狙えない弾を接触扫描が拾ってしまった（canBeHitByProjectile フィルタの回帰）: " + result);

        mob.discard();
        delayed.discard();
        helper.succeed();
    }
}