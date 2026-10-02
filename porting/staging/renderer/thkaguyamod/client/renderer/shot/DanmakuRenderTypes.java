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
 * <p>カリングは全モードで {@link RenderStateShard#NO_CULL}。1.7.10 では面ごとに
 * GL_CULL_FACE を切り替えていたが、{@code VertexFormat.Mode.QUADS} は常に全三角形を
 * GPU へ渡すため、NO_CULL が原実装の「両面描画」を最も安全に再現する。</p>
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

	/** (texture, mode, noDepthTest, noDepthWrite) -> RenderType のキャッシュ */
	private static final Map<String, RenderType> CACHE = new HashMap<>();

	private DanmakuRenderTypes() {}

	/** 深度テスト LEQUAL・深度書き込みあり・キャッシュ付き。 */
	public static RenderType get(ResourceLocation texture, String mode)
	{
		return get(texture, mode, false, false);
	}

	/**
	 * 弾テクスチャ用の RenderType を取得する（キャッシュ付き）。
	 * @param texture     テクスチャ（{@code textures/entity/shot/<name>.png}）
	 * @param mode        {@link #MODE_ADD_ONE_MINUS_SRC_COLOR} / {@link #MODE_ADD_SRC_ALPHA} / {@link #MODE_ADD_ONE} / {@link #MODE_OPAQUE}
	 * @param noDepthTest true で {@link RenderStateShard#NO_DEPTH_TEST}（1.7.10 の glDepthFunc(GL_ALWAYS) 相当）。
	 * @param noDepthWrite true で {@link RenderStateShard#COLOR_WRITE}（1.7.10 の glDepthMask(false) 相当）。
	 */
	public static RenderType get(ResourceLocation texture, String mode, boolean noDepthTest, boolean noDepthWrite)
	{
		String key = texture + "|" + mode + "|" + noDepthTest + "|" + noDepthWrite;
		RenderType type = CACHE.get(key);
		if (type == null)
		{
			type = create(texture, mode, noDepthTest, noDepthWrite);
			CACHE.put(key, type);
		}
		return type;
	}

	private static RenderType create(ResourceLocation texture, String mode, boolean noDepthTest, boolean noDepthWrite)
	{
		RenderType.CompositeState.CompositeStateBuilder builder = RenderType.CompositeState.builder()
				.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
				.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
				.setCullState(RenderStateShard.NO_CULL)
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

		String name = "thkaguyamod_danmaku/" + mode + (noDepthTest ? "_nodepthtest" : "") + (noDepthWrite ? "_nodepthwrite" : "")
				+ "/" + texture.getPath();
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
