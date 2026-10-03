package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHSetLaser;
import thkaguyamod.init.ModEntities;

/**
 * I8 回归族（第三批）：{@link EntityTHSetLaser} の命中路径（{@code canBeHitByProjectile} 修正点 #3）。
 *
 * <p>設置レーザーは {@code EntityTHLaser} を継承しつつ {@code hitCheck()} を更に上書きしており、
 * 命中扫描のコードが 3 番目に独立している（{@code vec3d}/{@code vec3d1} の組み立てが
 * {@code getVecFromAngle(getYRot(), getXRot())} ベース、走査箱は {@code inflate(getLaserLength())}）。
 * 7 处修正のうちここだけ素通りしていても他のレーザーは通るため、独立した回帰锚点が要る。
 *
 * <h2>テスト設計（tick を使わない理由）</h2>
 * <p>設置レーザーは {@code setting} 实体に追従して毎 tick {@code setPos} する上、
 * {@code setting.isAlive()} が false になると自己消滅する。tick の位相に依存すると
 * 「命中した tick」が環境でぶれるので、ここでは {@code hitCheck()} を<b>直接 1 回呼ぶ</b>：
 * 生成（＝位置決め）→ {@code updateAngle()}（角度から yaw/pitch を確定）→ {@code hitCheck()}。
 * これで筛选器（{@code canBeHitByProjectile}）を通るかどうかだけが結果を決める。
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #setLaserDamagesMobInSegment}：設置台（ArmorStand）と対象（ZombifiedPiglin）を
 *       置き、レーザー線分が対象を貫く → 対象が掉血；
 *       同时に {@code source}（＝設置台）が傷つかないことを負の対照として固定する
 *       （{@code !entity1.is(source)} の分岐）。</li>
 * </ol>
 *
 * <p>靶に {@link ZombifiedPiglin}（火免疫）を使う理由は
 * {@link DanmakuDamagePathTest} と同じ（昼間の日照ダメージが偽陽性になるため）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SetLaserDamagePathTest {

    private static final String TEMPLATE = "empty";

    /** 設置台の位置（相対座標）。 */
    private static final Vec3 SETTING_POS = new Vec3(2.5D, 3.0D, 3.5D);

    /** 対象の位置（相対座標。レーザー線分の +X 側に置く）。 */
    private static final Vec3 TARGET_POS = new Vec3(4.5D, 2.0D, 3.5D);

    private SetLaserDamagePathTest() {}

    @GameTest(template = TEMPLATE)
    public static void setLaserDamagesMobInSegment(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        ArmorStand setting = helper.spawn(EntityType.ARMOR_STAND, SETTING_POS);
        setting.setNoGravity(true);
        ZombifiedPiglin target = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, TARGET_POS);
        target.setNoAi(true);
        target.setNoGravity(true);

        // 負の対照：この時点で対象も設置台も無傷
        helper.assertValueEqual(target.getHealth(), target.getMaxHealth(), "対象の初期体力");
        helper.assertValueEqual(setting.getHealth(), setting.getMaxHealth(), "設置台の初期体力");

        // 設置レーザー：設置台から +X に 0.2 オフセットした位置から長さ 3 の線分が伸びる。
        // （hitCheck は getYRot() から角度を作るので、yaw=90° → +X 方向）
        LaserData laserData = LaserData.laser(DanmakuConstants.LASER_NORMAL, DanmakuConstants.RED,
                0.3F, 3.0F, 4.0F, 0, 200);
        EntityTHSetLaser laser = new EntityTHSetLaser(ModEntities.SET_LASER.get(), helper.getLevel(),
                user, setting, helper.absoluteVec(SETTING_POS), THShotLib.angle(1.0D, 0.0D, 0.0D), 0.0F,
                THShotLib.rotate_Default(), 0.0F, 9999, laserData, setting, 0.2D, 0.0D);

        // 位置決め（setting 追従分）と角度確定。
        // setting 追従で設置台と同じ高さに来ていることを先に確かめておく（角度は updateAngle が
        // angle=(1,0,0) から yaw=90°, pitch=0° を確定させる）。
        laser.updateAngle();
        helper.assertTrue(laser.getY() == setting.getY(),
                "レーザーの高さが設置台の高さと一致していない: laserY=" + laser.getY()
                        + " settingY=" + setting.getY());

        laser.hitCheck();

        helper.assertTrue(target.isRemoved() || target.getHealth() < target.getMaxHealth(),
                "固定レーザーが線分上の対象に当たらなかった（旧フィルタ canBeCollidedWith の回帰）: health="
                        + target.getHealth() + "/" + target.getMaxHealth()
                        + " laserPos=" + laser.position() + " len=" + laser.getLaserLength());

        // 負の対照：設置台（= source）は巻き込まれない
        helper.assertValueEqual(setting.getHealth(), setting.getMaxHealth(),
                "設置台（= source）が伤ついている（!entity1.is(source) の分岐漏れ）");

        target.discard();
        setting.discard();
        helper.succeed();
    }
}