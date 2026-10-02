package thkaguyamod.spellcard;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModSounds;

/** 恋符「ノンディレクショナルレーザー」 */
public class THSC_NonDirectionalLaser extends THSpellCard
{
	public THSC_NonDirectionalLaser()
	{
		this.setNeedLevel(3);
		this.setRemoveTime(60);
		this.setEndTime(103);
		this.setOriginalUserName(MARISA);
	}

	@Override
	public void spellcard_main()
	{
		if(time == 3)
		{
			// 1.7.10: world.playSoundAtEntity(user, "thKaguyaMod.masterspark", ...)
			level().playSound(null, user.blockPosition(), ModSounds.MASTER_SPARK.get(), SoundSource.NEUTRAL, ModConfig.MasterSparkVol, 1.0F);
			for(int i = 0; i < 5; i++)
			{
				Vec3 look = user.getLookAngle();
				Vec3 rotate = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() - 90F);
				Vec3 move = THShotLib.getVectorFromRotation(rotate.x, rotate.y, rotate.z, look.x, look.y, look.z, i * 72F);
				THShotLib.createLaserB(user, user, pos(0.0D, THShotLib.getPosYFromEye(user) - user.getY(), 0.0D), move, rotate, -4F, 9999, LaserData.laser(THShotLib.RED + i, 0.7F, 20.8F, 7.0F, 25, 60, 0), user, 1.5D, 1.0F);
			}
		}
		if(time == 63)
		{
			level().playSound(null, user.blockPosition(), ModSounds.MASTER_SPARK.get(), SoundSource.NEUTRAL, ModConfig.MasterSparkVol, 1.0F);
			for(int i = 0; i < 5; i++)
			{
				Vec3 look = user.getLookAngle();
				Vec3 rotate = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() - 90F);
				Vec3 move = THShotLib.getVectorFromRotation(rotate.x, rotate.y, rotate.z, look.x, look.y, look.z, i * 72F);
				THShotLib.createLaserB(user, user, pos(0.0D, THShotLib.getPosYFromEye(user) - user.getY(), 0.0D), move, rotate, 4F, 9999, LaserData.laser(THShotLib.RED + i, 0.7F, 20.8F, 7.0F, 25, 60, 0), user, 1.5D, 1.0F);
			}
		}
	}
}
