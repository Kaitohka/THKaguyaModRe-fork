package thkaguyamod.spellcard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_BUTTERFLY;

/** 魍魎「二重黒死蝶」 */
public class THSC_NijuuKokushichou extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_KOKUSHICHOU01 = 500;

	public THSC_NijuuKokushichou()
	{
		this.setNeedLevel(3);
		this.setRemoveTime(20);
		this.setEndTime(200);
		this.setOriginalUserName(YUKARI);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_KOKUSHICHOU01);
	}

	@Override
	public void spellcard_main()
	{
		if(time % 70 == 10 && time <= 100)
		{
			for(int i = 0; i < 30; i++)
			{
				double speed;
				float angleXZ, angleY;

				speed = rand.nextDouble() * 2.0 + 0.5D;
				angleXZ = rand.nextFloat() * 360F;
				angleY = rand.nextFloat() * 180F - 90F;
				THShotLib.createCircleShot(user, user, pos_User(), getVecFromAngle(angleXZ, angleY), speed, 0.0D, 1.0D, gravity_Zero(), shot(FORM_BUTTERFLY, i % 2, THShotLib.SIZE[FORM_BUTTERFLY], 8.0F, 1, 120, SPECIAL_KOKUSHICHOU01), 8, 0.2D, 0F);
			}
			THShotLib.playShotSound(user);
		}
	}

	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_KOKUSHICHOU01:// 二重黒死蝶の蝶弾
				if(shot.tickCount < 25)
				{
					shot.motionX *= 0.90D;
					shot.motionY *= 0.90D;
					shot.motionZ *= 0.90D;
				}
				else if(shot.tickCount == 25)
				{
					shot.shotLimitSpeed = 2.0D;
					shot.shotAcceleration = 0.03D;
				}
				else if(shot.tickCount < 50)
				{
					if(shot.getShotColor() % 2 == 0)
					{
						shot.setShotRotationYaw(10F);
					}
					else
					{
						shot.setShotRotationYaw(-10F);
					}
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
