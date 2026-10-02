package thkaguyamod.entity.effect;

import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.entity.item.EntitySakuyaWatch;
import thkaguyamod.entity.shot.EntityTHShot;

/** 霊夢の結界（白楼剣／霊夢の結界・共通のシールド挙動） */
public class EntityHakureiShield extends Entity
{
	//同期データ (1.7.10 の dataWatcher 19 に相当。旧版は set/getParasolMode という
	//コピペ由来の名前だったが、用途不明のため名前だけ整えた)
	private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(EntityHakureiShield.class, EntityDataSerializers.INT);

	public Player user;
	public int damage;
	public int count;
	public double xVec;
	public double yVec;
	public double zVec;

	/** ワールド読み込み時に呼び出されるコンストラクタ */
	public EntityHakureiShield(EntityType<? extends EntityHakureiShield> type, Level level)
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
	public EntityHakureiShield(EntityType<? extends EntityHakureiShield> type, Level level, Player entityPlayer)
	{
		this(type, level);
		user = entityPlayer;
		this.xo = user.getX();
		this.yo = user.getY();
		this.zo = user.getZ();
		setPos(user.getX(), user.getY() + user.getEyeHeight() - 0.4D, user.getZ());//初期位置を設定(x,y,z)
		setRot(-user.getYRot(), 0F);
		xVec = -Math.sin((double)getYRot() / 180.0D * Math.PI);
		zVec = Math.cos((double)getYRot() / 180.0D * Math.PI);
		yVec = 0.0D;
		count = 0;
	}

	/** 押すことができるかどうか */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/** 生成時に呼ばれる */
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
		}

		double speedRate = (double)(4 - count) * 0.2D;

		if(count < 4)
		{
			setPos(getX() - Math.sin((double)getYRot() / 180.0D * Math.PI) * speedRate,
					getY(),
					getZ() + Math.cos((double)getYRot() / 180.0D * Math.PI) * speedRate);
		}

		double width = 1.5D;
		double length = 3.0D;
		//始点（現在地）
		Vec3 vec3d = new Vec3(getX() - xVec * width, getY() - yVec * length / 2.0D, getZ() - zVec * width);
		//終点（現在地に移動量を足した点）
		Vec3 vec3d1 = new Vec3(getX() + xVec * width, getY() + yVec * length / 2.0D, getZ() + zVec * width);
		//始点と終点からブロックとの当たりを取得
		//1.7.10 の worldObj.rayTraceBlocks(vec3d, vec3d1, true) に相当（液体を無視）
		BlockHitResult movingObjectPosition = level().clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		boolean blockHit = movingObjectPosition.getType() != HitResult.Type.MISS;

		vec3d = new Vec3(getX() - xVec * width, getY() - yVec * length / 2.0D, getZ() - zVec * width);
		vec3d1 = new Vec3(getX() + xVec * width, getY() + yVec * length / 2.0D, getZ() + zVec * width);

		//何らかのブロックに当たっているなら
		if(blockHit)
		{
			//終点を当たった点に変更
			vec3d1 = movingObjectPosition.getLocation();
			if(!level().isClientSide)
			{
				discard();
			}
		}

		Entity entity = null;//実際に当たったことにするEntity
		double d = 0.0D;//そのEntityまでの仮の距離
		double xLength = 0.0D;
		double yLength = -Math.sin(getXRot() / 180F * 3.141593F) * length;
		double zLength = 0.0D;
		//判定を弾の大きさに変更
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(xLength, yLength, zLength).inflate(width * 2D, length, 0.01D));

		for(int j = 0; j < list.size(); j++)
		{
			Entity entity1 = list.get(j);
			//entity1が時計でないなら判定する
			if(!(entity1 instanceof EntitySakuyaWatch))
			{
				AABB axisalignedbb = entity1.getBoundingBox().inflate(width, width, width);
				Optional<Vec3> intercept = axisalignedbb.clip(vec3d, vec3d1);
				//この判定で何も当たっていないならパス
				if(intercept.isPresent())
				{
					boolean creative = false;
					if(entity1 instanceof Player entityPlayer)
					{
						//1.7.10 の capabilities.disableDamage に相当。旧版は
						//「無敵プレイヤー or user がプレイヤー」なら当たりを無効化していた（コメントアウト含め旧コード踏襲）。
						if(entityPlayer.getAbilities().invulnerable || user instanceof Player)
						{
							creative = true;
						}
					}
					if(!creative)
					{
						onImpact(entity1);
					}
				}
			}
		}

		setPos(getX(), getY(), getZ());
		setRot(getYRot(), getXRot());

		count++;
	}

	//ブロックやEntityに当たった時の処理
	protected void onImpact(Entity hitEntity)
	{
		if(!level().isClientSide && hitEntity != null)
		{
			//当たったEntityがいるなら
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