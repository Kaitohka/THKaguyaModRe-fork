package thkaguyamod.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
 * <h2>爆発・地形破壊との隔離（重要）</h2>
 * <p>{@code EntityNuclearShot#tick()} は「使用者が核制御棒を持っていない」と
 * {@code ExplosionInteraction.BLOCK} の爆発を起こす。GameTest の構造内でこれを走らせると
 * 構造が壊れ、後続テストの前提（空の 7³）も壊れる。したがって本テストは
 * <b>核弾を世界に追加しない</b>（{@code tick()} を一度も走らせない）。命中判定そのものは
 * {@code hitEntityCheck(...)} を直接呼んで検証する。念のため構造内に石を 1 つ置き、
 * テスト後も残っていること（＝地形破壊が起きていないこと）を確認する。
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #nuclearShotHitsLivingTarget}：線分上の ZombifiedPiglin（火免疫靶）を選び、
 *       ダメージが入る；</li>
 *   <li><b>負の対照</b>：同じ線分上に「遅延フェーズの弾（{@code isPickable()==false}）」を
 *       置いても選ばれない（＝判定を決めているのは {@code canBeHitByProjectile} そのもの）。</li>
 * </ol>
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearShotDamagePathTest {

    private static final String TEMPLATE = "empty";

    /** 核弾の発射位置（相対座標）。 */
    private static final Vec3 SHOT_POS = new Vec3(2.0D, 3.0D, 3.5D);

    /** 対象の位置（相対座標。+X 側）。 */
    private static final Vec3 TARGET_POS = new Vec3(4.5D, 2.0D, 3.5D);

    private NuclearShotDamagePathTest() {}

    @GameTest(template = TEMPLATE)
    public static void nuclearShotHitsLivingTarget(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        ZombifiedPiglin target = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, TARGET_POS);
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.assertValueEqual(target.getHealth(), target.getMaxHealth(), "対象の初期体力");

        // 地形破壊の検知用ブロック（構造の底。爆発が起きればこれを吹き飛ばし得る）
        BlockPos guard = new BlockPos(0, 1, 0);
        helper.setBlock(guard, Blocks.STONE);

        EntityNuclearShot nuclear = new EntityNuclearShot(ModEntities.NUCLEAR_SHOT.get(), helper.getLevel(),
                user, user, helper.absoluteVec(SHOT_POS), THShotLib.angle(1.0D, 0.0D, 0.0D), 0.0F,
                THShotLib.rotate_Default(), 0.0F, 9999,
                0.5D, 0.5D, 0.0D, THShotLib.gravity_Zero(),
                DanmakuConstants.FORM_SMALL, 1.0F, 8.0F, 0, 200, 0);
        // tick() は走らせないので、tick 内で計算される弾ダメージを手で入れておく（size * 8）
        nuclear.shotDamage = nuclear.getShotSize() * 8.0F;
        helper.assertFalse(nuclear.isRemoved(), "核弾の生成に失敗した");

        Vec3 start = nuclear.position().add(-0.25D, 0.0D, 0.0D);
        Vec3 end = nuclear.position().add(4.0D, 0.0D, 0.0D);

        // 負の対照①：線分上に「狙えない弾」を置く（遅延フェーズなので isPickable() == false）
        EntityTHShot untargetable = THShotLib.createShot(user, start.add(1.0D, 0.0D, 0.0D),
                THShotLib.angle(1.0D, 0.0D, 0.0D), 0.02D,
                ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.BLUE, 0.3F, 1.0F, 10, 200));
        helper.assertFalse(untargetable.isPickable(),
                "前提が崩れている: 遅延フェーズの弾 isPickable() が true になっている");

        HitResult result = nuclear.hitEntityCheck(null, start, end);

        helper.assertTrue(result instanceof EntityHitResult,
                "核弾の線分上に ZombifiedPiglin がいるのに当たり判定が返らなかった"
                        + "（旧フィルタ canBeCollidedWith の回帰）: result=" + result);
        EntityHitResult hit = (EntityHitResult) result;
        helper.assertTrue(hit.getEntity() == target,
                "当たった相手が ZombifiedPiglin ではない（狙えない弾を選んでいる可能性）: " + hit.getEntity());

        // 命中処理そのもの（onImpact）を通してダメージが入ること。
        // onImpact は protected なので、同じ効果のある公開経路を使う：走査対象のいない
        // 遠方の線分を渡すと hitEntityCheck は最後に onImpact(hitResult) を 1 回だけ呼ぶ。
        Vec3 far = new Vec3(1.0E7D, 100.0D, 1.0E7D);
        nuclear.hitEntityCheck(new EntityHitResult(target), far, far.add(0.001D, 0.0D, 0.0D));
        helper.assertTrue(target.isRemoved() || target.getHealth() < target.getMaxHealth(),
                "核弾の onImpact でダメージが入らなかった: health=" + target.getHealth() + "/"
                        + target.getMaxHealth());

        // 地形が壊れていないこと（tick を走らせていない = 爆発していない）
        helper.assertBlockPresent(Blocks.STONE, guard);

        nuclear.discard();
        untargetable.discard();
        target.discard();
        helper.succeed();
    }
}