package thkaguyamod.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;

/** 風祝のお祓い棒の詠唱時のサークル */
public class EntityMiracleCircle extends Entity
{
	//同期データ (1.7.10 の dataWatcher 19 = 五芒星の色番号 に相当)
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(EntityMiracleCircle.class, EntityDataSerializers.INT);

	/** 五芒星を描いているプレイヤー */
	public Player user;
	public int count;//五芒星を描いている時間
	public float turnAngle;//五芒星の角度
	public int befUseCount;//前に使用していたときのカウント

	public EntityMiracleCircle(EntityType<? extends EntityMiracleCircle> type, Level level)
	{
		super(type, level);
		//サイズは getDimensions() で指定する（1.7.10 の setSize(2.0F, 0.0F) / yOffset = 0.0F に相当）
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(2.0F, 0.0F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(2.0F, 0.0F);
	}

	public EntityMiracleCircle(EntityType<? extends EntityMiracleCircle> type, Level level, Player entityPlayer, int cl)
	{
		this(type, level);

		user = entityPlayer;
		Vec3 vec3 = THShotLib.getRotationVectorFromAngle(user.getYRot(), user.getXRot(), turnAngle, 3.0D);
		setPos(user.getX() + vec3.x, user.getY() + vec3.y + (double)user.getEyeHeight(), user.getZ() + vec3.z);//初期位置を設定(x,y,z)
		setRot(entityPlayer.getYRot(), 0.0F);
		setColor(cl);
		count = 0;
		befUseCount = 0;
		turnAngle = 0F;
	}

	/** 生成時に一度だけ呼ばれる */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_COLOR, 0);
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		if(!level().isClientSide && user == null)
		{
			discard();
		}
		super.tick();
		if(user != null)
		{
			//1.7.10 の ItemOharaibouS.getItemInUseCount() == 0 相当
			if(!user.isUsingItem())
			{
				if(-befUseCount >= 250 && user.experienceLevel >= 40)//大奇跡「八坂の神風」の使用条件を満たしているなら
				{
					//TODO K3-review: 1.7.10 はここで EntitySanaeWind を生成するコードがコメントアウト
					//されていた（実際には生成されない）。EntitySanaeWind は 1.21.1 に移植済みだが、
					//旧版の実行時挙動（何も生成しない）をそのまま踏襲する。
				}
				if(!level().isClientSide)
				{
					discard();
				}
			}
		}
		count++;
		if(count == 30 && getColor() < 4)//カウントが30なら
		{
			if(user != null)
			{
				//次の五芒星を作る
				EntityMiracleCircle entityMiracle = new EntityMiracleCircle(thkaguyamod.init.ModEntities.MIRACLE_CIRCLE.get(), level(), user, getColor() + 1);
				if(!level().isClientSide)
				{
					level().addFreshEntity(entityMiracle);
				}
			}
		}

		if(user != null)
		{
			this.xo = getX();
			this.yo = getY();
			this.zo = getZ();

			Vec3 vec3 = THShotLib.getRotationVectorFromAngle(user.getYRot(), user.getXRot(), turnAngle, 3.0D);
			setPos(user.getX() + vec3.x, user.getY() + vec3.y + (double)user.getEyeHeight(), user.getZ() + vec3.z);
			setYRot(user.getYRot());
			setXRot(user.getXRot());
			befUseCount = getColor() * 30 + count;//1.7.10 の userPlayer.getItemInUseCount() 相当
		}
		setPos(getX(), getY(), getZ());

		if(count > 30)
		{
			turnAngle += 2.4F;
		}

		if(getYRot() > 180F) setYRot(getYRot() - 360F);
		if(getYRot() < -180F) setYRot(getYRot() + 360F);
		if(getXRot() > 180F) setXRot(getXRot() - 360F);
		if(getXRot() < -180F) setXRot(getXRot() + 360F);

		setRot(getYRot(), getXRot());
	}

	/**
	 * 五芒星の色を設定
	 * @param color 設定する色
	 */
	public void setColor(int color)
	{
		this.entityData.set(DATA_COLOR, color);
	}

	/**
	 * 五芒星の色を返す
	 * @return 五芒星の色
	 */
	public int getColor()
	{
		return this.entityData.get(DATA_COLOR);
	}

	/**
	 * 現在の五芒星の描いた線の数を返す
	 * @return 描いた線の数
	 */
	public int getNumberOfDrewLine()
	{
		int time = count;
		if(time > 30)
		{
			return 5;
		}
		else
		{
			return time / 6 + 1;
		}
	}

	/**
	 * 今描いている線が何本目か返す
	 * @return 今書いている線が何本目か
	 */
	public int getLastLineTime()
	{
		int time = count;
		if(time > 30)
		{
			return 10;
		}
		else
		{
			return time % 6;
		}
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
	}
}