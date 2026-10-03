package thkaguyamod.entity.living;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.registry.DanmakuPatternRegistry;

/** 使い魔 */
public class EntityFamiliar extends EntityTHFairy
{

	/** 使い魔のコンストラクタ */
	public EntityFamiliar(EntityType<? extends EntityFamiliar> type, Level level)
	{
		super((EntityType<? extends EntityTHFairy>)(EntityType<?>)type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(0.6F, 0.6F) に相当。寸法は登録側でも指定する）
		lastTime = 0;

		xpReward = 0;//経験値の量

		setForm((byte)random.nextInt(3));
		// P0-4（audit-ai）: pattern 表が空の瞬間（DanmakuPatternRegistry は ModConfig の
		// Reloading で clear → 再登録するため理論上の空窓がある）に nextInt(0) を呼ぶと
		// IllegalArgumentException: bound must be positive で**この実体の生成そのものが失敗**する。
		// 1.7.10 は preInit で一度だけ構築していたため表は恒に非空だった（移植固有の窓）。
		// EntityTHFairy:45 と同じ Math.max(...,1) で下限を保証する（空表でも例外を出さない）。
		setPattern(random.nextInt(Math.max(DanmakuPatternRegistry.pattern.size(), 1)));

		lostTarget = 0;
		this.setSpeed(0.3D);
		this.setSpecies(this.SPECIES_SHIKIGAMI_FAMILIAR);
		this.isFlyingMode = true;

		this.setAttackDistance(8.0D);
		this.setDetectionDistance(0.0D);
		this.setFlyingHeight(2);
	}

	/** 属性（1.7.10 の applyEntityAttributes に相当） */
	public static AttributeSupplier.Builder createAttributes()
	{
		return EntityTHFairy.createAttributes();
	}

	//死んでいるときに呼ばれる（1.7.10 の onDeathUpdate: 即座に消滅）
	@Override
	protected void tickDeath()
	{
		if(!level().isClientSide)
		{
			discard();
		}
	}

	/**
	 * 弾を出したLivingEntityを返す（要は発射主）
	 *
	 * <p>1.7.10 は ridingEntity の連鎖をたどり、一番外側の LivingEntity（ただし使い魔は飛ばす）を
	 * 発射主としていた。1.21.1 では getVehicle() で同じ連鎖をたどる。
	 * @return 弾の発射主。いない場合はnullを返す
	 */
	@Override
	public LivingEntity getShooter()
	{
		// 何かに乗っている場合
		Entity riding = this.getVehicle();
		if(riding != null)
		{
			// 乗っているEntityが生物の場合
			if(riding instanceof LivingEntity currentEntity)
			{
				if(currentEntity.getHealth() <= 0.0F)
				{
					return null;
				}
				// 乗っているEntityが使い魔の場合
				if(riding instanceof EntityFamiliar)
				{
					do
					{
						if(currentEntity == null)
						{
							return null;
						}
						if(currentEntity.getVehicle() instanceof LivingEntity next)
						{
							currentEntity = next;
						}
						else
						{
							return null;
						}
					} while(currentEntity.getVehicle() instanceof EntityFamiliar);
					return currentEntity;
				}
				else
				{
					return currentEntity;
				}
			}
		}
		return null;
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		if(isTimeStopped())
		{
			return;
		}
		else
		{
			if(tickCount >= 1 && getShooter() == null)
			{
				if(!level().isClientSide)
				{
					discard();
				}
				return;
			}
			//1.7.10 は ridingEntity が EntityCreature ならその攻撃対象を引き継いでいた。
			//1.21.1 では PathfinderMob（旧 EntityCreature 相当）の getTarget() を引き継ぐ。
			if(this.getVehicle() instanceof net.minecraft.world.entity.PathfinderMob riding)
			{
				this.setTarget(riding.getTarget());
			}
			super.tick();
		}
	}

	//周りの妖精を呼び出すことができるか
	@Override
	protected boolean canFairyCall()
	{
		return false;
	}

	//妖精を呼び出せる最大距離
	@Override
	protected double getFairyCallDistance()
	{
		return 0.0D;
	}

	//倒れたときに落とすアイテム: 1.7.10 の dropFewItems は空実装

	//生きてるときに出す音
	@Override
	protected net.minecraft.sounds.SoundEvent getAmbientSound()
	{
		return null;
	}

	//攻撃を受けたときの音
	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource)
	{
		return null;
	}

	//倒れたときの音
	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound()
	{
		return null;
	}

	/**
	 * 使い魔が乗っているEntityの位置（1.7.10 の getMountedYOffset = 0.5D）。
	 */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float partialTick)
	{
		return new Vec3(0.0D, 0.5D, 0.0D);
	}

	/**
	 * 当たり判定を取るか返す（1.7.10 の canBeCollidedWith = false）。
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}

	/**
	 * TODO K3-review: 1.7.10 は getCollisionBox(Entity) / getBoundingBox() が null を返し、
	 * 当たり判定を完全に無効化していた。1.21.1 の getBoundingBox() は final で null を返せず、
	 * 代替として寸法を 0 にする必要がある（登録側の EntityType.Builder.sized(0, 0) で対応する想定）。
	 * canBeCollidedWith() = false で旧来の用途は概ね満たせるため、ここでは実装しない。
	 */

	/**
	 * 倒れたときは 1.7.10 の dropFewItems が空実装だったため、何も落とさない
	 * （getDropItem が point_item でも実質未使用）。
	 */
	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.damagesource.DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		//1.7.10 の dropFewItems は空実装
	}
}