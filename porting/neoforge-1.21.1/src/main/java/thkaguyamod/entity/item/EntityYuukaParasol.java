package thkaguyamod.entity.item;

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

import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.init.ModItems;

/** 幽香の日傘 */
public class EntityYuukaParasol extends Entity
{
	//同期データ (1.7.10 の dataWatcher 19 = パラソルモード に相当)
	private static final EntityDataAccessor<Integer> DATA_PARASOL_MODE = SynchedEntityData.defineId(EntityYuukaParasol.class, EntityDataSerializers.INT);

	/** 使用者 */
	public Player user;
	public int damage;
	public int mode;

	/**
	 * 1.7.10 の ItemYuukaParasol.PARASOL_MAX_DAMAGE に相当。
	 * TODO K3-review: 1.21.1 では耐久値が Item.Properties().durability(60) に移ったため
	 * 定数の単一の出所が無い。ModItems.YUUKAS_PARASOL の耐久値と一致させること（現在 60）。
	 */
	public static final int PARASOL_MAX_DAMAGE = 60;

	/** ワールド読み込み時に呼び出されるコンストラクタ */
	public EntityYuukaParasol(EntityType<? extends EntityYuukaParasol> type, Level level)
	{
		super(type, level);
		noCulling = true;//中心が画面から外れても描画される（1.7.10 の ignoreFrustumCheck に相当）
		mode = getParasolMode();
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(0.1F, 0.1F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.1F, 0.1F);
	}

	/** 出現時に呼び出されるコンストラクタ */
	public EntityYuukaParasol(EntityType<? extends EntityYuukaParasol> type, Level level, Player entityPlayer, int da)
	{
		this(type, level);
		setParasolMode(0);
		//1.7.10 の ridingEntity = entityPlayer に相当（傘がプレイヤーに乗る）
		this.startRiding(entityPlayer, true);
		user = entityPlayer;
		double posX = user.getX() - Math.sin((user.getYRot() - 23F) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * 0.5D;
		double posY = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * Math.cos(-23F / 180F * 3.141593F) * 0.5D + user.getEyeHeight() + 1.6D;
		double posZ = user.getZ() + Math.cos((user.getYRot() - 23F) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * 0.5D;
		setPos(posX, posY, posZ);//初期位置を設定(x,y,z)
		setRot(user.getYRot(), 0.0F);
		damage = da;
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_PARASOL_MODE, 0);
	}

	/**
	 * 押すことができるかどうか
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/**
	 * 当たり判定の有無
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	//照準で狙えるかどうか（1.7.10 の canBeCollidedWith が兼ねていた照準フィルタ相当）
	@Override
	public boolean isPickable()
	{
		return true;
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		//使用者がいないなら終了処理
		if(!level().isClientSide && (user == null || !user.isAlive()))
		{
			THKaguyaLib.itemEffectFinish(this, user, ModItems.YUUKAS_PARASOL.get(), damage);
			return;
		}

		float posAngle = -23F;
		double distance = 1.0D;

		//使用者がいるなら
		if(user != null)
		{
			if(getParasolMode() == 0)
			{
				//easyFalling();
				distance = 0.5F;
				setXRot(-user.getXRot());
			}
			else if(getParasolMode() == 1)
			{
				posAngle = -23F;
				setXRot(-user.getXRot() + 40F);
			}
			else
			{
				posAngle = -23F;
				setXRot(-user.getXRot() - 90F);
				distance = 0.7D;
			}

			if(getParasolMode() == 0)
			{
				//使用者がスニークしていて、出現から時間が経っているなら
				if(user.isShiftKeyDown() && tickCount > 10)
				{
					setParasolMode(1);
					tickCount = 0;
				}
				user.fallDistance *= 0.7F;
			}
			else if(getParasolMode() == 1)
			{
				if(user.isShiftKeyDown() && tickCount > 10)
				{
					setParasolMode(2);
					tickCount = 0;
				}
			}
			else
			{
				if(user.isShiftKeyDown() && tickCount > 10)
				{
					THKaguyaLib.itemEffectFinish(this, user, ModItems.YUUKAS_PARASOL.get(), damage);
				}
			}
			setYRot(user.getYRot());

			//1.7.10 の motionX/Y/Z = posX - prevPosX 相当
			double motionX = getX() - this.xo;
			double motionY = getY() - this.yo;
			double motionZ = getZ() - this.zo;

			double posX = user.getX() - Math.sin((user.getYRot() + posAngle) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * distance;
			double posY = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * Math.cos(posAngle / 180F * 3.141593F) * distance + user.getEyeHeight() + 1.6D;
			double posZ = user.getZ() + Math.cos((user.getYRot() + posAngle) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * distance;

			posX += motionX;
			posY += motionY;
			posZ += motionZ;

			setPos(posX, posY, posZ);
		}

		if(!level().isClientSide && damage >= PARASOL_MAX_DAMAGE)//ダメージが武器の耐久を越したら
		{
			discard();//消滅
		}

		setRot(getYRot(), getXRot());
	}

	/**
	 * 乗っているEntityの処理
	 * ここでは使用者の処理。
	 *
	 * TODO K3-review: 1.7.10 の updateRidden() は 1.21.1 に無いため rideTick() に置き換えた。
	 * 旧版は搭乗者（プレイヤー）の motionX/Z を 0.95 倍し、モード0なら落下速度を 0.7 倍にしていた。
	 * 1.21.1 では搭乗者の setDeltaMovement を直接触る必要があるため、同値の減衰を適用している。
	 */
	@Override
	public void rideTick()
	{
		super.rideTick();
		Entity ridingEntity = getVehicle();
		if(ridingEntity != null)
		{
			if(getParasolMode() == 0)
			{
				easyFalling(ridingEntity);
			}
			Vec3 motion = ridingEntity.getDeltaMovement();
			ridingEntity.setDeltaMovement(motion.x * 0.95D, motion.y, motion.z * 0.95D);
		}
		else
		{
			if(!level().isClientSide)
			{
				THKaguyaLib.itemEffectFinish(this, user, ModItems.YUUKAS_PARASOL.get(), damage);
			}
		}
	}

	protected void easyFalling(Entity ridingEntity)
	{
		Vec3 motion = ridingEntity.getDeltaMovement();
		if(motion.y < 0.0D)
		{
			ridingEntity.setDeltaMovement(motion.x, motion.y * 0.7D, motion.z);
		}
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putInt("damage", damage);
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		// # fix (audit-nbt-zero): damage 是「日傘の耐久」。缺键即 0 会让 tick() 的
		//   `damage >= PARASOL_MAX_DAMAGE` 永不成立 → 耐久耗尽后伞不归还（玩家丢道具）。
		if(nbtTagCompound.contains("damage"))
		{
			damage = nbtTagCompound.getInt("damage");
		}
	}

	/**
	 * 日傘のモードチェンジ
	 * @param mode チェンジするモード
	 */
	public void setParasolMode(int mode)
	{
		this.entityData.set(DATA_PARASOL_MODE, mode);
	}

	/**
	 * パラソルのモードを返す
	 * @return パラソルのモード
	 */
	public int getParasolMode()
	{
		return this.entityData.get(DATA_PARASOL_MODE);
	}
}