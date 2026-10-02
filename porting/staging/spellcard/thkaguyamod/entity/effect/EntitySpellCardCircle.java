package thkaguyamod.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** スペルカード使用時の魔法陣のエフェクト */
public class EntitySpellCardCircle extends Entity
{
	/** 使用者 */
	public LivingEntity user;
	private int type;
	private int count;
	private int endTime;
	protected int lastTime;

	//同期データ (1.7.10 の dataWatcher 19-22 に相当)
	private static final EntityDataAccessor<Float> DATA_CIRCLE_SIZE = SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_CIRCLE_TYPE = SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_END_TIME = SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANIMATION_COUNT = SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.INT);

	public EntitySpellCardCircle(EntityType<? extends EntitySpellCardCircle> type, Level level)
	{
		super(type, level);
		noCulling = true;//中心が画面から外れても描画される（1.7.10 の ignoreFrustumCheck に相当）
		//サイズは getDimensions() で指定する（1.7.10 の setSize(0.5F, 0.5F) に相当）
		lastTime = 0;
		setAnimationCount(0);
	}

	/**
	 * 当たり判定の大きさを返す（1.7.10 の setSize(0.5F, 0.5F) に相当）
	 */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.5F, 0.5F);
	}

	public EntitySpellCardCircle(EntityType<? extends EntitySpellCardCircle> type, Level level, LivingEntity entityLiving, int circleType, int end)
	{
		this(type, level);

		user = entityLiving;
		setCircleType(circleType);
		count = 0;
		setEndTime(end);
		/*if(endTime <= 0)
		{
			endTime = 1;
		}*/
		double posX = user.getX();
		double posY;
		if(entityLiving instanceof Player)
		{
			posY = user.getY() + 0.1D;
		}
		else
		{
			posY = user.getY() + user.getBbHeight() / 2.0D;
		}
		double posZ = user.getZ();
		this.setPos(posX, posY, posZ);
		this.setYRot(user.getYRot());
		this.setXRot(user.getXRot());
		this.setRot(user.getYRot(), user.getXRot());
	}

	/**
	 * 生成時に一度だけ呼ばれる
	 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_CIRCLE_SIZE, 0.0F);
		builder.define(DATA_CIRCLE_TYPE, 0);
		builder.define(DATA_END_TIME, 0);
		builder.define(DATA_ANIMATION_COUNT, 0);
	}

	/**
	 * 当たり判定を取るか返す
	 * falseだと右クリックの選択ですらできない。trueならsetSize()で設定したボックスの当たり判定が出現する
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		//スペルカードの使用者がいないなら
		if(!level().isClientSide && user == null )
		{
			//消滅させる
			discard();
			return;
		}
		super.tick();

		if(tickCount <= lastTime)
		{
			return;//処理を終了させる

		}
		else
		{
			if(!level().isClientSide)
			{
				setAnimationCount(lastTime);
			}
		}

		if(getEndTime() < tickCount && getEndTime() >= 0)
		{
			if(!level().isClientSide)
			{
				discard();
			}
		}



		//使用者がいるなら
		if(user != null)
		{
		}

		//endTime = 120;
		if(getAnimationCount() < 5)
		{
			setCircleSize(((float)getAnimationCount()) / 5.0F);
		}
		else
		{
			float endTime2 = (float)getEndTime();
			if(getCircleType() < 16)
			{
				setCircleSize((endTime2 - (float)getAnimationCount()) / endTime2);
			}
		}

		/*if(getCircleType() < 8)
		{
			//count++;
		}
		else
		{
			if(getAnimationCount() >= 5)
			{
				setAnimationCount(5);
			}
		}*/

		//使用者がいれば目の前に行くようにする
		if(user != null)
		{
			double posX = user.getX();
			double posY;
			if(user instanceof Player)
			{
				posY = user.getY() + 0.1D;
			}
			else
			{
				posY = user.getY() + user.getBbHeight() / 2.0D;
			}
			double posZ = user.getZ();
			setYRot(user.getYHeadRot());
			setXRot(user.getXRot());
			setPos(posX, posY, posZ);
		}

		if(getYRot() >  180F)setYRot(getYRot() - 360F);
		if(getYRot() < -180F)setYRot(getYRot() + 360F);
		if(getXRot() >  180F)setXRot(getXRot() - 360F);
		if(getXRot() < -180F)setXRot(getXRot() + 360F);

		setRot(getYRot(), getXRot());

		if(tickCount > lastTime)
		{
			//最後に時間が動いていたときの時間と移動量を保存する
			lastTime = tickCount;
		}
	}

	/**
	 * 魔法陣の大きさを設定する
	 * @param size 魔法陣の大きさ
	 */
	public void setCircleSize(float size)
	{
		this.entityData.set(DATA_CIRCLE_SIZE, size);
	}

	/**
	 * 魔法陣の大きさを返す
	 * @return 魔法陣の大きさ
	 */
	public float getCircleSize()
	{
		return this.entityData.get(DATA_CIRCLE_SIZE);
	}

	/**
	 * 魔法陣の見た目を設定する
	 * @param type 魔法陣の見た目
	 */
	public void setCircleType(int type)
	{
		this.entityData.set(DATA_CIRCLE_TYPE, type);
	}

	/**
	 * 魔法陣の見た目を返す
	 * @return 魔法陣の見た目
	 */
	public int getCircleType()
	{
		return this.entityData.get(DATA_CIRCLE_TYPE);
	}

	/**
	 * 魔法陣の終了時刻を設定する
	 * @param time 終了時刻
	 */
	public void setEndTime(int time)
	{
		this.entityData.set(DATA_END_TIME, time);
	}

	/**
	 * 魔法陣の終了時刻を返す
	 * @return 終了時刻
	 */
	public int getEndTime()
	{
		return this.entityData.get(DATA_END_TIME);
	}

	/**
	 * 魔法陣のアニメーションのカウントを設定する
	 * @param time アニメーションのカウント
	 */
	public void setAnimationCount(int time)
	{
		this.entityData.set(DATA_ANIMATION_COUNT, time);
	}

	/**
	 * 魔法陣のアニメーションのカウントを返す
	 * @return アニメーションのカウント
	 */
	public int getAnimationCount()
	{
		return this.entityData.get(DATA_ANIMATION_COUNT);
	}

	/**
	 * 保存するデータの書き込み
	 * @param nbtTagCompound : NTBタグ
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
	}

	/**
	 * 保存したデータの読み込み
	 * @param nbtTagCompound : NBTタグ
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
	}
}
