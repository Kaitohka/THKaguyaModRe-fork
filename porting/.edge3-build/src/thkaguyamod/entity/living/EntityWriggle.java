package thkaguyamod.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.item.ItemTHShot;
import thkaguyamod.spellcard.THSC_LittleBugStorm;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** リグル */
public class EntityWriggle extends EntityDanmakuMob
{

	public EntityWriggle(EntityType<? extends EntityWriggle> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当。寸法は登録側で指定）

		xpReward = 140;//経験値の量

		this.setDanmakuPattern(NORMAL_ATTACK01);
		this.setMaxHP(30.0F);
		setHealth(30.0F);
		this.setDanmakuMobName("Wriggle");
		this.setSpecies(this.SPECIES_YOUKAI_BUG);

		this.setAttackDistance(14.0D);
		this.setDetectionDistance(12.0D);
		this.setFlyingHeight(3);
		this.isFlyingMode = true;
	}

	/** 属性（1.7.10 の applyEntityAttributes に相当） */
	public static AttributeSupplier.Builder createAttributes()
	{
		return EntityDanmakuMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 2.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.25D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
				.add(Attributes.FOLLOW_RANGE, 30.0D);
	}

	/**
	 * 使用しているスペルカードNoを返す
	 * @return 使用しているスペルカードNo
	 */
	@Override
	public int getUsingSpellCardNo()
	{
		switch(getDanmakuPattern())
		{
		case SPELLCARD_ATTACK01:
			return 35;
		case SPELLCARD_ATTACK02:
			return 34;
		default:
			return -1;
		}
	}

	//死んでいるときに呼ばれる（1.7.10 の onDeathUpdate）
	/** 多段階ボス：HP 0 では真に死なず、tickDeath() が moveDanmakuAttack で次段階へ進む */
	@Override
	protected boolean canInterceptDeath()
	{
		return getDanmakuPattern() != NOT_ATTACK && getDanmakuPattern() != ATTACK_END;
	}

	@Override
	protected void tickDeath()
	{
		if(tickCount <= lastTime)
		{
			return;
		}

		switch(getDanmakuPattern())
		{
		case NORMAL_ATTACK01:
			moveDanmakuAttack(SPELLCARD_ATTACK01, 20, 36.0F, 40);
			this.setAttackDistance(16.0D);
			this.setDetectionDistance(30.0D);
			this.setFlyingHeight(2);
			break;
		case SPELLCARD_ATTACK01:
			moveDanmakuAttack(SPELLCARD_ATTACK02, 60, 42.0F, 80);
			this.setAttackDistance(15.0D);
			this.setDetectionDistance(30.0D);
			this.setFlyingHeight(4);
			break;
		case SPELLCARD_ATTACK02:
			moveDanmakuAttack(ATTACK_END, 30, 0F, 60);
			break;
		default:
			if(deathTime % 9 == 0)
			{
				THShotLib.explosionEffect(level(), getX(), getY(), getZ(), 1.0F + deathTime * 0.1F);
			}
			super.tickDeath();
		}
	}

