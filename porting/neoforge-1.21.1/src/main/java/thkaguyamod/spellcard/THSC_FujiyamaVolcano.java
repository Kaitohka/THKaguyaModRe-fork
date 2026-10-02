package thkaguyamod.spellcard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.*;
import static thkaguyamod.danmaku.THShotLib.*;

/** 蓬莱「凱風快晴 -フジヤマヴォルケイノ-」 */
public class THSC_FujiyamaVolcano extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_FUJUYAMA01 = 3300;
	public static final int SPECIAL_FUJUYAMA02 = 3301;
	public static final int SPECIAL_FUJUYAMA03 = 3302;
	public static final int SPECIAL_FUJUYAMA04 = 3303;

	public THSC_FujiyamaVolcano()
	{
		this.setNeedLevel(5);
		this.setRemoveTime(20);
		this.setEndTime(120);
		this.setOriginalUserName(MOKOU);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_FUJUYAMA01);
		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_FUJUYAMA02);
		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_FUJUYAMA03);
		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_FUJUYAMA04);
	}

	@Override
	public void spellcard_main()
	{
		if(time < 45 && time % 15 == 0)
		{
			ShotData shot = ShotData.shot(FORM_FAMILIAR, BLUE, 0, 40, SPECIAL_FUJUYAMA01);
			THShotLib.createShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, 2.4D, 0.0D, -(2.4D / user.distanceTo(target)), gravity_Zero(), shot);
			THShotLib.playShotSound(user);
		}
		if(time >= 60 && time <= 90 && time % 7 == 0)
		{
			ShotData shot = ShotData.shot(FORM_FAMILIAR, BLUE, 0, 40, SPECIAL_FUJUYAMA01);
			THShotLib.createShot(user, user, pos_User(), this.angle_UserToTarget(), 0F, 2.4D, 0.0D, -(2.4D / user.distanceTo(target)), gravity_Zero(), shot);
			THShotLib.playShotSound(user);
		}
	}

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_FUJUYAMA01:
				if(shot.getSpeed() <= 0.0D)
				{
					/*for(int i = 0; i < 44; i++)
					{
						THShotLib.createShot(shot.user, shot, shot.pos_Shot(), angle_Random(), 0.0F, 0.5D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_BIG, RED, SIZE[FORM_BIG], 10.0F, 0, 9, SPECIAL_FUJUYAMA02));
					}*/
					THShotLib.createSphereShot(shot.user, shot, shot.pos_Shot(), angle_Random(), 0F, 0.5D, 0.5D, 0.0D, gravity_Zero(), ShotData.shot(FORM_BIG, RED, SIZE[FORM_BIG], 10.0F, 0, 9, SPECIAL_FUJUYAMA02), 44, 0.0D, rand.nextFloat() * 360F);
					if(!world.isClientSide)
					{
						// 1.7.10: world.createExplosion(shot, x, y, z, 1.0F, false) → ブロック破壊ありの爆発
						world.explode(shot, shot.getX(), shot.getY(), shot.getZ(), 1.0F, Level.ExplosionInteraction.BLOCK);
						shot.discard();
					}
				}
				break;
			case SPECIAL_FUJUYAMA02:
				if(shot.isShotEndTime())
				{
					for(int i = 0; i < 2; i++)
					{
						THShotLib.createShot(shot.user, shot, shot.pos_Shot(), angle_LimitRandom(shot.angle, 50F), 0.0F, 0.5D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_MEDIUM, RED, SIZE[FORM_MEDIUM], 7.0F, 0, 5, SPECIAL_FUJUYAMA03));
					}
					if(!world.isClientSide)
					{
						shot.discard();
					}
				}
				break;
			case SPECIAL_FUJUYAMA03:
				if(shot.isShotEndTime())
				{
					for(int i = 0; i < 2; i++)
					{
						THShotLib.createShot(shot.user, shot, shot.pos_Shot(), angle_LimitRandom(shot.angle, 50F), 0.0F, 0.5D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_SMALL, RED, SIZE[FORM_SMALL], 5.0F, 0, 4, SPECIAL_FUJUYAMA04));
					}
					if(!world.isClientSide)
					{
						shot.discard();
					}
				}
				break;
			case SPECIAL_FUJUYAMA04:
				if(shot.isShotEndTime())
				{
					for(int i = 0; i < 2; i++)
					{
						THShotLib.createShot(shot.user, shot, shot.pos_Shot(), angle_LimitRandom(shot.angle, 50F), 0.0F, 0.5D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_TINY, RED, SIZE[FORM_TINY], 3.0F, 0, 2, 0));
					}
					if(!world.isClientSide)
					{
						shot.discard();
					}
				}
				break;
			default:
				break;
		}
	}

	/**
	 * ある初速度で、離れた地点に、特定の時間に丁度停止する加速度を返す。
	 * @param time
	 * @param distance
	 * @param firstSpeed
	 * @return
	 */
	public double getJustTimeAcceleration(int time, double distance, double firstSpeed)
	{
		return (distance - firstSpeed) / (double)time;
	}

	@Override
	public boolean specialShot_hitBlock(Level world, int id, EntityTHShot shot, BlockHitResult hitResult)
	{
		switch(id)
		{
			case SPECIAL_FUJUYAMA01:
				//if(shot.getSpeed() <= 0.0D)
				{
					for(int i = 0; i < 40; i++)
					{
						THShotLib.createShot(shot.user, shot, shot.pos_Shot(), angle_Random(), 0.0F, 0.5D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_BIG, RED, SIZE[FORM_BIG], 10.0F, 0, 6, SPECIAL_FUJUYAMA02));
					}
					if(!world.isClientSide)
					{
						shot.discard();
					}
				}
				return false;
			default:
				break;
		}
		return false;
	}

	@Override
	public boolean specialShot_hitEntity(Level world, int id, EntityTHShot shot, Entity hitEntity)
	{
		return false;
	}
}
