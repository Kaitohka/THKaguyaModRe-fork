package thkaguyamod.entity.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModSounds;
import thkaguyamod.spellcard.THSC_Zenzinrui_no_Hisouten;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_KISHITU;
import static thkaguyamod.danmaku.DanmakuConstants.RED;

/** 緋想の剣 */
public class EntityHisou extends Entity
{
	//同期データ (1.7.10 の dataWatcher 17-18 に相当)
	private static final EntityDataAccessor<Integer> DATA_NUM = SynchedEntityData.defineId(EntityHisou.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANGLE = SynchedEntityData.defineId(EntityHisou.class, EntityDataSerializers.INT);

	/** 使用者 */
	public LivingEntity userEntity;
	private Entity motherEntity;
	private int count;
	public int num;
	private int damage;
	private int spellCardUsedTime;

	public EntityHisou(EntityType<? extends EntityHisou> type, Level level)
	{
		super(type, level);
		//サイズは getDimensions() で指定する（1.7.10 の setSize(4.4F, 4.4F) / yOffset = 0.0F に相当）
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(4.4F, 4.4F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(4.4F, 4.4F);
	}

	public EntityHisou(EntityType<? extends EntityHisou> type, Level level, LivingEntity entityLivingBase, Entity mother, int n, int da)
	{
		this(type, level);

		userEntity = entityLivingBase;//使用者をuserEntityに保存
		this.xo = userEntity.getX();
		this.yo = userEntity.getY();
		this.zo = userEntity.getZ();
		setPos(userEntity.getX() - Math.sin(userEntity.getYRot() / 180F * (float)Math.PI) * Math.cos(userEntity.getXRot() / 180F * (float)Math.PI) * 1.2D,
				userEntity.getY() - Math.sin(userEntity.getXRot() / 180F * (float)Math.PI) * 1.2D + userEntity.getEyeHeight() - 0.7D,
				userEntity.getZ() + Math.cos(userEntity.getYRot() / 180F * (float)Math.PI) * Math.cos(userEntity.getXRot() / 180F * (float)Math.PI) * 1.2D);//初期位置を設定(x,y,z)
		setYRot(userEntity.getYRot());
		//1.7.10 の ridingEntity = userEntity に相当
		this.startRiding(userEntity, true);
		motherEntity = mother; //残像に対して本物
		count = 0;
		num = n;
		setNum(n);
		damage = da;
		spellCardUsedTime = 0;
		level().playSound(null, blockPosition(), ModSounds.MASTER_SPARK.get(), SoundSource.NEUTRAL, ModConfig.HisoutenVol, 1.0F);
	}

	/** 生成時に一度だけ呼ばれる */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_NUM, 0);
		builder.define(DATA_ANGLE, 0);
	}

	/**
	 * 押すことができるか
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/**
	 * 他のEntityと衝突するか
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
		super.tick();

		if(!level().isClientSide && userEntity == null)
		{
			if(num == 8)
			{
				THKaguyaLib_itemEffectFinish();
			}
			else
			{
				discard();
			}
			return;
		}

		setAngle(getAngle() + ((float)count + (float)getNum() / 3.0F) * 39.0F);

		THKaguyaLib_itemEffectFollowUser();
		if(userEntity != null)//userEntityが存在するなら、それをぬるぬる追尾する
		{
			setYRot(userEntity.getYRot());
			setXRot(userEntity.getXRot());
		}
		//else//使用者がいないならその場でアイテム化
		else
		{
			if(getNum() == 8)
			{
				THKaguyaLib_itemEffectFinish();
			}
			if(!level().isClientSide)
			{
				discard();
			}
		}

		//****気質弾の発射****//
		if(tickCount > spellCardUsedTime && userEntity != null && getNum() == 8)
		{
			//1.7.10 は EntityTHShot を直接生成していたが、1.21.1 では THShotLib 経由で生成する
			ShotData shot = ShotData.shot(FORM_KISHITU, RED, 0.5F, 6.0F, 3, 60, THSC_Zenzinrui_no_Hisouten.SPECIAL_HISOUTEN01);
			THShotLib.createRandomRingShot(userEntity, this, THShotLib.pos_Entity(this), userEntity.getLookAngle(), 0F, 9999, 0.1D, 0.8D, 0.05D, THShotLib.gravity_Zero(), shot, num, 0.1D, 30F);
		}
		//********//

		if(tickCount > spellCardUsedTime)
		{
			spellCardUsedTime = tickCount;
		}

		//時間で消滅
		if(tickCount >= 100)
		{
			if(getNum() == 8)
			{
				THKaguyaLib_itemEffectFinish();
			}
			if(!level().isClientSide)
			{
				discard();
			}
		}
	}

	/** 1.7.10 の THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.hisou_sword, damage) に相当 */
	private void THKaguyaLib_itemEffectFinish()
	{
		thkaguyamod.danmaku.THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SWORD_OF_SCARLET_PERCEPTION.get(), damage);
	}

	/** 1.7.10 の THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, 0F) に相当 */
	private void THKaguyaLib_itemEffectFollowUser()
	{
		thkaguyamod.danmaku.THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, 0F);
	}

	/**
	 * 乗っているEntityの処理
	 * ここでは使用者の処理。使用者の motionX などは恐らくここでしかまともに操作できない
	 *
	 * TODO K3-review: 1.7.10 は updateRidden() をオーバーライドして搭乗者の回転と移動量を
	 * 直接減衰させていた（motionX/Y/Z *= 0.1）。1.21.1 では updateRidden が存在せず、
	 * rideTick() が毎tick呼ばれる。ここでは rideTick をオーバーライドして同じ減衰を適用する近似に留めた。
	 * 回転の lerp（prev→現在の0.3F補間）は setYRot 経由では再現できないため省略している。
	 */
	@Override
	public void rideTick()
	{
		super.rideTick();
		if(getVehicle() != null && getNum() == 8)
		{
			Vec3 motion = getVehicle().getDeltaMovement();
			getVehicle().setDeltaMovement(motion.x * 0.1D, motion.y * 0.1D, motion.z * 0.1D);
		}
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("count", (short)count);
		nbtTagCompound.putShort("damage", (short)damage);
		nbtTagCompound.putByte("number", (byte)num);
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		count = nbtTagCompound.getShort("count");
		damage = nbtTagCompound.getShort("damage");
		num = (int)nbtTagCompound.getByte("number");
	}

	public void setNum(int num)
	{
		this.entityData.set(DATA_NUM, num);
	}

	public int getNum()
	{
		return this.entityData.get(DATA_NUM);
	}

	public void setAngle(float angle)
	{
		this.entityData.set(DATA_ANGLE, (int)(angle * 1000F));
	}

	public float getAngle()
	{
		return (float)this.entityData.get(DATA_ANGLE) / 1000F;
	}
}