	@Override
	public void danmakuPattern(int level)
	{
		switch(getDanmakuPattern())
		{
		case NORMAL_ATTACK01:
			Vec3 look = this.getLookAngle();
			Vec3 angle;
			Vec3 angle2;
			if(attackCounter < 30)
			{
				if(attackCounter % 15 >= 1  && attackCounter % 15 < 8)
				{
					angle = THShotLib.getRotationVectorFromAngle(this.getYRot(), this.getXRot(), (attackCounter % 8) * 10F, 1.0);
					THShotLib.playShotSound(this);
					THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.2, ShotData.shot(FORM_RICE, YELLOW, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_01));
					if(level >= HARD)
					{
						angle2 = THShotLib.getRotationVectorFromAngle(this.getYRot(), this.getXRot(), 5F + (attackCounter % 8) * 10F, 1.0);
						//TODO K3-review: 1.7.10 も angle2 を計算したあと angle を渡していた（angle2 は未使用）。
						//旧コードのままの挙動を維持する。
						THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.4, ShotData.shot(FORM_RICE, YELLOW, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_01));
					}
				}
				else if(attackCounter % 15 >= 8 && attackCounter % 15 < 15)
				{
					angle = THShotLib.getRotationVectorFromAngle(this.getYRot(), this.getXRot(),  - (attackCounter % 8) * 10F, 1.0);
					THShotLib.playShotSound(this);
					THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.2, ShotData.shot(FORM_RICE, AQUA, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_02));
					if(level >= HARD)
					{
						angle2 = THShotLib.getRotationVectorFromAngle(this.getYRot(), this.getXRot(), -5F - (attackCounter % 8) * 10F, 1.0);
						THShotLib.createShot(this, pos(), angle, 1.0, 0.0, -0.4, ShotData.shot(FORM_RICE, AQUA, 3, 15, THSC_LittleBugStorm.SP_ATTACK01_02));
					}
				}
			}
			else if(attackCounter == 24)
			{
				moveRight(0.3D, 20);
			}
			else if(attackCounter >= 62 && attackCounter < 72)
			{
				THShotLib.playShotSound(this);
				double firstSpeed = 0.3 + 0.1 * level;
				double limit = 0.3;
				double acceleration = -(attackCounter - 61) * 0.005;
				THShotLib.createWideShot(this, pos(), look, firstSpeed, limit, acceleration, ShotData.shot(FORM_SMALL, YELLOW, 10), 5, 90F);
			}
			else if(attackCounter == 67)
			{
				moveLeft(0.3D, 20);
			}
			if(attackCounter >= 147)
			{
				attackCounter = 0;
			}
			break;
		case SPELLCARD_ATTACK01:
			Vec3 lookSC = this.getLookAngle();
			if(attackCounter % 15 == 1 && attackCounter < 120)
			{
				if(level == NORMAL)
				{
					THShotLib.createSphereShot(this, pos(), lookSC, 0.2D, ShotData.shot(FORM_MEDIUM, BLUE), 16, tickCount * 13F);
				}
				else if(level == HARD)
				{
					THShotLib.createSphereShot(this, pos(), lookSC, 0.3D, ShotData.shot(FORM_MEDIUM, BLUE), 24, tickCount * 13F);
				}
				else if(level == LUNATIC)
				{
					THShotLib.createSphereShot(this, pos(), lookSC, 0.4D, ShotData.shot(FORM_MEDIUM, BLUE), 32, tickCount * 13F);
				}
			}

			if(attackCounter == 1)
			{
				useSpellCard(getUsingSpellCardNo());
			}
			else if(attackCounter >= 180)
			{
				attackCounter = 0;
			}
			break;
		case SPELLCARD_ATTACK02:
			if(attackCounter == 1)
			{
				useSpellCard(getUsingSpellCardNo());
			}
			else if(attackCounter >= 260)
			{
				attackCounter = 0;
			}
			break;
		default:
			break;
		}
	}

	//倒れたときに落とすアイテム
	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.damagesource.DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		super.dropCustomDeathLoot(level, damageSource, hasBeenAttackedByPlayer);

		if(hasBeenAttackedByPlayer)
		{
			if(this.isSpellCardAttack())
			{
				this.dropShotItem(ItemTHShot.RICE, 8 + random.nextInt(2), 5, 32, YELLOW, 0, 0, 2);

				this.dropPowerUpItem(8 + this.random.nextInt(5) + this.random.nextInt(1));

				this.dropPointItem(8 + this.random.nextInt(5) + this.random.nextInt(1));
			}
		}
	}

	@Override
	public int getMaxSpawnClusterSize()
	{
		return 1;
	}

	/**
	 * Checks to make sure the light is not too bright where the mob is spawning
	 * （1.7.10 の isValidLightLevel）
	 */
	protected boolean isValidLightLevel()
	{
		int i = Mth.floor(this.getX());
		int j = Mth.floor(this.getBoundingBox().minY);
		int k = Mth.floor(this.getZ());

		if (this.level().getBrightness(LightLayer.SKY, new BlockPos(i, j, k)) > this.random.nextInt(32))
		{
			return false;
		}
		else
		{
			int l = this.level().getBrightness(LightLayer.BLOCK, new BlockPos(i, j, k));

			if (this.level().isThundering())
			{
				//1.7.10 の skylightSubtracted = 10 相当
				l = this.level().getMaxLocalRawBrightness(new BlockPos(i, j, k), 10);
			}

			return l <= this.random.nextInt(8);
		}
	}

	/**
	 * 自然スポーンするときに呼ばれる
	 * @return trueならスポーン成功
	 */
	@Override
		public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		//P19: 自然生成の全条件（config・確率・地形・明るさ）は ModSpawns の SpawnPlacement 謂詞に一元化。
		//ここで再判定すると確率が二重に掛かる（妖精 50%→25% 等）ため、実体側は常に true。
		return true;
	}

	/**
	 * Gets how bright this entity is.（1.7.10 の getBrightness）
	 */
	@Override
	public float getLightLevelDependentMagicValue()
	{
		return 1.0F;
	}
}