package thkaguyamod.entity.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import thkaguyamod.danmaku.THKaguyaLib;

/** 魔人経巻 */
public class EntityMazinkyoukan extends net.minecraft.world.entity.Entity
{
	/** 使用者 */
	public Player user;
	public int count;
	public float turnAngle;
	public int befUseCount;

	/** ワールド読み込み時に呼び出されるコンストラクタ */
	public EntityMazinkyoukan(EntityType<? extends EntityMazinkyoukan> type, Level level)
	{
		super(type, level);
		//サイズは getDimensions() で指定する（1.7.10 の setSize(2.0F, 2.0F) に相当）
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(2.0F, 2.0F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(2.0F, 2.0F);
	}

	/** 出現時に呼び出されるコンストラクタ */
	public EntityMazinkyoukan(EntityType<? extends EntityMazinkyoukan> type, Level level, Player player)
	{
		this(type, level);
		user = player;
		this.xo = player.getX();
		this.yo = player.getY();
		this.zo = player.getZ();
		setPos(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());//初期位置を設定(x,y,z)
		setRot(player.getYRot(), 0.0F);
		count = 0;
		befUseCount = 0;
		turnAngle = 0F;
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	//当たり判定の有無
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
		//使用者がいないなら消す
		if(!level().isClientSide && (user == null || !user.isAlive()))
		{
			discard();
			return;
		}
		super.tick();

		//使用者が右クリックをやめたら消す
		if(user != null)
		{
			if(!user.isUsingItem())
			{
				int time = count;
				time /= 3;
				{
					if(!level().isClientSide)
					{
						int removeLevel = (int)((float)time / 8.0F) + 1;

						if((user.experienceLevel - removeLevel) < 0)
						{
							time += (user.experienceLevel - removeLevel) * 8F;
						}
						user.giveExperienceLevels(-removeLevel);
						//1.7.10 の PotionEffect(1, ...) = 移動速度上昇
						user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, time * 10, 4));//スピードアップ
						//1.7.10 の PotionEffect(5, ...) = 攻撃力上昇
						user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, time * 10, 0));//攻撃力アップ
						//1.7.10 の PotionEffect(8, ...) = 跳躍力上昇
						user.addEffect(new MobEffectInstance(MobEffects.JUMP, time * 10, 1));//跳躍力アップ
						//1.7.10 の PotionEffect(11, ...) = 耐性
						user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, time * 10, 0));//防御力アップ
					}
				}
				if(!level().isClientSide)
				{
					discard();
				}
			}
			else
			{
				THKaguyaLib.itemEffectFollowUser(this, user, 0.5D, 0F);
				setYRot(user.getYRot());
				setXRot(0F);
			}
		}

		count++;
		if(count > 114)
		{
			count = 114;
		}
		else if(count > 50)
		{
			turnAngle += 2.88F;
		}

		if(getYRot() > 180F) setYRot(getYRot() - 360F);
		if(getYRot() < -180F) setYRot(getYRot() + 360F);
		if(getXRot() > 180F) setXRot(getXRot() - 360F);
		if(getXRot() < -180F) setXRot(getXRot() + 360F);
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