package thkaguyamod.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Quaternionf;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import thkaguyamod.blockentity.DivineSpiritBlockEntity;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.init.ModBlocks;

/**
 * 小神霊ブロックの描画。1.7.10 {@code thKaguyaMod.client.TileEntityDivineSpiritRenderer} の移植。
 *
 * <p>1.7.10 は {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} + {@code glDepthMask(false)} で
 * 大きさ 0.7 / 0.6 / 0.4 の 3 枚の面片を alpha 0.3 / 0.7 / 1.0 で重ねていた。
 * {@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR}（深度書き込み無し）で再現する。
 * テクスチャは 1.7.10 の {@code textures/shot/MusouFuuin.png} →
 * {@code textures/entity/shot/musoufuuin.png}（既存・バイト一致）。</p>
 *
 * <p>1.7.10 のアニメーション:</p>
 * <ul>
 *   <li>{@code time = worldTime + x*y*z}（ブロックごとに位相をずらす）</li>
 *   <li>Y 方向の浮遊 {@code posY = y + 0.5 + sin(time * 5F / 180F * PI) * 0.16F}</li>
 *   <li>拡大縮小の脈動 {@code scale = 0.8F + sin(time / 180 * PI * 5) * 0.16F}</li>
 *   <li>各面片の四隅を {@code sin/cos(time_r * 3..7)} で最大 ±0.1 揺らす</li>
 * </ul>
 *
 * <p>1.7.10 は {@code glTranslatef(x + 0.5, y + 0.5, z + 0.5)} でブロック中心へ移動していた。
 * 1.21.1 の BER は呼び出し側（{@code LevelRenderer}）が既にブロック原点ぶん translate 済みなので、
 * ここでは中心への相対移動 {@code (0.5, 0.5 + bob, 0.5)} だけを行う。</p>
 *
 * <p>ビルボード: 1.7.10 はブロック中心とプレイヤーの差分から viewYaw / viewPitch を求めていた。
 * 1.21.1 では弟分の {@code RenderDivineSpirit}（エンティティ版）と同じ
 * {@link EntityRenderDispatcher#cameraOrientation()} に置換する。面片は
 * {@link DanmakuRenderTypes#get(ResourceLocation, String, boolean, boolean)} の NO_CULL 版で描くため
 * 表裏は問わない。</p>
 *
 * <p>色: 1.7.10 のメタデータ 0-7 は 1.21.1 では 8 個の別ブロックに分割されている
 * （{@code ModBlocks#divineSpiritBlocks()} の並び = red, blue, green, yellow, purple, aqua, orange,
 * white）。パレットは TESR 側の定義（{@code RenderDivineSpiritBlock} と同一、yellow の G が 224/255）
 * を使う。エンティティ版 {@code RenderDivineSpirit} の yellow は 64/255 で、1.7.10 でも別定義だった。</p>
 *
 * <p>1.7.10 は {@code GL_LIGHTING} を切らずに描いていたが、自己発光の面片なので
 * {@link LightTexture#FULL_BRIGHT} を与える（エンティティ版と同じ扱い）。</p>
 */
public class DivineSpiritBlockEntityRenderer implements BlockEntityRenderer<DivineSpiritBlockEntity>
{
	/** 1.7.10 {@code textures/shot/MusouFuuin.png} → {@code textures/entity/shot/musoufuuin.png}（バイト一致）。 */
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("MusouFuuin");

	/** 1.7.10 TESR のパレット（RenderDivineSpiritBlock と同一値） */
	private static final float[] COLOR_R = { 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F, 255F/255F};
	private static final float[] COLOR_G = {   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 224F/255F, 128F/255F, 255F/255F};
	private static final float[] COLOR_B = {   0F/255F, 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F};

	/**
	 * ブロック → 色 ID の対応表。1.7.10 のメタデータ 0-7 は 1.21.1 では 8 個の別ブロックなので、
	 * {@code ModBlocks#divineSpiritBlocks()} の並びをそのまま索引に使う。
	 */
	private static final Block[] COLOR_BLOCKS = ModBlocks.divineSpiritBlocks();

	private final EntityRenderDispatcher entityRenderDispatcher;

	public DivineSpiritBlockEntityRenderer(BlockEntityRendererProvider.Context context)
	{
		this.entityRenderDispatcher = context.getEntityRenderer();
	}

