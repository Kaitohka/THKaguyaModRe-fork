package thkaguyamod.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.living.EntityDanmakuCreeper;
import thkaguyamod.entity.living.EntityFamiliar;
import thkaguyamod.entity.living.EntityTHPhantom;
import thkaguyamod.init.ModEntities;

/**
 * FIX-3a 回归族：audit-ai 报出的两个 P0。
 *
 * <h2>1. P0-2：{@link EntityDanmakuCreeper} 在「起爆冷却窗」内被击杀后永不 {@code remove()}</h2>
 * <p>1.7.10 由来の同源バグ（原作 {@code onUpdate()} の
 * {@code if(t < -100 && t > -280) { if(getHealth() > 0) onDeathUpdate(); } } と
 * {@code if(t < 20 && t >= 0) super.onUpdate(); } の組み合わせ）。
 * 起爆すると {@code timeSinceIgnited2 = -300} になり、そこから毎 tick +1 で
 * {@code -100} まで戻る約 180 tick の間は上記 2 条件がどちらも偽になりうる。
 * その窓の間に体力が 0 になると、
 * <ul>
 *   <li>{@code getHealth() <= 0} → 手動の死亡更新がスキップ；</li>
 *   <li>{@code isAlive() == false} → {@code timeSinceIgnited2} が増えず、窓から永久に出られない；</li>
 *   <li>{@code t < 20} も成立しない → {@code super.tick()} も呼ばれない
 *       → 原版の自動死亡推進（{@code LivingEntity.baseTick()} の
 *       {@code isDeadOrDying() → tickDeath()}）が一度も走らない。</li>
 * </ul>
 * 結果、実体が世界に残留し続ける（毎回のチャンクロードで tick する「生きた死体」）。
 *
 * <p>本テストはこの窓を<b>実 tick で作ってから</b>倒し、その後 {@code isRemoved()} になることを見る。
 * 修正前は {@code isRemoved()} が永遠に false のままになる。
 *
 * <h2>2. P0-4：样式表が空の瞬間に妖精系を構築すると実体生成そのものが失敗する</h2>
 * <p>{@link thkaguyamod.registry.DanmakuPatternRegistry} は 1.21.1 では config の
 * Loading/Reloading ごとに {@code clear()} → 再登録するため、表が空の瞬間が理論上存在する。
 * その状態で {@code random.nextInt(pattern.size())} を呼ぶと
 * {@code IllegalArgumentException: bound must be positive} でコンストラクタが落ち、
 * {@code EntityType#create} が例外を投げる（= {@code /summon} 不能）。
 * 1.7.10 は preInit で一度だけ構築していたため表は恒に非空だった（移植固有の窓）。
 *
 * <p>本テストは {@code pattern.clear()} で窓を人工的に作り、使い魔・幽霊・妖精の
 * <b>実体生成（コンストラクタ経由）が例外を出さない</b>ことを確かめる。
 * テスト終了時に必ず表を復元する（他テストへの影響を防ぐ）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuMobP0RegressionTest {

    private static final String TEMPLATE = "empty";

    /** 冷却窓の内側であることを確認する境界（-280 < t < -100）。 */
    private static final int COOLDOWN_WINDOW_UPPER = -100;
    private static final int COOLDOWN_WINDOW_LOWER = -280;

    /** 击杀後、原版の死亡更新（deathTime 40）が走り切るまでの余裕 tick。 */
    private static final int REMOVAL_CHECK_TICKS = 60;

    private DanmakuMobP0RegressionTest() {}

    /**
     * P0-2 回归：起爆冷却窗内で击杀 → その実体が {@code remove()} されること。
     *
     * <p>時間割（1.7.10 と同一のカウンタ挙動）:
     * <ol>
     *   <li>t=0 から毎 tick +1（原版 {@code getSwellDir()} が 0 なので加算のみ）；
     *       t=30 で起爆 → {@code t = -300} に飛び、以降 +1/tick；</li>
     *   <li>t∈(-280,-100) は「冷却窓」。この窓の中だけで手動死亡更新が回る原作構造。</li>
     * </ol>
     * したがって<b>窓の入口は起爆の 20 tick 後（絶対 tick で約 50）</b>。
     * 窓に入るのを待ってから {@code kill()} する（窓の外で倒すと原版経路で普通に消えるため、
     * バグを踏まない）。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void creeperKilledDuringCooldownIsRemoved(GameTestHelper helper) {
        // 前提: EntityDanmakuMob.tick() は難易度 PEACEFUL で実体を discard する（1.7.10 同）。
        // GameTestServer の既定難易度は PEACEFUL なので、これを行わないと「1 tick で消えて
        // テストが常に緑」になる（＝本テストが何も検証しない偽陽性になる）。
        helper.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);

        EntityDanmakuCreeper creeper = helper.spawn(ModEntities.DANMAKU_CREEPER.get(), 3, 2, 3);
        helper.assertTrue(creeper.isAlive(), "前提: spawn 直後の花田尖叫者 は生存しているはず");

        // 起爆カウンタ（timeSinceIgnited2）は Entity#tickCount とは別系統で、
        // 「点火された（getSwellDir()==1）」間だけ進む。GameTest にはプレイヤーがいないため
        // 自然点火は起きない。ここでは実プレイの時間経過（点火→起爆 30 tick→冷却窓入り 20 tick）
        // を待たずに、**冷却窓の最中**という状態を直接置く（原版 Creeper の fuse=swell も
        // 同時進行して実体が爆散してしまうため、状態を直接置く方が確実）。
        // 冷却窓の値は 1.7.10 の起爆時ジャンプ先と同じ -300 の直後を選ぶ。
        // また、この実装は冷却窓では super.tick()（= 原版 Creeper.tick()）を呼ばないため、
        // 点火状態を表す swellDir も自動では立たない。原作 1.7.10 の
        // 「点火中（getCreeperState()==1）は t が +1/tick で戻る」を再現するため明示的に 1 を入れる
        // （既定値は -1 で、そのままだと t は -300 → -∞ へ単調減少し冷却窓に入らない）。
        creeper.setSwellDir(1);
        creeper.setIgnitionCooldownTicks(-300);

        java.util.concurrent.atomic.AtomicBoolean killedInWindow = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicInteger cooldownAtKill = new java.util.concurrent.atomic.AtomicInteger(Integer.MIN_VALUE);
        java.util.concurrent.atomic.AtomicInteger removedAtTick = new java.util.concurrent.atomic.AtomicInteger(-1);
        java.util.concurrent.atomic.AtomicInteger minSeen = new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE);

        // 冷却窓（-280 < t < -100）に入ったら倒す。毎 tick 判定する。
        helper.onEachTick(() -> {
            if (creeper.isRemoved()) {
                if (removedAtTick.get() < 0) {
                    removedAtTick.set((int) helper.getTick());
                }
                return;
            }
            int t = cooldownOf(creeper);
            minSeen.updateAndGet(prev -> Math.min(prev, t));
            if (!killedInWindow.get() && t < COOLDOWN_WINDOW_UPPER && t > COOLDOWN_WINDOW_LOWER) {
                cooldownAtKill.set(t);
                killedInWindow.set(true);
                creeper.kill();// damageSources().genericKill() で体力 0 → die() 経由
            }
        });

        // 冷却窓は t=-300 → -100 の約 200 tick 続く。十分な余裕を取る。
        helper.runAtTickTime(REMOVAL_CHECK_TICKS + 240, () -> {
            helper.assertTrue(killedInWindow.get(),
                    "前提が崩れている: 冷却窓（-280 < t < -100）に入らずにテストが終わった"
                            + "（この場合このテストは何も検証していない）。"
                            + " difficulty=" + helper.getLevel().getDifficulty()
                            + " minSeen=" + minSeen.get()
                            + " removedAtTick=" + removedAtTick.get()
                            + " t=" + creeper.getIgnitionCooldownTicks()
                            + " isAlive=" + creeper.isAlive()
                            + " removed=" + creeper.isRemoved());

            helper.assertTrue(creeper.isRemoved(),
                    "起爆冷却窓の間に倒した花田尖叫者 が remove() されていない（P0-2 の再発）: "
                            + "kill時 t=" + cooldownAtKill.get()
                            + " isAlive=" + creeper.isAlive()
                            + " health=" + creeper.getHealth()
                            + " cooldown=" + creeper.getIgnitionCooldownTicks()
                            + " deathTime=" + creeper.deathTime);
            helper.succeed();
        });
    }

    /**
     * P0-4 回归：样式表が空でも妖精系の実体生成が例外を出さないこと。
     *
     * <p>1.7.10 は表を一度だけ構築していたので空窓は無かった。移植版は config の
     * Reloading ごとに {@code clear()} → 再登録するため、その窓で構築すると
     * {@code nextInt(0)} が {@code IllegalArgumentException} を投げていた。
     */
    @GameTest(template = TEMPLATE)
    public static void entityConstructionWithEmptyPatternTableDoesNotThrow(GameTestHelper helper) {
        var registry = thkaguyamod.registry.DanmakuPatternRegistry.pattern;
        int savedSize = registry.size();
        helper.assertTrue(savedSize > 0,
                "前提: 通常時は样式表が非空であるはず（空なら別の回帰）");

        registry.clear();
        try {
            helper.assertTrue(registry.isEmpty(), "前提: 表を空にできているはず");

            // 実体生成の実経路（EntityType#create → コンストラクタ → setPattern）を通す。
            // 空表で nextInt(0) を呼ぶ版ではここで IllegalArgumentException が上がる。
            EntityFamiliar familiar = createQuietly(helper, ModEntities.FAMILIAR.get());
            EntityTHPhantom phantom = createQuietly(helper, ModEntities.TH_PHANTOM.get());
            var fairy = createQuietly(helper, ModEntities.TH_FAIRY.get());

            helper.assertTrue(familiar != null, "空表で 使い魔 を生成できなかった（P0-4）");
            helper.assertTrue(phantom != null, "空表で 幽霊 を生成できなかった（P0-4）");
            helper.assertTrue(fairy != null, "空表で 妖精 を生成できなかった（P0-4）");

            // 空表フォールバックでは样式は既定値（0）のまま = 弾幕なしで安全に動ける。
            helper.assertValueEqual(familiar.getDanmakuPattern(), 0,
                    "空表フォールバック時の 使い魔 の样式は既定 0 のはず");
            helper.assertValueEqual(phantom.getDanmakuPattern(), 0,
                    "空表フォールバック時の 幽霊 の样式は既定 0 のはず");
        } finally {
            // 他テストへ影響させないため、必ず表を組み直す
            thkaguyamod.registry.DanmakuPatternRegistry.setDanmakuPattern();
            helper.assertTrue(registry.size() == savedSize,
                    "样式表の復元に失敗した（他テストへ影響する）: " + registry.size() + " != " + savedSize);
        }

        helper.succeed();
    }

    /** 実体生成を「例外を投げずに」試みる。投げたらテスト失敗にする（null なら生成不可として扱う）。 */
    private static <T extends net.minecraft.world.entity.Entity> T createQuietly(
            GameTestHelper helper, EntityType<T> type) {
        try {
            return type.create(helper.getLevel());
        } catch (RuntimeException e) {
            throw new AssertionError("空の样式表で " + type.toShortString()
                    + " の生成が例外で落ちた（P0-4）: " + e, e);
        }
    }

    /** 起爆処理で使われる「冷却カウンタ」を読むための小さなユーティリティ（テスト専用）。 */
    private static int cooldownOf(EntityDanmakuCreeper creeper) {
        return creeper.getIgnitionCooldownTicks();
    }
}
