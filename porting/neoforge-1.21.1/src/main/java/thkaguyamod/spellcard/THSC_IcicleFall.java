package thkaguyamod.spellcard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.GRAVITY_DEFAULT;

/** 氷符「アイシクルフォール」 */
public class THSC_IcicleFall extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_ICECLEFALL01 = 1900;
	public static final int SPECIAL_ICECLEFALL02 = 1901;

	public THSC_IcicleFall()
	{
		this.setNeedLevel(1);
		this.setRemoveTime(60);
		this.setEndTime(180);
		this.setOriginalUserName(CIRNO);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_ICECLEFALL01);
		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_ICECLEFALL02);
	}

	@Override
	public void spellcard_main()
	{
		if(time % 5 == 0)// 0.25秒に１回弾を発射する
		{
			float angle = (float)Math.sin(time * 13F / 180F * 3.141593F) * (30F + level * 30F) + 90F;// 角度は毎回変化
			int num = 4 + level * 2;// 難易度によって弾数が異なる。levelは1～４でEasy、Normal、Hard、Lunaticを表す
			float damage = 6.0F;
			for(int i = 1; i < num; i++)
			{
				THShotLib.createShot(user, user, pos_User(), getVecFromAngle(user.getYRot() + angle, -30F), 0F, i * 1.0D, 0.0D, -i * 0.1D, gravity_Zero(), shot(THShotLib.FORM_CRYSTAL, THShotLib.BLUE, 0.5F, damage, 0, num, SPECIAL_ICECLEFALL01)).rotate = THShotLib.rotate_Default();
				THShotLib.createShot(user, user, pos_User(), getVecFromAngle(user.getYRot() - angle, -30F), 0F, i * 1.0D, 0.0D, -i * 0.1D, gravity_Zero(), shot(THShotLib.FORM_CRYSTAL, THShotLib.BLUE, 0.5F, damage, 0, num, SPECIAL_ICECLEFALL02)).rotate = THShotLib.rotate_Default();
			}
		}
	}

	// 特殊弾の動きを設定する
	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_ICECLEFALL01:
				if(shot.tickCount >= shot.getDeadTime())
				{
					shot.setShotRotationYaw(90F);
					shot.setDeadTime(120);
					// 1.7.10 の gravity.y 直接代入を不変 Vec3 用に差し替え
					shot.gravity = new net.minecraft.world.phys.Vec3(shot.gravity.x, GRAVITY_DEFAULT, shot.gravity.z);
					shot.shotAcceleration = 0.01D;
					shot.shotLimitSpeed = 10.0D;
					shot.updateMotion();
					shot.specialEnd();
				}
				break;
			case SPECIAL_ICECLEFALL02:
				if(shot.tickCount == shot.getDeadTime())
				{
					shot.setShotRotationYaw(-90F);
					shot.setDeadTime(120);
					shot.gravity = new net.minecraft.world.phys.Vec3(shot.gravity.x, GRAVITY_DEFAULT, shot.gravity.z);
					shot.shotAcceleration = 0.01D;
					shot.shotLimitSpeed = 10.0D;
					shot.updateMotion();
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
