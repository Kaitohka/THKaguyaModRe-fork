package thkaguyamod.spellcard;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntitySakuyaWatch;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** メイド秘技「殺人ドール」 */
public class THSC_SatuzinDoll extends THSpellCard
{
	public THSC_SatuzinDoll()
	{
		this.setNeedLevel(4);
		this.setRemoveTime(30);
		this.setEndTime(110);
		this.setOriginalUserName(SAKUYA);
	}

	@Override
	public void spellcard_main()
	{
		if(time < 5)
		{
			return;
		}

		if(time < 15 /*&& time % 2 == 0*/)
		{
			THShotLib.createSphereShot(user, user, pos_User(), user.getLookAngle(), 0F, rotate_Default(), 0F, 9999,
					0.4D, 0.4D, 0.0D, gravity_Zero(), shot(FORM_KNIFE, BLUE, THShotLib.SIZE[FORM_KNIFE], 12.0F, 3, 80), 32, 1.0D, time * 6F);
		}
		if(time > 14 && time < 24 && time % 4 == 0)
		{
			int way = 9;
			float angleXZ = user.getYRot();
			float angleY = user.getXRot();
			//thShotLib.createCircleShot01(user, angleXZ,      angleY, 0.4D, thShotLib.KNIFE[thShotLib.RED], way);
			//thShotLib.createCircleShot01(user, angleXZ + 5F, angleY, 0.4D, thShotLib.KNIFE[thShotLib.RED], way);
			THShotLib.createCircleShot(user, user, pos_User(), getVecFromAngle(angleXZ, angleY),
					0.4D, 0.4D, 0.0D, gravity_Zero(), shot(FORM_KNIFE, RED, THShotLib.SIZE[FORM_KNIFE], 12.0F, 3, 80), way, 1.0D, 0F);
			THShotLib.createCircleShot(user, user, pos_User(), getVecFromAngle(angleXZ + 5F, angleY),
					0.4D, 0.4D, 0.0D, gravity_Zero(), shot(FORM_KNIFE, RED, THShotLib.SIZE[FORM_KNIFE], 12.0F, 3, 80), way, 1.0D, 0F);
		}
		if(time == 36)
		{
			boolean flag = true;
			List<Entity> list = level().getEntities(card, card.getBoundingBox().inflate(20.0D));
			for(Entity entity : list)
			{
				if(entity instanceof EntitySakuyaWatch entityPrivateSquare && entityPrivateSquare.userEntity == user)
				{
					flag = false;
				}
			}
			if(flag)
			{
				EntitySakuyaWatch entityPrivateSquare = new EntitySakuyaWatch(ModEntities.SAKUYA_WATCH.get(), level(), user, 1);
				if(!level().isClientSide)
				{
					level().addFreshEntity(entityPrivateSquare);//時間停止空間を生み出す
				}
			}
		}
	}

	/**
	 * 時間停止中にのみ実行される
	 */
	@Override
	public void specialProcessInTimeStop()
	{
		super.specialProcessInTimeStop();

		if(time > 44 && time < 68 && time % 8 == 3)
		{
			List<Entity> list = level().getEntities(card, card.getBoundingBox().inflate(20.0D));
			for(Entity entity : list)
			{
				if(entity instanceof EntityTHShot entityTHShot)
				{
					if(entityTHShot.getShotForm() == FORM_KNIFE
							&& (entityTHShot.getShotColor() == BLUE || entityTHShot.getShotColor() == RED)
							// P36（audit-common）: EntityTHShot.user は同期されない public フィールドで、
							// クライアント側では null になりうる。旧コードはそれを直に receiver にしていたため
							// NPE の潜伏があった（等価比較なので receiver を this.user 側に寄せる）。
							&& this.user != null && this.user.equals(entityTHShot.user) && rand.nextInt(100) < 25)
					{
						//thShotLib.createShot03(user, ...);
						Vec3 angle;
						if(target != null)
						{
							angle = THShotLib.angle_ToPos(THShotLib.pos_Entity(entityTHShot), pos_Target());
						}
						else
						{
							angle = THShotLib.angle_Random();
						}
						THShotLib.createShot(user, user, pos(entityTHShot.getX(), entityTHShot.getY() - ((double)user.getEyeHeight() - 0.25D), entityTHShot.getZ()),
								angle, 0F, 0.2D + rand.nextDouble() * 0.5D, 0.7D, 0.02D, gravity_Zero(),
								shot(FORM_KNIFE, GREEN, THShotLib.SIZE[FORM_KNIFE], 12.0F, 0, 80));
						if(!level().isClientSide)
						{
							entityTHShot.discard();
						}
					}
				}
			}
		}
	}

	/**
	 * 時間の停止した状態でも処理を続行できるか（使用者自信が時間停止していないと意味は無い）
	 * @return 処理を続けられるならtrue
	 */
	@Override
	public boolean canMoveInTimeStop()
	{
		return true;
	}
}
