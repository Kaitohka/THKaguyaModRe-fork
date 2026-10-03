package thkaguyamod.spellcard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** 蛍符「地上の流星」 */
public class THSC_MeteorOnEarth extends THSpellCard implements ISpecialShot
{
	public static final int SP_METEORONEARTH01 = 3500;
	public static final int SP_METEORONEARTH11 = 3510;

	public THSC_MeteorOnEarth()
	{
		this.setNeedLevel(1);
		this.setRemoveTime(25);
		this.setEndTime(120);
		this.setOriginalUserName(WRIGGLE);

		SpecialShotRegistry.registerSpecialShot(this, SP_METEORONEARTH01);
		SpecialShotRegistry.registerSpecialShot(this, SP_METEORONEARTH11);
	}

	@Override
	public void spellcard_main()
	{
		int way[] = {0, 5, 7, 4, 6};

		if(time == 5)
		{
			Vec3 angle = this.angle_UserToTarget();
			THShotLib.createRingShot(user, user, pos_User(), angle, 2.0F, 60, 0.3D, 0.3D, 1.0D,
					gravity_Zero(),
					ShotData.shot(FORM_FAMILIAR, GREEN, 0, 60, SP_METEORONEARTH01),
					way[level], 0.5D, 30F, rand.nextFloat() * 360F);
			if(level >= HARD)
			{
				THShotLib.createRingShot(user, user, pos_User(), angle, 2.0F, 60, 0.3D, 0.3D, 1.0D,
						gravity_Zero(),
						ShotData.shot(FORM_FAMILIAR, GREEN, 0, 60, SP_METEORONEARTH01),
						8, 0.5D, 70F, rand.nextFloat() * 360F);
			}
			THShotLib.playShotSound(user);
		}
		if(time == 65)
		{
			Vec3 angle = this.angle_UserToTarget();
			THShotLib.createRingShot(user, user, pos_User(), angle, -2.0F, 60, 0.3D, 0.3D, 1.0D,
					gravity_Zero(),
					ShotData.shot(FORM_FAMILIAR, GREEN, 0, 60, SP_METEORONEARTH01),
					way[level], 0.5D, 30F, rand.nextFloat() * 360F);
			if(level >= HARD)
			{
				THShotLib.createRingShot(user, user, pos_User(), angle, -2.0F, 60, 0.3D, 0.3D, 1.0D,
						gravity_Zero(),
						ShotData.shot(FORM_FAMILIAR, GREEN, 0, 60, SP_METEORONEARTH01),
						8, 0.5D, 70F, rand.nextFloat() * 360F);
			}
			THShotLib.playShotSound(user);
		}
	}

	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SP_METEORONEARTH01:
				if(shot.tickCount == 1)
				{
					shot.rotate = shot.user.getLookAngle();
				}
				if(shot.tickCount % 2 == 0)
				{
					THShotLib.createShot(shot.user, shot, shot.pos(), THShotLib.getVectorFromRotation(shot.angle, shot.rotate, shot.tickCount * 13.0F), 0.0F, 0.0D, 0.5D, 0.00D, gravity_Zero(),
							ShotData.shot(FORM_SMALL, AQUA, 5, 40, SP_METEORONEARTH11));
				}
				break;
			case SP_METEORONEARTH11:
				if(shot.tickCount == 30)
				{
					shot.shotAcceleration = 0.03D;
					shot.updateMotion();
				}
				break;
			default:
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
