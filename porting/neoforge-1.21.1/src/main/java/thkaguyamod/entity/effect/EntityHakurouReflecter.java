package thkaguyamod.entity.effect;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHLaser;
import thkaguyamod.entity.shot.EntityTHShot;

/** 白楼剣の跳ね返すシールド */
public class EntityHakurouReflecter extends Entity
{
	//同期データ (1.7.10 の dataWatcher 19 に相当)
	private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(EntityHakurouReflecter.class, EntityDataSerializers.INT);

	/** 使用者 */
	public Player user;
	public int damage;
	public int count;
	public double xVec;
	public double yVec;
	public double zVec;

	/** ワールド読み込み時に呼び出されるコンストラクタ */
	public EntityHakurouReflecter(EntityType<? extends EntityHakurouReflecter> type, Level level)
	{
		super(type, level);
		//サイズは getDimensions() で指定する（1.7.10 の setSize(1.5F, 1.5F) / yOffset = 0.0F に相当）
		count = 4;
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(1.5F, 1.5F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(1.5F, 1.5F);
	}

	/** 出現時に呼び出されるコンストラクタ */
	public EntityHakurouReflecter(EntityType<? extends EntityHakurouReflecter> type, Level level, Player entityPlayer)
	{
		this(type, level);
		user = entityPlayer;
		Vec3 vec3 = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot(), 1.0D);
		setPos(user.getX() + vec3.x, user.getY() + vec3.y + user.getEyeHeight(), user.getZ() + vec3.z);//初期位置を設定(x,y,z)
		setRot(-user.getYRot(), user.getXRot());
		Vec3 vec3_2 = THShotLib.getVecFromAngle(getYRot(), getXRot(), 0.18D);
		xVec = vec3_2.x;
		zVec = vec3_2.z;
		yVec = vec3_2.y;
		count = 0;
	}

	/** 押すことができるかどうか */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/** 生成時に一度だけ呼ばれる */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_MODE, 0);
	}

	//当たり判定の有無
	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	//Entityが存在する限り毎フレーム呼び出されるメソッド
	@Override
	public void tick()
	{
		super.tick();

		//使用者がいないなら終了処理
		if(!level().isClientSide)
		{
			if(user == null || !user.isAlive())
			{
				discard();
			}
			if(tickCount >= 20)
			{
				discard();
			}
		}

		//1.7.10 の motionX/motionY/motionZ = xVec/yVec/zVec 相当
		double motionX = xVec;
		double motionY = yVec;
		double motionZ = zVec;

		setPos(getX() - motionX, getY() + motionY, getZ() + motionZ);

		double width = 0.5D;
		double length = 0.5D;
		//始点（現在地）
		Vec3 vec3d = new Vec3(getX(), getY(), getZ());
		//終点（現在地に移動量を足した点）
		Vec3 vec3d1 = new Vec3(getX() + motionX, getY() + motionY, getZ() + motionZ);
		//始点と終点からブロックとの当たりを取得
		//1.7.10 の worldObj.rayTraceBlocks(vec3d, vec3d1, false) に相当（液体を含む）
		BlockHitResult movingObjectPosition = level().clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));

		vec3d = new Vec3(getX(), getY(), getZ());
		vec3d1 = new Vec3(getX() + motionX, getY() + motionY, getZ() + motionZ);

		//何らかのブロックに当たっているなら
		if(movingObjectPosition.getType() != HitResult.Type.MISS)
		{
			//終点を当たった点に変更
			vec3d1 = movingObjectPosition.getLocation();
			if(!level().isClientSide)
			{
				discard();
			}
		}

		//ここから移動量分の線分を作り、それに当たったEntityをリスト化する
		double yLength = motionY - Math.sin(getXRot() / 180F * 3.141593F) * length;
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(0.0D, yLength, 0.0D).inflate(width * 2D, width * 2D, width * 2D));

		for(int j = 0; j < list.size(); j++)
		{
			Entity entity1 = list.get(j);
			if(entity1 instanceof EntityTHShot && !(entity1 instanceof EntityTHLaser))
			{
				EntityTHShot shot = (EntityTHShot)entity1;
				//TODO K3-review: 1.7.10 は axisalignedbb.calculateIntercept の結果判定を
				//コメントアウトしており（常に反射）、当たり判定は上のリスト取得だけで行っていた。
				//その挙動をそのまま踏襲している（intercept の結果は使わない）。
				if(user != null && user != shot.user)
				{
					ShotData reflectShot = ShotData.shot(THShotLib.FORM_SCALE, THShotLib.AQUA, THShotLib.SIZE[THShotLib.FORM_SCALE], 3.0F, 0, 40, 0);
					THShotLib.createShot(user, THShotLib.pos_Entity(shot), THShotLib.angle(-getYRot(), getXRot()), 0.3D, 0.7D, 0.05D, reflectShot);

					if(!level().isClientSide)
					{
						entity1.discard();
					}
					//音を出す（1.7.10 の "random.bow"）
					level().playSound(null, shot.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 4F + 0.8F));
				}
			}
		}

		//1.7.10 の setPosition(posX, posY, posZ) に相当（座標は変わらないため実質は当たり判定の再計算）
		setPos(getX(), getY(), getZ());
		setRot(getYRot(), getXRot());

		count++;
	}

	//ブロックやEntityに当たった時の処理
	protected void onImpact(Entity hitEntity)
	{
		if(!level().isClientSide && hitEntity != null)
		{
			//それがEntityTHShotに属していないなら
			if(!(hitEntity instanceof EntityTHShot))
			{
				//指定したダメージ分の魔法ダメージを与える
				double slope = ((this.getX() + this.xVec) - (this.getX() - this.xVec)) / ((this.getZ() + this.zVec) - (this.getZ() - this.zVec));
				double height = this.getX() - slope * this.getZ();

				if(hitEntity.getX() > hitEntity.getZ() * slope + height)
				{
					hitEntity.setDeltaMovement(-Math.sin(getYRot() / 180.0D * Math.PI), hitEntity.getDeltaMovement().y, -Math.cos(getYRot() / 180.0D * Math.PI));
				}
				else
				{
					hitEntity.setDeltaMovement(Math.sin(getYRot() / 180.0D * Math.PI), hitEntity.getDeltaMovement().y, Math.cos(getYRot() / 180.0D * Math.PI));
				}

				hitEntity.hurt(level().damageSources().indirectMagic(this, user), 1.0F);
			}
			//EntityTHShotに属しているなら
			else
			{
				if(!level().isClientSide)
				{
					hitEntity.discard();
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

	//Entityに攻撃されたときに呼び出されるメソッド
	@Override
	public boolean hurt(DamageSource par1DamageSource, float par2)
	{
		return true;
	}

	public void setMode(int mode)
	{
		this.entityData.set(DATA_MODE, mode);
	}

	public int getMode()
	{
		return this.entityData.get(DATA_MODE);
	}

	protected void addAdditionalSaveData(CompoundTag par1NBTTagCompound)
	{
	}

	protected void readAdditionalSaveData(CompoundTag par1NBTTagCompound)
	{
	}
}