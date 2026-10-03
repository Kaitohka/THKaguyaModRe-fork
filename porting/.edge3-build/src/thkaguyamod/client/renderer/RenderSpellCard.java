package thkaguyamod.client.renderer;

import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.registry.SpellCardRegistry;
import thkaguyamod.spellcard.EntitySpellCard;
import thkaguyamod.spellcard.THSpellCard;

/**
 * スペルカード（{@link EntitySpellCard}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderSpellCard} の移植。
 *
 * <p>カード面（表裏 2 枚 → 枠 表裏 2 枚）の頂点順・UV は 1.7.10 のまま。
 * 1.7.10 は {@code bindEntityTexture}（カード面）と {@code bindTexture(getFrameTexture)}（枠）を
 * 切り替えていたが、1.21.1 ではそれぞれ別 {@link RenderType} のバッファへ書く。</p>
 *
 * <p>パスの改名: 1.7.10 {@code textures/items/spellCard/&lt;CardName&gt;.png}（CamelCase）は
 * stage3 で {@code textures/item/spell_card/spell_card_&lt;snake_case&gt;.png} に改名済み。
 * ここでは登録名 → stage3 のファイル名への変換（{@link #snake(String)}）を挟む。</p>
 */
public class RenderSpellCard extends EntityRenderer<EntitySpellCard>
{
	public RenderSpellCard(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySpellCard spellCard)
	{
		int number = spellCard.getSpellCardNumber();
		String domain = SpellCardRegistry.getSpellCardModDomain(number);
		return ResourceLocation.fromNamespaceAndPath(domain,
				"textures/item/spell_card/spell_card_" + snake(SpellCardRegistry.getSpellCardName(number)) + ".png");
	}

	protected ResourceLocation getFrameTexture(EntitySpellCard spellCard)
	{
		int number = spellCard.getSpellCardNumber();
		String domain = SpellCardRegistry.getSpellCardModDomain(number);
		return ResourceLocation.fromNamespaceAndPath(domain,
				"textures/item/spell_card/spellcard_lv" + this.getLevel(number) + ".png");
	}

	/**
	 * スペルカードIDからスペルカードの宣言コストのレベルを取得する。
	 *
	 * <p>TODO K3-review: 1.7.10 は {@code Class.newInstance()} でレベルを読んでいた。
	 * 1.21.1 は {@link SpellCardRegistry#getSpellCardFactory(int)} のファクトリから
	 * インスタンスを作って読む。{@code THSpellCard} はフィールドを共有する
	 * シングルトン的な設計（{@code THSpellCard.instance}）なので、ここで作った
	 * インスタンスはレベル読み取りにしか使わない。</p>
	 */
	protected int getLevel(int spellCardID)
	{
		int needLevel = 1;
		java.util.function.Supplier<? extends THSpellCard> factory = SpellCardRegistry.getSpellCardFactory(spellCardID);

		if (factory != null)
		{
			needLevel = factory.get().getNeedLevel();
		}

		return needLevel;
	}

	@Override
	public void render(EntitySpellCard spellCard, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();

		float sizeRate = 1.0F;
		poseStack.scale(sizeRate, sizeRate, sizeRate);

		float rvl = 14F / 128F;
		float rul = 0.0F;
		float rvr = 114F / 128F;
		float rur = 1.00F;
		// 1.7.10 でも宣言されるだけで未使用（そのまま残す）
		float fvl = 0.25F;
		float ful = 0.0F;
		float fvr = 0.50F;
		float fur = 0.75F;

		poseStack.mulPose(Axis.YP.rotationDegrees((float)spellCard.tickCount * 21F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(30F));

		// --- カード面 ---
		VertexConsumer cardBuffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(spellCard)));

		//裏面
		quad(cardBuffer, poseStack,
				 0.2F,  0.3F, 0.0F, rvl, rul,
				-0.2F,  0.3F, 0.0F, rvr, rul,
				-0.2F, -0.3F, 0.0F, rvr, rur,
				 0.2F, -0.3F, 0.0F, rvl, rur);
		//表面
		quad(cardBuffer, poseStack,
				-0.2F,  0.3F, 0.0F, rvl, rul,
				 0.2F,  0.3F, 0.0F, rvr, rul,
				 0.2F, -0.3F, 0.0F, rvr, rur,
				-0.2F, -0.3F, 0.0F, rvl, rur);

		// --- 枠 ---
		VertexConsumer frameBuffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(this.getFrameTexture(spellCard)));

		//裏面
		quad(frameBuffer, poseStack,
				 0.2F,  0.3F,  0.001F, rvl, rul,
				-0.2F,  0.3F,  0.001F, rvr, rul,
				-0.2F, -0.3F,  0.001F, rvr, rur,
				 0.2F, -0.3F,  0.001F, rvl, rur);
		//表面
		quad(frameBuffer, poseStack,
				-0.2F,  0.3F, -0.001F, rvl, rul,
				 0.2F,  0.3F, -0.001F, rvr, rul,
				 0.2F, -0.3F, -0.001F, rvr, rur,
				-0.2F, -0.3F, -0.001F, rvl, rur);

		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(255, 255, 255, 255).setUv(u0, v0)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v2)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(255, 255, 255, 255).setUv(u3, v3)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
	}

	/**
	 * stage3 の生成器（{@code porting/tools/build_phase3_assets.py} の {@code snake()}）と
	 * 同じ規則で登録名 → ファイル名（snake_case）へ変換する。
	 */
	private static String snake(String name)
	{
		String s = name.replace("'", "").replace('-', '_');
		StringBuilder out = new StringBuilder();
		char prev = 0;

		for (int i = 0; i < s.length(); i++)
		{
			char ch = s.charAt(i);

			if (Character.isUpperCase(ch) && prev != 0 && (Character.isLowerCase(prev) || Character.isDigit(prev)))
			{
				out.append('_');
			}

			out.append(ch);
			prev = ch;
		}

		return out.toString().toLowerCase(Locale.ROOT);
	}
}