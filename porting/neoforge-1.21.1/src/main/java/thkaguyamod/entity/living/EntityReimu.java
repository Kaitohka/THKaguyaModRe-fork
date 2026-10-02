package thkaguyamod.entity.living;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.spellcard.EntitySpellCard;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** 楽園の素敵な巫女　博麗 霊夢 */
public class EntityReimu extends EntityDanmakuMob
{

	public EntityReimu(EntityType<? extends EntityReimu> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当。寸法は登録側で指定）

		this.xpReward = 250;//経験値の量

		this.setDanmakuMobName("Reimu Hakurei");
		this.setSpecies(this.SPECIES_HUMAN);

		this.setDanmakuPattern(SPELLCARD_ATTACK01);
		this.setMaxHP(74.0F);
		this.setHealth(74.0F);
		this.setSpeed(0.4F);
		this.setAttackDistance(14.0D);
		this.setDetectionDistance(0.0D);
		this.setFlyingHeight(4);
		this.isFlyingMode = false;

		this.isSpellCardMode = false;
		this.attackInterval = 0;
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
		switch(this.getDanmakuPattern())
		{
			case SPELLCARD_ATTACK01:
				return EntitySpellCard.SC_REIMU_MusouFuuin;
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
		switch(getDanmakuPattern())
		{
			case NORMAL_ATTACK01:
				setFlyingHeight(2);
				moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 60.0F, 160);
				break;
			case SPELLCARD_ATTACK01:
				moveDanmakuAttack(ATTACK_END, 90, 40.0F, 160);
				break;
			default:
				if(deathTime % 6 == 0)
				{
					THShotLib.explosionEffect(level(), getX(), getY(), getZ(), 1.0F + deathTime * 0.1F);
				}
				super.tickDeath();
				break;
		}
	}

	/** 常時呼ばれる */
	@Override
	public void tick()
	{
		super.tick();
	}

	/**
	 * 弾幕のパターンを記述
	 * @param level : EASY～LUNATICの難易度
	 */
	@Override
	public void danmakuPattern(int level)
	{
		switch(getDanmakuPattern())
		{
			case NORMAL_ATTACK01:
				//danmaku01(look, level);
				break;
			case SPELLCARD_ATTACK01:
				if(attackCounter == 1)
				{
					this.useSpellCard(EntitySpellCard.SC_REIMU_MusouFuuin);
				}
				else if(attackCounter >= 140)
				{
					attackCounter = 0;
				}
				break;
			default:
				break;
		}
	}

	//周りの妖精を呼び出すことができるか
	@Override
	protected boolean canFairyCall()
	{
		return false;
	}

	/**
	 * 無敵かどうかを返す（1.7.10 の isEntityInvulnerable）
	 */
	@Override
	public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource damageSource)
	{
		if(getDanmakuPattern() == NOT_ATTACK)
		{
			return true;
		}
		return super.isInvulnerableTo(damageSource);
	}

	//倒れたときに落とすアイテム
	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.damagesource.DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		super.dropCustomDeathLoot(level, damageSource, hasBeenAttackedByPlayer);

		if(this.isSpellCardAttack())
		{
			int j = 12;
			int k;
			ItemEntity item;
			Vec3 vec3;

			for (k = 0; k < j; ++k)
			{
				item = this.spawnAtLocation(new ItemStack(ModItems.POWER_UP_ITEM_SMALL.get()));
				if(item == null)
				{
					continue;
				}
				item.setYRot(k * 30F);
				item.setXRot(-60F);
				vec3 = THShotLib.getVecFromAngle(item.getYRot(), item.getXRot(), 0.5F);
				item.setPos(item.getX() + vec3.x * 2.0D, item.getY(), item.getZ() + vec3.z * 2.0D);

				item.setDeltaMovement(vec3);
			}

			for (k = 0; k < j; ++k)
			{
				item = this.spawnAtLocation(new ItemStack(ModItems.POINT_ITEM.get()));
				if(item == null)
				{
					continue;
				}
				item.setYRot(k * 30F + 15F);
				item.setXRot(-60F);
				vec3 = THShotLib.getVecFromAngle(item.getYRot(), item.getXRot(), 0.3F);
				item.setPos(item.getX() + vec3.x * 2.0D, item.getY(), item.getZ() + vec3.z * 2.0D);

				item.setDeltaMovement(vec3);
			}
		}
	}

	@Override
	public int getMaxSpawnClusterSize()
	{
		return 1;
	}

	//自然スポーンするときに呼ばれる。trueならスポーンする
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType)
	{
		return false;
	}

	/**
	 * 1.7.10 の getDropItem() = null（自然ドロップなし）。
	 * getDropItem に相当するフックは 1.21.1 に無いため、dropCustomDeathLoot 側で処理している。
	 */
}