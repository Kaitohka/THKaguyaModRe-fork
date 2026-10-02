package thkaguyamod.spellcard;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityHomingAmulet;
import thkaguyamod.entity.shot.EntityMusouFuuin;
import thkaguyamod.entity.shot.EntityTHLaser;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.*;
import static thkaguyamod.danmaku.THShotLib.SIZE;
import static thkaguyamod.danmaku.THShotLib.angle_Random;

/** 凍符「パーフェクトフリーズ」 */
public class THSC_PerfectFreeze extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_FREEZE = 900;

	public THSC_PerfectFreeze()
	{
		this.setNeedLevel(1);
		this.setRemoveTime(20);
		this.setEndTime(110);
		this.setOriginalUserName(CIRNO);

		// 特殊弾の登録はコンストラクタ内で行う
		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_FREEZE);
	}

	@Override
	public void spellcard_main()
	{
		if(time < 5)
		{
			return;
		}

		// 時間が58になるまでランダムな方向に弾を出し続ける
		if(time < 58)
		{
			int num = 4 + level;
			if(num > 10) num = 10;
			Vec3 angle;
			for(int i = 0; i < num; i++)
			{
				angle = angle_Random();// 全方向からランダムな方向を取得
				THShotLib.createShot(user, user, pos_User(angle, 1.0D), angle, 0F, level * 0.1D, 0.5D, 0.03D, gravity_Zero(), shot(FORM_CIRCLE, RAINBOW, SIZE[FORM_CIRCLE], 4.0F, 5, 120));
			}
			THShotLib.playShotSound(user);
		}

		// 時間が64なら弾幕を凍らせる
		if(time == 64)
		{
			List<Entity> list = level().getEntities(user, user.getBoundingBox().inflate(20.0D));
			for(int k = 0; k < list.size(); k++)
			{
				Entity entity = list.get(k);
				if(entity instanceof EntityTHShot && entity instanceof EntityTHLaser == false)
				{
					if(entity instanceof EntityHomingAmulet == false && entity instanceof EntityMusouFuuin == false)
					{
						EntityTHShot shot = (EntityTHShot)entity;
						THShotLib.createShot(user, user, THShotLib.pos_Entity(shot), getVecFromAngle(rand.nextFloat() * 360F, rand.nextFloat() * 180F - 90F), 0F,
								0.0001D, 0.0001D, 0.0D, gravity_Zero(), shot(shot.getShotForm(), WHITE, shot.getShotSize(), shot.shotDamage, 0, shot.getShotEndTime() - shot.tickCount + 50, SPECIAL_FREEZE));
						shot.discard();
					}
				}
			}
		}
	}

	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_FREEZE:
				if(shot.tickCount >= 20)
				{
					shot.shotSpeed = 0.01D;
					shot.shotLimitSpeed = 0.3D;
					shot.shotAcceleration = 0.03D;
					shot.specialEnd();
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
