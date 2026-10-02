package thkaguyamod.entity.living;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.item.ItemTHShot;

/** ひまわり妖精 */
public class EntitySunFlowerFairy extends EntityTHFairy
{

	/** ひまわり妖精のコンストラクタ */
	public EntitySunFlowerFairy(EntityType<? extends EntitySunFlowerFairy> type, Level level)
	{
		super((EntityType<? extends EntityTHFairy>)(EntityType<?>)type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(0.9F, 1.7F) に相当。寸法は登録側でも指定する）

		xpReward = 30;//経験値の量

		setForm((byte)10);
		this.setMaxHP(20.0F);
		this.setHealth(20.0F);
	}

	/** 属性（1.7.10 の applyEntityAttributes に相当） */
	public static AttributeSupplier.Builder createAttributes()
	{
		return EntityTHFairy.createAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D);
	}

	/**
	 * 1.7.10 の onSpawnWithEgg。使い魔（EntityFamiliar）を生成してこの妖精に騎乗させていた。
	 */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
			net.minecraft.world.entity.MobSpawnType spawnType, SpawnGroupData spawnGroupData)
	{
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

		// TODO K3-review: 1.7.10 は使い魔の設定として EntityFamiliar を出現させ、この妖精に
		// familiar.mountEntity(this) で騎乗させていた。1.21.1 では EntityFamiliar が EntityTHFairy を
		// 継承しているため、ここで参照すると型循環（サブクラスの相互参照）になる。実装は
		// startRiding を使う形に置き換え可能だが、そもそも EntityFamiliar 側の乗車処理
		// （旧 ridingEntity 走査）の移植が未完。確定までこの生成は行わない。
		//if(!level.isClientSide())
		//{
		//	EntityFamiliar familiar = new EntityFamiliar(ModEntities.FAMILIAR.get(), level.getLevel());
		//	familiar.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
		//	level.getLevel().addFreshEntity(familiar);
		//	familiar.startRiding(this);
		//}

		return data;
	}

	/** 毎tick呼ばれる処理 */
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
	}

	//自然スポーンするときに呼ばれる
	/** 1.7.10 の getDropItem() = power_item（ダメージを受けたプレイヤーがいる場合のみ） */
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
			this.spawnAtLocation(new net.minecraft.world.item.ItemStack(thkaguyamod.init.ModItems.POWER_UP_ITEM_SMALL.get()));

			this.dropShotItem(ItemTHShot.MEDIUM, random.nextInt(12), 7, 8, THShotLib.GREEN, 0, 0, 2);

			this.dropPowerUpItem(this.random.nextInt(20) + this.random.nextInt(3));

			this.dropPointItem(this.random.nextInt(20) + this.random.nextInt(3));

			// ５％の確立でエクステンドアイテムをドロップ
			if(random.nextInt(100) < 5)
			{
				this.dropExtendItem(this.pos(), this.angle(this.getYRot(), -90F));
			}
			// ２０％の確立でスペルカードアイテムをドロップ（エクステンドと重複はしない）
			else if(random.nextInt(100) < 25)
			{
				this.dropSpellCardItem(this.pos(), this.angle(this.getYRot(), -90F));
			}
		}
	}

	//一つのチャンクに湧く最大数
	@Override
	public int getMaxSpawnClusterSize()
	{
		return 3;
	}
}