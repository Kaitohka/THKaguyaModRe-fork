package thkaguyamod.client.renderer.shot;

import java.util.Random;

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
import thkaguyamod.entity.shot.EntityTHShot;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/**
 * 弾（EntityTHShot）の弾幕レンダラ。1.7.10 の
 * {@code thKaguyaMod.client.render.shot.RenderTHShot} の忠実移植。
 *
 * <p>GL11 / Tessellator.instance は使用せず、{@link EntityRenderer#render} の
 * {@link PoseStack} + {@link MultiBufferSource} で描画する。頂点演算（角度・半径・
 * 掃引パラメータ）は 1.7.10 の実装から一字も変えずに残している。</p>
 *
 * <p>座標系: 1.7.10 の {@code GL11.glTranslatef(x,y,z)} に相当する移動は
 * EntityRenderDispatcher が既に poseStack に適用済みなので、ここでは行わない。</p>
 *
 * <p>ビルボード: 1.7.10 の {@code THKaguyaRenderLib.getGLRotatefByTherdPersonView(renderManager)}
 * は {@code poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation())} に置換した。</p>
 *
 * <p>TODO K3-review: 1.7.10 は {@code glDepthMask} を描画の途中で頻繁に切り替えていた。
 * 1.21.1 のバッチ式（begin/end）では途中変更できないため、各サブパスを「深度書き込みあり/なし」の
 * 別 RenderType に分けて近似している。実際にどこで depthMask が変わっているかはブランチ単位で
 * 追い切れていない箇所があるため、統合時に 1.7.10 の該当メソッドと突き合わせて確認すること。</p>
 */
public class RenderTHShot extends EntityRenderer<EntityTHShot>
{
	// --- テクスチャ（1.7.10 の textures/shot/***.png → textures/entity/shot/***.png, 全小文字） ---
	private static final ResourceLocation resourceLocation_Small = DanmakuRenderTypes.texture("SmallShot");
	private static final ResourceLocation resourceLocation_Pearl = DanmakuRenderTypes.texture("PearlShot");
	private static final ResourceLocation resourceLocation_Circle = DanmakuRenderTypes.texture("CircleShot");
	private static final ResourceLocation resourceLocation_Big = DanmakuRenderTypes.texture("BigShot");
	private static final ResourceLocation resourceLocation_Talisman = DanmakuRenderTypes.texture("TalismanShot");
	private static final ResourceLocation resourceLocation_Heart = DanmakuRenderTypes.texture("HeartShot");
	private static final ResourceLocation resourceLocation_Normal = DanmakuRenderTypes.texture("Shot");
	private static final ResourceLocation resourceLocation_Butterfly = DanmakuRenderTypes.texture("ButterflyShot");
	private static final ResourceLocation resourceLocation_Star = DanmakuRenderTypes.texture("StarShot");
	private static final ResourceLocation resourceLocation_Scale = DanmakuRenderTypes.texture("ScaleShot");
	private static final ResourceLocation resourceLocation_Wind = DanmakuRenderTypes.texture("WindShot");
	private static final ResourceLocation resourceLocation_Knife_Blue = DanmakuRenderTypes.texture("SilverKnife_Blue");
	private static final ResourceLocation resourceLocation_Knife_Red = DanmakuRenderTypes.texture("SilverKnife_Red");
	private static final ResourceLocation resourceLocation_Knife_Green = DanmakuRenderTypes.texture("SilverKnife_Green");
	private static final ResourceLocation resourceLocation_Knife_Yellow = DanmakuRenderTypes.texture("SilverKnife_Yellow");
	private static final ResourceLocation resourceLocation_Knife_Purple = DanmakuRenderTypes.texture("SilverKnife_Purple");
	private static final ResourceLocation resourceLocation_Knife_Orange = DanmakuRenderTypes.texture("SilverKnife_Orange");
	private static final ResourceLocation resourceLocation_Knife_Aqua = DanmakuRenderTypes.texture("SilverKnife_Aqua");
	private static final ResourceLocation resourceLocation_Knife_White = DanmakuRenderTypes.texture("SilverKnife_White");
	private static final ResourceLocation resourceLocation_Light = DanmakuRenderTypes.texture("MusouFuuin");
	private static final ResourceLocation resourceLocation_Kunai = DanmakuRenderTypes.texture("KunaiShot");
	private static final ResourceLocation resourceLocation_Laser = DanmakuRenderTypes.texture("Laser");
	private static final ResourceLocation resourceLocation_Phantom = DanmakuRenderTypes.texture("MusouFuuin");
	private static final ResourceLocation resourceLocation_Amulet = DanmakuRenderTypes.texture("HomingAmulet");
	private static final ResourceLocation resourceLocation_Diffusion = DanmakuRenderTypes.texture("DiffusionAmulet");

	private static final ResourceLocation resourceLocation_Familiar = ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/Familiar.png");
	private static final ResourceLocation resourceLocation_Arrow = DanmakuRenderTypes.texture("ArrowShot");

	/** 1.7.10 の色パレット（数値はそのまま）。 */
	protected float colorR[] = { 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F, 255F/255F};
	protected float colorG[] = {   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 224F/255F, 128F/255F, 255F/255F};
	protected float colorB[] = {   0F/255F, 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F};

