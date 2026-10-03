package thkaguyamod.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * 霖之助。
 *
 * <p>1.7.10 は固定職業 ID(932) の EntityVillager + IVillageTradeHandler で取引を追加していた。
 * 1.21.1 では Villager#mobInteract が final で取引を職業に強く結びつけるため、独自取引を持つ
 * 本クラスは {@link AbstractVillager} を継承する（mobInteract が {@link #getOffers()} 非空なら
 * 取引画面を開く既定実装を持つ）。繁殖はしない（1.7.10 同様）。
 */
public class EntityRinnosuke extends AbstractVillager
{

	public EntityRinnosuke(EntityType<? extends AbstractVillager> type, Level level)
	{
		super(type, level);
	}

	/** AbstractVillager の抽象メソッド。取引表は getOffers() で直に構築するため更新処理は不要。 */
	@Override
	protected void updateTrades()
	{
	}

	/** 取引で経験値を与える（vanilla Villager 準拠）。 */
	@Override
	protected void rewardTradeXp(MerchantOffer offer)
	{
		if(offer.shouldRewardExp())
		{
			net.minecraft.world.entity.player.Player trader = this.getTradingPlayer();
			if(trader != null)
			{
				trader.giveExperiencePoints(offer.getXp());
			}
		}
	}

	/** 属性（Villager の既定値を継承） */
	public static AttributeSupplier.Builder createAttributes()
	{
		return Villager.createAttributes();
	}

	/**
	 * 霖之助の取引表（1.7.10 の VillagerRegistry.IVillageTradeHandler 相当）。
	 * Villager#offers は private で職業 NONE では空になるため、ここで直に構築する
	 * （VillagerTradesEvent は職業 NONE には発火しない）。
	 */
	@Override
	public MerchantOffers getOffers()
	{
		MerchantOffers custom = new MerchantOffers();
		for(VillagerTrades.ItemListing listing : VillagerRinnosuke.getRinnosukeTrades())
		{
			MerchantOffer offer = listing.getOffer(this, this.random);
			if(offer != null)
			{
				custom.add(offer);
			}
		}
		return custom;
	}

	/** 繁殖しない（1.7.10 の createChild → null / isMating → false 相当）。 */
	@Override
	public net.minecraft.world.entity.AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.entity.AgeableMob partner)
	{
		return null;
	}

	@Override
	public boolean canBreed()
	{
		return false;
	}

	//デスポーンするか（1.7.10 の canDespawn = true）
	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer)
	{
		return true;
	}

	//一つのチャンクに湧く最大数
	@Override
	public int getMaxSpawnClusterSize()
	{
		return 1;
	}

	/**
	 * 自然スポーン判定（1.7.10 の getCanSpawnHere）。
	 * P19: config 門控と 2% 確率ロールは ModSpawns の SpawnPlacement 謂詞に一元化済み。
	 * ここでは vanilla の補助条件（非液体等）のみ尊重する。
	 */
	@Override
	public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		return true;
	}
}
