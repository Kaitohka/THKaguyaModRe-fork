package thkaguyamod.client.renderer.living;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelRumia;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.living.EntityRumia;

/**
 * ルーミア（{@link EntityRumia}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderRumia} の移植。
 *
 * <p>モデル描画は {@link RenderTHBoss}（＝HP ゲージ／スペルカード名付きの
 * {@code LivingEntityRenderer}）に任せ、その後に「全身を覆う暗闇の球」を手続き的に足す。
 * 1.7.10 の {@code renderDark} は Z 軸回転 18 分割 × 奥行き 9 分割の四角形掃引で
 * 半球を作っていた。頂点演算（{@code Math.sin/cos} の使い方、{@code zPos}/{@code widthOld} の
 * 更新順）は 1.7.10 から一字も変えない。</p>
 *
 * <p><b>深度の写像</b>（1.7.10 の順序をそのまま再現）:</p>
 * <ol>
 *   <li>{@code getDistanceToCamera(...) < 36.0D} なら {@code glDepthFunc(GL_ALWAYS)}
 *       → {@code noDepthTest = true}、そうでなければ何もしない（LEQUAL のまま）</li>
 *   <li>直後の {@code glDepthMask(false)} は無条件 → 常に {@code noDepthWrite = true}</li>
 * </ol>
 *
 * <p><b>ブレンド</b>: 1.7.10 は {@code glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_COLOR)}。
 * これは 1.21.1 の {@link RenderStateShard} に該当が無く、
 * {@link DanmakuRenderTypes} の 4 モードにも含まれないため、このクラス内に
 * {@link #DARK_TRANSPARENCY} として同じ書式で自前定義している
 * （{@code glBlendFunc} と等価。{@link DanmakuRenderTypes} へ取り込むのが本筋）。</p>
 *
 * <p>TODO K3-review: 貼り先 {@code DarkTexture.png} はまだ移植先にコピーされていない。
 * 1.7.10 の {@code textures/DarkTexture.png} を
 * {@code assets/thkaguyamod/textures/entity/darktexture.png} に置く想定でパスを書いてある
 * （他の同種レンダラ {@code RenderPrivateSquare} / {@code RenderSakuyaStopWatch} /
 * {@code RenderSpiritualStrikeTalisman} も同じ貼り先を使うため、コピー時にパスを確定すること）。
 * なお {@code DarkTexture.png} は全面ほぼ単色（白）のテクスチャで、掃引の見た目は
 * 頂点色とブレンドだけで決まる。</p>
 *
 * <p>TODO K3-review: 1.7.10 は 18×9 の四角形を 1 枚ずつ即時モードで描いていた。
 * 1.21.1 では同一 RenderType の 1 バッファにまとめて積まれる（162 枚）。
 * 加法ではなく減法系ブレンドなので重なり順の差が結果に出やすい。見た目が異なる場合は
 * 掃引を分割して複数の RenderType に振り分けること。</p>
 */
