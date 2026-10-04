package perf_eval;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;

/**
 * B1（ホーミング追尾の対象再選択スロットル）の実測ハーネス。
 *
 * <p>測るもの: 1 回の対象探索（{@code EntityTHShot#scanHomingTarget} の中身）の
 * <b>候補 1 体あたりコスト</b>と<b>割り当て数</b>、そしてそれを
 * 「N 発の追尾弾 × 1 tick」に積み上げたときの差（現状 = 4 tick ごと再選択 vs 毎 tick 再選択）。
 *
 * <p><b>測っている範囲（正直な申告）</b>:
 * <ul>
 *   <li>実測: 候補ループの数値計算（正規化・外積不要な角度・距離・halfAbsSin）、
 *       {@link Vec3} の割り当て数、{@code clipBlocks} 呼び出し回数（スタブで数えるだけ）、
 *       ループ全体の wall time。</li>
 *   <li>実測しない: {@code Level#getEntitiesOfClass} の AABB 走査そのもの（実 {@code ServerLevel} が
 *       要るため）。これは §REPORT で「実装の読み + 1 セクションあたりの実体数」から見積もる。</li>
 *   <li>候補は {@link Candidate} という軽量レコードで代用する。{@code LivingEntity#getX()} 等の
 *       ゲッタ呼び出し 1 回ぶんのコストは含まれる（同じく 1 回呼ぶため）が、
 *       実体側のフィールド解決コストは本物より軽い可能性がある。</li>
 * </ul>
 *
 * <p>実行: {@code bash porting/staging/perf-eval/harness/run_b1.sh}
 */
public final class B1HomingScanBench {

    /** {@code EntityTHShot} の定数（主樹の値）。 */
    private static final int HOMING_TARGET_INTERVAL = 4;
    private static final double HOMING_TARGET_RANGE = 24.0D;

    /** 探索ボックスは 24 格インフレート＝1 辺 48 格。実運用でこの箱に入る「生きた候補」の数。 */
    private static final int[] CANDIDATE_COUNTS = { 1, 4, 16, 64 };

    /** 追尾弾の数（レポートで使う代表値）。 */
    private static final int SHOTS = 100;

    public static void main(String[] args) {
        System.out.println("== B1 homing target-scan bench ==");
        System.out.println("range=" + HOMING_TARGET_RANGE + " interval=" + HOMING_TARGET_INTERVAL
                + " shots=" + SHOTS);

        // ウォームアップ（JIT）
        for (int i = 0; i < 20_000; i++) {
            scan(8, false);
        }

        System.out.println();
        System.out.printf("%-10s %14s %14s %14s %14s%n",
                "candidates", "ns/scan", "Vec3 allocs", "rays", "ns/candidate");
        for (int k : CANDIDATE_COUNTS) {
            long best = Long.MAX_VALUE;
            long allocs = 0;
            long rays = 0;
            for (int rep = 0; rep < 9; rep++) {
                ScanResult r = timedScan(k);
                best = Math.min(best, r.nanos);
                allocs = r.vecAllocs;
                rays = r.rays;
            }
            System.out.printf("%-10d %14d %14d %14d %14.1f%n", k, best, allocs, rays, best / (double) k);
        }

        System.out.println();
        System.out.println("-- getEntitiesOfClass の AABB 走査コスト（モデル推定。実 Level は使えないため）--");
        aabbTraversalModel();

        System.out.println();
        System.out.println("-- 100 発の追尾弾が 1 tick に払う対象探索コスト（実測 ns/scan から積み上げ）--");
        System.out.printf("%-12s %-14s %16s %16s %10s%n",
                "candidates", "strategy", "ns/tick", "us/tick", "scans/tick");
        for (int k : CANDIDATE_COUNTS) {
            long perScan = bestOf(k);
            double scansEveryTick = SHOTS;                       // 毎 tick 再選択（1.7.10 相当）
            double scansThrottled = SHOTS / (double) HOMING_TARGET_INTERVAL; // 現状（4 tick ごと）
            System.out.printf("%-12d %-14s %16.0f %16.2f %10.1f%n",
                    k, "every-tick", perScan * scansEveryTick, perScan * scansEveryTick / 1000.0, scansEveryTick);
            System.out.printf("%-12d %-14s %16.0f %16.2f %10.1f%n",
                    k, "interval=4", perScan * scansThrottled, perScan * scansThrottled / 1000.0, scansThrottled);
        }
    }

    private static long bestOf(int k) {
        long best = Long.MAX_VALUE;
        for (int rep = 0; rep < 9; rep++) {
            best = Math.min(best, timedScan(k).nanos);
        }
        return best;
    }