	@Override
	public void render(DivineSpiritBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
			int packedLight, int packedOverlay)
	{
		Level level = blockEntity.getLevel();
		if (level == null)
		{
			return;
		}

		BlockPos pos = blockEntity.getBlockPos();
		int color = colorIndex(blockEntity.getBlockState().getBlock());

		// 1.7.10: int time = worldTime + x*y*z（ブロックごとに位相をずらす）
		long time = level.getGameTime() + (long)pos.getX() * pos.getY() * pos.getZ();
		double timeRad = time / 180.0D * Math.PI;

		float bob = (float)Math.sin(time * 5F / 180F * Math.PI) * 0.16F;
		float pulse = 0.8F + (float)Math.sin(timeRad * 5.0D) * 0.16F;

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D + bob, 0.5D);

		// 1.7.10 の viewYaw / viewPitch 相当（カメラを常に正面に向ける）
		Quaternionf orientation = this.entityRenderDispatcher == null ? null : this.entityRenderDispatcher.cameraOrientation();
		if (orientation != null)
		{
			poseStack.mulPose(orientation);
		}

		// 1.7.10 は glScalef(s, s, 0) だったが、Z を潰す意味は無いので等方にスケールする
		poseStack.scale(pulse, pulse, pulse);

		// pattern = 0 固定: u ∈ [0, 0.5]（テクスチャ左半分 32/64）
		float umin = 0.0F;
		float umax = 0.5F;
		float vmin = 0.0F;
		float vmax = 1.0F;

		float r = COLOR_R[color];
		float g = COLOR_G[color];
		float b = COLOR_B[color];

		VertexConsumer buffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true));

		quad(buffer, poseStack, r, g, b, 0.3F,
				(float)(-0.7F + Math.sin(timeRad * 3.0D) * 0.1F), (float)(-0.7F - Math.cos(timeRad * 7.0D) * 0.1F), 0.0F, umin, vmax,
				(float)( 0.7F - Math.cos(timeRad * 4.0D) * 0.1F), (float)(-0.7F - Math.sin(timeRad * 5.0D) * 0.1F), 0.0F, umax, vmax,
				(float)( 0.7F + Math.sin(timeRad * 5.0D) * 0.1F), (float)( 0.7F + Math.cos(timeRad * 4.0D) * 0.1F), 0.0F, umax, vmin,
				(float)(-0.7F - Math.cos(timeRad * 7.0D) * 0.1F), (float)( 0.7F + Math.sin(timeRad * 3.0D) * 0.1F), 0.0F, umin, vmin);

		quad(buffer, poseStack, r, g, b, 0.7F,
				(float)(-0.6F - Math.cos(timeRad * 7.0D) * 0.1F), (float)(-0.6F + Math.sin(timeRad * 5.0D) * 0.1F), 0.001F, umin, vmax,
				(float)( 0.6F + Math.sin(timeRad * 3.0D) * 0.1F), (float)(-0.6F + Math.cos(timeRad * 4.0D) * 0.1F), 0.001F, umax, vmax,
				(float)( 0.6F - Math.cos(timeRad * 4.0D) * 0.1F), (float)( 0.6F - Math.sin(timeRad * 3.0D) * 0.1F), 0.001F, umax, vmin,
				(float)(-0.6F + Math.sin(timeRad * 5.0D) * 0.1F), (float)( 0.6F - Math.cos(timeRad * 7.0D) * 0.1F), 0.001F, umin, vmin);

		quad(buffer, poseStack, r, g, b, 1.0F,
				-0.4F, -0.4F, 0.002F, umin, vmax,
				 0.4F, -0.4F, 0.002F, umax, vmax,
				 0.4F,  0.4F, 0.002F, umax, vmin,
				-0.4F,  0.4F, 0.002F, umin, vmin);

		poseStack.popPose();
	}

	/** 色 ID（{@code ModBlocks#divineSpiritBlocks()} の並び）を返す。未知のブロックは赤扱い。 */
	private static int colorIndex(Block block)
	{
		Block[] blocks = COLOR_BLOCKS;
		for (int i = 0; i < blocks.length; i++)
		{
			if (blocks[i] == block)
			{
				return i;
			}
		}
		return 0;
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack, float r, float g, float b, float a,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		int color = FastColor.ARGB32.color((int)(a * 255F), (int)(r * 255F), (int)(g * 255F), (int)(b * 255F));
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(color).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(color).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(color).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(color).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}