public class RenderRumia extends RenderTHBoss<EntityRumia, ModelRumia>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "rumia"), "main");

	/** 1.7.10: textures/mob/RumiaTexture.png */
	private static final ResourceLocation RUMIA_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/rumiatexture.png");

	/** 1.7.10: textures/DarkTexture.png（未コピー。TODO 参照） */
	private static final ResourceLocation DARK_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/darktexture.png");

	/** 暗闇の描画を始める距離しきい値（1.7.10 のハードコード 36.0D）。 */
	private static final double DARK_NEAR_DISTANCE = 36.0D;

	public RenderRumia(EntityRendererProvider.Context context)
	{
		super(context, new ModelRumia(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityRumia rumia)
	{
		return RUMIA_TEXTURE;
	}

	@Override
	public void render(EntityRumia entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight)
	{
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		this.renderRumia(entity, poseStack, bufferSource);
	}

	/** 1.7.10 {@code renderRumia}。 */
	public void renderRumia(EntityRumia rumia, PoseStack poseStack, MultiBufferSource bufferSource)
	{
		boolean noDepthTest = this.entityRenderDispatcher.distanceToSqr(rumia) < DARK_NEAR_DISTANCE * DARK_NEAR_DISTANCE;

		poseStack.pushPose();
		// 1.7.10: glRotatef(-playerViewY, Y) → glRotatef(playerViewX, X)（カメラ正対）
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		// 1.7.10: glTranslatef(0.0F, 1.3F, 0.0F)
		poseStack.translate(0.0F, 1.3F, 0.0F);

		VertexConsumer buffer = bufferSource.getBuffer(RenderType.create(
				"thkaguyamod_rumia_dark" + (noDepthTest ? "_nodepthtest" : ""),
				DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
				RenderType.CompositeState.builder()
						.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
						.setTextureState(new RenderStateShard.TextureStateShard(DARK_TEXTURE, false, false))
						.setCullState(RenderStateShard.NO_CULL)
						.setDepthTestState(noDepthTest ? RenderStateShard.NO_DEPTH_TEST : RenderStateShard.LEQUAL_DEPTH_TEST)
						.setWriteMaskState(RenderStateShard.COLOR_WRITE)
						.setTransparencyState(DARK_TRANSPARENCY)
						.createCompositeState(false)));

		renderDark(buffer, poseStack.last(), 12.0D, 12.0F, 0.0D, 1.0F, 0);

		poseStack.popPose();
	}

	/**
	 * 暗闇の描画をする（1.7.10 {@code renderDark} の移植）。
	 *
	 * <p>1.7.10 は 1 枚ごとに {@code tessellator.draw()} していたが、1.21.1 は
	 * 同一バッファへ積む（{@link #DARK_TRANSPARENCY} の TODO 参照）。</p>
	 *
	 * @param length 1.7.10 でも実質未使用（掃引は {@code width} と分割数のみで決まる）
	 * @param zPos   呼び出し直後に上書きされる（原作どおりの死引数）
	 * @param time   0 以外なら色変動あり（呼び出し元は常に 0）
	 */
	protected void renderDark(VertexConsumer buffer, PoseStack.Pose pose, double length, float width,
			double zPos, float alpha, int time)
	{
		float maxWidth = (float)width / 2.0F;//最大の太さをmaxWidthとして保存

		int zAngleDivNum = 18;//Z軸回転の分割数
		double angleZ = 0F;//Z軸回転変数
		double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;//Z軸回転の変化量

		int zDivNum = 9;//レーザーの奥方向への分割数。必ず奇数
		double zLength = width;//レーザーの長さ（Z方向の長さ、奥行き）
		double zDivLength = zLength / (double)(zDivNum - 1);//Z方向へ分割したときの1分割分の長さ
		double zLength2 = zLength / 2.0D;//長さの半分

		zPos = Math.sin(-Math.PI / 2.0D) * maxWidth;
		double zPosOld = zPos;//ひとつ前の描画位置
		//初期のXとYの座標（レーザーの始点は点）
		float xPos = 0F;
		float yPos = 0F;
		float xPos2 = 0F;
		float yPos2 = 0F;
		//初期のXとYの座標
		float xPosOld = xPos;
		float yPosOld = yPos;
		float xPos2Old = xPos2;
		float yPos2Old = yPos2;
		//半円を描くようにレーザーが太くなるための変数。cos0 ~ cos180で処理
		float angle = -(float)Math.PI / 2.0F;
		float angleSpan = (float)Math.PI / (float)(zDivNum);
		angle += angleSpan;
		//レーザーの太さ。Z軸方向への進行で２つ必要
		float widthOld = 0.0F;

		for(int j = 0; j < zDivNum; j++)
		{
			zPos = Math.sin(angle) * maxWidth;
			width = (float)Math.cos(angle) * maxWidth;

			//XとY座標は初期値、0度のときの座標に戻る。
			xPos = width;
			yPos = 0F;
			angleZ = 0F;
			xPosOld = (float)Math.cos(angleZ) * width;
			yPosOld = (float)Math.sin(angleZ) * width;
			xPos2Old = (float)Math.cos(angleZ) * widthOld;
			yPos2Old = (float)Math.sin(angleZ) * widthOld;
			//Z軸回転の始点
			angleZ = angleSpanZ;

			for(int i = 1; i <= zAngleDivNum; i++)
			{
				xPos = (float)Math.cos(angleZ) * width;
				yPos = (float)Math.sin(angleZ) * width;
				xPos2 = (float)Math.cos(angleZ) * widthOld;
				yPos2 = (float)Math.sin(angleZ) * widthOld;

				// 1.7.10 の colorVar は計算だけして未使用
				float r = 1.0F, g = 1.0F, b = 1.0F;
				buffer.addVertex(pose, xPos, yPos, (float)zPos).setColor(r, g, b, alpha).setUv(1.0F, 0.0F);
				buffer.addVertex(pose, xPosOld, yPosOld, (float)zPos).setColor(r, g, b, alpha).setUv(0.0F, 0.0F);
				buffer.addVertex(pose, xPos2Old, yPos2Old, (float)zPosOld).setColor(r, g, b, alpha).setUv(0.0F, 1.0F);
				buffer.addVertex(pose, xPos2, yPos2, (float)zPosOld).setColor(r, g, b, alpha).setUv(1.0F, 1.0F);

				xPosOld = xPos;
				yPosOld = yPos;
				xPos2Old = xPos2;
				yPos2Old = yPos2;
				angleZ += angleSpanZ;
			}
			zPosOld = zPos;//古い奥行きを今の奥行きに更新
			angle += angleSpan;//レーザーの描く半円の角度を更新
			widthOld = width;
		}
	}

	// ---------------------------------------------------------------------
	// 1.7.10 の glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_COLOR) に対応する shard は
	// 1.21.1 に無いため、RenderStateShard と同じ書式で自前定義する
	// （DanmakuRenderTypes の ONE_MINUS_SRC_COLOR_TRANSPARENCY と同型）。
	// ---------------------------------------------------------------------

	/** glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_COLOR) */
	private static final RenderStateShard.TransparencyStateShard DARK_TRANSPARENCY =
			new RenderStateShard.TransparencyStateShard(
					"thkaguyamod_rumia_dark_transparency",
					() ->
					{
						RenderSystem.enableBlend();
						RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
					},
					() ->
					{
						RenderSystem.disableBlend();
						RenderSystem.defaultBlendFunc();
					});
}