    // ------------------------------------------------------------------
    // getEntitiesOfClass の AABB 走査コストのモデル推定。
    //
    // 実装（EntityGetter#getEntities → Level#getEntities）は
    //   for (EntitySection section : getEntitySection(area)) for (Entity e : section.getEntities())
    //       if (e.getBoundingBox().intersects(area)) list.add(e);
    // という「交差する全セクション × その中の全実体」の二重ループ。
    // 実 ServerLevel を差し込めないので、ここでは
    //   ・セクション数 = 24 格インフレート（1 辺 48 格）の AABB が覆う 16³ セクション数
    //   ・1 セクションあたりの実体数 = ダンクマク弾幕中の代表値（既定 3 体）
    // を仮定して「ボックス交差判定の回数」を数える。判定式は主樹と同じ AABB#intersects を使う。
    // ------------------------------------------------------------------
    private static void aabbTraversalModel() {
        int[] entitiesPerSection = { 1, 3, 8 };
        // 弾の位置を原点、探索ボックスは ±24 格（1 辺 48）
        AABB search = new AABB(-24, -24, -24, 24, 24, 24);
        List<AABB> sectionBoxes = new ArrayList<>();
        int minSec = Math.floorDiv(-24, 16);
        int maxSec = Math.floorDiv(24 - 1, 16);
        for (int sx = minSec; sx <= maxSec; sx++) {
            for (int sy = minSec; sy <= maxSec; sy++) {
                for (int sz = minSec; sz <= maxSec; sz++) {
                    sectionBoxes.add(new AABB(sx * 16, sy * 16, sz * 16, sx * 16 + 16, sy * 16 + 16, sz * 16 + 16));
                }
            }
        }
        long sectionsTouched = sectionBoxes.stream().filter(search::intersects).count();
        System.out.printf("sections overlapped by the 48^3 box: %d%n", sectionsTouched);
        System.out.printf("%-22s %14s %18s %16s%n",
                "entities/section", "AABB tests/scan", "tests/tick (100 shots)", "tests/tick (interval=4)");
        for (int eps : entitiesPerSection) {
            long tests = sectionsTouched * eps;
            System.out.printf("%-22d %14d %18d %16d%n", eps, tests, tests * 100, tests * 25);
        }
        System.out.println("（AABB 交差 1 回 ≈ 6 回の浮動小数比較。実 Level での実測は未実施）");
    }

    private record ScanResult(long nanos, long vecAllocs, long rays) {}

    private static ScanResult timedScan(int candidates) {
        long allocBefore = ALLOC.get();
        long raysBefore = RAYS.get();
        long t0 = System.nanoTime();
        int iterations = 20_000;
        for (int i = 0; i < iterations; i++) {
            scan(candidates, true);
        }
        long t1 = System.nanoTime();
        return new ScanResult((t1 - t0) / iterations, (ALLOC.get() - allocBefore) / iterations,
                (RAYS.get() - raysBefore) / iterations);
    }

    // ------------------------------------------------------------------
    // 以下は EntityTHShot#scanHomingTarget の忠実な写し（数値計算部分）。
    // 割り当て数と clipBlocks 呼び出しを数えるためだけに ThreadLocal カウンタを使う。
    // ------------------------------------------------------------------

    private static final ThreadLocal<Long> ALLOC = ThreadLocal.withInitial(() -> 0L);
    private static final ThreadLocal<Long> RAYS = ThreadLocal.withInitial(() -> 0L);

    private static Vec3 alloc(double x, double y, double z) {
        ALLOC.set(ALLOC.get() + 1);
        return new Vec3(x, y, z);
    }

    /** clipBlocks のスタブ（実際の DDA は実 Level が要るので「呼ばれた回数」だけ数える）。 */
    private static Vec3 clipStub(Vec3 from, Vec3 to) {
        RAYS.set(RAYS.get() + 1);
        return null;   // 「見えている」= 命中なし
    }

    private record Candidate(double x, double y, double z, double eyeHeight) {}

    private static Candidate[] makeCandidates(int n, long seed) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Candidate[] out = new Candidate[n];
        for (int i = 0; i < n; i++) {
            // 弾の前方 48 格箱の中に散らす（range=24 の inflate で 1 辺 48）
            double x = rnd.nextDouble(-24, 24);
            double y = rnd.nextDouble(-12, 12);
            double z = rnd.nextDouble(0, 48);
            out[i] = new Candidate(x, y, z, 1.62D);
        }
        return out;
    }

    /** {@code scanHomingTarget(false)} の写し。shot は原点、shotVec は +Z 方向。 */
    private static Candidate scan(int candidateCount, boolean realWork) {
        Candidate[] list = makeCandidates(candidateCount, 12345L);
        if (!realWork) {
            // ウォームアップでは候補生成だけ
            return list.length > 0 ? list[0] : null;
        }

        Vec3 shotVec = Vec3.ZERO.add(0, 0, 1);   // getShotVector 相当（単位ベクトル）
        Candidate nearEntity = null;
        // 1.7.10 の初期値（999.9 × halfAbsSin(π)）
        double nearValue = 999.9D * THShotLib.halfAbsSin(180F / 180F * (float) Math.PI);

        for (int i = 0; i < list.length; i++) {
            Candidate candidate = list[i];
            Vec3 targetVec = alloc(candidate.x(), candidate.y() + candidate.eyeHeight(), candidate.z());
            targetVec = THShotLib.getVectorNomalize(targetVec);
            float angleSpan = Math.abs(THShotLib.getVectorAndVectorAngle(shotVec, targetVec));
            double distance = Math.sqrt(candidate.x() * candidate.x()
                    + (candidate.y() + candidate.eyeHeight()) * (candidate.y() + candidate.eyeHeight())
                    + candidate.z() * candidate.z());
            double value = distance * THShotLib.halfAbsSin(angleSpan / 180F * (float) Math.PI);

            if (value >= nearValue) {
                continue;
            }
            // 第二段（最良値を更新する候補だけ射線）
            Vec3 vec3d = alloc(0, 0, 0);
            Vec3 vec3d1 = alloc(candidate.x(), candidate.y() + candidate.eyeHeight(), candidate.z());
            if (clipStub(vec3d, vec3d1) != null) {
                continue;
            }
            nearEntity = candidate;
            nearValue = value;
        }
        return nearEntity;
    }

    private B1HomingScanBench() {}
}
