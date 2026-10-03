package thkaguyamod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;

/** 弾の素（アイテム付き） */
public class EntityTHItem extends EntityShotMaterial
{
	public static final byte ITEM_ID_SHOTMATERIAL	=  0;
	public static final byte ITEM_ID_SMALLPOWER		=  1;
	public static final byte ITEM_ID_BIGPOWER		=  2;
	public static final byte ITEM_ID_POINT			=  4;
	public static final byte ITEM_ID_BOMB			= 10;
	public static final byte ITEM_ID_EXTEND			= 11;

	private static final EntityDataAccessor<Byte> DATA_ITEM_TYPE = SynchedEntityData.defineId(EntityTHItem.class, EntityDataSerializers.BYTE);

	protected byte itemId;

	public EntityTHItem(EntityType<? extends EntityTHItem> type, Level level)
	{
		super(type, level);
		limitSpeed = 0.5D;
	}

	public EntityTHItem(EntityType<? extends EntityTHItem> type, Level level, double posX, double posY, double posZ, Vec3 angle, byte itemId)
	{
		super(type, level, posX, posY, posZ);

		setItemType(itemId);
		limitSpeed = 0.5D;
		motionX = angle.x * limitSpeed;
		motionY = angle.y * limitSpeed;
		motionZ = angle.z * limitSpeed;
		lastMotionX = motionX;
		lastMotionY = motionY;
		lastMotionZ = motionZ;
	}

	/**
	 * Entity生成時に一度だけ呼ばれる
	 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_ITEM_TYPE, (byte)0);
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		if(level().isClientSide)
		{
			return;
		}

		if(tickCount > 16)
		{
			super.tick();
		}

		if(target != null)
		{
			return;
		}

		//時間が進んでいないなら
		if(tickCount <= lastTime)
		{
			motionX = 0.0D;
			motionY = 0.0D;
			motionZ = 0.0D;
			return;
		}
		else
		{
			motionX = lastMotionX;
			motionY = lastMotionY;
			motionZ = lastMotionZ;
		}

		if(Math.abs(motionX) < 0.01)
		{
			motionX = 0.0D;
		}
		else
		{
			motionX *= 0.87D;
		}

		if(Math.abs(motionZ) < 0.01)
		{
			motionZ = 0.0D;
		}
		else
		{
			motionZ *= 0.87D;
		}

		if(motionY > -0.2D)
		{
			motionY -= 0.03D;
		}
		if(motionY < -0.2D)
		{
			motionY = -0.2D;
		}

		double newPosX = getX() + motionX;
		double newPosY = getY() + motionY;
		double newPosZ = getZ() + motionZ;

		Vec3 startPos = THShotLib.pos(newPosX, newPosY, newPosZ);
		Vec3 endPos = THShotLib.pos(newPosX + motionX, newPosY + motionY, newPosZ + motionZ);
		BlockHitResult blockHit = level().clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		//何らかのブロックに当たっているなら
		if(blockHit.getType() != HitResult.Type.MISS)
		{
			switch(blockHit.getDirection().get3DDataValue())
			{
				case 0:
					motionY = 0.0;
					newPosY = blockHit.getBlockPos().getY();
					break;
				case 1:
					if(!level().isClientSide)
					{
						discard();
					}
					break;
				case 2:
					motionZ = -motionZ;
					newPosZ = blockHit.getBlockPos().getZ();
				case 3:
					motionZ = -motionZ;
					newPosZ = blockHit.getBlockPos().getZ();
				case 4:
					motionX = -motionX;
					newPosX = blockHit.getBlockPos().getX();
				default:
					motionX = -motionX;
					newPosX = blockHit.getBlockPos().getX();
			}
		}

		lastMotionX = motionX;
		lastMotionY = motionY;
		lastMotionZ = motionZ;

		setPos(newPosX, newPosY, newPosZ);
	}

	@Override
	protected boolean check(Player player)
	{
		return player.isShiftKeyDown();
	}

	@Override
	protected void moveToPlayer(Player nearPlayer)
	{
		if(nearPlayer != null)
		{
			limitSpeed = 0.3D;
			super.moveToPlayer(nearPlayer);
		}
	}

	//保存するデータの書き込み（1.7.10 では空実装）
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
	}

	//保存したデータの読み込み（1.7.10 では空実装）
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
	}

	/**
	 * プレイヤーと接触したときの処理
	 */
	@Override
	public void playerTouch(Player player)
	{
		boolean itemGet = false;

		if(!level().isClientSide)
		{
			switch(getItemType())
			{
				case ITEM_ID_SMALLPOWER:
					itemGet = player.getInventory().add(new ItemStack(ModItems.POWER_UP_ITEM_SMALL.get()));
					break;
				case ITEM_ID_BIGPOWER:
					itemGet = player.getInventory().add(new ItemStack(ModItems.POWER_UP_ITEM_BIG.get()));
					break;
				case ITEM_ID_POINT:
					itemGet = player.getInventory().add(new ItemStack(ModItems.POINT_ITEM.get()));
					break;
				case ITEM_ID_BOMB:
					itemGet = player.getInventory().add(new ItemStack(ModItems.BOMB_ITEM.get()));
					if(!itemGet)
					{
						player.drop(new ItemStack(ModItems.BOMB_ITEM.get()), false);
					}
					break;
				case ITEM_ID_EXTEND:
					itemGet = player.getInventory().add(new ItemStack(ModItems.EXTEND_ITEM.get()));
					if(!itemGet)
					{
						player.drop(new ItemStack(ModItems.EXTEND_ITEM.get()), false);
					}
					break;
				default:
					break;
			}

			if (itemGet)
			{
				level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 4.0F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 0.3F);//1.7.10 の "random.pop"
				player.take(this, 1);
			}

			discard();
		}
	}

	/**
	 * アイテムの種類を設定
	 * @param id アイテムの種類ID
	 */
	public void setItemType(byte id)
	{
		itemId = id;
		this.entityData.set(DATA_ITEM_TYPE, id);
	}

	/**
	 * アイテムの種類を返す
	 * @return アイテムの種類ID
	 */
	public byte getItemType()
	{
		return this.entityData.get(DATA_ITEM_TYPE);
	}
}
