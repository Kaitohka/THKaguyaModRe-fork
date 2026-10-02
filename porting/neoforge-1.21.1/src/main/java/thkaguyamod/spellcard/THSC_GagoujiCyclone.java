package thkaguyamod.spellcard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.living.EntityDanmakuMob;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** 雷矢「ガゴウジサイクロン」 */
public class THSC_GagoujiCyclone extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_GAGOUJI01 = 4000;
	//public static final int SPECIAL_GAGOUJI02 = 4001;

	public static final int SPECIAL_TOZIKO01 = 4020;

	public THSC_GagoujiCyclone()
	{
		this.setNeedLevel(2);
		this.setRemoveTime(30);
		this.setEndTime(120);
		this.setOriginalUserName(TOZIKO);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_GAGOUJI01);
		//SpecialShotRegistry.registerSpecialShot(this, SPECIAL_GAGOUJI02);
		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_TOZIKO01);
	}

	@Override
	public void spellcard_main()
	{
		if(time % 120 <= 40 && time % 2 == 0)// 0.25秒に１回弾を発射する
		{
			float angle = (float)Math.sin(time * 13F / 180F * 3.141593F) * (30F + level * 30F) + 90F;// 角度は毎回変化
			int num = 4 + level * 2;// 難易度によって弾数が異なる。levelは1～４でEasy、Normal、Hard、Lunaticを表す
			float damage = 6.0F;

			THShotLib.createRingShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, 9999, 1.0D, 1.0D, 0.0D, gravity_Zero(), ShotData.shot(FORM_ARROW, YELLOW, 0, 50, SPECIAL_GAGOUJI01), 9, 1.0D, 80F, time / 2F);
			if(level >= 2)
			THShotLib.createShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, angle(user.getYRot(), user.getXRot() + 90F), 0F, 99999, 1.0D, 0.0D, -0.1D, gravity_Zero(), ShotData.shot(FORM_ARROW, YELLOW, THShotLib.SIZE[FORM_ARROW], 5.0F, 0, 60, SPECIAL_GAGOUJI01));//.setShotRotationYaw(-45F);
		}

		if(time % 120 >= 60 && time % 120 < 100 && time % 2 == 0)// 0.25秒に１回弾を発射する
		{
			float angle = (float)Math.sin(time * 13F / 180F * 3.141593F) * (30F + level * 30F) + 90F;// 角度は毎回変化
			int num = 4 + level * 2;// 難易度によって弾数が異なる。levelは1～４でEasy、Normal、Hard、Lunaticを表す
			float damage = 6.0F;

			THShotLib.createRingShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, 9999, 1.0D, 1.0D, 0.0D, gravity_Zero(), ShotData.shot(FORM_ARROW, YELLOW, 0, 50, SPECIAL_GAGOUJI01), 9, 1.0D, 80F, -time / 2F);
			if(level >= 2)
			THShotLib.createShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, angle(user.getYRot(), user.getXRot() + 90F), 0F, 99999, 1.0D, 0.0D, -0.1D, gravity_Zero(), ShotData.shot(FORM_ARROW, YELLOW, THShotLib.SIZE[FORM_ARROW], 5.0F, 0, 60, SPECIAL_GAGOUJI01));//.setShotRotationYaw(-45F);
		}

		if(level == 4)
		{
			if(time % 120 > 30 && time % 120 <= 90)
			{
				THShotLib.createRingShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, 99999, 1.0D, 0.0D, -0.1D, gravity_Zero(), ShotData.shot(FORM_ARROW, RED, 0, 60, SPECIAL_GAGOUJI01), 3, 1.0D, 10F, time / 2F);
			}
		}

		if(time % 20 == 0)
		{
			THShotLib.createRingShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, 99999, 1.2D, 0.15D + level * 0.05D, -0.1D, gravity_Zero(), ShotData.shot(FORM_CIRCLE, YELLOW, THShotLib.SIZE[FORM_CIRCLE], 5.0F, 0, 90, 0), 8 + level * 4, 1.0D, 70F, 0F);
			THShotLib.createRingShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, 99999, 1.2D, 0.15D + level * 0.05D, -0.1D, gravity_Zero(), ShotData.shot(FORM_CIRCLE, YELLOW, THShotLib.SIZE[FORM_CIRCLE], 5.0F, 0, 90, 0), 4 + level * 2, 1.0D, 20F, 0F);
			if(level >= 3)
			{
				THShotLib.createShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, rotate_Default(), 0F, 99999, 1.2D, 0.15D + level * 0.05D, -0.1D, gravity_Zero(), ShotData.shot(FORM_CIRCLE, YELLOW, THShotLib.SIZE[FORM_CIRCLE], 5.0F, 0, 90, 0));
			}
			THShotLib.playShotSound(user);
		}
	}

	// 特殊弾の動きを設定する
	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_GAGOUJI01:
				if(shot.tickCount == 1)
				{
					shot.setShotRotationYaw(45F);
					//shot.setSpeed(1.0D);
					shot.shotAcceleration = -0.099D;
					shot.shotLimitSpeed = 0.0D;
					shot.setMotion();
					//shot.shotSpecial = SPECIAL_GAGOUJI02;
				}
				//break;
			//case SPECIAL_GAGOUJI02:
				if(shot.tickCount % 20 == 9)
				{
					shot.setShotRotationYaw(-90F);
					shot.setSpeed(1.0D);
					shot.setMotion();
				}
				else if(shot.tickCount % 20 == 19)
				{
					shot.setShotRotationYaw(90F);
					shot.setSpeed(1.0D);
					shot.setMotion();
				}
				break;
			case SPECIAL_TOZIKO01:
				if(shot.tickCount >= shot.getDeadTime())
				{
					if(shot.user instanceof EntityDanmakuMob danmakuMob)
					{
						if(danmakuMob.getTarget() != null)
						{
							shot.angle = THShotLib.angle_ToPos(danmakuMob.pos(), pos(danmakuMob.getTarget().getX(), danmakuMob.getTarget().getY(), danmakuMob.getTarget().getZ()));
							shot.setMotion();
						}
					}
					shot.setDeadTime(90);
					shot.shotSpeed = 0.01D;
					shot.shotAcceleration = 0.03D;
					shot.shotLimitSpeed = 0.2D + ModConfig.danmakuLevel * 0.1D;
					shot.specialEnd();
				}
				break;
		}
	}

	@Override
	public boolean specialShot_hitBlock(Level level, int id, EntityTHShot shot, BlockHitResult hitResult)
	{
		return false;
	}

	@Override
	public boolean specialShot_hitEntity(Level level, int id, EntityTHShot shot, Entity hitEntity)
	{
		return false;
	}
}
