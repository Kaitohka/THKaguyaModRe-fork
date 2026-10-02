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

import static thkaguyamod.danmaku.DanmakuConstants.FORM_STAR;
import static thkaguyamod.danmaku.DanmakuConstants.RAINBOW;
import static thkaguyamod.danmaku.THShotLib.pos_Entity;

/** 魔符「スターダストレヴァリエ」 */
public class THSC_StardustReverie extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_STARDUST01 = 1200;

	public THSC_StardustReverie()
	{
		this.setNeedLevel(4);
		this.setRemoveTime(60);
		this.setEndTime(120);
		this.setOriginalUserName(MARISA);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_STARDUST01);
	}

	@Override
	public void spellcard_main()
	{
		if(time < 5) return;

		if(time == 5)
		{
			int way[] = { 4, 4, 7, 9, 10 };

			THShotLib.createCircleShot(user, user, pos_User(),
					user.getLookAngle(), 0.3D, 0.3D, 0.0D, gravity_Zero(),
					shot(FORM_STAR, RAINBOW, 1.2F, 7.0F, 2, 120, SPECIAL_STARDUST01), way[level], 0.2D, 0F);
		}
	}

	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_STARDUST01:
				ShotData shotData = ShotData.shot(shot.getShotForm(), shot.getShotColor(), 0.3F, 5.0F, 10, 50);
				shot.setShotRotationYaw(2.0F);
				Vec3 vec = THShotLib.getVectorFromRotation(shot.rotate, shot.angle, shot.tickCount * 13F);
				THShotLib.createShot(shot.user, shot, pos_Entity(shot), angle(vec.x, vec.y, vec.z), shot.getAngleZ(), shot.rotate, 0F, 9999, 0.1D, 0.3D, 0.03D, THShotLib.gravity_Zero(), shotData);
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
		switch(id)
		{
			case SPECIAL_STARDUST01:
				return true;
			default:
				return false;
		}
	}
}
