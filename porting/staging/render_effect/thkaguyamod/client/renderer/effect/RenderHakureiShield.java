package thkaguyamod.client.renderer.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.client.renderer.shot.RenderTHShot;
import thkaguyamod.entity.effect.EntityHakureiShield;

/**
 * 霊夢の結界（{@link EntityHakureiShield}）のレンダラ。
 * 1.7.10 の {@code thKaguyaMod.client.render.effect.RenderHakureiShield} の忠実移植。
 *
 * <p><b>1.7.10 では ClientProxy の登録がコメントアウトされており、このレンダラは
 * 実際には使われていなかった</b>（{@code //RenderingRegistry.registerEntityRenderingHandler(
 * EntityHakureiShield.class, new RenderHakureiShield());}）。移植の指示により
 * 形状（3.0×3.0 の 1 枚板、yaw のみ回転、色 0.9/0.6/0.1、alpha 0.8、
 * {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)}）をそのまま再現して備える。</p>
 *
 * <p>深度: 1.7.10 は {@code glDepthMask} を変更していないため深度書き込みは有効のまま
 * （{@link RenderTHShot.Ctx#buf(String)} を使用）。</p>
 *
 * <p>TODO K3-review: テクスチャについて。1.7.10 の
 * {@code field_110782_f = new ResourceLocation("thkaguyamod", "textures/Amulet.png")} は
 * アセットに存在せず（jar 内にも無い）、さらに {@code bindEntityTexture()} が参照する
 * {@code getEntityTexture(Entity)} が {@code null} を返していたため、原実装では
 * テクスチャが未バインドのまま直前のテクスチャで描かれていた（実質不定）。
 * 移植では「本来意図されていた Amulet.png」の代替として、同じ 64x32 でこの板が使う
 * u=0.0-0.5 の左半分が不透明な {@code textures/entity/shot/homingamulet.png} を
 * 暫定で参照している。専用の {@code amulet.png} を用意できたら差し替えること。</p>
 */
public class RenderHakureiShield extends EntityRenderer<EntityHakureiShield>
{
	//霊夢の結界の描画（1.7.10: "textures/Amulet.png" — アセット欠落のため暫定で護符弾テクスチャ）
	private static final ResourceLocation hakureiShieldTexture = DanmakuRenderTypes.texture("HomingAmulet");

	/** コンストラクタ */
	public RenderHakureiShield(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityHakureiShield entity)
	{
		return hakureiShieldTexture;
	}

	@Override
	public void render(EntityHakureiShield entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		ResourceLocation texture = this.getTextureLocation(entity);
		poseStack.pushPose();
		RenderTHShot.Ctx ctx = new RenderTHShot.Ctx(poseStack, bufferSource, texture, this.entityRenderDispatcher);
		this.doRenderHakureiShield(ctx, entity, entityYaw, entityYaw);
		poseStack.popPose();
	}

	/**
	 * @param yaw,pitch 1.7.10 の doRender から渡される f/f1。f1（pitch）は原実装でも未使用。
	 */
	public void doRenderHakureiShield(RenderTHShot.Ctx ctx, EntityHakureiShield entityHakureiShield, float yaw, float pitch)
	{
		// 1.7.10: GL11.glScalef(1.0F, 1.0F, 1.0F)（恒等変換だが原実装どおり残す）
		ctx.pose.scale(1.0F, 1.0F, 1.0F);

		float minU = 0.0F;
		float maxU = 0.5F;
		float minV = 0.0F;
		float maxV = 1.0F;
		float width = 1.5F;
		float height = 1.5F;

		// 1.7.10: GL11.glRotatef(f, 0.0F, 1.0F, 0.0F) → glRotatef(0F, 1.0F, 0.0F, 0.0F)（X 回転は恒等）
		// TODO K3-review: 1.7.10 の f は RenderManager が渡す yaw だが、1.21.1 の entityYaw は
		//   エンティティの向き（setYRot の値）なので意味が異なる。この Entity は
		//   setRotation(-user.rotationYaw, 0F) しているため、二重に yaw が掛かる恐れがある。
		//   統合時に entity.getYRot() を使うか、この回転を削るかを検証すること。
		ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
		ctx.pose.mulPose(Axis.XP.rotationDegrees(0.0F));

		// 1.7.10: glEnable(GL_BLEND) + glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)
		VertexConsumer vertex = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR);
		quad(vertex, ctx.pose.last(),
				-width, -height, 0.0F, minU, maxV,
				 width, -height, 0.0F, maxU, maxV,
				 width,  height, 0.0F, maxU, minV,
				-width,  height, 0.0F, minU, minV,
				0.9F, 0.6F, 0.1F, 0.8F);
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