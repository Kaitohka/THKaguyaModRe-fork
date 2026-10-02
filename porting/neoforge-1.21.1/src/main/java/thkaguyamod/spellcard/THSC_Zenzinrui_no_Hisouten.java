package thkaguyamod.spellcard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_KISHITU;
import static thkaguyamod.danmaku.DanmakuConstants.RED;
import static thkaguyamod.danmaku.THShotLib.angle_Random;
import static thkaguyamod.danmaku.THShotLib.pos_Entity;

/** 「全人類の緋想天」 */
public class THSC_Zenzinrui_no_Hisouten extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_HISOUTEN01 = 700;

	public THSC_Zenzinrui_no_Hisouten()
	{
		this.setNeedLevel(5);
		this.setRemoveTime(30);
		this.setEndTime(150);
		this.setOriginalUserName(TENSHI);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_HISOUTEN01);
	}

	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_HISOUTEN01:// 気質弾
				if(shot.tickCount < 20)
				{
					shot.setShotSize(shot.getShotSize() + 0.1F);
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
			case SPECIAL_HISOUTEN01:
				ShotData shotData = ShotData.shot(FORM_KISHITU, RED, 0.3F, 4.0F, 0, 90);
				THShotLib.createShot(shot.user, shot, pos_Entity(shot), angle_Random(), 0F, 0.01D, 0.15D, 0.01D, THShotLib.gravity_Zero(), shotData);
				return false;
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
