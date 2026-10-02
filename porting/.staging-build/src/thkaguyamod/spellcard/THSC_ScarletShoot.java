package thkaguyamod.spellcard;

import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** 紅符「スカーレットシュート」 */
public class THSC_ScarletShoot extends THSpellCard
{
	public THSC_ScarletShoot()
	{
		this.setNeedLevel(4);
		this.setRemoveTime(30);
		this.setEndTime(120);
		this.setOriginalUserName(REMILIA);
	}

	@Override
	public void spellcard_main()
	{
		if(time % 25 == 20 && time < 60)
		{
			Vec3 lookAt = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot(), 1.0D);
			if(target.isAlive())
			{
				lookAt = THShotLib.angle_ToPos(this.pos_User(), this.pos_Target());
			}
			Vec3 rotate = THShotLib.getOverVector(lookAt);
			float angle = -90F;
			float angleSpan = 180F / 4F;

			for(int i = 0; i < 5; i++)
			{
				scarletShot(THShotLib.getVectorFromRotation(rotate, lookAt, angle), 1.0D, false);
				angle += angleSpan;
			}
			THShotLib.playShotSound(user);
		}
		else if(time == 80)
		{
			Vec3 lookAt = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot(), 1.0D);
			if(target.isAlive())
			{
				lookAt = THShotLib.angle_ToPos(this.pos_User(), this.pos_Target());
			}
			Vec3 rotate = THShotLib.getOverVector(lookAt);
			float angle = -3F;
			float angleSpan = 6F / 2F;

			for(int i = 0; i < 3; i++)
			{
				scarletShot(THShotLib.getVectorFromRotation(rotate, lookAt, angle), 1.0D, false);
				angle += angleSpan;
			}
			THShotLib.playShotSound(user);
		}
	}

	// スカーレットシュートのまとまった弾（カタディオプトリックの弾に変更も可）
	public void scarletShot(Vec3 angle, double speed, boolean isCatadi)
	{
		int special = 0;
		int color = THShotLib.RED;
		double speed2;

		if(isCatadi)
		{
			special = THShotLib.BOUND04;
			color = THShotLib.BLUE;
		}
		THShotLib.createShot(user, user, pos_User(), angle, 0F, speed, speed, 0.0D, gravity_Zero(), shot(FORM_BIG, color, THShotLib.SIZE[FORM_BIG], 6F, 0, 120, special));
		for(int i = 0; i < 11; i++)
		{
			speed2 = (0.5D + rand.nextDouble() * 0.3D) * speed;
			//thShotLib.createShot(user, user, user.posX + (rand.nextDouble() * 2.0D - 1.0D) * 0.2D, ...);
			THShotLib.createRandomRingShot(user, pos_User(), angle, speed2, speed2, 0.0D, shot(FORM_MEDIUM, color, THShotLib.SIZE[FORM_MEDIUM], 4.0F, 0, 120, special), 1, 0.0D, 30F);
		}
		for(int i = 0; i < 22; i++)
		{
			speed2 = (0.2D + rand.nextDouble() * 0.3D) * speed;
			//thShotLib.createShot(user, user, user.posX + (rand.nextDouble() * 2.0D - 1.0D) * 0.3D, ...);
			THShotLib.createRandomRingShot(user, pos_User(), angle, speed2, speed2, 0.0D, shot(FORM_SMALL, color, THShotLib.SIZE[FORM_SMALL], 2.0F, 0, 120, special), 1, 0.0D, 60F);
		}
	}
}
