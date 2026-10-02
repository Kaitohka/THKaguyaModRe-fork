package thkaguyamod.entity.living;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.spellcard.EntitySpellCard;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/**
 * 十六夜 咲夜
 *
 * <p>1.7.10 は {@code IMerchant} を実装していた。1.21.1 の対応インタフェースは
 * {@link net.minecraft.world.item.trading.Merchant}（同名のメソッドが並ぶが、
 * MerchantRecipeList → MerchantOffers、MerchantRecipe → MerchantOffer に変わっている）。
 */
public class EntitySakuya extends EntityDanmakuMob implements IDanmakuMob, Merchant
{
	private Player buyingPlayer;
	private MerchantOffers buyingList;
	private float field_82191_bN;
	public static final Map<Item, Tuple> villagersSellingList = new HashMap<>();

	public EntitySakuya(EntityType<? extends EntitySakuya> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当。寸法は登録側で指定）

		this.xpReward = 250;//経験値の量

		this.setDanmakuMobName("Sakuya Izayoi");
		this.setSpecies(this.SPECIES_HUMAN);

		this.setDanmakuPattern(NOT_ATTACK);
		this.setMaxHP(44.0F);
		this.setHealth(44.0F);
		this.setSpeed(0.5F);
		this.setAttackDistance(14.0D);
		this.setDetectionDistance(0.0D);
		this.setFlyingHeight(0);
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

	//Entityの初期処理（1.7.10 の entityInit で取引リストを作っていた）
	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		buyingList = new MerchantOffers();
		//1.7.10: new MerchantRecipe(new ItemStack(shot_material,16,0), new ItemStack(silver_knife,1,0))
		//  → 1.21.1: ItemCost(買う物, 個数) + 結果 ItemStack
		buyingList.add(new MerchantOffer(new ItemCost(ModItems.SHOT_MATERIAL.get(), 16),
				new ItemStack(ModItems.SILVER_KNIFE_BLUE.get()), 12, 1, 0.05F));
		buyingList.add(new MerchantOffer(new ItemCost(ModItems.SHOT_MATERIAL.get(), 64),
				new ItemStack(ModItems.SAKUYAS_STOPWATCH.get()), 12, 1, 0.05F));
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
				return EntitySpellCard.SC_SAKUYA_SatuzinDoll;
			default:
				return -1;
		}
	}

	//死んでいるときに呼ばれる（1.7.10 の onDeathUpdate）
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
					this.useSpellCard(getUsingSpellCardNo());
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

	//保存したデータを読み込む（1.7.10 の readEntityFromNBT の "Offers"）
	@Override
	public void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		super.readAdditionalSaveData(nbtTagCompound);
		if (nbtTagCompound.contains("Offers", 10))
		{
			RegistryOps<net.minecraft.nbt.Tag> ops = RegistryOps.create(NbtOps.INSTANCE, this.registryAccess());
			this.buyingList = MerchantOffers.CODEC.parse(ops, nbtTagCompound.getCompound("Offers")).result().orElse(null);
		}
	}

	//保存するデータを書き込む（1.7.10 の writeEntityToNBT の "Offers"）
	@Override
	public void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		super.addAdditionalSaveData(nbtTagCompound);
		if (this.buyingList != null)
		{
			RegistryOps<net.minecraft.nbt.Tag> ops = RegistryOps.create(NbtOps.INSTANCE, this.registryAccess());
			//TODO K3-review: 1.7.10 は "Offers" に MerchantRecipeList.getRecipiesAsTags() を入れていた。
			//1.21.1 は MerchantOffers.CODEC（"Recipes" リスト + ItemCost 形式）に変わっており、
			//旧 NBT とは非互換（セーブデータ移行は modid 変更により対象外）。
			nbtTagCompound.put("Offers", MerchantOffers.CODEC.encodeStart(ops, this.buyingList).result().orElse(new CompoundTag()));
		}
	}

	/**
	 * 右クリックをされたときの処理
	 * （1.7.10 の interact → player.displayGUIMerchant(this, "entity.Sakuya.name")）
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand)
	{
		ItemStack itemstack = player.getInventory().getSelected();
		boolean flag = itemstack != null && itemstack.getItem() instanceof net.minecraft.world.item.SpawnEggItem;

		if (!flag && this.isAlive() && !player.isShiftKeyDown())
		{
			if (!this.level().isClientSide)
			{
				this.setTradingPlayer(player);
				//1.21.1 の Merchant#openTradingScreen（1.7.10 の displayGUIMerchant に相当）
				openTradingScreen(player, Component.translatable("entity.Sakuya.name"), 1);
			}

			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}
		else
		{
			return super.mobInteract(player, hand);
		}
	}

	//=========================================================================================//
	//                          Merchant（1.7.10 の IMerchant）実装                            //
	//=========================================================================================//

	@Override
	public void setTradingPlayer(Player player)
	{
		this.buyingPlayer = player;
	}

	@Override
	public Player getTradingPlayer()
	{
		return this.buyingPlayer;
	}

	@Override
	public MerchantOffers getOffers()
	{
		if(this.buyingList == null)
		{
			this.buyingList = new MerchantOffers();
		}
		return this.buyingList;
	}

	@Override
	public void overrideOffers(MerchantOffers merchantOffers)
	{
		//1.7.10 の setRecipes は実質空実装だった（コメントアウトのみ）
	}

	@Override
	public void notifyTrade(MerchantOffer merchantOffer)
	{
		merchantOffer.increaseUses();
		//1.7.10 の useRecipe: incrementToolUses() + livingSoundTime のリセット
		this.ambientSoundTime = -this.getAmbientSoundInterval();

		if (this.buyingPlayer != null)
		{
			//1.7.10 の "mob.villager.yes"（1.21.1 の VILLAGER_YES）
			this.playSound(SoundEvents.VILLAGER_YES, this.getSoundVolume(), this.getVoicePitch());
		}
	}

	@Override
	public void notifyTradeUpdated(ItemStack itemStack)
	{
		if (!this.level().isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20)
		{
			this.ambientSoundTime = -this.getAmbientSoundInterval();

			//1.7.10 の mob.villager.yes / no
			this.playSound(itemStack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES,
					this.getSoundVolume(), this.getVoicePitch());
		}
	}

	@Override
	public int getVillagerXp()
	{
		return 0;
	}

	@Override
	public void overrideXp(int xp)
	{
	}

	@Override
	public boolean showProgressBar()
	{
		return false;
	}

	@Override
	public SoundEvent getNotifyTradeSound()
	{
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public boolean isClientSide()
	{
		return this.level().isClientSide;
	}

	/**
	 * based on the villagers profession add items, equipment, and recipies adds par1 random items to the list of things
	 * that the villager wants to buy. (at most 1 of each wanted type is added)
	 */
	private void addDefaultEquipmentAndRecipies(int par1)
	{
		if (this.buyingList != null)
		{
			this.field_82191_bN = net.minecraft.util.Mth.sqrt((float)this.buyingList.size()) * 0.2F;
		}
		else
		{
			this.field_82191_bN = 0.0F;
		}

		MerchantOffers merchantrecipelist;
		merchantrecipelist = new MerchantOffers();

		func_146091_a(merchantrecipelist, Items.WHEAT, this.random, this.adjustProbability(0.9F));
		func_146091_a(merchantrecipelist, net.minecraft.world.item.Item.byBlock(Blocks.WHITE_WOOL), this.random, this.adjustProbability(0.5F));

		Collections.shuffle(merchantrecipelist);

		if (this.buyingList == null)
		{
			this.buyingList = new MerchantOffers();
		}

		for (int l = 0; l < par1 && l < merchantrecipelist.size(); ++l)
		{
			this.buyingList.add(merchantrecipelist.get(l));
		}
	}

	public static void func_146091_a(MerchantOffers p_146091_0_, Item p_146091_1_, net.minecraft.util.RandomSource p_146091_2_, float p_146091_3_)
	{
		if (p_146091_2_.nextFloat() < p_146091_3_)
		{
			p_146091_0_.add(new MerchantOffer(new ItemCost(p_146091_1_, func_146092_b(p_146091_1_, p_146091_2_)),
					new ItemStack(Items.EMERALD), 12, 1, 0.05F));
		}
	}

	private static int func_146092_b(Item p_146092_0_, net.minecraft.util.RandomSource p_146092_1_)
	{
		Tuple tuple = villagersSellingList.get(p_146092_0_);
		return tuple == null ? 1 : (((Integer)tuple.getA()).intValue() >= ((Integer)tuple.getB()).intValue()
				? ((Integer)tuple.getA()).intValue()
				: ((Integer)tuple.getA()).intValue() + p_146092_1_.nextInt(((Integer)tuple.getB()).intValue() - ((Integer)tuple.getA()).intValue()));
	}

	/**
	 * Adjusts the probability of obtaining a given recipe being offered by a villager
	 */
	private float adjustProbability(float par1)
	{
		float f1 = par1 + this.field_82191_bN;
		return f1 > 0.9F ? 0.9F - (f1 - 0.9F) : f1;
	}

	static
	{
		villagersSellingList.put(Items.COAL, new Tuple<>(16, 24));
		villagersSellingList.put(Items.IRON_INGOT, new Tuple<>(8, 10));
		villagersSellingList.put(Items.GOLD_INGOT, new Tuple<>(8, 10));
		villagersSellingList.put(Items.DIAMOND, new Tuple<>(4, 6));
		villagersSellingList.put(Items.PAPER, new Tuple<>(24, 36));
		villagersSellingList.put(Items.BOOK, new Tuple<>(11, 13));
	}

	/**
	 * TODO K3-review: 1.7.10 は VillagerRegistry.manageVillagerTrades を呼んでいたが、
	 * 1.21.1 の対応は {@link VillagerTrades} ではなく NeoForge の VillagerTradesEvent（主控側で接続）。
	 * addDefaultEquipmentAndRecipies は現状どこからも呼ばれない（1.7.10 でも実質未使用）。
	 */
}