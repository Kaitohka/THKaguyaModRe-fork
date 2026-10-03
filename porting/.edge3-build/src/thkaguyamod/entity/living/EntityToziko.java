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
import thkaguyamod.spellcard.THSC_GagoujiCyclone;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** 屠自古 */
public class EntityToziko extends EntityDanmakuMob
{

	public EntityToziko(EntityType<? extends EntityToziko> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当。寸法は登録側で指定）

		xpReward = 160;//経験値の量

		this.setDanmakuPattern(NORMAL_ATTACK01);
		this.setMaxHP(40.0F);
		setHealth(40.0F);
		this.setDanmakuMobName("Soga no Toziko");
		this.setSpecies(this.SPECIES_PHANTOM_GHOST);

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
			return 40;
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
			if(attackCounter % 80 < 20)
			{
				THShotLib.playShotSound(this);
				for(int i = 0; i < 1 + level; i++)
				{
					THShotLib.createShot(this, this, THShotLib.pos_Random(pos(), attackCounter * 0.2D), THShotLib.angle_Random(), 0F, 0.3D, 0.0D, -0.03D, gravity_Zero(), ShotData.shot(FORM_ARROW, BLUE, 1, 20, THSC_GagoujiCyclone.SPECIAL_TOZIKO01));
				}
			}
			else if(attackCounter == 24)
			{
				if(random.nextInt(2) == 0)
				{
					this.moveRight(random.nextDouble() * 0.3D + 0.2D, 16);
				}
				else
				{
					this.moveLeft(random.nextDouble() * 0.3D + 0.2D, 16);
				}
			}
			if(attackCounter >= 80)
			{
				attackCounter = 0;
			}
			break;
		case SPELLCARD_ATTACK01:
			if(attackCounter == 1)
			{
				useSpellCard(getUsingSpellCardNo());
			}
			else if(attackCounter >= 200)
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
			this.dropShotItem(ItemTHShot.ARROW, 8 + random.nextInt(2), 5, 32, YELLOW, 0, THSC_GagoujiCyclone.SPECIAL_GAGOUJI01, 2);

			this.dropPowerUpItem(8 + this.random.nextInt(5) + this.random.nextInt(1));

			this.dropPointItem(8 + this.random.nextInt(5) + this.random.nextInt(1));
		}
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
		//P19: 自然生成の全条件（config・確率・地形・明るさ）は ModSpawns の SpawnPlacement 謂詞に一元化。
		//ここで再判定すると確率が二重に掛かる（妖精 50%→25% 等）ため、実体側は常に true。
		return true;
	}
}