package thkaguyamod.client.renderer.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.living.EntityDanmakuMob;
import thkaguyamod.registry.SpellCardRegistry;

/**
 * BOSS 用の共通レンダラ。1.7.10 {@code thKaguyaMod.client.render.living.RenderTHBoss} の移植。
 *
 * <p>1.7.10 は {@code RenderLiving.doRender} の後に {@code renderTHBossStatus} を呼び、
 * Tessellator + {@code GL11.glRotatef} + {@code FontRenderer.drawStringWithShadow} で
 * 頭上に HP ゲージとスペルカード名を描いていた。1.21.1 では
 * {@link LivingEntityRenderer#render} の後に {@link PoseStack} +
 * {@link MultiBufferSource} で同じ内容を描く（{@code EntityRenderer.renderNameTag} と同じ路線）。</p>
 *
 * <p><b>写像（1.7.10 → 1.21.1）</b></p>
 * <ul>
 *   <li>{@code glTranslatef(x, y + height + 1.5F, z)} → {@code poseStack.translate(0, bbHeight + 1.5, 0)}
 *       （1.21.1 では EntityRenderDispatcher が既にエンティティ位置まで poseStack を移動済み）</li>
 *   <li>{@code glRotatef(-playerViewY, Y)} + {@code glRotatef(playerViewX, X)} →
 *       {@code poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation())}
 *       （バニラのネームタグと同じカメラ正対ビルボード）</li>
 *   <li>{@code glScalef(size, size, size)} → {@code poseStack.scale(size, size, size)}（同じく距離補正）</li>
 *   <li>テキストは {@code FontRenderer.drawStringWithShadow} →
 *       {@code Font.drawInBatch(String, x, y, argb, true, Matrix4f, MultiBufferSource, DisplayMode, 0, light)}。
 *       1.21.1 のフォントは Y 下向きなので、バニラのネームタグと同じく Y を負にスケールする
 *       （1.7.10 の {@code glRotatef(180F, 0,0,1)} に相当する 180° 回転を、X 反転（cameraOrientation の
 *       Y180）+ Y 反転（負スケール）で再現している）。</li>
 * </ul>
 *
 * <p><b>色・アルファ</b>: 1.7.10 は {@code tessellator.setColorRGBA_F(1,1,1,alpha)} を
 * <i>テキスト描画の直前だけ</i>呼んでいた（ゲージ面の setColorRGBA_F はコメントアウト）。
 * つまりフェード（{@code alpha}）が掛かるのは文字だけで、ゲージ面は不透明のまま
 * {@code |span| <= 20} の範囲でポップ表示される。ここでも同じにしている。</p>
 *
 * <p><b>面の Z オフセット</b>: 1.7.10 は背景を z=+0.001、HP ゲージを z=0.0 に置いていた
 * （深度 LEQUAL・深度書き込みありなので、+Z がカメラから遠い側＝背景が奥）。
 * {@code cameraOrientation()} のビルボードでは +Z がカメラ側を向くため、符号を反転して
 * 背景 z=-0.001 / HP ゲージ z=0.0 としている（描画順・前後関係は 1.7.10 と同じ）。</p>
 *
 * <p>TODO K3-review: 1.7.10 の固定機能モデルビュー行列そのものは再現できていない。
 * ゲージ面の U 反転（HP が左から伸びる向き）と、2 行のテキストの水平位置（ハードコードの
 * {@code -50}）は、1.7.10 の {@code playerViewY/X} ビルボードの handedness に依存していた。
 * 統合時に実機で「HP ゲージの伸びる向き」「文字の左右位置」を確認し、必要なら
 * {@code cameraOrientation()} の代わりに {@code Axis.YP.rotationDegrees(-cameraYaw)} +
 * {@code Axis.XP.rotationDegrees(cameraPitch)} へ切り替えること。</p>
 *
 * <p>TODO K3-review: スペルカード名は 1.7.10 の {@code "item.thSpellCard." + cardNo + ".name"}
 * ではなく、1.21.1 の {@code item.thkaguyamod.spell_card_<snake>} を
 * {@link SpellCardRegistry} の登録名から組み立てている（{@link #spellCardTranslationKey(int)}）。</p>
 */
