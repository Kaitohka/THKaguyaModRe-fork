package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import thkaguyamod.client.model.ModelHisou;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.item.EntityHisou;

/**
 * 緋想の剣（{@link EntityHisou}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderHisou} の移植。
 *
 * <p>1.7.10 は {@code glDisable(GL_LIGHTING)} + {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)} で
 * モデルを発光させていた。ここでは {@link DanmakuRenderTypes#MODE_ADD_SRC_ALPHA}
 * （＝同じブレンド関数）のバッファへ {@link LightTexture#FULL_BRIGHT} で書き込む。</p>
 *
 * <p>TODO K3-review: 貼り先テクスチャは未コピー。1.7.10 の
 * {@code textures/HisouTexture.png} を {@code textures/entity/shot/hisoutexture.png} に置く想定で
 * パスを書いてある（コピー時にパスを確定すること）。</p>
 */
public class RenderHisou extends EntityRenderer<EntityHisou>
{
	// 1.7.10: textures/HisouTexture.png → textures/entity/shot/hisoutexture.png（全小文字）
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("HisouTexture");

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "hisou"), "main");

	private final ModelHisou model;

	public RenderHisou(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.5F;
		this.model = new ModelHisou(context.bakeLayer(LAYER_LOCATION));
	}

	@Override
	public ResourceLocation getTextureLocation(EntityHisou entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityHisou entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		// 1.7.10 の GL11.glTranslatef(x, y, z) は EntityRenderDispatcher が poseStack へ適用済み
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
		poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getAngle()));
		poseStack.scale(0.3F, 0.7F, 0.3F);

		// 1.7.10: GL11.glColor4f(1.0F, 1.0F, 1.0F, num * 0.125F)
		int alpha = Mth.clamp((int)((float)entity.getNum() * 0.125F * 255F), 0, 255);
		VertexConsumer buffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_SRC_ALPHA));
		this.model.renderToBuffer(poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
				FastColor.ARGB32.color(alpha, 255, 255, 255));
		poseStack.popPose();
	}
}
