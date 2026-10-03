package thkaguyamod.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityNuclearShot;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

/**
 * I8 回归族（第三批）：{@link EntityNuclearShot} の命中路径（{@code canBeHitByProjectile} 修正点 #4）。
 *
 * <p>核弾は {@code EntityTHShot#hitEntityCheck} を丸ごと上書きしており、
 * {@code (LivingEntity || EntityTHShot)} かつ {@code userHitCheck} という独自のフィルタを持つ。
 * ここが旧 API のままだと「核弾だけ生物を素通りする」状態になる。
 *
 * <h2>戻り値ではなく<b>副作用</b>で判定する（重要な実装事実）</h2>
 * <p>{@code EntityNuclearShot#hitEntityCheck} は 1.7.10 と同じく、
 * ループ内で見つけた相手に対してその場で {@code onImpact(...)} を呼び、
 * <b>最後に受け取った {@code hitResult} をそのまま返す</b>（見つけた相手を返さない）。
 * よって戻り値で命中を判定すると常に {@code null} になり誤判定する（本テストの初版が踏んだ罠）。
 * ここでは「対象が実際に掉血したか」という副作用で判定する。
 *
 * <h2>爆発・地形破壊との隔離</h2>
 * <p>{@code EntityNuclearShot#tick()} は使用者が核制御棒を持っていないと
 * {@code ExplosionInteraction.BLOCK} の爆発を起こす。構造を壊すと後続テストの前提も壊れるため、
 * 本テストは<b>核弾を世界に追加せず {@code tick()} を一度も走らせない</b>。
 * 念のため構造内に石を 1 つ置き、テスト後も残っていること（＝爆発していない）を確認する。
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #nuclearShotDamagesLivingTarget}：線分上の ZombifiedPiglin（火免疫靶）が掉血；
 *       判定線分と対象位置は走査箱（{@code bb + motion + inflate(size*0.5)}）から式で導く；</li>
 *   <li>{@link #nuclearShotIgnoresUnpickableShot}（負の対照）：同じ線分上に
 *       「狙えない弾（遅延フェーズで {@code isPickable()==false}）」だけを置くと、
 *       弾同士の相殺（{@code shotDamage} の減算）が起きない
 *       ＝選択を決めているのが {@code canBeHitByProjectile} であることの裏取り。</li>
 * </ol>
 *
 * <p>靶に {@link ZombifiedPiglin}（火免疫）を使う理由は {@link DanmakuDamagePathTest} と同じ。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearShotDamagePathTest {

    private static final String TEMPLATE = "empty";

    /** 核弾の発射位置（相対座標。判定線分はここから導く）。 */
    private static final Vec3 SHOT_POS = new Vec3(2.0D, 3.0D, 3.5D);

    private NuclearShotDamagePathTest() {}

    @GameTest(template = TEMPLATE)
    public static void nuclearShotDamagesLivingTarget(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        ZombifiedPiglin target = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, SHOT_POS.add(1.0D, 0.0D, 0.0D));
        target.setNoAi(true);
        target.setNoGravity(true);

        // 地形破壊の検知用ブロック（構造の底。爆発が起きればこれを吹き飛ばし得る）
        BlockPos guard = new BlockPos(0, 1, 0);
        helper.setBlock(guard, Blocks.STONE);

        EntityNuclearShot nuclear = newNuclearShot(helper, user);

        // 判定に使われる走査箱は「核弾の判定箱 + 移動量 + hitSize」で決まる。
        // 座標をハードコードせず、確定した箱から対象と線分を配置する（実装式に追従させる）。
        AABB scan = scanBox(nuclear);
        Vec3 from = new Vec3(nuclear.getX() - 0.25D, scan.getCenter().y, nuclear.getZ());
        Vec3 to = new Vec3(scan.maxX - 0.05D, from.y, from.z);

        // 対象：線分上（核弾 +1.0 格）・走査箱の内側・胴体中心が線分の高さに一致。
        // 始点（核弾 -0.25 格）は膨張判定箱（+0.5 格）の外なので AABB.clip が成立する。
        target.setPos(nuclear.getX() + 1.0D, from.y - target.getBbHeight() * 0.5D, nuclear.getZ());
        helper.assertValueEqual(target.getHealth(), target.getMaxHealth(), "対象の初期体力");
        helper.assertTrue(scan.intersects(target.getBoundingBox()),
                "対象が走査箱の外にいる（配置の前提が崩れている）: scan=" + scan + " targetBox="
                        + target.getBoundingBox());

        // 戻り値は入力の hitResult（= null）がそのまま返る実装なので、副作用で判定する
        nuclear.hitEntityCheck(null, from, to);

        helper.assertTrue(target.isRemoved() || target.getHealth() < target.getMaxHealth(),
                "核弾が線分上の対象に当たらなかった（旧フィルタ canBeCollidedWith の回帰）: health="
                        + target.getHealth() + "/" + target.getMaxHealth()
                        + " nuclearPos=" + nuclear.position() + " scan=" + scan);

        // 地形が壊れていないこと（tick を走らせていない = 爆発していない）
        helper.assertBlockPresent(Blocks.STONE, guard);

        nuclear.discard();
        target.discard();
        helper.succeed();
    }

    /**
     * 負の対照：線分上に「狙えない弾」だけがいる場合、核弾はそれを検出しない。
     * <p>検出されれば弾同士の相殺で {@code shotDamage} が減るので、値が据え置きであることが
     * 「{@code canBeHitByProjectile} で弾かれている」ことの直接の証拠になる。
     */
    @GameTest(template = TEMPLATE)
    public static void nuclearShotIgnoresUnpickableShot(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        EntityNuclearShot nuclear = newNuclearShot(helper, user);

        AABB scan = scanBox(nuclear);
        Vec3 from = new Vec3(nuclear.getX() - 0.25D, scan.getCenter().y, nuclear.getZ());
        Vec3 to = new Vec3(scan.maxX - 0.05D, from.y, from.z);

        // 遅延フェーズの弾（isPickable() == false）を線分上に置く
        EntityTHShot untargetable = THShotLib.createShot(user,
                new Vec3(nuclear.getX() + 0.8D, from.y, nuclear.getZ()),
                THShotLib.angle(1.0D, 0.0D, 0.0D), 0.02D,
                ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.BLUE, 0.3F, 1.0F, 10, 200));
        untargetable.shotDamage = 1.0F;
        helper.assertFalse(untargetable.isPickable(),
                "前提が崩れている: 遅延フェーズの弾 isPickable() が true になっている");
        helper.assertTrue(scan.intersects(untargetable.getBoundingBox()),
                "前提が崩れている: 狙えない弾が走査箱の外にいる: " + untargetable.getBoundingBox());

        float damageBefore = nuclear.shotDamage;
        nuclear.hitEntityCheck(null, from, to);

        helper.assertValueEqual(nuclear.shotDamage, damageBefore,
                "狙えない弾を核弾が検出して相殺してしまった（canBeHitByProjectile フィルタの回帰）");
        helper.assertFalse(untargetable.isRemoved(), "狙えない弾が核弾に破壊された（検出されてしまっている）");

        nuclear.discard();
        untargetable.discard();
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    /** 掃描箱（核弾の判定箱 + 移動量 + {@code hitSize}）。{@code hitEntityCheck} と同じ式。 */
    private static AABB scanBox(EntityNuclearShot nuclear) {
        return nuclear.getBoundingBox()
                .expandTowards(nuclear.motionX, nuclear.motionY, nuclear.motionZ)
                .inflate(nuclear.getShotSize() * 0.5F);
    }

    /**
     * 核弾を組み立てる（{@code tick()} は走らせないので、tick 内で計算される
     * {@code shotDamage = size * 8} を手で入れておく）。
     */
    private static EntityNuclearShot newNuclearShot(GameTestHelper helper, Player user) {
        EntityNuclearShot nuclear = new EntityNuclearShot(ModEntities.NUCLEAR_SHOT.get(), helper.getLevel(),
                user, user, helper.absoluteVec(SHOT_POS), THShotLib.angle(1.0D, 0.0D, 0.0D), 0.0F,
                THShotLib.rotate_Default(), 0.0F, 9999,
                0.5D, 0.5D, 0.0D, THShotLib.gravity_Zero(),
                DanmakuConstants.FORM_SMALL, 1.0F, 8.0F, 0, 200, 0);
        nuclear.shotDamage = nuclear.getShotSize() * 8.0F;
        return nuclear;
    }
}