public abstract class RenderTHBoss<T extends EntityDanmakuMob, M extends EntityModel<T>>
		extends LivingEntityRenderer<T, M>
{
	/** 1.7.10: {@code textures/mob/Status.png} → 1.21.1 は {@code textures/entity/living/} 配下・全小文字 */
	protected static final ResourceLocation STATUS_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/status.png");

	protected RenderTHBoss(EntityRendererProvider.Context context, M model, float shadowRadius)
	{
		super(context, model, shadowRadius);
	}

	@Override
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight)
	{
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		this.renderTHBossStatus(entity, partialTick, poseStack, bufferSource, packedLight);
	}

	/**
	 * 頭上の HP ゲージとスペルカード名を描く。1.7.10 の {@code renderTHBossStatus} と同じ数式
	 * （視野角フェード alpha、距離スケール size）を使う。
	 */
	public void renderTHBossStatus(T danmakuMob, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight)
	{
		// 1.7.10: if(danmakuMob.getDanmakuPattern() == danmakuMob.NOT_ATTACK) return;
		if(danmakuMob.getDanmakuPattern() == EntityDanmakuMob.NOT_ATTACK)
		{
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();

		// 1.7.10: renderManager.playerViewY / playerViewX（カメラの yaw / pitch）
		float viewY = camera.getYRot() % 360F;
		if(viewY > 180F)
		{
			viewY -= 360F;
		}
		else if(viewY <= -180F)
		{
			viewY += 360F;
		}

		// 1.7.10: look = THShotLib.getVecFromAngle(viewY, playerViewX)
		//         toEntity = Vec3(x, y, z)（カメラから見たエンティティへのベクトル）
		Vec3 look = THShotLib.getVecFromAngle(viewY, camera.getXRot());
		Vec3 toEntity = danmakuMob.getPosition(partialTick).subtract(camera.getPosition());
		float span = THShotLib.getVectorAndVectorAngle(look, toEntity);
		float alpha = 1F - (Math.abs(span) - 20F) / 30F;

		// 1.7.10: renderManager.getDistanceToCamera(posX, posY, posZ)（実距離）
		double distance = Math.sqrt(this.entityRenderDispatcher.distanceToSqr(danmakuMob));
		float size = 1.0F + (float)distance / 64F;
		if(size > 5.0F)
		{
			size = 5.0F;
		}

		if(Math.abs(span) > 20F)
		{
			return;
		}

		poseStack.pushPose();
		// 1.7.10: glTranslatef(x, y + danmakuMob.height + 1.5F, z)
		poseStack.translate(0.0D, (double)danmakuMob.getBbHeight() + 1.5D, 0.0D);
		// 1.7.10: glRotatef(-playerViewY, Y) → glRotatef(playerViewX, X)（カメラ正対）
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		poseStack.scale(size, size, size);

		int cardNo = danmakuMob.getUsingSpellCardNo();

		float hp = danmakuMob.getHealth() / danmakuMob.getMaxHealth();
		float hp2 = danmakuMob.getHealth() / danmakuMob.getMaxHealth() * 2.0F - 1.0F;

		// 1.7.10: this.bindTexture(statusTexture) + Tessellator（ブレンド無効・深度 LEQUAL・深度書き込みあり）
		VertexConsumer bar = bufferSource.getBuffer(
				DanmakuRenderTypes.get(STATUS_TEXTURE, DanmakuRenderTypes.MODE_OPAQUE));
		PoseStack.Pose pose = poseStack.last();

		// ゲージの下地（1.7.10: z=+0.001 → 符号反転して -0.001）
		quad(bar, pose,
				-1.02F, 0.07F, -0.001F, 0.0F,  3F / 16F,
				 1.02F, 0.07F, -0.001F, 1.0F,  3F / 16F,
				 1.02F, -0.02F, -0.001F, 1.0F, 4F / 16F,
				-1.02F, -0.02F, -0.001F, 0.0F, 4F / 16F);

		// スペルカードを使用中なら（1.7.10: v = 8/16..11/16）
		if(cardNo >= 0)
		{
			quad(bar, pose,
					-hp2, 0.05F, 0.0F, hp,   8F / 16F,
					 1.0F, 0.05F, 0.0F, 0.0F, 8F / 16F,
					 1.0F, 0.00F, 0.0F, 0.0F, 11F / 16F,
					-hp2, 0.00F, 0.0F, hp,  11F / 16F);
		}
		// スペルカードを使用していないなら（1.7.10: v = 0/16..3/16）
		else
		{
			quad(bar, pose,
					-hp2, 0.05F, 0.0F, hp,   0F / 16F,
					 1.0F, 0.05F, 0.0F, 0.0F, 0F / 16F,
					 1.0F, 0.00F, 0.0F, 0.0F, 3F / 16F,
					-hp2, 0.00F, 0.0F, hp,   3F / 16F);
		}

		// 1.7.10: glRotatef(180F, 0,0,1) + glScalef(0.02F, 0.02F, 0.02F)
		//         → カメラ正対ビルボード + フォントの Y 下向きに合わせた負の Y スケール
		poseStack.scale(0.02F, -0.02F, 0.02F);
		Matrix4f matrix = poseStack.last().pose();

		int textAlpha = Mth.clamp((int)(alpha * 255F), 0, 255);
		Font font = minecraft.font;

		// 1.7.10: font.drawStringWithShadow(danmakuMob.getDanmakuMobName(), -50, 0, 0x00FF88)
		font.drawInBatch(danmakuMob.getDanmakuMobName(), -50.0F, 0.0F,
				(textAlpha << 24) | 0x00FF88, true, matrix, bufferSource,
				Font.DisplayMode.NORMAL, 0, packedLight);

		// スペルカードを使用中なら、スペルカード名を表示する
		if(cardNo >= 0)
		{
			// 1.7.10: font.drawStringWithShadow(StatCollector.translateToLocal("item.thSpellCard." + cardNo + ".name"), -50, -12, 0xFFFFFF)
			//         → 1.21.1 の符卡キーは spell_card_<snake>。未訳ならキーそのものが表示される（原作の
			//            translateToLocal も未訳時はキーを返すので挙動は同じ）
			font.drawInBatch(Component.translatable(spellCardTranslationKey(cardNo)).getString(),
					-50.0F, -12.0F, (textAlpha << 24) | 0xFFFFFF, true, matrix, bufferSource,
					Font.DisplayMode.NORMAL, 0, packedLight);
		}

		poseStack.popPose();
	}

	/**
	 * 1.7.10 の {@code "item.thSpellCard." + cardNo + ".name"} に対応する 1.21.1 の翻訳キー。
	 *
	 * <p>1.21.1 の符卡は {@code spell_card_<clean snake_case>} の個別アイテムなので、
	 * {@link SpellCardRegistry} の登録名（例 {@code "KachoufuugetuShoufuurougetu"}）を
	 * フェーズ 3 の生成器（{@code porting/tools/build_phase3_assets.py} の {@code snake()}）と
	 * 同じ規則で snake_case に落としてキーを組み立てる。</p>
	 */
	protected static String spellCardTranslationKey(int cardNo)
	{
		String name = SpellCardRegistry.getSpellCardName(cardNo);
		if(name == null)
		{
			// 未登録 ID。1.7.10 のキー体系にフォールバックする（表示は翻訳キーそのものになる）
			return "item.thSpellCard." + cardNo + ".name";
		}

		StringBuilder key = new StringBuilder("item.thkaguyamod.spell_card_");
		String raw = name.replace("'", "").replace('-', '_');
		char prev = 0;
		for(int i = 0; i < raw.length(); i++)
		{
			char c = raw.charAt(i);
			if(Character.isUpperCase(c) && prev != 0
					&& (Character.isLowerCase(prev) || Character.isDigit(prev)))
			{
				key.append('_');
			}
			key.append(Character.toLowerCase(c));
			prev = c;
		}
		return key.toString();
	}

	/** 4 頂点（QUADS）を 1 枚書き込む。{@code DanmakuRenderTypes} の POSITION_TEX_COLOR 用。 */
	protected static void quad(VertexConsumer buffer, PoseStack.Pose pose,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		// 1.7.10 の setColorRGBA_F はコメントアウトされていた（直前の色がそのまま使われる）。
		// 既定色＝白・不透明で描く。
		buffer.addVertex(pose, x0, y0, z0).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v0);
		buffer.addVertex(pose, x1, y1, z1).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v1);
		buffer.addVertex(pose, x2, y2, z2).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u2, v2);
		buffer.addVertex(pose, x3, y3, z3).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u3, v3);
	}

	/**
	 * 1.7.10 の {@code getEntityTexture(EntityDanmakuMob)} は statusTexture を返していた
	 * （各サブクラスが上書きする）。1.21.1 でも同じ既定値を返す。
	 */
	@Override
	public ResourceLocation getTextureLocation(T entity)
	{
		return STATUS_TEXTURE;
	}
}
