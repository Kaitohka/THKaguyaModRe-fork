package thkaguyamod.entity.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.init.ModItems;

/**
 * ナズーリンのペンデュラム
 * 指定したブロックが近くにあるか探す
 */
public class EntityPendulum extends Entity
{
	//同期データ (1.7.10 の dataWatcher 16-17 に相当)
	private static final EntityDataAccessor<Integer> DATA_SEARCH_ANGLE = SynchedEntityData.defineId(EntityPendulum.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_SEARCH_COUNT = SynchedEntityData.defineId(EntityPendulum.class, EntityDataSerializers.INT);

	/** 使用者 */
	public Player user;
	private int mode;//ペンデュラムのモード
	//TODO K3-review: 1.7.10 の searchBlockID（ブロックID）は 1.21.1 に存在しない。
	//アイテムの登録名で照合するため未使用。
	private int searchCount;//探索したブロックの数
	private int searchLength;//探索する距離
	private int searchX;
	private int searchY;
	private int searchZ;
	private int searchMaxX;
	private int searchMaxY;
	private int searchMaxZ;
	private int count;

	/*
	 * 1.7.10 は「StatCollector.translateToLocal(block.getUnlocalizedName() + ".name")」で
	 * ブロックのローカライズ名を取得し、検索対象アイテムの表示名と文字列比較していた。
	 * これは言語・リソースパック依存で壊れやすい（＝旧版の悪い臭い）ため、
	 * 1.21.1 では「アイテムの登録名（ResourceLocation）」でブロックを照合する方式に修正した。
	 * searchBlockName は旧版との比較用に検索対象の登録名（namespace:path）を保持する。
	 */
	private String searchBlockName;
	private ItemStack searchItem;

	public EntityPendulum(EntityType<? extends EntityPendulum> type, Level level)
	{
		super(type, level);
		//TODO K3-review: 1.7.10 の preventEntitySpawning / yOffset は 1.21.1 に対応が無い。

		mode = 0;
		searchCount = 0;
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(0.1F, 0.1F) / yOffset = 0.0F に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.1F, 0.1F);
	}

	public EntityPendulum(EntityType<? extends EntityPendulum> type, Level level, Player player, int pmode, String name, ItemStack itemStack)
	{
		this(type, level);

		user = player;//使用者をuserに保存
		THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F, false, (double)user.getEyeHeight() - 0.5D);

		mode = pmode;
		if(mode == 1)
		{
			searchLength = 24;
		}
		else
		{
			searchLength = 12;
		}

		searchBlockName = name;
		searchItem = itemStack;
	}

	/** 生成時に一度だけ呼ばれる */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_SEARCH_ANGLE, 0);
		builder.define(DATA_SEARCH_COUNT, 0);
	}

	//押すことができるか
	@Override
	public boolean isPushable()
	{
		return false;
	}

	//ペンデュラムを終了した時の処理
	private void finish()
	{
		if(user instanceof Player entityPlayer)
		{
			if(entityPlayer.getAbilities().instabuild)
			{
				discard();
				return;
			}
		}
		if(user != null)
		{
			if(user.getInventory().add(new ItemStack(ModItems.NAZRIN_PENDULUM.get())) == false)
			{
				//インベントリが一杯なら使用者の目の前でアイテム化
				if(!level().isClientSide)
				{
					//1.7.10 の user.dropItem に相当
					user.drop(new ItemStack(ModItems.NAZRIN_PENDULUM.get()), false);
				}
			}
		}
		else//使用者がいない場合（再起動した際など）にはその場にアイテム化
		{
			if(!level().isClientSide)
			{
				spawnAtLocation(ModItems.NAZRIN_PENDULUM.get());
			}
		}
		discard();
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		super.tick();

		if(!level().isClientSide && user == null)
		{
			finish();
		}

		if(!level().isClientSide && user != null)
		{
			if(tickCount > 10 && user.isShiftKeyDown())
			{
				finish();
			}
			if(user.hurtTime > 0)
			{
				finish();
			}
		}

		//サーチの処理
		if(count % 60 == 0 && !level().isClientSide)
		{
			searchCount = 0;
			searchX = (int)getX() - searchLength;
			searchZ = (int)getZ() - searchLength;
			searchY = (int)getY() - searchLength;
			searchMaxX = searchX + searchLength * 2 + 1;
			searchMaxY = searchY + searchLength * 2 + 1;
			searchMaxZ = searchZ + searchLength * 2 + 1;

			for(int si = searchX; si < searchMaxX; si++)
			{
				for(int sk = searchZ; sk < searchMaxZ; sk++)
				{
					for(int sj = searchY; sj < searchMaxY; sj++)
					{
						BlockPos pos = new BlockPos(si, sj, sk);
						if(!level().isLoaded(pos))
						{
							continue;
						}
						BlockState state = level().getBlockState(pos);
						if(matchesSearchBlock(state))
						{
							searchCount++;
						}
					}
				}
			}
			if(searchCount > 32)
			{
				searchCount = 32;
			}
			setSearchCount(searchCount);
		}

		if(getSearchCount() != 0)
		{
			setSearchAngle(getSearchAngle() + (float)getSearchCount() / 3.0F);
			setYRot(getYRot() + 3.0F);
		}
		if(user != null)
		{
			THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F, false, (double)user.getEyeHeight() - 0.5D);
		}
		count++;
	}

	/**
	 * 検索対象ブロックかどうかを返す。
	 * 1.7.10 のローカライズ名比較に代えて、ブロックアイテムの登録名で照合する。
	 */
	private boolean matchesSearchBlock(BlockState state)
	{
		if(searchItem == null || searchItem.isEmpty())
		{
			return false;
		}
		Block block = state.getBlock();
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(searchItem.getItem());
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
		//ブロックに対応するアイテム（ItemBlock）が同じ登録名を持つ場合に一致とみなす
		if(itemId.equals(blockId))
		{
			return true;
		}
		//旧版は name（ローカライズ名）で比較していたため、名前ベースの互換経路も残す
		return searchBlockName != null && searchBlockName.equals(itemId.toString());
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbtTagCompound)
	{
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbtTagCompound)
	{
	}

	//サーチによる回転角度を設定する
	public void setSearchAngle(float angle)
	{
		this.entityData.set(DATA_SEARCH_ANGLE, (int)(angle * 100.0F));
	}

	//サーチによる回転角度を返す
	public float getSearchAngle()
	{
		return (float)this.entityData.get(DATA_SEARCH_ANGLE) / 100.0F;
	}

	//サーチで引っかかったブロックの数を設定する
	public void setSearchCount(int count)
	{
		this.entityData.set(DATA_SEARCH_COUNT, count);
	}

	//サーチで引っかかったブロックの数を返す
	public int getSearchCount()
	{
		return this.entityData.get(DATA_SEARCH_COUNT);
	}
}