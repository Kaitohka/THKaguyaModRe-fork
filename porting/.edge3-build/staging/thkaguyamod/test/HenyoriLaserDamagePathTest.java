package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.entity.shot.EntityTHHenyoriLaser;
import thkaguyamod.danmaku.THShotLib;

/**
 * I8 回归族（第三批）：{@link EntityTHHenyoriLaser} の命中路径（{@code canBeHitByProjectile} 修正点 #5）。
 *
 * <p>へにょりレーザーは {@code EntityTHLaser} を継承しつつ {@code hitCheck()} を更に上書きし、
 * 13 セグメントの蛇行線分ごとに命中扫描を回す（走査箱は蛇行オフセット込み、
 * 判定線分は {@code moveLength} ベースの直線）。修正点 7 处のうちここだけが
 * 「セグメントごとの走査」なので、独立した回帰锚点が要る。
 *
 * <h2>このクラスは登録されていない（重要）</h2>
 * <p>{@code EntityTHHenyoriLaser} は {@code ModEntities} に登録が無く、生成元も
 * 1.7.10 でコメントアウトされたまま（{@code ItemHoutou} の使用箇所が未移植）。
 * つまり {@code EntityType} をテスト側で組み立てる必要がある。登録名を持たないので
 * {@code EntityType.Builder#build("")}（非シリアライズ）で作る：
 * {@code noSave()} 相当なのでレジストリを汚さず、GameTest 内で完結する。
 *
 * <h2>幾何（実装から逆算）</h2>
 * <ul>
 *   <li>セグメント i の走査箱の終点は
 *       {@code nextX = nowX + angle.x*moveLength - sin(baseAngle)*3}、
 *       {@code nextZ = nowZ + angle.z*moveLength + cos(baseAngle)*3}；</li>
 *   <li>一方で {@code clip} に渡す判定線分は {@code xLength = motionX + sin(yaw)*cos(pitch)*moveLength}
 *       ベースの<b>直線</b>（蛇行オフセットを含まない）；</li>
 *   <li>そこで {@code animationCount = 0}・{@code firstSpeed = 0}（= {@code motionX/Y/Z = 0}）・
 *       {@code length = 13}（= セグメント長 1.0）・{@code angle = (1,0,0)} とすると、
 *       判定線分は「レーザー位置から +X へ 1.0 格」の水平線になる。</li>
 * </ul>
 * 対象はその線分の 0.5 格地点に置く（始点が対象の箱の外に来るので {@code clip} が成立する）。
 *
 * <p>靶は火免疫の {@link ZombifiedPiglin}（日照ダメージの偽陽性回避）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HenyoriLaserDamagePathTest {

    private static final String TEMPLATE = "empty";

    /** レーザーの位置（相対座標。判定線分はここから +X へ 1.0 格）。 */
    private static final Vec3 LASER_POS = new Vec3(2.5D, 3.0D, 3.5D);

    /** 対象の位置（相対座標。レーザーの 0.5 格 +X、判定線分の高さに胴体が掛かる）。 */
    private static final Vec3 TARGET_POS = new Vec3(3.0D, 2.1D, 3.5D);

    private HenyoriLaserDamagePathTest() {}

    @GameTest(template = TEMPLATE)
    public static void henyoriLaserDamagesMobInSegment(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        ZombifiedPiglin target = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, TARGET_POS);
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.assertValueEqual(target.getHealth(), target.getMaxHealth(), "対象の初期体力");

        EntityTHHenyoriLaser laser = new EntityTHHenyoriLaser(henyoriType(), helper.getLevel());
        laser.user = user;
        laser.source = user;
        laser.setPos(helper.absoluteVec(LASER_POS).x, helper.absoluteVec(LASER_POS).y, helper.absoluteVec(LASER_POS).z);
        laser.angle = THShotLib.angle(1.0D, 0.0D, 0.0D);
        laser.shotSpeed = 0.0D;
        laser.motionX = 0.0D;
        laser.motionY = 0.0D;
        laser.motionZ = 0.0D;
        laser.shotDamage = 4.0F;
        laser.setShotId(DanmakuConstants.LASER_NORMAL * 8 + DanmakuConstants.RED);
        laser.setShotSize(0.3F);
        laser.setShotEndTime(200);
        laser.setLaserLength(13.0D);
        laser.setMaxLaserLength(13.0D);
        laser.setAnimationCount(0);
        laser.updateAngle();// yaw/pitch を angle から確定させる

        // 前提：判定線分の角度（yaw=90° → +X、pitch=0）
        helper.assertTrue(Math.abs(laser.getYRot() - 90.0F) < 1.0E-3F,
                "レーザーの yaw が +X 方向（90°）になっていない: " + laser.getYRot());
        helper.assertTrue(Math.abs(laser.getXRot()) < 1.0E-3F,
                "レーザーの pitch が水平（0°）になっていない: " + laser.getXRot());
        helper.assertValueEqual(laser.getAnimationCount(), 0, "セグメント位相（animationCount）の前提");

        laser.hitCheck();

        helper.assertTrue(target.isRemoved() || target.getHealth() < target.getMaxHealth(),
                "へにょりレーザーが判定線分上の対象に当たらなかった（旧フィルタ canBeCollidedWith の回帰）: health="
                        + target.getHealth() + "/" + target.getMaxHealth()
                        + " laserPos=" + laser.position());

        laser.discard();
        target.discard();
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    /**
     * 登録を持たない实体のための {@code EntityType} をテスト内で組み立てる。
     * {@code build("")} は非シリアライズ（{@code noSave} 相当）なのでレジストリに登録されない。
     */
    @SuppressWarnings("unchecked")
    private static EntityType<EntityTHHenyoriLaser> henyoriType() {
        return (EntityType<EntityTHHenyoriLaser>) EntityType.Builder
                .<EntityTHHenyoriLaser>of(EntityTHHenyoriLaser::new, net.minecraft.world.entity.MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .build("");
    }
}