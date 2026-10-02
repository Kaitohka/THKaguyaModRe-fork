package thkaguyamod.spellcard;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.danmaku.ISpecialShot;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.registry.SpecialShotRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_SCALE;

/** 妖怪退治「妖力スポイラー」 */
public class THSC_YouryokuSpoiler extends THSpellCard implements ISpecialShot
{
	public static final int SPECIAL_SPOILER01 = 1600;
	public static final int SPECIAL_SPOILER02 = 1601;

	public THSC_YouryokuSpoiler()
	{
		this.setNeedLevel(2);
		this.setRemoveTime(10);
		this.setEndTime(80);
		this.setOriginalUserName(SANAE);

		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_SPOILER01);
		SpecialShotRegistry.registerSpecialShot(this, SPECIAL_SPOILER02);
	}

	@Override
	public void spellcard_main()
	{
		if(time < 20)
		{
			if(target != null)
			{
				level().playSound(null, user.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.NEUTRAL, 2.0F, 1.9F);// 音を出す
				THShotLib.createCircleShot(user, user, pos(targetPosX, targetPosY, targetPosZ), getVecFromAngle(card.getYRot(), card.getXRot()), 2.0D, 0.2D, -0.1D, gravity_Zero(), shot(FORM_SCALE, rand.nextInt(7), 0.3F, 3.0F, 6, 70, SPECIAL_SPOILER01), 7, 1.5D, time * 3F);
			}
		}
		if(time > 40 && time < 60)
		{
			if(target != null)
			{
				level().playSound(null, user.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.NEUTRAL, 2.0F, 1.9F);// 音を出す
				THShotLib.createCircleShot(user, user, pos(targetPosX, targetPosY, targetPosZ), getVecFromAngle(card.getYRot(), card.getXRot()), 2.0D, 0.2D, -0.1D, gravity_Zero(), shot(FORM_SCALE, rand.nextInt(7), 0.3F, 3.0F, 6, 70, SPECIAL_SPOILER01), 7, 1.5D, -time * 3F);
			}
		}
	}

	@Override
	public void specialShot_move(Level level, int id, EntityTHShot shot)
	{
		switch(id)
		{
			case SPECIAL_SPOILER01:
				if(shot.tickCount >= 20)
				{
					if(shot.user != null)
					{
						shot.shotLimitSpeed = 1.0D;
						shot.shotAcceleration = 0.05D;

						double lengthToUserX = shot.user.getX() - shot.getX();
						double lengthToUserZ = shot.user.getZ() - shot.getZ();
						shot.setYRot((float)Math.atan2(lengthToUserX, lengthToUserZ) / 3.141593F * 180F);
						shot.setXRot((float)Math.atan2(shot.user.getY() + 1.0D - shot.getY(), Math.sqrt(lengthToUserX * lengthToUserX + lengthToUserZ * lengthToUserZ)) / 3.141593F * 180F);

						shot.setVector();

						if(shot.distanceTo(shot.user) < 1.5F)
						{
							if(!level.isClientSide)
							{
								shot.discard();
							}
						}
					}
				}
				break;
			case SPECIAL_SPOILER02:
				if(shot.tickCount >= 20)
				{
					if(shot.user != null)
					{
						shot.shotLimitSpeed = 1.0D;
						shot.shotAcceleration = 0.05D;

						double lengthToUserX = shot.user.getX() - shot.getX();
						double lengthToUserZ = shot.user.getZ() - shot.getZ();
						shot.setYRot((float)Math.atan2(lengthToUserX, lengthToUserZ) / 3.141593F * 180F);
						shot.setXRot((float)Math.atan2(shot.user.getY() + 1.0D - shot.getY(), Math.sqrt(lengthToUserX * lengthToUserX + lengthToUserZ * lengthToUserZ)) / 3.141593F * 180F);

						shot.setVector();
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
		switch(id)
		{
			case SPECIAL_SPOILER01:
				if(hitEntity instanceof LivingEntity living)
				{
					if(!living.is(shot.user))
					{
						living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 20, 1));// ウィザー
						living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 20, 1));// 攻撃低下
						shot.shotSpecial = SPECIAL_SPOILER02;
						return true;
					}
					else
					{
						return false;
					}
				}
				// 非 LivingEntity の場合はそのまま消滅
				return false;
			case SPECIAL_SPOILER02:
				if(shot.user != null && hitEntity.is(shot.user))
				{
					LivingEntity living = (LivingEntity)hitEntity;
					living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));// 回復
					living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 1));// 攻撃上昇
					return false;
				}
				else
				{
					return true;
				}
			default:
				return false;
		}
	}
}