	/** コンストラクタ */
	public RenderTHShot(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTHShot entity)
	{
		int color = entity.getShotColor();
		int form = entity.getShotForm();//形状

		if(entity.getAnimationCount() < 0)
		{
			return resourceLocation_Light;
		}
		else
		{
			switch(form)
			{
				case FORM_SMALL:
				case FORM_TINY:
				case FORM_MEDIUM:
					return resourceLocation_Small;
				case FORM_PEARL:
					return resourceLocation_Pearl;
				case FORM_CIRCLE:
					return resourceLocation_Circle;
				case FORM_SCALE:
					// 1.7.10 の resourceLocation_Scale は使用箇所で上書きされていたため Laser を返す
					return resourceLocation_Laser;
				case FORM_BUTTERFLY:
					return resourceLocation_Butterfly;
				case FORM_SMALLSTAR:
				case FORM_STAR:
					return resourceLocation_Star;
				case FORM_RICE:
					return resourceLocation_Laser;
				case FORM_CRYSTAL:
					return resourceLocation_Star;
				case FORM_LIGHT:
					return resourceLocation_Light;
				case FORM_HEART:
					return resourceLocation_Heart;
				case FORM_KUNAI:
					return resourceLocation_Kunai;
				case FORM_TALISMAN:
					return resourceLocation_Talisman;
				case FORM_BIGLIGHT:
					return resourceLocation_Light;
				case FORM_OVAL:
					return resourceLocation_Laser;
				case FORM_FAMILIAR:
					return resourceLocation_Familiar;
				case FORM_ARROW:
					return resourceLocation_Arrow;
				case FORM_PHANTOM:
					return resourceLocation_Phantom;
				case FORM_AMULET:
					switch(color)
					{
						case 1:
							return resourceLocation_Diffusion;
						default:
							return resourceLocation_Amulet;
					}
				case FORM_KNIFE:
					switch(color)
					{
						case 0:
							return resourceLocation_Knife_Red;
						case 1:
							return resourceLocation_Knife_Blue;
						case 2:
							return resourceLocation_Knife_Green;
						case 3:
							return resourceLocation_Knife_Yellow;
						case 4:
							return resourceLocation_Knife_Purple;
						case 5:
							return resourceLocation_Knife_Aqua;
						case 6:
							return resourceLocation_Knife_Orange;
						default:
							return resourceLocation_Knife_White;
					}
				case FORM_WIND:
					return resourceLocation_Wind;
				case FORM_BIG:
					return resourceLocation_Big;
				case FORM_KISHITU:
					return resourceLocation_Light;
				default:
					return resourceLocation_Light;
			}
		}
	}

	@Override
	public void render(EntityTHShot entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		// 1.7.10 は getBrightnessForRender で 0xf000f0 固定 → ここでは LightTexture.FULL_BRIGHT を使う
		// packedLight は使用しない（FULL_BRIGHT 固定のため）
		ResourceLocation texture = this.getTextureLocation(entity);
		poseStack.pushPose();
		Ctx ctx = new Ctx(poseStack, bufferSource, texture, this.entityRenderDispatcher);
		this.renderTHShot(ctx, entity, entityYaw, entityYaw, partialTick);
		poseStack.popPose();
	}

	/**
	 * @param thShot 弾
	 * @param yaw    1.7.10 の doRender の yaw（entityYaw を渡す）
	 * @param f1     1.7.10 の doRender の pitch（entityYaw と同じ値を渡す。1.7.10 の呼び出し元は yaw/pitch に
	 *               同じ値(renderYaw)を渡していたため）
	 */
	public void renderTHShot(Ctx ctx, EntityTHShot thShot, float yaw, float f1, float partialTick)
	{
		float size = thShot.getShotSize();
		float size2;
		int color = thShot.getShotColor();//色
		int form = thShot.getShotForm();//形状

		// 弾の遅延描画（光だけ見える）
		if(thShot.getAnimationCount() < 0)
		{
			int delayCount = -thShot.getAnimationCount();
			if(delayCount > 10)
			{
				delayCount = 10;
			}
			size2 = delayCount * 0.3F * size;
			if(size2 > 1.0F)
			{
				size2 = 1.0F;
			}
			// 1.7.10: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR) + 常に正面
			ctx.pose.pushPose();
			billboard(ctx);
			renderLightEffect(ctx, color, size2, thShot.getAnimationCount());
			ctx.pose.popPose();
		}
		// 弾が実体化している場合
		else
		{
			float pitch = thShot.getXRot();// 1.7.10 の rotationPitch
			switch(form)
			{
				// 小弾
				case FORM_SMALL:
					renderNormalShot(ctx, size, color, false);
					break;
				// 粒弾
				case FORM_TINY:
					renderNormalShot(ctx, size, color, false);
					break;
				// 中弾
				case FORM_MEDIUM:
					renderNormalShot(ctx, size, color, false);
					break;
				// 真珠弾
				case FORM_PEARL:
					renderNormalShot(ctx, size, color, false);
					break;
				// 輪弾
				case FORM_CIRCLE:
					renderNormalShot(ctx, size, color, false);
					break;
				// 鱗弾
				case FORM_SCALE:
					renderScaleShot(ctx, size, color, yaw, pitch, thShot.getAngleZ(), 0.0D, 0.0D, 0.0D);
					break;
				// 蝶弾
				case FORM_BUTTERFLY:
					renderButterflyShot(ctx, size, color, thShot.getAnimationCount(), yaw, pitch, thShot.getAngleZ());
					break;
				// 小星弾
				case FORM_SMALLSTAR:
					renderStarShot(ctx, size, color, thShot.getAnimationCount());
					break;
				// 星弾
				case FORM_STAR:
					renderStarShot(ctx, size, color, thShot.getAnimationCount());
					break;
				// 米弾
				case FORM_RICE:
					renderRiceShot(ctx, size, yaw, pitch, 4.0F,  1.20F, -2.0D, 7, 5, color, 0.7F);
					break;
				// 結晶弾
				case FORM_CRYSTAL:
					renderCrystalShot(ctx, size, color, yaw, pitch);
					break;
				// 光弾
				case FORM_LIGHT:
					renderLightShot(ctx, size, color, thShot.getAnimationCount() % 2);
					break;
				// ハート弾
				case FORM_HEART:
					renderNormalShot(ctx, size, color, false);
					break;
				// クナイ弾
				case FORM_KUNAI:
					renderKunaiShot(ctx, size, color, yaw, pitch, thShot.getAngleZ());
					break;
				// 札弾
				case FORM_TALISMAN:
					renderTalismanShot(ctx, size, color, yaw, pitch, thShot.getAngleZ());
					break;
				// 大光弾
				case FORM_BIGLIGHT:
					renderLightShot(ctx, size, color, thShot.getAnimationCount() % 2);
					break;
				// 楕円弾
				case FORM_OVAL:
					renderOvalShot(ctx, size, yaw, pitch, 2.0F,  1.0F, -1.0D, 7, 5, color, 0.7F);
					break;
				// 使い魔
				case FORM_FAMILIAR:
					renderFamiliar(ctx, size, color, thShot.getAnimationCount());
					break;
				// 矢弾
				case FORM_ARROW:
					renderArrowShot(ctx, size, color, yaw, pitch, thShot.getAngleZ());
					break;
				// 幽霊弾
				case FORM_PHANTOM:
					renderPhantom(ctx, thShot, color);
					break;
				// アミュレット弾
				case FORM_AMULET:
					renderAmuletShot(ctx, size, color, thShot.getAnimationCount(), yaw, pitch, thShot.getAngleZ());
					break;
				// ナイフ弾
				case FORM_KNIFE:
					// TODO K3-review: 1.7.10 は ModelSilverKnife (ModelBase) を entity 座標系で描画していた。
					//   1.21.1 では ModelSilverKnife がまだ移植されておらず、エンティティモデル描画は
					//   EntityRenderer + EntityModel + RenderType.entityCutout へ書き換えが必要。
					//   現状は他のフォームと同じ手続き的描画にフォールバックし、ナイフテクスチャを貼る。
					//   (移植後は poseStack に yaw/-pitch/angleZ を適用し ModelSilverKnife をレンダリングすること)
					renderNormalShot(ctx, size, color, false);
					break;
				// 風弾
				case FORM_WIND:
					renderWindShot(ctx, size, color, thShot.getAnimationCount());
					break;
				// 大弾
				case FORM_BIG:
					renderNormalShot(ctx, size, color, true);
					break;
				// 気質弾
				case FORM_KISHITU:
					renderKishituShot(ctx, size, color, thShot.getAnimationCount());
					break;
				default:
			}
		}
	}

