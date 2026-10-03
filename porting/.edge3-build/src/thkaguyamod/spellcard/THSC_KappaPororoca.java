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
import static thkaguyamod.danmaku.THShotLib.pos_Entity;

/** 水符「河童のポロロッカ」 */
public class THSC_KappaPororoca extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_KAPPA_POROROCA01 = 1100;

	public THSC_KappaPororoca()
	{
		this.setNeedLevel(3);
		this.setRemoveTime(30);
		this.setEndTime(140);
		this.setOriginalUserName(NITORI);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_KAPPA_POROROCA01);
	}

	@Override
	public void spellcard_main()
	{
		if(time < 5) return;

		if(time % 10 == 0)
		{
			THShotLib.createShot(user, user, pos_User(), getVecFromAngle(user.getYRot() + 90F, -60F), 0F, 0.3D, 0.3D, 0.0D, gravity_Default(),
					shot(THShotLib.FORM_LIGHT, THShotLib.BLUE, 0.45F, 8.0F, 2, 60, SPECIAL_KAPPA_POROROCA01));
			THShotLib.createShot(user, user, pos_User(), getVecFromAngle(user.getYRot() - 90F, -60F), 0F, 0.3D, 0.3D, 0.0D, gravity_Default(),
					shot(THShotLib.FORM_LIGHT, THShotLib.BLUE, 0.45F, 8.0F, 2, 60, SPECIAL_KAPPA_POROROCA01));
		}
	}

	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_KAPPA_POROROCA01:
				if(shot.tickCount % 4 == 3)
				{
					ShotData shotData = ShotData.shot(FORM_LIGHT, BLUE, 0.3F, 5.0F, 0, 100, BOUND01);
					THShotLib.createShot(shot.user, shot, pos_Entity(shot), angle(shot.user.getYRot() + rand.nextFloat() * 30F - 15F, -15F), 0F, 0.7D, 0.7D, 0.0D, gravity_Default(), shotData);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public boolean specialShot_hitBlock(Level level, int id, EntityTHShot shot, BlockHitResult hitResult)
	{
		switch(id)
		{
			case SPECIAL_KAPPA_POROROCA01:
				shot.bound(hitResult, 0.9D, 4);
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean specialShot_hitEntity(Level level, int id, EntityTHShot shot, Entity hitEntity)
	{
		return false;
	}
}
