package thkaguyamod.entity.living;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import thkaguyamod.init.ModItems;

/**
 * 霖之助の村人取引。
 *
 * <p>1.7.10 は {@code VillagerRegistry.IVillageTradeHandler#manipulateTradesForVillager} を実装し、
 * 登録は {@code VillagerRegistry.instance().registerVillageTradeHandler(villagerRinnosukeId, ...)} だった。
 * 1.21.1 では {@link VillagerTrades.ItemListing} のリストを提供する形に変わり、
 * 登録は NeoForge の {@code VillagerTradesEvent} で行う。
 *
 * <p>TODO K3-review: 登録タイミングは本バッチ外（主控が VillagerTradesEvent で
 * {@link #getRinnosukeTrades()} を profession に紐付ける）。専用職業 rinnosuke の登録も併せて必要。
 */
public class VillagerRinnosuke
{

	/**
	 * 1.7.10 の manipulateTradesForVillager と同等の取引リストを返す。
	 *
	 * <p>旧: MerchantRecipe(買う物 ItemStack, 売る物 Item/ItemStack)。
	 * 新: MerchantOffer(ItemCost(買う物), 売る物 ItemStack, maxUses, xp, priceMultiplier)。
	 * 旧 MerchantRecipe の既定 maxUses は 7、xp は 1 だったのでそれに合わせる。
	 */
	public static List<VillagerTrades.ItemListing> getRinnosukeTrades()
	{
		List<VillagerTrades.ItemListing> trades = new ArrayList<>();

		trades.add(offer(new ItemCost(ModItems.POWER_UP_ITEM_SMALL.get(), 22),
				new ItemStack(ModItems.SPIRITUAL_STRIKE_TALISMAN.get())));
		trades.add(offer(new ItemCost(Items.IRON_INGOT, 20),
				new ItemStack(ModItems.MINI_HAKKERO.get())));
		trades.add(offer(new ItemCost(Items.NETHER_STAR, 1),
				new ItemStack(ModItems.MAGIC_BROOM.get())));
		trades.add(offer(new ItemCost(ModItems.POWER_UP_ITEM_SMALL.get(), 10),
				new ItemStack(ModItems.POINT_ITEM.get(), 9)));
		trades.add(offer(new ItemCost(ModItems.POINT_ITEM.get(), 10),
				new ItemStack(ModItems.POWER_UP_ITEM_SMALL.get(), 9)));
		trades.add(offer(new ItemCost(Items.REDSTONE, 1),
				new ItemStack(ModItems.POWER_UP_ITEM_SMALL.get(), 64)));
		//1.7.10 の Items.dye(meta 4) = ラピスラズリ
		trades.add(offer(new ItemCost(Items.LAPIS_LAZULI, 1),
				new ItemStack(ModItems.POINT_ITEM.get(), 64)));
		trades.add(offer(new ItemCost(Items.GOLD_INGOT, 1),
				new ItemStack(Items.ENDER_PEARL, 1)));

		return trades;
	}

	private static VillagerTrades.ItemListing offer(ItemCost cost, ItemStack result)
	{
		return new VillagerTrades.ItemListing()
		{
			@Override
			public MerchantOffer getOffer(Entity trader, RandomSource random)
			{
				return new MerchantOffer(cost, result, 7, 1, 0.05F);
			}
		};
	}

	private VillagerRinnosuke() {}
}