package thkaguyamod.client.renderer.shot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 弾幕描画用の RenderType を集中定義するクラス。
 *
 * <p>1.7.10 の {@code RenderTHShot} / {@code RenderTHLaser} は Tessellator +
 * {@code GL11.glBlendFunc} / {@code glDepthMask} / {@code glDepthFunc} を直接叩いていた。
 * 1.21.1 では固定機能のブレンド/深度が使えないため、用途ごとに
 * {@link RenderType.CompositeRenderType} を作る。</p>
 *
 * <p>シェーダは必ず {@code position_tex_color}（{@link GameRenderer#getPositionTexColorShader()}）を
 * 使う。頂点フォーマットは {@link DefaultVertexFormat#POSITION_TEX_COLOR} なので、
 * 各頂点は {@code addVertex(pose, x, y, z).setColor(r,g,b,a).setUv(u,v)} の順で書くこと。
 * このシェーダは {@code alpha < 0.1} のフラグメントを discard するため、
 * 1.7.10 の GL_ALPHA_TEST(GL_GREATER, 0.1) 相当の挙動が自動的に得られる。</p>
 *
 * <p><b>ブレンドモードの対応（重要）:</b> 1.21.1 の
 * {@link RenderStateShard#ADDITIVE_TRANSPARENCY} は {@code glBlendFunc(GL_ONE, GL_ONE)} である
 * （{@code ONE_MINUS_SRC_COLOR} ではない）。GL 定数と 1.21.1 の対応は:</p>
 * <ul>
 *   <li>{@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} … 1.21.1 に無いため自前 shard
 *       （{@link #MODE_ADD_ONE_MINUS_SRC_COLOR}）</li>
 *   <li>{@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)} … {@link RenderStateShard#LIGHTNING_TRANSPARENCY} と同一
 *       （{@link #MODE_ADD_SRC_ALPHA}）</li>
 *   <li>{@code glBlendFunc(GL_ONE, GL_ONE)} … {@link RenderStateShard#ADDITIVE_TRANSPARENCY} と同一
 *       （{@link #MODE_ADD_ONE}）</li>
 *   <li>ブレンド無効 … {@link RenderStateShard#NO_TRANSPARENCY}（{@link #MODE_OPAQUE}）</li>
 * </ul>
 *
 * <p>深度: {@code glDepthFunc(GL_ALWAYS)} は {@link RenderStateShard#NO_DEPTH_TEST}、
 * {@code glDepthMask(false)} は {@link RenderStateShard#COLOR_WRITE} で再現する。</p>
 *
 * <p><b>カリング（# perf: A5）:</b> 1.7.10 は描画パスごとに
 * {@code GL11.glEnable/glDisable(GL_CULL_FACE)} を切り替えていた。
 * 以前は全モードを {@link RenderStateShard#NO_CULL} に固定していたため、
 * {@code VertexFormat.Mode.QUADS} の quad 表裏が両方ラスタライズされ、
 * フラグメント量（加算合成のオーバードロー）が 1.7.10 の倍になっていた。
 * 現在は {@link #getCulled} 系（{@link RenderStateShard#CULL}）と
 * {@link #get} 系（NO_CULL）を 1.7.10 の分岐どおりに使い分ける:</p>
 * <ul>
 *   <li><b>CULL</b>: 通常弾、光弾、気質弾、使い魔、クナイ/矢の下地と色付き本体、
 *       通常レーザー（母子レーザー含む）、光の予告エフェクト</li>
 *   <li><b>NO_CULL</b>: 蝶弾、星弾（凹多角形で三角形の向きが揃わない）、結晶弾（板が放射状）、
 *       幽霊弾、札弾、風弾、アミュレット弾、半楕円（鱗弾の外側）、クナイ/矢の取っ手、
 *       マスタースパーク、グングニル、レーヴァテイン</li>
 * </ul>
 * <p>1.7.10 側の根拠: {@code RenderTHShot.java:431/452(鱗弾)/510/570(蝶弾)/835-843(クナイ取っ手)/
 * 1024-1031(札弾)/1055-1069(風弾)/1341-1395(半楕円)/1418-1442(アミュレット)}、
 * {@code RenderTHLaser.java:95-105(マスタースパーク)/110-120(レーヴァテイン)/126-136(グングニル)/
 * 145-165(母子・通常レーザーは GL_CULL_FACE 有効)}。</p>
 */
public final class DanmakuRenderTypes
{
	/** 弾テクスチャのネームスペース */
	private static final String NAMESPACE = "thkaguyamod";

	// --- 1.7.10 の glBlendFunc に対応する 4 モード ---

	/** 1.7.10: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR) ＝ 光加算（光弾・使い魔・クナイ着色パス 等） */
	public static final String MODE_ADD_ONE_MINUS_SRC_COLOR = "add_one_minus_src_color";
	/** 1.7.10: glBlendFunc(GL_SRC_ALPHA, GL_ONE) ＝ 通常弾の blend 指定時・星・蝶・結晶 等 */
	public static final String MODE_ADD_SRC_ALPHA = "add_src_alpha";
	/** 1.7.10: glBlendFunc(GL_ONE, GL_ONE) ＝ 幽霊弾（FORM_PHANTOM） */
	public static final String MODE_ADD_ONE = "add_one";
	/** 1.7.10: ブレンド無効（通常弾・札・結晶・米/楕円の内側パス等） */
	public static final String MODE_OPAQUE = "opaque";

	/**
	 * キャッシュキー。
	 *
	 * <p># perf: A6 — 以前は {@code texture + "|" + mode + "|" + bool + "|" + bool} という
	 * String キーを描画呼び出しごとに組み立てていた（{@code ResourceLocation.toString()} ＋
	 * 文字列連結で 2〜5 個の短命 String）。record キーにすることで文字列連結を完全に無くす。
	 * {@link ResourceLocation} / {@code String} の {@code hashCode()} は各オブジェクト内で
	 * キャッシュされるため、ルックアップも従来より軽い。</p>
	 *
	 * <p>cull もキーに含める: A5 のカリング有無で別の RenderType になるため、
	 * 同じ (texture, mode, flags) でも取り違えないようにする。</p>
	 */
	private record Key(ResourceLocation texture, String mode, boolean noDepthTest, boolean noDepthWrite, boolean cull) {}

	/** (texture, mode, noDepthTest, noDepthWrite, cull) -> RenderType のキャッシュ */
	private static final Map<Key, RenderType> CACHE = new HashMap<>();

	private DanmakuRenderTypes() {}

	/** 深度テスト LEQUAL・深度書き込みあり・両面（NO_CULL）・キャッシュ付き。 */
	public static RenderType get(ResourceLocation texture, String mode)
	{
		return get(texture, mode, false, false);
	}

	/**
	 * 弾テクスチャ用の RenderType を取得する（キャッシュ付き・両面 {@link RenderStateShard#NO_CULL}）。
	 * @param texture     テクスチャ（{@code textures/entity/shot/<name>.png}）
	 * @param mode        {@link #MODE_ADD_ONE_MINUS_SRC_COLOR} / {@link #MODE_ADD_SRC_ALPHA} / {@link #MODE_ADD_ONE} / {@link #MODE_OPAQUE}
	 * @param noDepthTest true で {@link RenderStateShard#NO_DEPTH_TEST}（1.7.10 の glDepthFunc(GL_ALWAYS) 相当）。
	 * @param noDepthWrite true で {@link RenderStateShard#COLOR_WRITE}（1.7.10 の glDepthMask(false) 相当）。
	 */
	public static RenderType get(ResourceLocation texture, String mode, boolean noDepthTest, boolean noDepthWrite)
	{
		return get(texture, mode, noDepthTest, noDepthWrite, false);
	}

	/** # perf: A5 — 背面カリング版。深度テスト LEQUAL・深度書き込みあり。 */
	public static RenderType getCulled(ResourceLocation texture, String mode)
	{
		return get(texture, mode, false, false, true);
	}

	/**
	 * # perf: A5 — 背面カリング版。
	 *
	 * <p>根因: 以前は全モードで {@link RenderStateShard#NO_CULL} だった。1.7.10 は描画パスごとに
	 * {@code glEnable/glDisable(GL_CULL_FACE)} を切り替えており、通常弾・通常レーザー主チャネルなどは
	 * カリング有効のまま描かれていた。{@code VertexFormat.Mode.QUADS} では NO_CULL だと quad の
	 * 表裏が両方ラスタライズされ、フラグメント量（＝加算合成のオーバードロー）が倍になる。
	 * 1.7.10 で GL_CULL_FACE が有効だったパスだけこの版に切り替える。</p>
	 */
	public static RenderType getCulled(ResourceLocation texture, String mode, boolean noDepthTest, boolean noDepthWrite)
	{
		return get(texture, mode, noDepthTest, noDepthWrite, true);
	}

	private static RenderType get(ResourceLocation texture, String mode, boolean noDepthTest, boolean noDepthWrite, boolean cull)
	{
		Key key = new Key(texture, mode, noDepthTest, noDepthWrite, cull);
		RenderType type = CACHE.get(key);
		if (type == null)
		{
			type = create(texture, mode, noDepthTest, noDepthWrite, cull);
			CACHE.put(key, type);
		}
		return type;
	}

	private static RenderType create(ResourceLocation texture, String mode, boolean noDepthTest, boolean noDepthWrite, boolean cull)
	{
		RenderType.CompositeState.CompositeStateBuilder builder = RenderType.CompositeState.builder()
				.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
				.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
				.setCullState(cull ? RenderStateShard.CULL : RenderStateShard.NO_CULL)
				.setDepthTestState(noDepthTest ? RenderStateShard.NO_DEPTH_TEST : RenderStateShard.LEQUAL_DEPTH_TEST)
				.setWriteMaskState(noDepthWrite ? RenderStateShard.COLOR_WRITE : RenderStateShard.COLOR_DEPTH_WRITE);

		switch (mode)
		{
			case MODE_OPAQUE:
				builder.setTransparencyState(RenderStateShard.NO_TRANSPARENCY);
				break;
			case MODE_ADD_SRC_ALPHA:
				// (SRC_ALPHA, ONE)
				builder.setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY);
				break;
			case MODE_ADD_ONE:
				// (ONE, ONE)
				builder.setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY);
				break;
			case MODE_ADD_ONE_MINUS_SRC_COLOR:
			default:
				builder.setTransparencyState(ONE_MINUS_SRC_COLOR_TRANSPARENCY);
				break;
		}

		// # perf: A5 — カリング有無で別 RenderType になるため、名前にも cull を入れて区別する
		String name = "thkaguyamod_danmaku/" + mode + (noDepthTest ? "_nodepthtest" : "") + (noDepthWrite ? "_nodepthwrite" : "")
				+ (cull ? "_cull" : "_nocull") + "/" + texture.getPath();
		return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR,
				VertexFormat.Mode.QUADS, 1536, false, false, builder.createCompositeState(false));
	}

	/**
	 * {@code textures/entity/shot/<name>.png} を指す {@link ResourceLocation} を作る。
	 * 1.7.10 の {@code textures/shot/***.png}（ファイル名は CamelCase）は、
	 * 移植先では {@code textures/entity/shot/***.png}（ファイル名は全小文字）に置かれている。
	 */
	public static ResourceLocation texture(String name)
	{
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, "textures/entity/shot/" + name.toLowerCase(Locale.ROOT) + ".png");
	}

	/** 全キャッシュを破棄する（通常は不要。リソースパック入替時などに呼ぶ）。 */
	public static void reset()
	{
		CACHE.clear();
	}

	// ---------------------------------------------------------------------
	// 1.7.10 の (ONE, ONE_MINUS_SRC_COLOR) に対応する shard は 1.21.1 に無いため、
	// RenderStateShard と同じ書式で自前定義する（固定機能の glBlendFunc と等価）。
	// ---------------------------------------------------------------------

	/** glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR) */
	private static final RenderStateShard.TransparencyStateShard ONE_MINUS_SRC_COLOR_TRANSPARENCY =
			new RenderStateShard.TransparencyStateShard(
					"thkaguyamod_one_minus_src_color_transparency",
					() ->
					{
						RenderSystem.enableBlend();
						RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
					},
					() ->
					{
						RenderSystem.disableBlend();
						RenderSystem.defaultBlendFunc();
					});
}
