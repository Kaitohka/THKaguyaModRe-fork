package thkaguyamod.client.renderer.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.client.renderer.shot.RenderTHShot;
import thkaguyamod.entity.effect.EntityHakurouReflecter;

/**
 * 白楼剣の反射エフェクト（{@link EntityHakurouReflecter}）のレンダラ。
 * 1.7.10 の {@code thKaguyaMod.client.render.effect.RenderHakurouReflecter} の忠実移植。
 *
 * <p>扇状の弧面（中心 = 0,0 の三角形状 QUADS）を 3 重（半径 1.8 / 1.5 / 1.2）に、
 * 角度 π/8 刻みで 16 枚ずつ重ねて加算合成する。角度・半径・UV の数値は 1.7.10 のまま。</p>
 *
 * <p>1.7.10 は {@code glDepthMask(false)} で描いていたため、このレンダラは
 * {@link RenderTHShot.Ctx#buf(String, boolean, boolean)} の noDepthWrite 版
 * （{@link net.minecraft.client.renderer.RenderStateShard#COLOR_WRITE}）を使う。
 * 描画後に {@code glDepthMask(true)} へ戻す処理は RenderType のバッチ単位でしか
 * 表現できないため、後続のバッチが破棄した状態に依存しないよう明示的に分離している。</p>
 *
 * <p>色: 1.7.10 は {@code alpha = 0.2F + j * 0.3F}（j は 0-15）をそのまま
 * {@code setColorRGBA_F} に渡しており、固定機能パイプラインが [0,1] にクランプしていた。
 * 1.21.1 の {@code setColor(float...)} は 255 倍して byte 化するため 4.0 等の値が
 * 折り返してしまう。ここでは {@link Mth#clamp} でクランプし、原実装の
 * 「j が増えるほど不透明になり、途中から 1.0 で飽和する」挙動を再現する。</p>
 *
 * <p>テクスチャ: 1.7.10 の {@code textures/HakurouReflect.png} は移植先の規約に合わせて
 * {@code textures/entity/shot/hakuroureflect.png} を参照する（要アセット配置）。
 * 描画に使う u=0.0-0.5 の左半分は原テクスチャでは不透明、右半分は空。</p>
 *
 * <p>TODO K3-review: 1.7.10 は {@code rotationPitch} を直接読んでいた。ここでは
 * {@code getXRot()}（Mojmap での rotationPitch 相当）を使う。Entity の同期が
 * 1.7.10 と同じタイミングか、統合時に確認すること。</p>
 */
public class RenderHakurouReflecter extends EntityRenderer<EntityHakurouReflecter>
{
	//白楼剣の反射エフェクトの描画（1.7.10: "textures/HakurouReflect.png"）
	private static final ResourceLocation hakurouReflecterTexture = DanmakuRenderTypes.texture("HakurouReflect");

	/** コンストラクタ */
	public RenderHakurouReflecter(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityHakurouReflecter entity)
	{
		return hakurouReflecterTexture;
	}

	@Override
	public void render(EntityHakurouReflecter entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		ResourceLocation texture = this.getTextureLocation(entity);
		poseStack.pushPose();
		RenderTHShot.Ctx ctx = new RenderTHShot.Ctx(poseStack, bufferSource, texture, this.entityRenderDispatcher);
		this.doRenderHakurouReflecter(ctx, entity, entityYaw);
		poseStack.popPose();
	}

	/**
	 * @param yaw 1.7.10 の doRender から渡される yaw（EntityRenderDispatcher が計算した補間済みの値）。
	 */
	public void doRenderHakurouReflecter(RenderTHShot.Ctx ctx, EntityHakurouReflecter hakurouReflecter, float yaw)
	{
		// 1.7.10: GL11.glScalef(1.0F, 1.0F, 1.0F)（恒等変換だが原実装どおり残す）
		ctx.pose.scale(1.0F, 1.0F, 1.0F);

		float minU = 0.0F;
		float maxU = 0.5F;
		float minV = 0.0F;
		float maxV = 1.0F;
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		// 1.7.10: GL11.glRotatef(hakurouReflecter.rotationPitch, 1.0F, 0.0F, 0.0F)
		ctx.pose.mulPose(Axis.XP.rotationDegrees(hakurouReflecter.getXRot()));

		float angle = hakurouReflecter.tickCount / 5F;// 1.7.10 の ticksExisted
		float span = (float)Math.PI / 8F;
		double length = 1.8D;

		// 1.7.10: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR) + glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true);

		for(int i = 0; i < 3; i++)
		{
			double xPos1 = Math.cos(angle) * length;
			double xPos2 = Math.cos(angle + span) * length;
			double yPos1 = Math.sin(angle) * length;
			double yPos2 = Math.sin(angle + span) * length;
			for(int j = 0; j < 16; j++)
			{
				// 1.7.10 は 0.2F + j * 0.3F をそのまま渡していた（固定機能が [0,1] にクランプ）
				quad(b, ctx.pose.last(),
						(float)xPos1, (float)yPos1, 0.0F, minU, maxV,
						(float)xPos2, (float)yPos2, 0.0F, maxU, maxV,
						0.0F, 0.0F, 0.0F, maxU, minV,
						0.0F, 0.0F, 0.0F, minU, minV,
						1.0F, 1.0F, 1.0F, Mth.clamp(0.2F + j * 0.3F, 0.0F, 1.0F));
				angle += span;
				xPos1 = Math.cos(angle) * length;
				xPos2 = Math.cos(angle + span) * length;
				yPos1 = Math.sin(angle) * length;
				yPos2 = Math.sin(angle + span) * length;
			}
			length -= 0.3D;
		}
	}

	/** 4 頂点（QUADS）を 1 枚書き込む。UV は addVertexWithUV の (u,v) をそのまま渡す。 */
	private static void quad(VertexConsumer b, PoseStack.Pose pose,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3,
			float r, float g, float bl, float a)
	{
		// 1.7.10 の getBrightnessForRender() は常に 0xf000f0 を返す → LightTexture.FULL_BRIGHT。
		// POSITION_TEX_COLOR にライトマップ要素は無いため setLight は no-op だが、
		// 「常に最大輝度」という原仕様をここに明示しておく。
		b.addVertex(pose, x0, y0, z0).setColor(r, g, bl, a).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x1, y1, z1).setColor(r, g, bl, a).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x2, y2, z2).setColor(r, g, bl, a).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x3, y3, z3).setColor(r, g, bl, a).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}