	// ====================================================================
	// 各形状の描画
	// ====================================================================

	/** 使い魔の描画 */
	public void renderFamiliar(Ctx ctx, float size, int color, int count)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		billboard(ctx);
		ctx.pose.mulPose(Axis.ZP.rotationDegrees((float)count * 3.7F));
		float width = 1.0F;
		float height = 1.0F;
		float u1 = 0F;
		float u2 = 1F;
		float v1 = 0F;
		float v2 = 1F;
		quad(b, ctx.pose.last(),
				-width, -height, 0.0F, u1, v2,
				 width, -height, 0.0F, u2, v2,
				 width,  height, 0.0F, u2, v1,
				-width,  height, 0.0F, u1, v1,
				colorR[color], colorG[color], colorB[color], 0.5F);
		ctx.pose.popPose();
	}

	/** 円形弾の描画 */
	public void renderNormalShot(Ctx ctx, float size, int color, boolean blend)
	{
		VertexConsumer b = ctx.buf(blend ? DanmakuRenderTypes.MODE_ADD_SRC_ALPHA : DanmakuRenderTypes.MODE_OPAQUE, false, blend);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		billboard(ctx);
		float width = 1.0F;
		float height = 1.0F;
		float u1 = (float)color / 8F;
		float u2 = (float)(color + 1) / 8F;
		float v1 = 0F;
		float v2 = 1F;
		quad(b, ctx.pose.last(),
				-width, -height, 0.0F, u1, v2,
				 width, -height, 0.0F, u2, v2,
				 width,  height, 0.0F, u2, v1,
				-width,  height, 0.0F, u1, v1,
				1.0F, 1.0F, 1.0F, 1.0F);
		ctx.pose.popPose();
	}

	/** 光の弾の描画 */
	public void renderLightShot(Ctx ctx, float size, int color, int count)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		billboard(ctx);
		int c = color % 8;
		float uMin = (float)(count * 32 +  0) / 64F;
		float uMax = (float)(count * 32 + 32) / 64F;
		float vMin = 0F;
		float vMax = 1F;
		float width = 2.0F;
		float alpha = 1.0F;
		quad(b, ctx.pose.last(),
				-width, -width, 0.0F, uMin, vMax,
				 width, -width, 0.0F, uMax, vMax,
				 width,  width, 0.0F, uMax, vMin,
				-width,  width, 0.0F, uMin, vMin,
				colorR[c], colorG[c], colorB[c], 0.8F * alpha);
		width = 1.0F;
		quad(b, ctx.pose.last(),
				-width, -width, 0.01F, uMin, vMax,
				 width, -width, 0.01F, uMax, vMax,
				 width,  width, 0.01F, uMax, vMin,
				-width,  width, 0.01F, uMin, vMin,
				1F, 1F, 1F, 1.0F * alpha);
		ctx.pose.popPose();
	}

	/** 気質弾の描画 */
	public void renderKishituShot(Ctx ctx, float size, int color, int count)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		billboard(ctx);
		int c = count % 2;
		float uMin = (float)(c * 32 +  0) / 64F;
		float uMax = (float)(c * 32 + 32) / 64F;
		float vMin = 0F;
		float vMax = 1F;
		float width = 2.0F;
		quad(b, ctx.pose.last(),
				-width, -width, 0.0F, uMin, vMax,
				 width, -width, 0.0F, uMax, vMax,
				 width,  width, 0.0F, uMax, vMin,
				-width,  width, 0.0F, uMin, vMin,
				colorR[color], colorG[color], colorB[color], 0.5F);
		width = 1.6F;
		quad(b, ctx.pose.last(),
				-width, -width, 0.01F, uMin, vMax,
				 width, -width, 0.01F, uMax, vMax,
				 width,  width, 0.01F, uMax, vMin,
				-width,  width, 0.01F, uMin, vMin,
				1F, 1F, 1F, 0.8F);
		ctx.pose.popPose();
	}

	/** 光のエフェクト（遅延期の予告） */
	public void renderLightEffect(Ctx ctx, int color, float size, int count)
	{
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA);
		int c = count % 2;
		float u1 = (float)(c * 32 +  0) / 64F;
		float u2 = (float)(c * 32 + 32) / 64F;
		float v1 = 0F;
		float v2 = 1F;
		float width = 1.0F;
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		quad(b, ctx.pose.last(),
				-width, -width, 0.0F, u1, v2,
				 width, -width, 0.0F, u2, v2,
				 width,  width, 0.0F, u2, v1,
				-width,  width, 0.0F, u1, v1,
				colorR[c], colorG[c], colorB[c], 0.5F);
		ctx.pose.popPose();
	}

	/** 鱗弾の描画 */
	public void renderScaleShot(Ctx ctx, float size, int color, float yaw, float pitch, float slope, double x, double y, double z)
	{
		// TODO K3-review: x,y,z は 1.7.10 ではコメントアウトされ未使用（エンティティ座標系を前提）。
		double length = 2.0D;
		float width = 1.0F;
		double zPos = -1.0D;
		int zAngleDivNum = 7;
		int zDivNum = 8;
		float alpha = 0.31F;

		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw + 180F));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(pitch));
		// 内側（ブレンドなし・深度書き込みあり）
		renderOvalShotPart(ctx, ctx.buf(DanmakuRenderTypes.MODE_OPAQUE), ctx.pose.last(), size, yaw, pitch, length * 0.4D, width * 0.5F, -0.85D, zAngleDivNum, 5, 7, 1.0F);
		// 外側（(SRC_ALPHA, ONE)・glDepthMask(false)）
		renderHarfOvalShotPart(ctx, ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true), ctx.pose.last(), size, yaw, pitch, length, width * 1.2F, zPos, zAngleDivNum, zDivNum, color, alpha);
		ctx.pose.popPose();
	}

	/** 蝶弾 */
	public void renderButterflyShot(Ctx ctx, float size, int color, int count, float yaw, float pitch, float slope)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		float wingAngle = Mth.sin((float)count / 3.0F) * 45F;
		if(pitch > 90F)
		{
			pitch = 90F - pitch % 90F;
		}
		else if(pitch < -90F)
		{
			pitch = -90F - pitch % 90F;
		}
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));
		ctx.pose.mulPose(Axis.ZP.rotationDegrees(slope));
		ctx.pose.mulPose(Axis.ZP.rotationDegrees(wingAngle));

		int c = color % 8;
		float minU =  0F / 128F;
		float maxU = 32F / 128F;
		float minV = 0F;
		float maxV = 1F;
		float width = 2.0F;
		float width2 = 1.8F;

		quad(b, ctx.pose.last(),
				 0.0F, 0.0F,  width, maxU, minV,
				 width, 0.0F,  width, minU, minV,
				 width, 0.0F, -width, minU, maxV,
				 0.0F, 0.0F, -width, maxU, maxV,
				colorR[c], colorG[c], colorB[c], 1.0F);
		quad(b, ctx.pose.last(),
				 0.0F , 0.000F,  width2, maxU, minV,
				 width2, 0.000F,  width2, minU, minV,
				 width2, 0.000F, -width2, minU, maxV,
				 0.0F , 0.000F, -width2, maxU, maxV,
				1.0F, 1.0F, 1.0F, 0.4F);

		ctx.pose.mulPose(Axis.ZP.rotationDegrees(-wingAngle * 2F));
		width *= 1.0F;
		width2 *= 1.0F;

		quad(b, ctx.pose.last(),
				 0.0F, 0.0F,  width, maxU, minV,
				-width, 0.0F,  width, minU, minV,
				-width, 0.0F, -width, minU, maxV,
				 0.0F, 0.0F, -width, maxU, maxV,
				colorR[c], colorG[c], colorB[c], 1.0F);
		quad(b, ctx.pose.last(),
				 0.0F , 0.000F,  width2, maxU, minV,
				-width2, 0.000F,  width2, minU, minV,
				-width2, 0.000F, -width2, minU, maxV,
				 0.0F , 0.000F, -width2, maxU, maxV,
				1.0F, 1.0F, 1.0F, 0.4F);
		ctx.pose.popPose();
	}

	/** 星弾の描画 */
	public void renderStarShot(Ctx ctx, float size, int color, int count)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		billboard(ctx);
		ctx.pose.mulPose(Axis.ZP.rotationDegrees((float)count * 3.7F));

		float[] topPointX = new float[11];
		float[] topPointY = new float[11];
		float angle = 0F;
		float spanAngle = (float)Math.PI * 0.2F;//36°
		for(int i = 0; i < 10; i+=2)
		{
			topPointX[i] = (float)Math.cos(angle) * 2.0F;
			topPointY[i] = (float)Math.sin(angle) * 2.0F;
			angle += spanAngle;
			topPointX[i+1] = (float)Math.cos(angle) * 1.2F;
			topPointY[i+1] = (float)Math.sin(angle) * 1.2F;
			angle += spanAngle;
		}
		topPointX[10] = topPointX[0];
		topPointY[10] = topPointY[0];
		for(int i = 0; i < 9; i+=2)
		{
			//星の枠部
			quad(b, ctx.pose.last(),
					topPointX[i]  , topPointY[i]  , 0.0F, 0F, 0F,
					topPointX[i+1], topPointY[i+1], 0.0F, 1F, 0F,
					topPointX[i+2], topPointY[i+2], 0.0F, 1F, 1F,
					          0.0F,           0.0F, 0.0F, 0F, 1F,
					colorR[color], colorG[color], colorB[color], 0.7F);
			//星の中心部　白い
			quad(b, ctx.pose.last(),
					topPointX[i]   * 0.7F, topPointY[i]   * 0.7F,  0.01F, 0F, 0F,
					topPointX[i+1] * 0.95F, topPointY[i+1] * 0.95F,  0.01F, 1F, 0F,
					topPointX[i+2] * 0.7F, topPointY[i+2] * 0.7F,  0.01F, 1F, 1F,
					          0.0F,           0.0F, 0.01F, 0F, 1F,
					1.0F, 1.0F, 1.0F, 0.7F);
		}
		ctx.pose.popPose();
	}

	/** 結晶弾の描画 */
	public void renderCrystalShot(Ctx ctx, float size, int color, float yaw, float pitch)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));
		float width = 0.96F;
		double length = 4.0D, length_b = 1.0D;
		float width2 = width * 0.8F;
		double length2 = length * 0.8D, length2_b = length * 0.4D;
		int i;
		float alpha = 1.0F;

		for(i = 0; i < 4; i++)
		{
			ctx.pose.mulPose(Axis.ZP.rotationDegrees(90F));
			quad(b, ctx.pose.last(),
					  0.0F  , width2  , 0.0F  , 1F, 0F,
					-width2  , 0.0F   , 0.0F  , 0F, 0F,
					  0.0F  , 0.0F   , (float)length2, 0F, 1F,
					  0.0F  , 0.0F   , (float)length2, 0F, 1F,
					1.0F, 1.0F, 1.0F, alpha);
			quad(b, ctx.pose.last(),
					 0.0F   , width2  ,  0.0F  , 1F, 0F,
					 width2  , 0.0F   ,  0.0F  , 0F, 0F,
					 0.0F   , 0.0F   , (float)(-length2_b), 0F, 1F,
					 0.0F   , 0.0F   , (float)(-length2_b), 0F, 1F,
					1.0F, 1.0F, 1.0F, alpha);
		}
		for(i = 0; i < 4; i++)
		{
			ctx.pose.mulPose(Axis.ZP.rotationDegrees(90F));
			quad(b, ctx.pose.last(),
					  0.0F  , width  , 0.0F  , 1F, 0F,
					-width  , 0.0F   , 0.0F  , 0F, 0F,
					  0.0F  , 0.0F   , (float)length, 0F, 1F,
					  0.0F  , 0.0F   , (float)length, 0F, 1F,
					colorR[color], colorG[color], colorB[color], alpha * 0.7F);
			quad(b, ctx.pose.last(),
					 0.0F   , width  ,  0.0F  , 1F, 0F,
					 width  , 0.0F   ,  0.0F  , 0F, 0F,
					 0.0F   , 0.0F   , (float)(-length_b), 0F, 1F,
					 0.0F   , 0.0F   , (float)(-length_b), 0F, 1F,
					colorR[color], colorG[color], colorB[color], alpha * 0.7F);
		}
		ctx.pose.popPose();
	}

	/** クナイ弾の描画 */
	public void renderKunaiShot(Ctx ctx, float size, int color, float yaw, float pitch, float slope)
	{
		renderBlade(ctx, size, color, yaw, pitch, slope, -2.6F, 0.375F);
	}

	/** 矢弾の描画 */
	public void renderArrowShot(Ctx ctx, float size, int color, float yaw, float pitch, float slope)
	{
		renderBlade(ctx, size, color, yaw, pitch, slope, -5.8F, 0.0F);
	}

	/**
	 * クナイ/矢の共通形状。1.7.10 の renderKunaiShot / renderArrowShot は
	 * 取っ手の先端(root)と取っ手 UV の v 始点のみ異なる（それ以外は同一）。
	 */
	private void renderBlade(Ctx ctx, float size, int color, float yaw, float pitch, float slope, float root, float handleV)
	{
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));
		ctx.pose.mulPose(Axis.ZP.rotationDegrees(slope));

		// --- 下地（ブレンドなし・白） ---
		VertexConsumer b0 = ctx.buf(DanmakuRenderTypes.MODE_OPAQUE);
		PoseStack.Pose p = ctx.pose.last();
		float width = 0.8F;
		float width2 = 0.6F;
		float flont = 1.12F;
		float back  = -0.8F;
		float back2 = -1.0F;
		float height = 0.24F;

		//上
		quad(b0, p, -width, 0.0F, back, 0.50F, 1.0F,   0.0F, 0.0F, flont, 0.25F, 0.0F,   width, 0.0F, back, 0.00F, 1.0F,   0.0F, height, back, 0.25F, 1.0F, 1F,1F,1F,1F);
		//下左
		quad(b0, p, 0.0F, 0.0F, flont, 0.25F, 0.0F,  0.0F, 0.0F, flont, 0.25F, 0.0F,  0.0F, -height, back, 0.25F, 1.0F,  width, 0.0F, back, 0.00F, 1.0F, 1F,1F,1F,1F);
		//下右
		quad(b0, p, 0.0F, 0.0F, flont, 0.25F, 0.0F,  0.0F, 0.0F, flont, 0.25F, 0.0F,  -width, 0.0F, back, 0.50F, 1.0F,  0.0F, -height, back, 0.25F, 1.0F, 1F,1F,1F,1F);
		//上左
		quad(b0, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, height, back, 0.25F, 1.0F,  width, 0.0F, back, 0.00F, 1.0F, 1F,1F,1F,1F);
		//下左
		quad(b0, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  width, 0.0F, back, 0.00F, 1.0F,  0.0F, -height, back, 0.25F, 1.0F, 1F,1F,1F,1F);
		//上右
		quad(b0, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  -width, 0.0F, back, 0.50F, 1.0F,  0.0F, height, back, 0.25F, 1.0F, 1F,1F,1F,1F);
		//下右
		quad(b0, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, -height, back, 0.25F, 1.0F,  -width, 0.0F, back, 0.50F, 1.0F, 1F,1F,1F,1F);

		// --- 色付き（(ONE, ONE_MINUS_SRC_COLOR)） ---
		width = 1.0F;
		flont = 1.4F;
		back  = -0.8F;
		back2 = -1.0F;
		height = 0.3F;
		VertexConsumer b1 = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR);
		float r = colorR[color], g = colorG[color], bl = colorB[color];
		//上
		quad(b1, p, -width, 0.0F, back, 0.50F, 1.0F,   0.0F, 0.0F, flont, 0.25F, 0.0F,   width, 0.0F, back, 0.00F, 1.0F,   0.0F, height, back, 0.25F, 1.0F, r,g,bl,1F);
		//下左
		quad(b1, p, 0.0F, 0.0F, flont, 0.25F, 0.0F,  0.0F, 0.0F, flont, 0.25F, 0.0F,  0.0F, -height, back, 0.25F, 1.0F,  width, 0.0F, back, 0.00F, 1.0F, r,g,bl,1F);
		//下右
		quad(b1, p, 0.0F, 0.0F, flont, 0.25F, 0.0F,  0.0F, 0.0F, flont, 0.25F, 0.0F,  -width, 0.0F, back, 0.50F, 1.0F,  0.0F, -height, back, 0.25F, 1.0F, r,g,bl,1F);
		//上左
		quad(b1, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, height, back, 0.25F, 1.0F,  width, 0.0F, back, 0.00F, 1.0F, r,g,bl,1F);
		//下左
		quad(b1, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  width, 0.0F, back, 0.00F, 1.0F,  0.0F, -height, back, 0.25F, 1.0F, r,g,bl,1F);
		//上右
		quad(b1, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  -width, 0.0F, back, 0.50F, 1.0F,  0.0F, height, back, 0.25F, 1.0F, r,g,bl,1F);
		//下右
		quad(b1, p, 0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, 0.0F, back2, 0.25F, 0.0F,  0.0F, -height, back, 0.25F, 1.0F,  -width, 0.0F, back, 0.50F, 1.0F, r,g,bl,1F);
		//取っ手（両面）
		quad(b1, p,  width2, 0.0F, back2, 0.50F, handleV,  -width2, 0.0F, back2, 0.75F, handleV,  -width2, 0.0F, root, 0.75F, 1.0F,  width2, 0.0F, root, 0.50F, 1.0F, r,g,bl,1F);

		ctx.pose.popPose();
	}

	/** 札弾の描画 */
	public void renderTalismanShot(Ctx ctx, float size, int color, float yaw, float pitch, float slope)
	{
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_OPAQUE);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));
		ctx.pose.mulPose(Axis.ZP.rotationDegrees(slope));

		float u1 = (float)color / 8F;
		float u2 = (float)(color + 1) / 8F;
		float v1 = 0F;
		float v2 = 1F;
		float width = 1.6F;
		double length = 2.0D;
		quad(b, ctx.pose.last(),
				 width, 0.0F,  (float)length, u2, v1,
				-width, 0.0F,  (float)length, u1, v1,
				-width, 0.0F, (float)-length, u1, v2,
				 width, 0.0F, (float)-length, u2, v2,
				1F, 1F, 1F, 1F);
		ctx.pose.popPose();
	}

	/** 風弾の描画 */
	public void renderWindShot(Ctx ctx, float size, int color, int count)
	{
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR);
		ctx.pose.pushPose();
		float size2 = size * 0.5F;
		ctx.pose.scale(size2, size2, size2);

		Random random = new Random();
		float rand1 = (float)random.nextInt(50) / 100F;
		float rand2 = (float)random.nextInt(100) / 100F + 4F;
		int pattern = count % 4;
		float u1 = (float)((pattern % 2) * 32) / 64F;
		float u2 = (float)((pattern % 2) * 32 + 32) / 64F;
		float v1 = (float)((pattern / 2) * 16) / 32F;
		float v2 = (float)((pattern / 2) * 16 + 16) / 32F;
		float f9 = 0.5F;

		for(int i = 0; i < 8; i++)
		{
			ctx.pose.mulPose(Axis.YP.rotationDegrees(45F));
			quad(b, ctx.pose.last(),
					0.0F - rand1, -1.0F - f9, 0.0F, u1, v2,
					0.0F + rand1, -1.0F - f9, 0.0F, u2, v2,
					0.0F + rand2,  2.0F - f9, 2.0F, u2, v1,
					0.0F - rand2,  2.0F - f9, 2.0F, u1, v1,
					colorR[color], colorG[color], colorB[color], 0.7F);
		}
		ctx.pose.popPose();
	}

	/** 米弾の描画 */
	protected void renderRiceShot(Ctx ctx, float size, float yaw, float pitch, double length, float width, double zPos, int zAngleDivNum, int zDivNum, int color, float alpha)
	{
		ctx.pose.pushPose();
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));
		ctx.pose.scale(size, size, size);

		renderRiceShotPart(ctx, ctx.buf(DanmakuRenderTypes.MODE_OPAQUE, false, false), ctx.pose.last(),
				size * 0.8F, yaw, pitch, length * 0.8D, width * 0.8F, zPos + length * 0.1D, zAngleDivNum, zDivNum, 7, 1.0F);
		// 1.7.10: glDepthMask(false)
		renderRiceShotPart(ctx, ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true), ctx.pose.last(),
				size, yaw, pitch, length, width, zPos, zAngleDivNum, zDivNum, color, alpha);
		ctx.pose.popPose();
	}

	/**
	 * @param size,yaw,pitch 1.7.10 でも実質未使用（掃引は length/width/zPos のみで決まる）。
	 *                      互換のためシグネチャに残す。
	 */
	private void renderRiceShotPart(Ctx ctx, VertexConsumer b, PoseStack.Pose pose, float size, float yaw, float pitch, double length, float width, double zPos, int zAngleDivNum, int zDivNum, int color, float alpha)
	{
		float maxWidth = (float)width;//最大の太さをmaxWidthとして保存

		double angleZ = 0F;//Z軸回転変数
		double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;//Z軸回転の変化量

		double zLength = length;//レーザーの長さ（Z方向の長さ、奥行き）
		double zDivLength = zLength / (double)(zDivNum - 1);//Z方向へ分割したときの1分割分の長さ
		double zPosOld = zPos;//ひとつ前の描画位置
		float xPos = 0F;
		float yPos = 0F;
		float xPos2 = 0F;
		float yPos2 = 0F;
		float xPosOld = xPos;
		float yPosOld = yPos;
		float xPos2Old = xPos2;
		float yPos2Old = yPos2;
		float angle = -(float)Math.PI / 2.0F;
		float angleSpan = (float)Math.PI / (float)(zDivNum);
		angle += angleSpan;
		float widthOld = 0.0F;

		for(int j = 0; j < zDivNum; j++)
		{
			zPos += zDivLength;//奥行きを１段階増やす
			width = (float)Math.cos(angle) * maxWidth;
			xPos = width;
			yPos = 0F;
			xPosOld = (float)Math.cos(angleZ) * width;
			yPosOld = (float)Math.sin(angleZ) * width;
			xPos2Old = (float)Math.cos(angleZ) * widthOld;
			yPos2Old = (float)Math.sin(angleZ) * widthOld;
			angleZ = angleSpanZ;

			for(int i = 0; i <= zAngleDivNum; i++)
			{
				xPos = (float)Math.cos(angleZ) * width;
				yPos = (float)Math.sin(angleZ) * width;
				xPos2 = (float)Math.cos(angleZ) * widthOld;
				yPos2 = (float)Math.sin(angleZ) * widthOld;

				quad(b, pose,
						xPosOld, yPosOld, (float)zPos, 0.0F, 0.0F,
						xPos2Old, yPos2Old, (float)zPosOld, 0.0F, 1.0F,
						xPos2, yPos2, (float)zPosOld, 1.0F, 1.0F,
						xPos, yPos, (float)zPos, 1.0F, 0.0F,
						colorR[color], colorG[color], colorB[color], alpha);

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

	/** 楕円弾の描画 */
	protected void renderOvalShot(Ctx ctx, float size, float yaw, float pitch, double length, float width, double zPos, int zAngleDivNum, int zDivNum, int color, float alpha)
	{
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));

		renderOvalShotPart(ctx, ctx.buf(DanmakuRenderTypes.MODE_OPAQUE, false, false), ctx.pose.last(),
				size, yaw, pitch, length * 0.8D, width * 0.8F, zPos + length * 0.1D, zAngleDivNum, zDivNum, 7, 1.0F);
		// 1.7.10: glDepthMask(false)
		renderOvalShotPart(ctx, ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true), ctx.pose.last(),
				size, yaw, pitch, length, width, zPos, zAngleDivNum, zDivNum, color, alpha);
		ctx.pose.popPose();
	}

	/** @param size,yaw,pitch 1.7.10 でも実質未使用。互換のため残す。 */
	private void renderOvalShotPart(Ctx ctx, VertexConsumer b, PoseStack.Pose pose, float size, float yaw, float pitch, double length, float width, double zOffset, int zAngleDivNum, int zDivNum, int color, float alpha)
	{
		float maxWidth = (float)width;//最大の太さをmaxWidthとして保存

		double angleZ = 0F;//Z軸回転変数
		double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;//Z軸回転の変化量

		double zPos;
		double zPosOld = zOffset - length;
		float xPos = 0F;
		float yPos = 0F;
		float xPos2 = 0F;
		float yPos2 = 0F;
		float xPosOld = xPos;
		float yPosOld = yPos;
		float xPos2Old = xPos2;
		float yPos2Old = yPos2;
		float angle = -(float)Math.PI / 2.0F;
		float angleSpan = (float)Math.PI / (float)(zDivNum);
		angle += angleSpan;
		float widthOld = 0.0F;

		for(int j = 0; j < zDivNum; j++)
		{
			zPos = zOffset + Math.sin(angle) * length;
			width = (float)Math.cos(angle) * maxWidth;
			xPos = width;
			yPos = 0F;
			xPosOld = (float)Math.cos(angleZ) * width;
			yPosOld = (float)Math.sin(angleZ) * width;
			xPos2Old = (float)Math.cos(angleZ) * widthOld;
			yPos2Old = (float)Math.sin(angleZ) * widthOld;
			angleZ = angleSpanZ;

			for(int i = 0; i <= zAngleDivNum; i++)
			{
				xPos = (float)Math.cos(angleZ) * width;
				yPos = (float)Math.sin(angleZ) * width;
				xPos2 = (float)Math.cos(angleZ) * widthOld;
				yPos2 = (float)Math.sin(angleZ) * widthOld;

				quad(b, pose,
						xPosOld, yPosOld, (float)zPos, 0.0F, 0.0F,
						xPos2Old, yPos2Old, (float)zPosOld, 0.0F, 1.0F,
						xPos2, yPos2, (float)zPosOld, 1.0F, 1.0F,
						xPos, yPos, (float)zPos, 1.0F, 0.0F,
						colorR[color], colorG[color], colorB[color], alpha);

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

	/** @param size,yaw,pitch 1.7.10 でも実質未使用。互換のため残す。 */
	private void renderHarfOvalShotPart(Ctx ctx, VertexConsumer b, PoseStack.Pose pose, float size, float yaw, float pitch, double length, float width, double zPos, int zAngleDivNum, int zDivNum, int color, float alpha)
	{
		float maxWidth = (float)width;//最大の太さをmaxWidthとして保存

		double angleZ = 0F;//Z軸回転変数
		double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;//Z軸回転の変化量

		double zPosOld = -length;
		float xPos = 0F;
		float yPos = 0F;
		float xPos2 = 0F;
		float yPos2 = 0F;
		float xPosOld = xPos;
		float yPosOld = yPos;
		float xPos2Old = xPos2;
		float yPos2Old = yPos2;
		float angle = -(float)Math.PI / 2.0F;
		float angleSpan = (float)Math.PI / (float)(zDivNum);
		angle += angleSpan;
		float widthOld = 0.0F;

		zDivNum = (zDivNum / 2) + 1;

		for(int j = 0; j < zDivNum; j++)
		{
			zPos = Math.sin(angle) * length;
			width = (float)Math.cos(angle) * maxWidth;
			xPos = width;
			yPos = 0F;
			xPosOld = (float)Math.cos(angleZ) * width;
			yPosOld = (float)Math.sin(angleZ) * width;
			xPos2Old = (float)Math.cos(angleZ) * widthOld;
			yPos2Old = (float)Math.sin(angleZ) * widthOld;
			angleZ = angleSpanZ;

			for(int i = 0; i <= zAngleDivNum; i++)
			{
				xPos = (float)Math.cos(angleZ) * width;
				yPos = (float)Math.sin(angleZ) * width;
				xPos2 = (float)Math.cos(angleZ) * widthOld;
				yPos2 = (float)Math.sin(angleZ) * widthOld;

				quad(b, pose,
						xPosOld, yPosOld, (float)zPos, 0.0F, 0.0F,
						xPos2Old, yPos2Old, (float)zPosOld, 0.0F, 1.0F,
						xPos2, yPos2, (float)zPosOld, 1.0F, 1.0F,
						xPos, yPos, (float)zPos, 1.0F, 0.0F,
						colorR[color], colorG[color], colorB[color], alpha);

				xPosOld = xPos;
				yPosOld = yPos;
				xPos2Old = xPos2;
				yPos2Old = yPos2;
				angleZ += angleSpanZ;
			}

			alpha -= j * 0.05F;

			zPosOld = zPos;//古い奥行きを今の奥行きに更新
			angle += angleSpan;//レーザーの描く半円の角度を更新
			widthOld = width;
		}
	}

	protected void renderAmuletShot(Ctx ctx, float size, int color, int count, float yaw, float pitch, float slope)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));
		ctx.pose.mulPose(Axis.ZP.rotationDegrees(slope));

		float xLength = 1.0F;
		double zLength = 1.0F;
		float uMin = 0.0F;
		float uMax = 0.5F;
		float vMin = 0.0F;
		float vMax = 1.0F;

		ctx.pose.mulPose(Axis.YP.rotationDegrees(180F - (float)count * 23F));
		quad(b, ctx.pose.last(),
				-xLength, 0.0F,  (float)zLength, uMin, vMin,
				 xLength, 0.0F,  (float)zLength, uMax, vMin,
				 xLength, 0.0F, (float)-zLength, uMax, vMax,
				-xLength, 0.0F, (float)-zLength, uMin, vMax,
				1F, 1F, 1F, 1F);

		xLength = 1.2F;
		zLength = 1.2F;
		uMin = 0.5F;
		uMax = 1.0F;
		quad(b, ctx.pose.last(),
				-xLength, 0.0F,  (float)zLength, uMin, vMin,
				 xLength, 0.0F,  (float)zLength, uMax, vMin,
				 xLength, 0.0F, (float)-zLength, uMax, vMax,
				-xLength, 0.0F, (float)-zLength, uMin, vMax,
				colorR[color], colorG[color], colorB[color], 0.6F);
		ctx.pose.popPose();
	}

	/** 幽霊弾（本体）。1.7.10 の switch case 内の手続きをそのまま切り出した。 */
	private void renderPhantom(Ctx ctx, EntityTHShot thShot, int color)
	{
		int count = thShot.getAnimationCount();
		float size;
		// TODO K3-review: 1.7.10 の switch 文内の実装は
		//   "GL11.glBlendFunc(GL_ONE, GL_ONE)" を設定した後、グローバルな getGLRotatefByTherdPersonView
		//   を呼び、その後 renderPhantomPart を 3 回呼んでいた。3 回の呼び出しは
		//   translate/scalef が積み重なっていく（popMatrix しない）点に注意。
		//   ここでは元の行列の積み上げを忠実に再現する。
		ctx.pose.pushPose();
		billboard(ctx);
		double time_r = (double)count / 180.0D * Math.PI;

		size = 1.0F;
		ctx.pose.scale(size, size, size);
		renderPhantomPart(ctx, color, time_r, 0);

		ctx.pose.translate((float)Math.sin(time_r * 5) * 0.2F, 0.1F + (count % 20) * 0.06F, 0F);
		size = 0.9F * ((20F - (float)(count % 20)) / 20F);
		ctx.pose.scale(size, size, size);
		renderPhantomPart(ctx, color, time_r, 0);

		ctx.pose.translate((float)Math.cos(time_r * 5) * 0.2F, 0.1F + (count % 20) * 0.07F, 0F);
		size = 0.8F * ((20F - (float)(count % 20)) / 20F);
		ctx.pose.scale(size, size, size);
		renderPhantomPart(ctx, color, time_r, 0);
		ctx.pose.popPose();
	}

	private void renderPhantomPart(Ctx ctx, int color, double time_r, int damage)
	{
		// 1.7.10: glDepthMask(false)
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE, false, true);// 1.7.10: glBlendFunc(GL_ONE, GL_ONE)
		PoseStack.Pose p = ctx.pose.last();
		int pattern = 0;//time % 2;
		float umin = (float)((pattern % 32) * 32 + 0) / 64F;
		float umax = (float)((pattern % 32) * 32 + 32) / 64F;
		float vmin = 0.0F;
		float vmax = 1.0F;

		float alpha = (40F - (float)damage) / 40F;

		quad(b, p,
				(float)(-0.7F + Math.sin(time_r * 3.0D) * 0.1F), (float)(-0.2F - Math.cos(time_r * 7.0D) * 0.1F), 0.0F, umin, vmax,
				(float)( 0.7F - Math.cos(time_r * 4.0D) * 0.1F), (float)(-0.2F - Math.sin(time_r * 5.0D) * 0.1F), 0.0F, umax, vmax,
				(float)( 0.7F + Math.sin(time_r * 5.0D) * 0.1F), (float)( 1.2F + Math.cos(time_r * 4.0D) * 0.1F), 0.0F, umax, vmin,
				(float)(-0.7F - Math.cos(time_r * 7.0D) * 0.1F), (float)( 1.2F + Math.sin(time_r * 3.0D) * 0.1F), 0.0F, umin, vmin,
				colorR[color] * 0.3F, colorG[color] * 0.3F, colorB[color] * 0.3F, 0.3F * alpha);

		quad(b, p,
				(float)(-0.6F - Math.cos(time_r * 7.0D) * 0.1F), (float)(-0.1F + Math.sin(time_r * 5.0D) * 0.1F), 0.001F, umin, vmax,
				(float)( 0.6F + Math.sin(time_r * 3.0D) * 0.1F), (float)(-0.1F + Math.cos(time_r * 4.0D) * 0.1F), 0.001F, umax, vmax,
				(float)( 0.6F - Math.cos(time_r * 4.0D) * 0.1F), (float)( 1.1F - Math.sin(time_r * 3.0D) * 0.1F), 0.001F, umax, vmin,
				(float)(-0.6F + Math.sin(time_r * 5.0D) * 0.1F), (float)( 1.1F - Math.cos(time_r * 7.0D) * 0.1F), 0.001F, umin, vmin,
				colorR[color] * 0.3F, colorG[color] * 0.3F, colorB[color] * 0.3F, 0.7F * alpha);

		for(int i = 0; i < 3; i++)
		{
			float r = 1.0F, g = 1.0F, bl = 1.0F;
			if(damage > 0)
			{
				r = 1.0F;
				g = 0.0F;
				bl = 0.0F;
			}
			quad(b, p,
					-0.5F,  0.0F, 0.002F, umin, vmax,
					 0.5F,  0.0F, 0.002F, umax, vmax,
					 0.5F,  1.0F, 0.002F, umax, vmin,
					-0.5F,  1.0F, 0.002F, umin, vmin,
					r, g, bl, alpha);
		}
	}

	// ====================================================================
	// 補助
	// ====================================================================

	/** プレイヤーに常に正面を向ける（1.7.10 の getGLRotatefByTherdPersonView）。 */
	private static void billboard(Ctx ctx)
	{
		ctx.pose.mulPose(ctx.entityRenderDispatcher.cameraOrientation());
	}

	/** 4 頂点（QUADS）を 1 枚書き込む。UV は addVertexWithUV の (u,v) をそのまま渡す。 */
	private static void quad(VertexConsumer b, PoseStack.Pose pose,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3,
			float r, float g, float bl, float a)
	{
		// 1.7.10 の getBrightnessForRender() は常に 0xf000f0 を返した → LightTexture.FULL_BRIGHT。
		// POSITION_TEX_COLOR にはライトマップ要素が無いため setLight は no-op だが、
		// 「常に最大輝度」という原仕様をここに明示しておく。
		b.addVertex(pose, x0, y0, z0).setColor(r, g, bl, a).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x1, y1, z1).setColor(r, g, bl, a).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x2, y2, z2).setColor(r, g, bl, a).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x3, y3, z3).setColor(r, g, bl, a).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}

	/** 描画コンテキスト（poseStack / bufferSource / 現在の弾テクスチャ）。 */
	public static final class Ctx
	{
		public final PoseStack pose;
		public final MultiBufferSource buffers;
		public final ResourceLocation texture;
		public final net.minecraft.client.renderer.entity.EntityRenderDispatcher entityRenderDispatcher;

		public Ctx(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, net.minecraft.client.renderer.entity.EntityRenderDispatcher erd)
		{
			this.pose = pose;
			this.buffers = buffers;
			this.texture = texture;
			this.entityRenderDispatcher = erd;
		}

		public VertexConsumer buf(String mode)
		{
			return buf(mode, false, false);
		}

		/**
		 * @param noDepthTest  true で glDepthFunc(GL_ALWAYS) 相当
		 * @param noDepthWrite true で glDepthMask(false) 相当（深度を書かない）
		 */
		public VertexConsumer buf(String mode, boolean noDepthTest, boolean noDepthWrite)
		{
			return this.buffers.getBuffer(DanmakuRenderTypes.get(this.texture, mode, noDepthTest, noDepthWrite));
		}
	}
}
