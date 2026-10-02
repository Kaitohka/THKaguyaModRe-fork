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
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.item.ItemTHShot;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** ルーミア */
public class EntityRumia extends EntityDanmakuMob
{
	public EntityRumia(EntityType<? extends EntityRumia> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当。寸法は登録側で指定）

		xpReward = 160;//経験値の量
		noCulling = true;//中心が画面から外れても描画される（1.7.10 の ignoreFrustumCheck）

		this.setDanmakuPattern(NORMAL_ATTACK01);
		this.setMaxHP(36.0F);
		setHealth(36.0F);
		this.setDanmakuMobName("Rumia");
		this.setSpecies(this.SPECIES_YOUKAI);

		this.setAttackDistance(14.0D);
		this.setDetectionDistance(12.0D);
		this.setFlyingHeight(2);
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
			return 28;
		default:
			return -1;
		}
	}

	//死んでいるときに呼ばれる（1.7.10 の onDeathUpdate）
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
			moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 44.0F, 60);
			this.setAttackDistance(14.0D);
			this.setDetectionDistance(30.0D);
			this.setFlyingHeight(2);
			break;
		case SPELLCARD_ATTACK01:
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
			if(attackCounter == 1)
			{
				THShotLib.playShotSound(this);
				for(int i = 0; i < 1 + level; i++)
				{
					THShotLib.createCircleShot(this, pos(), angle(getYRot(), getXRot()), 2.0D, 0.4D, -0.3D, ShotData.shot(FORM_CIRCLE, BLUE, 10 + i), 12 + 3 * level );
				}
			}
			else if(attackCounter == 14)
			{
				THShotLib.playShotSound(this);
				for(int i = 0; i < 1 + level; i++)
				{
					THShotLib.createCircleShot(this, pos(), angle(getYRot(), getXRot()), 2.0D, 0.4D, -0.3D, ShotData.shot(FORM_CIRCLE, THShotLib.RED, 10 + i), 12 + 3 * level);
				}
			}
			else if(attackCounter == 24)
			{
				moveRight(0.3D, 20);
			}
			else if(attackCounter >= 62 && attackCounter < 67)
			{
				THShotLib.playShotSound(this);
				int i = attackCounter - 62;
				THShotLib.createCircleShot(this, pos(), angle(getYRot() + i * 5F, getXRot()), 0.2D + level * 0.1D, ShotData.shot(FORM_TINY, attackCounter % 7), 5 + level * 2);
			}
			else if(attackCounter == 67)
			{
				moveLeft(0.3D, 20);
			}
			if(attackCounter >= 127)
			{
				attackCounter = 0;
			}
			break;
		case SPELLCARD_ATTACK01:
			if(attackCounter == 1)
			{
				useSpellCard(getUsingSpellCardNo());
			}
			else if(attackCounter < 60 && attackCounter % 9 == 0)
			{
				THShotLib.createCircleShot(this, pos(), angle(getYRot(), getXRot()), 0.1D + level * 0.1D, ShotData.shot(FORM_TINY, WHITE), 15 + level * 3);
				THShotLib.playShotSound(this);
			}
			else if(attackCounter >= 120)
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
			this.dropShotItem(ItemTHShot.SMALL, 8 + random.nextInt(2), 5, 32, BLUE, 0, 0, 2);

			this.dropPowerUpItem(8 + this.random.nextInt(5) + this.random.nextInt(1));

			this.dropPointItem(8 + this.random.nextInt(5) + this.random.nextInt(1));
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

	//自然スポーンするときに呼ばれ、trueならスポーンする
	@Override
	public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		//ゾンビなどと同じ湧き条件
		return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && this.isValidLightLevel();
	}
}