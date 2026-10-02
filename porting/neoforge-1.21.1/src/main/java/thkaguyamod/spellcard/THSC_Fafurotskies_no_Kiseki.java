package thkaguyamod.spellcard;

import java.time.LocalDate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.living.EntityDanmakuCreeper;
import thkaguyamod.entity.shot.EntityNuclearShot;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_KNIFE;
import static thkaguyamod.danmaku.DanmakuConstants.RANDOM;
import static thkaguyamod.danmaku.THShotLib.*;

/** 奇跡「ファフロッキーズの奇跡」 */
public class THSC_Fafurotskies_no_Kiseki extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_FAFUROTSKIES01 = 1500;

	public THSC_Fafurotskies_no_Kiseki()
	{
		this.setNeedLevel(1);
		this.setRemoveTime(20);
		this.setEndTime(40);
		this.setOriginalUserName(SANAE);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_FAFUROTSKIES01);
	}

	@Override
	public void spellcard_main()
	{
		if(time == 1)
		{
			int pattern = rand.nextInt(100);
			for(int i = 0; i < 10; i++)
			{
				Vec3 angle = getVecFromAngle(user.getYRot() + rand.nextFloat() * 50F - 25F, rand.nextFloat() * 20F - 69F);
				Vec3 angle2 = angle(user.getYRot(), 0.0F);

				THShotLib.createShot(user, user, pos_Distance(pos_User(), angle2, 0.5D), angle, 0F, 1.2D, pattern, 0.0D, gravity_Default(), shot(THShotLib.FORM_CIRCLE, THShotLib.AQUA, 0.4F, 6.0F, i, 20, SPECIAL_FAFUROTSKIES01));
			}
			THShotLib.playShotSound(user);
		}
	}

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_FAFUROTSKIES01:
				if(shot.tickCount == shot.getDeadTime())
				{
					int pattern = (int)shot.shotLimitSpeed;

					// 1.7.10: world.getCurrentDate() → java.time.LocalDate.now()
					LocalDate now = LocalDate.now();

					if(now.getMonthValue() == 12 && now.getDayOfMonth() == 25)
					{
						EntityDanmakuCreeper entityCreeper = new EntityDanmakuCreeper(ModEntities.DANMAKU_CREEPER.get(), world);
						// 1.7.10 の setLocationAndAngles に相当（位置と向きを同時に設定）
						entityCreeper.moveTo((int)shot.getX(), (int)shot.getY(), (int)shot.getZ(), rand.nextFloat() * 360F, 0.0F);
						if(!world.isClientSide)
						{
							world.addFreshEntity(entityCreeper);
							shot.discard();
						}
						return;
					}

					if(now.getMonthValue() == 1 && (now.getDayOfMonth() >= 1 && now.getDayOfMonth() <= 3))
					{
						if(!world.isClientSide)
						{
							EntityNuclearShot nuclearShot = new EntityNuclearShot(ModEntities.NUCLEAR_SHOT.get(), world, shot.user, shot.user,
									pos(shot.getX(), shot.getY(), shot.getZ()), angle_Random(), 0.0F, rotate_Default(), 0.0F, 9999,
									0.2D, 0.8D, 0.00D, gravity_Default(), ShotData.shot(0, rand.nextInt(7), 3.2F, 10.0F, 0, 70, 0));
							if(!world.isClientSide)
							{
								world.addFreshEntity(nuclearShot);// 核弾を出現させる
							}
							nuclearShot.shootingFlag = true;
							shot.discard();
						}
						return;
					}

					if(pattern <= 70)
					{
						for(int i = 0; i < 8; i++)
						{
							ShotData shotData = ShotData.shot(FORM_KNIFE, RANDOM, 0.6F, 10.0F, 3, 120);
							THShotLib.createShot(shot.user, shot, pos_Entity(shot), angle(rand.nextFloat() * 360F, rand.nextFloat() * 180 - 90F), 0F, 0.6D, 0.6D, 0.0D, gravity_Default(), shotData);
						}
						if(!world.isClientSide)
						{
							shot.discard();
						}
					}
					else if(pattern <= 80)
					{
						if(!world.isClientSide)
						{
							world.addFreshEntity(new ItemEntity(world, shot.getX(), shot.getY(), shot.getZ(), new ItemStack(Items.COD)));
							shot.discard();
						}
					}
					else if(pattern <= 90)
					{
						Creeper entityCreeper = new Creeper(EntityType.CREEPER, world);
						entityCreeper.setPos((int)shot.getX(), (int)shot.getY(), (int)shot.getZ());
						if(!world.isClientSide)
						{
							world.addFreshEntity(entityCreeper);
							shot.discard();
						}
					}
					else
					{
						Chicken entitychicken = new Chicken(EntityType.CHICKEN, world);
						entitychicken.setPos((int)shot.getX(), (int)shot.getY(), (int)shot.getZ());
						if(!world.isClientSide)
						{
							world.addFreshEntity(entitychicken);
							shot.discard();
						}
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
