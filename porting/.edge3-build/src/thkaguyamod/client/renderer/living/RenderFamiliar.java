package thkaguyamod.client.renderer.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.living.EntityFamiliar;

/**
 * 使い魔（{@link EntityFamiliar}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderFamiliar} の移植。
 *
 * <p>1.7.10 は純粋な {@code Render}（モデル無し）で、手続き的な面片 1 枚だけを描いていた。
 * よって 1.21.1 では {@link EntityRenderer} を直接継承する。</p>
 *
 * <p>写像:</p>
 * <ul>
 *   <li>{@code glTranslatef(x, y, z)} → EntityRenderDispatcher が poseStack に適用済み</li>
 *   <li>{@code glRotatef(180 - playerViewY, Y)} + {@code glRotatef(-playerViewX, X)} →
 *       {@code poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation())}</li>
 *   <li>{@code glRotatef(ticksExisted * 5F, Z)} → {@code poseStack.mulPose(Axis.ZP.rotationDegrees(...))}</li>
 *   <li>{@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} → {@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR}</li>
 *   <li>{@code glDepthMask(false)} → {@code noDepthWrite = true}</li>
 * </ul>
 *
 * <p>1.7.10 の {@code MathHelper.sin/cos} は度数法なので、1.21.1 の {@link Mth#sin}/{@link Mth#cos}
 * （同じく度数法）をそのまま使う。{@code glColor4f} 相当の色は
 * {@code (sin(color), cos(color), -sin(color) * 2F, 0.60F)}。第 3 成分が負になるのが原作どおりで、
 * 1.21.1 でも {@code setColor(float...)} をそのまま渡す（頂点属性は float 正規化で 0..1 に
 * クランプされるため、負値は 0 として扱われる。1.7.10 の色クランプと等価）。</p>
 */
public class RenderFamiliar extends EntityRenderer<EntityFamiliar>
{
	/** 1.7.10: textures/mob/Familiar.png */
	private static final ResourceLocation FAMILIAR_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/familiar.png");

	public RenderFamiliar(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityFamiliar familiar)
	{
		return FAMILIAR_TEXTURE;
	}

	@Override
	public void render(EntityFamiliar entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight)
	{
		this.renderFamiliar(entity, poseStack, bufferSource);
	}

	/** 1.7.10 {@code renderFamiliar}。 */
	public void renderFamiliar(EntityFamiliar familiar, PoseStack poseStack, MultiBufferSource bufferSource)
	{
		// 形状IDがマイナスなら描画しない
		if(familiar.getForm() < 0)
		{
			return;
		}

		VertexConsumer buffer = bufferSource.getBuffer(DanmakuRenderTypes.get(
				FAMILIAR_TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true));

		poseStack.pushPose();

		// 1.7.10: float size = 3.0F + (float)Math.sin(ticksExisted * 10 / 180.0D * Math.PI) * 0.6F;
		float size = 3.0F + (float)Math.sin(familiar.tickCount * 10 / 180.0D * Math.PI) * 0.6F;
		poseStack.scale(size, size, size);

		// カメラ正対 + 自転
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		poseStack.mulPose(Axis.ZP.rotationDegrees(familiar.tickCount * 5F));

		float umin = 0.0F;
		float umax = 1.0F;
		float vmin = 0.0F;
		float vmax = 1.0F;

		// 1.7.10: int color = familiar.getForm() % 8;
		//          setColorRGBA_F(MathHelper.sin(color), MathHelper.cos(color), -MathHelper.sin(color) * 2F, 0.60F)
		int color = familiar.getForm() % 8;
		float r = Mth.sin((float)color);
		float g = Mth.cos((float)color);
		float b = -Mth.sin((float)color) * 2F;

		PoseStack.Pose pose = poseStack.last();
		buffer.addVertex(pose, -0.6F, -0.6F, 0.0F).setColor(r, g, b, 0.60F).setUv(umin, vmax);
		buffer.addVertex(pose,  0.6F, -0.6F, 0.0F).setColor(r, g, b, 0.60F).setUv(umax, vmax);
		buffer.addVertex(pose,  0.6F,  0.6F, 0.0F).setColor(r, g, b, 0.60F).setUv(umax, vmin);
		buffer.addVertex(pose, -0.6F,  0.6F, 0.0F).setColor(r, g, b, 0.60F).setUv(umin, vmin);

		poseStack.popPose();
	}
}