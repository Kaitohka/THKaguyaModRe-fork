package perf_eval;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * A7 (P29) fixed buffer の実測ハーネス — 描画は行わず、バッファ管理部分だけを再現する。
 *
 * <p>測るもの: 1 フレームあたりの「フラッシュ回数（= draw call 数）」と「アップロード頂点数」を、
 * <ul>
 *   <li><b>shared</b>: {@code MultiBufferSource.immediate(sharedBuffer)}（現状。バニラ LevelRenderer と同じ）</li>
 *   <li><b>fixed</b>: {@code immediateWithBuffers(全 RenderType を登録, sharedBuffer)}（{@code RegisterRenderBuffersEvent} 相当）</li>
 * </ul>
 * で比較する。{@code BufferSource#getBuffer} は「前と違う型を要求されたら
 * {@code endBatch(lastSharedType)} してから新しく {@code BufferBuilder} を作る」ので、
 * 型が交互に切り替わる弾幕描画では 1 弾ごとにフラッシュが起きる。fixed buffer なら
 * 型ごとに専用 {@code ByteBufferBuilder} を持ち、フレーム末の {@code endBatch()} まで溜められる。</p>
 *
 * <p>GL を触らないための仕掛け: {@link RenderType} は public なコンストラクタを持つ抽象クラスなので、
 * {@link CountingType} が {@code draw(MeshData)} を上書きして「呼ばれた回数」だけ数える
 * （本来は {@code BufferUploader.drawWithShader} を呼ぶ箇所）。</p>
 *
 * <p>再現する描画列は主樹の実装から抽出したもの（{@code RenderTHShot}/{@code RenderTHLaser} の
 * {@code ctx.buf(...)} 呼び出し順）:
 * 通常弾 1、光弾 1、鱗/米/楕円 2、クナイ/矢 3、通常レーザー 7。テクスチャは 43 種の実在名から選ぶ。</p>
 *
 * <p>実行: {@code bash porting/staging/perf-eval/harness/run_a7.sh}</p>
 */
public final class A7BufferFlushBench {

    private static final String[] TEXTURES = {
            "bigshot", "circleshot", "butterflyshot", "arrowshot", "rice", "oval", "amulet",
            "star", "crystalshot", "talisman", "windshot", "phantom", "lasershot", "knife"
    };

    /** 弾 1 個あたりの {@code ctx.buf(...)} 呼び出し列（型と頂点数）。主樹の実測分布に合わせる。 */
    private static final int[][] SHOT_PATTERNS = {
            {1}, {1}, {1}, {1}, {1}, {1},      // 通常弾・光弾・蝶・星・結晶・札・風 …
            {2}, {2}, {2},                     // 鱗弾 / 米弾 / 楕円弾（2 パス）
            {3},                               // クナイ / 矢（下地＋本体＋取っ手）
            {7},                               // 通常レーザー（母子レーザー 7 パス）
    };

    /** 1 quad = 4 頂点。 */
    private static final int VERTS_PER_QUAD = 4;

    public static void main(String[] args) throws Exception {
        // RenderType の <clinit> が BuiltInRegistries を触るため、バニラのブートストラップが要る
        // （GL は一切触らない。専用サーバ起動時と同じ初期化）。
        // NeoForge の FeatureFlagLoader が LoadingModList を参照するので、空の一覧で先に埋めておく。
        net.neoforged.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        int[] shotCounts = { 50, 100, 200 };
        int frames = 60;

        System.out.println("== A7 fixed-buffer flush bench (no GL) ==");
        System.out.printf("%-8s %-10s %12s %12s %14s %14s%n",
                "shots", "strategy", "draws/frame", "draws/shot", "verts/frame", "bytes/frame");
        for (int n : shotCounts) {
            Result shared = run(n, frames, false);
            Result fixed = run(n, frames, true);
            print(n, "shared", shared);
            print(n, "fixed", fixed);
            System.out.printf("         -> draws/frame %.1f -> %.1f (-%.1f%%), verts %.0f -> %.0f%n",
                    shared.drawsPerFrame, fixed.drawsPerFrame,
                    100.0 * (shared.drawsPerFrame - fixed.drawsPerFrame) / Math.max(1.0, shared.drawsPerFrame),
                    shared.vertsPerFrame, fixed.vertsPerFrame);
        }
    }

    private static void print(int n, String label, Result r) {
        System.out.printf("%-8d %-10s %12.1f %12.3f %14.0f %14.0f%n",
                n, label, r.drawsPerFrame, r.drawsPerFrame / (double) n, r.vertsPerFrame, r.bytesPerFrame);
    }

    private record Result(double drawsPerFrame, double vertsPerFrame, double bytesPerFrame) {}

    /**
     * 1 フレーム = 弾 n 個ぶんの描画列。フレーム末に {@code endBatch()}（バニラ LevelRenderer 相当）。
     */
    private static Result run(int shots, int frames, boolean fixed) {
        // 使う型を作る（実在のテクスチャ名・4 モードの代表）
        List<CountingType> types = new ArrayList<>();
        Map<String, CountingType> byKey = new LinkedHashMap<>();
        String[] modes = { "opaque", "add_src_alpha", "add_one_minus_src_color", "add_one" };
        for (int i = 0; i < TEXTURES.length; i++) {
            String mode = modes[i % modes.length];
            CountingType t = new CountingType("bench/" + mode + "/" + TEXTURES[i], 1536);
            byKey.put(TEXTURES[i] + "|" + mode, t);
            types.add(t);
        }

        SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers = new Object2ObjectLinkedOpenHashMap<>();
        if (fixed) {
            for (CountingType t : types) {
                fixedBuffers.put(t, new ByteBufferBuilder(t.bufferSize()));
            }
        }
        ByteBufferBuilder shared = new ByteBufferBuilder(786432);   // バニラと同じ 768 KiB
        MultiBufferSource.BufferSource source = MultiBufferSource.immediateWithBuffers(fixedBuffers, shared);

        long[] totalDraws = { 0 };
        long[] totalVerts = { 0 };
        // フレームをまたいで BufferSource を使い回す（バニラと同じ）
        for (int f = 0; f < frames; f++) {
            for (int s = 0; s < shots; s++) {
                int[] pattern = SHOT_PATTERNS[s % SHOT_PATTERNS.length];
                int quads = pattern[0];
                for (int q = 0; q < quads; q++) {
                    CountingType t = types.get((s * 7 + q * 3) % types.size());
                    writeQuad(source.getBuffer(t));
                }
            }
            for (CountingType t : types) {
                totalDraws[0] += t.draws;
                totalVerts[0] += t.verts;
                t.draws = 0;
                t.verts = 0;
            }
            source.endBatch();   // フレーム末
        }
        return new Result(totalDraws[0] / (double) frames, totalVerts[0] / (double) frames,
                totalVerts[0] / (double) frames * 24.0);   // POSITION_TEX_COLOR = 24 byte/頂点
    }

    private static void writeQuad(com.mojang.blaze3d.vertex.VertexConsumer vc) {
        vc.addVertex(0, 0, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 0f);
        vc.addVertex(1, 0, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 0f);
        vc.addVertex(1, 1, 0).setColor(1f, 1f, 1f, 1f).setUv(1f, 1f);
        vc.addVertex(0, 1, 0).setColor(1f, 1f, 1f, 1f).setUv(0f, 1f);
    }

    /** {@link RenderType} の最小サブクラス。{@code draw} を数えるだけで GL を触らない。 */
    static final class CountingType extends RenderType {
        final String label;
        int draws;
        long verts;

        CountingType(String name, int bufferSize) {
            super(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, bufferSize,
                    false, false, () -> {}, () -> {});
            this.label = name;
        }

        @Override
        public void draw(MeshData meshData) {
            draws++;
            verts += meshData.drawState().vertexCount();
            meshData.close();
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** 未使用（コンパイル時の型解決を明示するためだけに置く）。 */
    @SuppressWarnings("unused")
    private static ResourceLocation tex(String n) {
        return ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/shot/" + n + ".png");
    }

    private A7BufferFlushBench() {}
}
