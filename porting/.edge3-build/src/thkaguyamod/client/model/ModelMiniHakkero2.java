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

import thkaguyamod.entity.item.EntityMiniHakkero;

/**
 * ミニ八卦炉（展開時、8 枚の側面）（{@code EntityMiniHakkero}）のモデル。1.7.10
 * {@code thKaguyaMod.client.model.ModelMiniHakkero2} の移植。8 枚の板は
 * {@code rotateAngleZ = i * PI/4} だけが異なるため、同じボックス定義を 8 つの子パーツに複製する。
 *
 * <p>1.7.10 は {@code addBox(-4, -10, -0.001F, 8, 2, 6)} と Z 原点を 0.001 手前にずらしていた。
 * {@link CubeListBuilder#addBox} は float を受けるのでこの値をそのまま保持している。</p>
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:mini_hakkero2}</p>
 */
public class ModelMiniHakkero2 extends EntityModel<EntityMiniHakkero>
{
	/** 側面 8 枚（miniHakkero[0..7]、Z 回転 PI/4 刻み） */
	public final ModelPart[] sides = new ModelPart[8];

	public ModelMiniHakkero2(ModelPart root)
	{
		for (int i = 0; i < 8; i++)
		{
			this.sides[i] = root.getChild("side" + i);
		}
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		float pi8 = (float)Math.PI / 4.0F;
		for (int i = 0; i < 8; i++)
		{
			root.addOrReplaceChild("side" + i,
					CubeListBuilder.create().texOffs(32, 0)
							.addBox(-4.0F, -10.0F, -0.001F, 8.0F, 2.0F, 6.0F),
					PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, pi8 * i));
		}

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(EntityMiniHakkero entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 の setRotationAngles は空実装
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		for (int i = 0; i < 8; i++)
		{
			this.sides[i].render(poseStack, buffer, packedLight, packedOverlay, color);
		}
	}
}