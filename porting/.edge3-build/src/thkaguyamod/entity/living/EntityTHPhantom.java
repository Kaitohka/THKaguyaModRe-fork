package thkaguyamod.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.item.ItemTHShot;
import thkaguyamod.registry.DanmakuPatternRegistry;

/** 幽霊 */
public class EntityTHPhantom extends EntityTHFairy
{

	public EntityTHPhantom(EntityType<? extends EntityTHPhantom> type, Level level)
	{
		super((EntityType<? extends EntityTHFairy>)(EntityType<?>)type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.0F) に相当。寸法は登録側でも指定する）
		lastTime = 0;

		xpReward = 5;//経験値の量

		setForm((byte)random.nextInt(8));
		setPattern(random.nextInt(DanmakuPatternRegistry.pattern.size()));

		lostTarget = 0;
		this.setSpeed(0.18D);
		this.setSpecies(this.SPECIES_PHANTOM);
		isFlyingMode = true;

		this.setAttackDistance(6.0D);
		this.setDetectionDistance(30.0D);
		this.setFlyingHeight(3);
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

	//常時呼ばれる
	@Override
	public void tick()
	{
		//体力がないなら動かない
		if(this.getHealth() <= 0)
		{
			setDeltaMovement(0.0D, 0.0D, 0.0D);
		}

		if(isTimeStopped())
		{
			return;
		}
		super.tick();
		if(this.attackCounter > danmakuSpan)
		{
			attackCounter = 0;
		}

		//強い光で消滅（1.7.10 の getBrightness / canBlockSeeTheSky）
		float f = this.getLightLevelDependentMagicValue();

		if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F
				&& this.level().canSeeSky(BlockPos.containing(this.getX(), this.getY(), this.getZ())))
		{
			setHealth(0.0F);
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

	//倒れたときに落とすアイテム
	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.damagesource.DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		super.dropCustomDeathLoot(level, damageSource, hasBeenAttackedByPlayer);

		if(getForm() < 0)
		{
			return;
		}

		if(hasBeenAttackedByPlayer)
		{
			//1.7.10 の getDropItem() = power_item
			this.spawnAtLocation(new ItemStack(ModItems.POWER_UP_ITEM_SMALL.get()));

			this.dropShotItem(ItemTHShot.SMALL, random.nextInt(2), 3, 0, THShotLib.RED, 0, 0, 2);

			this.dropPowerUpItem(this.random.nextInt(5) + this.random.nextInt(1));

			this.dropPointItem(this.random.nextInt(5) + this.random.nextInt(1));

			//1.7.10 の dropItem(THKaguyaItems.soulTorch, ...)
			this.spawnAtLocation(new ItemStack(ModItems.SOUL_TORCH.get(), (random.nextInt(10) / 9) + this.random.nextInt(2)));
		}
	}

	//一つのチャンクに湧く最大数
	@Override
	public int getMaxSpawnClusterSize()
	{
		return 10;
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
				//1.7.10 は skylightSubtracted を一時的に 10 にして再取得していた。1.21.1 では
				//getMaxLocalRawBrightness(pos, 10) が同じ意味を持つ。
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

	//Entityの発する音の大きさ
	@Override
	protected float getSoundVolume()
	{
		return 0.0F;
	}
}