package thkaguyamod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import thkaguyamod.entity.item.EntityYuukaParasol;

/**
 * 幽香の日傘（取手部・骨部）のモデル。1.7.10
 * {@code thKaguyaMod.client.model.ModelYuukaParasol} の移植。ボックス座標・サイズ・
 * テクスチャ UV は一対一（テクスチャ 64x32）。
 *
 * <p>柄（{@code stick[i]}）は {@code addBox(-2, i*28, -2, 4, 28, 4)} と回転点指定なしで、
 * 4 本が 28 ずつ積み上がる。取手（{@code handle[2]}）の回転点は 1.7.10 では
 * {@code MathHelper.cos/sin(2π/3)*10} で計算していたので、その式を {@link Mth} でそのまま残す
 * （1.7.10 の {@code MathHelper} と {@link Mth} は同一実装）。</p>
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:yuuka_parasol}</p>
 */
public class ModelYuukaParasol extends EntityModel<EntityYuukaParasol>
{
	/** 骨 4 本（stick[0..3]、Y へ 28 ずつ積み上げ） */
	public final ModelPart[] stick = new ModelPart[4];
	/** 取手 3 本（handle[0..2]） */
	public final ModelPart[] handle = new ModelPart[3];

	public ModelYuukaParasol(ModelPart root)
	{
		for (int i = 0; i < 4; i++)
		{
			this.stick[i] = root.getChild("stick" + i);
		}
		for (int i = 0; i < 3; i++)
		{
			this.handle[i] = root.getChild("handle" + i);
		}
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		for (int i = 0; i < 4; i++)
		{
			root.addOrReplaceChild("stick" + i,
					CubeListBuilder.create().texOffs(0, 0)
							.addBox(-2.0F, i * 28.0F, -2.0F, 4.0F, 28.0F, 4.0F),
					PartPose.ZERO);
		}

		// handle[0]: 持つ場所 / addBox(-3, -10, -3, 6, 10, 6) / 回転点指定なし
		root.addOrReplaceChild("handle0",
				CubeListBuilder.create().texOffs(32, 16)
						.addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F),
				PartPose.ZERO);

		// handle[1]: rotationPoint(-3, -10, -3) / rotateAngleX = 2π/3
		float handleAngle2 = (float)Math.PI / 3F * 2F;
		root.addOrReplaceChild("handle1",
				CubeListBuilder.create().texOffs(32, 16)
						.addBox(0.0F, 0.0F, -6.0F, 6.0F, 10.0F, 6.0F),
				PartPose.offsetAndRotation(-3.0F, -10.0F, -3.0F, handleAngle2, 0.0F, 0.0F));

		// handle[2]: rotationPoint(-3, -10 + cos(2π/3)*10, -3 + sin(2π/3)*10) / rotateAngleX = π/3
		root.addOrReplaceChild("handle2",
				CubeListBuilder.create().texOffs(32, 16)
						.addBox(0.0F, 0.0F, -6.0F, 6.0F, 10.0F, 6.0F),
				PartPose.offsetAndRotation(-3.0F,
						-10.0F + Mth.cos(handleAngle2) * 10F,
						-3.0F + Mth.sin(handleAngle2) * 10F,
						(float)Math.PI / 3F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(EntityYuukaParasol entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 はアニメーションなし（柄の面はレンダラが Tessellator で描いていた）
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		for (int i = 0; i < 4; i++)
		{
			this.stick[i].render(poseStack, buffer, packedLight, packedOverlay, color);
		}
		for (int i = 0; i < 3; i++)
		{
			this.handle[i].render(poseStack, buffer, packedLight, packedOverlay, color);
		}
	}
}