package thkaguyamod.entity;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;

/** 弾の素 */
public class EntityShotMaterial extends Entity
{
	public short color;//弾の素

	protected int lastTime;
	private double lastSpeed;

	private double speed;
	protected double limitSpeed;

	protected double lastMotionX;
	protected double lastMotionY;
	protected double lastMotionZ;

	/*
	 * 弾の移動量。EntityTHShot と同じく 1.7.10 の motionX/Y/Z フィールド相当の独自フィールドで管理する
	 * （このEntityは物理演算を受けず、tick() で位置に明示的に加算する）。
	 */
	public double motionX;
	public double motionY;
	public double motionZ;

	Player target;

	public EntityShotMaterial(EntityType<? extends EntityShotMaterial> type, Level level)
	{
		super(type, level);
		limitSpeed = 0.5D;
	}

	public EntityShotMaterial(EntityType<? extends EntityShotMaterial> type, Level level, double posX, double posY, double posZ)
	{
		super(type, level);
		setPos(posX + motionX * 0.1D,
				posY + motionY * 0.1D,
				posZ + motionZ * 0.1D);

		lastMotionX = motionX;
		lastMotionY = motionY;
		lastMotionZ = motionZ;
		lastTime = -1;
		speed = 0.0D;
		lastSpeed = 0.0D;
		limitSpeed = 0.5D;

		target = null;
	}

	//Entity生成時に一度だけ呼ばれる
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
	}

	/**
	 * 押すことができるか返す
	 * @return 押せるならtrue
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/**
	 * 移動速度を返す
	 * @return 移動速度
	 */
	public double getEntitySpeed()
	{
		double xx, yy, zz;
		xx = motionX;
		yy = motionY;
		zz = motionZ;
		return Math.sqrt(xx*xx + yy*yy + zz*zz);
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

		//時間が進んでいないなら
		if(tickCount < lastTime)
		{
			motionX = 0.0D;
			motionY = 0.0D;
			motionZ = 0.0D;
			lastSpeed = 0.0D;
			return;
		}
		else
		{
			super.tick();
			motionX = lastMotionX;
			motionY = lastMotionY;
			motionZ = lastMotionZ;
			speed = lastSpeed;
		}

		//300tickで消滅
		if(tickCount > 300)
		{
			if(!level().isClientSide)
			{
				discard();
			}
		}

		//***********Entityの当たり判定を取る****************//
		if(target == null)
		{
			double effectiveBoundary = 20.0D;//停止空間の有効範囲
			double distance = 999.0D;
			Player nearPlayer = null;
			//空間内のEntityを全て取得
			List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary));

			for (int j1 = 0; j1 < list.size(); j1++)//取得したEntityリストの最初から最後まで見る
			{
				Entity entity = list.get(j1);
				if(entity instanceof Player player)//プレイヤーなら
				{
					if(distance > entity.distanceTo(this))
					{
						distance = entity.distanceTo(this);
						nearPlayer = player;
					}
				}
			}

			if(nearPlayer != null)
			{
				if(check(nearPlayer))
				{
					target = nearPlayer;
				}
			}
			else
			{
				if(!level().isClientSide)
				{
					discard();
				}
			}
		}

		if(target != null)
		{
			moveToPlayer(target);
		}

		if(!level().isClientSide)
		{
			setPos(getX(), getY(), getZ());
		}

		if(tickCount > lastTime)
		{
			lastMotionX = motionX;
			lastMotionY = motionY;
			lastMotionZ = motionZ;
			lastSpeed = speed;
		}
	}

	protected boolean check(Player player)
	{
		return true;
	}

	protected void moveToPlayer(Player nearPlayer)
	{
		if(speed < limitSpeed)
		{
			if(!level().isClientSide)
			{
				speed += 0.1D;
			}
		}
		else
		{
			speed = limitSpeed;
		}

		setRot(getYRot(), (float)Math.atan2(nearPlayer.getY() + nearPlayer.getEyeHeight() - getY(), Math.sqrt((nearPlayer.getX() - getX()) * (nearPlayer.getX() - getX()) + (nearPlayer.getZ() - getZ()) * (nearPlayer.getZ() - getZ()))) / (float)Math.PI * 180F);

		Vec3 vec = THShotLib.angle_ToPos(THShotLib.pos_Entity(this), THShotLib.pos_Living(nearPlayer));

		motionX += vec.x * limitSpeed;
		motionY += vec.y * limitSpeed;
		motionZ += vec.z * limitSpeed;

		setPos(getX() + motionX, getY() + motionY, getZ() + motionZ);
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
		if (!level().isClientSide && player.getInventory().add(new ItemStack(ModItems.SHOT_MATERIAL.get())))
		{
			level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 4.0F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 0.3F);//1.7.10 の "random.pop"
			player.take(this, 1);
		}

		if(!level().isClientSide)
		{
			discard();
		}
	}

	/**
	 * 当たり判定を取るか返す
	 * @return 当たり判定を取るならtrue
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}
}
