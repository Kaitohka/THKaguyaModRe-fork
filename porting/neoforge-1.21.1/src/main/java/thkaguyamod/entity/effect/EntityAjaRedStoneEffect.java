package thkaguyamod.entity.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THShotLib;

/** エイジャの赤石の光のエフェクト */
public class EntityAjaRedStoneEffect extends Entity
{
	/**
	 * 1.7.10 の ItemAjaRedStone.SPECIAL_AJA_REDSTONE に相当。
	 * TODO K3-review: ItemAjaRedStone はまだ 1.21.1 に移植されていないため、
	 * 特殊弾 ID の単一の出所が無い。同クラスが移植されたらこの定数を削除し、
	 * ItemAjaRedStone 側の定数を参照すること（移植後は SpecialShotRegistry にも登録される）。
	 */
	public static final int SPECIAL_AJA_REDSTONE = 351;

	//同期データ (1.7.10 の dataWatcher 19 = 光レベル に相当)
	private static final EntityDataAccessor<Integer> DATA_LIGHT_POWER = SynchedEntityData.defineId(EntityAjaRedStoneEffect.class, EntityDataSerializers.INT);

	/** 使用者 */
	public LivingEntity user;
	/** 取り込んだ光の量 */
	public int lightLevel;
	private double length = 1.2D;

	public EntityAjaRedStoneEffect(EntityType<? extends EntityAjaRedStoneEffect> type, Level level)
	{
		super(type, level);
		//サイズは getDimensions() で指定する（1.7.10 の setSize(2.0F, 0.0F) / yOffset = 0.0F に相当）
		lightLevel = 0;
		setLightPower(0);
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(2.0F, 0.0F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(2.0F, 0.0F);
	}

	public EntityAjaRedStoneEffect(EntityType<? extends EntityAjaRedStoneEffect> type, Level level, LivingEntity entityLiving)
	{
		this(type, level);

		user = entityLiving;
		double posX = user.getX() - Math.sin((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
		double posY = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * length + (double)user.getEyeHeight();
		double posZ = user.getZ() + Math.cos((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
		//1.7.10 の setPositionAndRotation に相当
		this.moveTo(posX, posY, posZ, user.getYRot(), user.getXRot());
	}

	/** 生成時に呼ばれる */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_LIGHT_POWER, 0);
	}

	/**
	 * 当たり判定を取るか返す
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
		//エイジャの赤石の使用者がいないなら
		if(!level().isClientSide && user == null)
		{
			//消滅させる
			discard();
			return;
		}
		super.tick();

		//使用者がいるなら
		if(user != null)
		{
			if(user instanceof Player userEntity_p)
			{
				if(!userEntity_p.isUsingItem())
				{
					int damage = (int)((double)lightLevel / 40.0);
					if(damage > 30)//最大ダメージはハート15個分。頑張ってもそこまで
					{
						damage = 30;
					}

					if(damage > 0)//ダメージがあるなら
					{
						//レーザーを発射する
						THShotLib.createLaserA(user, this, THShotLib.pos(getX(), getY(), getZ()), THShotLib.angle(user.getYRot(), user.getXRot()), 0.1D, 4.0D, 0.3D, THShotLib.gravity_Zero(),
								LaserData.laser(DanmakuConstants.RED, (float)damage * 0.01F, (float)damage * 0.3F, damage, 0, 60, SPECIAL_AJA_REDSTONE));
						if(!level().isClientSide)//サーバーなら
						{
							discard();//光のエフェクトを消滅させる
						}
					}
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

		//今ある場所の光レベルに合わせて光の量を増やす
		setLightPower(level().getMaxLocalRawBrightness(BlockPos.containing(getX(), getY(), getZ())));
		lightLevel += getLightPower();

		//使用者がいれば目の前に行くようにする
		if(user != null)
		{
			double posX = user.getX() - Math.sin((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
			double posY = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * length + user.getEyeHeight();
			double posZ = user.getZ() + Math.cos((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
			setYRot(user.getYHeadRot());
			setXRot(user.getXRot());
			setPos(posX, posY, posZ);
		}

		if(getYRot() > 180F) setYRot(getYRot() - 360F);
		if(getYRot() < -180F) setYRot(getYRot() + 360F);
		if(getXRot() > 180F) setXRot(getXRot() - 360F);
		if(getXRot() < -180F) setXRot(getXRot() + 360F);

		setRot(getYRot(), getXRot());
	}

	/**
	 * 取り込んでいる光の量を設定
	 * @param lightPower 設定する光レベル
	 */
	public void setLightPower(int lightPower)
	{
		this.entityData.set(DATA_LIGHT_POWER, lightPower);
	}

	/**
	 * 取り込んでいる光の量を返す
	 * @return 溜め込んだ光レベル
	 */
	public int getLightPower()
	{
		return this.entityData.get(DATA_LIGHT_POWER);
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