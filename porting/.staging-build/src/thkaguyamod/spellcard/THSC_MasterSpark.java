package thkaguyamod.spellcard;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_STAR;
import static thkaguyamod.danmaku.THShotLib.gravity;

/** 恋符「マスタースパーク」 */
public class THSC_MasterSpark extends THSpellCard
{
	private Vec3 tgVec;

	public THSC_MasterSpark()
	{
		setSpellCardName("");
		setIconName("MasterSpark");
		this.setNeedLevel(5);
		this.setRemoveTime(99);
		this.setEndTime(109);
		this.setOriginalUserName(MARISA);
	}

	@Override
	public void spellcard_main()
	{
		if(time == 1)
		{
			tgVec = user.getLookAngle();
			// TODO K3-review: 依存実体 EntityMiniHakkero（ミニ八卦炉）待移植。以下3行は移植後に有効化する。
			// EntityMiniHakkero miniHakkero = new EntityMiniHakkero(level(), user, target);
			// if(!level().isClientSide) { level().addFreshEntity(miniHakkero); }
		}
		if(time >= 30 && time < 99)
		{
			//EntityTHShot[] entityTHShot = new EntityTHShot[7];
			double xVector, yVector, zVector, xVectorG, yVectorG, zVectorG, gRate, angleXZ = 0, angleY = 0, X1, Z1, X2, Z2;
			Vec3 lookAt = tgVec;
			// 1.7.10 の Vec3 は可変だったため再代入していた。1.21.1 の Vec3 は不変なので新しい Vec3 を作る。
			lookAt = new Vec3(
					-Mth.sin(card.getYRot() / 180F * 3.141593F) * Mth.cos((card.getXRot() + 90F) / 180F * 3.141593F),
					-Mth.sin((card.getXRot() + 90F) / 180F * 3.141593F),
					 Mth.cos(card.getYRot() / 180F * 3.141593F) * Mth.cos((card.getXRot() + 90F) / 180F * 3.141593F));
			lookAt = lookAt.yRot((float)Math.PI * 2);
			float angle = (float)time * 6F;
			float angleSpan = 360F / 7F;
			//gRate = (double)(ticksExisted % 9) / 9D + 0.1D;
			gRate = 0.034 + 0.03D * Math.sin(angle / 180F * 3.141593F);

			xVectorG = -Mth.sin(card.getYRot() / 180F * 3.141593F) * Mth.cos(card.getXRot() / 180F * 3.141593F) * gRate;
			yVectorG = -Mth.sin(card.getXRot() / 180F * 3.141593F) * gRate;
			zVectorG =  Mth.cos(card.getYRot() / 180F * 3.141593F) * Mth.cos(card.getXRot() / 180F * 3.141593F) * gRate;

			for(int i = 0; i < 7; i++)
			{
				angleXZ = angle;// 横の角度　0=正面　+1ごとに左にずれていき360で正面に戻る
				angleY = 0;// 縦の角度　0=正面　+1ごとに上にずれていき360で正面に戻る

				X1 = Math.sin(angleXZ / 180.0F * Math.PI) * Math.cos(card.getYRot() / 180.0F * Math.PI);
				Z1 = Math.sin(angleXZ / 180.0F * Math.PI) * Math.sin(card.getYRot() / 180.0F * Math.PI);
				X2 = Math.cos(angleXZ / 180.0F * Math.PI) * Math.sin(angleY / 180.0F * Math.PI) * Math.sin((card.getXRot() + 90F) / 180.0F * Math.PI) * Math.sin(card.getYRot() / 180.0F * Math.PI);
				Z2 = Math.cos(angleXZ / 180.0F * Math.PI) * Math.sin(angleY / 180.0F * Math.PI) * Math.sin((card.getXRot() + 90F) / 180.0F * Math.PI) * Math.cos(card.getYRot() / 180.0F * Math.PI);

				yVector = -Math.cos(angleXZ / 180.0F * Math.PI) * Math.sin((card.getXRot() + 90F - angleY) / 180.0F * Math.PI);//Y方向　上下
				xVector =  Math.cos(angleXZ / 180.0F * Math.PI) * Math.cos(angleY / 180.0F * Math.PI) * lookAt.x + X1 - X2;//X方向　水平方向
				zVector =  Math.cos(angleXZ / 180.0F * Math.PI) * Math.cos(angleY / 180.0F * Math.PI) * lookAt.z + Z1 + Z2;//Z方向　水平方向

				ShotData shot = ShotData.shot(FORM_STAR, time % 7, 0.4F, 8.0F, 0, 55);
				THShotLib.createShot(user, card, pos_Card(), angle(xVector, yVector, zVector), 0F, 0.2D, 0.3D, 0.05D, gravity(xVectorG, yVectorG, zVectorG), shot);
				/*entityTHShot[i] = new EntityTHShot(world, user, card, xVector, yVector, zVector,
					0.2D, 0.3D, 0.05D, xVectorG, yVectorG, zVectorG, 8, THShotLib.STAR[0] + time % 7, 0.4F, 55);*/
				angle += angleSpan;
				/*if(!world.isRemote)
				{
					world.spawnEntityInWorld(entityTHShot[i]);
				}*/
			}
		}
	}
}
