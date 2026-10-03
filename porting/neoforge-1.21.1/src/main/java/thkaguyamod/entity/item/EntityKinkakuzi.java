package thkaguyamod.entity.item;

import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModItems;

/** 金閣寺の一枚天井 */
public class EntityKinkakuzi extends Entity
{
	//同期データ (1.7.10 の dataWatcher 17-19 に相当)
	private static final EntityDataAccessor<Integer> DATA_TIME_SINCE_HIT = SynchedEntityData.defineId(EntityKinkakuzi.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FORWARD_DIRECTION = SynchedEntityData.defineId(EntityKinkakuzi.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_DAMAGE_TAKEN = SynchedEntityData.defineId(EntityKinkakuzi.class, EntityDataSerializers.FLOAT);

	private double speedMultiplier;

	private LivingEntity shootingEntity;

	private int lastTime;
	public double lastMotionX;
	public double lastMotionY;
	public double lastMotionZ;

	/** ワールド読み込み時に呼び出されるコンストラクタ */
	public EntityKinkakuzi(EntityType<? extends EntityKinkakuzi> type, Level level)
	{
		super(type, level);
		this.speedMultiplier = 0.07D;
		//TODO K3-review: 1.7.10 の preventEntitySpawning / yOffset は 1.21.1 に対応が無い。
		//yOffset = height / 2.0F は getPassengerAttachmentPoint() で表現する。
	}

	/** 出現時に呼び出されるコンストラクタ */
	public EntityKinkakuzi(EntityType<? extends EntityKinkakuzi> type, Level level, double x, double y, double z, LivingEntity entityLivingBase, double pow)
	{
		this(type, level);
		//1.7.10 の motionX/Y/Z 代入に相当
		this.setDeltaMovement(
				-Math.sin((float)Math.toRadians(entityLivingBase.getYRot())) * 0.2D,
				pow,
				Math.cos((float)Math.toRadians(entityLivingBase.getYRot())) * 0.2D);
		lastMotionX = getDeltaMovement().x;
		lastMotionY = getDeltaMovement().y;
		lastMotionZ = getDeltaMovement().z;
		lastTime = 0;
		this.xo = x;
		this.yo = y;
		this.zo = z;
		this.setYRot(entityLivingBase.getYRot());
		this.setXRot(0.0F);
		this.moveTo(x, y, z, getYRot(), getXRot());
		shootingEntity = entityLivingBase;//使用者をshootingEntityに保存
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(6F, 0.3F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(6F, 0.3F);
	}

	/** 搭乗者の位置（1.7.10 の getMountedYOffset = height * 0.0D + 0.3D に相当） */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick)
	{
		return new Vec3(0.0D, 0.3D, 0.0D);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_TIME_SINCE_HIT, 0);//dataWatcher 17
		builder.define(DATA_FORWARD_DIRECTION, 1);//dataWatcher 18
		builder.define(DATA_DAMAGE_TAKEN, 0.0F);//dataWatcher 19
	}

	//押すことができるかどうか
	@Override
	public boolean isPushable()
	{
		return true;
	}

	//Entityに攻撃されたときに呼び出されるメソッド
	@Override
	public boolean hurt(DamageSource par1DamageSource, float par2)
	{
		if(this.isInvulnerableTo(par1DamageSource))
		{
			return false;
		}
		else if(!level().isClientSide && !isRemoved())
		{
			setForwardDirection(-getForwardDirection());
			setTimeSinceHit(10);
			setDamageTaken(getDamageTaken() + par2 * 10.0F);
			markHurt();
			boolean flag = par1DamageSource.getEntity() instanceof Player player && player.getAbilities().instabuild;

			if(flag || getDamageTaken() > 40.0F)//相手がプレイヤーでクリエイティブ、または、４以上のダメージを受けているなら
			{
				if(getFirstPassenger() != null)
				{
					getFirstPassenger().stopRiding();
				}
				if(!flag)
				{
					//破壊されたら、金閣寺の一枚天井を一つ落とす
					//1.7.10 の dropItem に相当
					spawnAtLocation(ModItems.SEAMLESS_CEILING_OF_KINKAKUJI.get());
				}
				discard();
			}

			return true;
		}
		else
		{
			return true;
		}
	}

	/**
	 * 触れることができるかどうか
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return !isRemoved();
	}

	//照準で狙えるかどうか（1.7.10 の canBeCollidedWith が兼ねていた照準フィルタ相当）
	@Override
	public boolean isPickable()
	{
		return !isRemoved();
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		if(getTimeSinceHit() > 0)
		{
			setTimeSinceHit(getTimeSinceHit() - 1);
		}

		if(getDamageTaken() > 0)
		{
			setDamageTaken(getDamageTaken() - 1);
		}

		//以前のポジションを保存
		this.xo = getX();
		this.yo = getY();
		this.zo = getZ();

		int i = 5;
		double d = 0.0D;

		for(int j = 0; j < i; j++)
		{
			double d2 = (getBoundingBox().minY + ((getBoundingBox().maxY - getBoundingBox().minY) * (double)(j + 0)) / (double)i) - 0.125D;
			double d8 = (getBoundingBox().minY + ((getBoundingBox().maxY - getBoundingBox().minY) * (double)(j + 1)) / (double)i) - 0.125D;

			//1.7.10 の isAABBInMaterial(aabb, Material.water) に相当
			if(containsWater(new net.minecraft.world.phys.AABB(
					getBoundingBox().minX, d2, getBoundingBox().minZ,
					getBoundingBox().maxX, d8, getBoundingBox().maxZ)))
			{
				d += 1.0D / (double)i;
			}
		}

		Vec3 motion = getDeltaMovement();
		double motionX = motion.x;
		double motionY = motion.y;
		double motionZ = motion.z;

		double lengthXZ = Math.sqrt(motionX * motionX + motionZ * motionZ);
		double newPosX;
		double newPosY;
		double newPosZ;

		if(d < 1.0D)
		{
			newPosX = d * 2D - 1.0D;
			motionY += 0.039999999105930328D * newPosX;
		}
		else
		{
			//上昇していないなら
			if(motionY < 0.0D)
			{
				motionY /= 2.0D;//落下速度を半分にする
			}

			//わずかに浮上させる
			motionY += 0.0070000002160668373D;
		}

		//誰かが乗っているなら
		if(getFirstPassenger() instanceof LivingEntity rider)
		{
			//乗っているEntityの前進入力（zza）を金閣寺の移動量に反映させる
			//1.7.10 の riddenByEntity.moveForward に相当
			newPosX = rider.zza;

			if(newPosX > 0.0D)
			{
				newPosY = -Math.sin((double)(rider.getYRot() * (float)Math.PI / 180.0F));
				newPosZ = Math.cos((double)(rider.getYRot() * (float)Math.PI / 180.0F));
				motionX += newPosY * this.speedMultiplier * 0.05000000074505806D;
				motionZ += newPosZ * this.speedMultiplier * 0.05000000074505806D;
			}
		}

		newPosX = Math.sqrt(motionX * motionX + motionZ * motionZ);

		if(newPosX > 0.35D)
		{
			newPosY = 0.35D / newPosX;
			motionX *= newPosY;
			motionZ *= newPosY;
			newPosX = 0.35D;
		}

		if(newPosX > lengthXZ && speedMultiplier < 0.35D)
		{
			this.speedMultiplier += (0.35D - this.speedMultiplier) / 35.0D;

			if(this.speedMultiplier > 0.35D)
			{
				this.speedMultiplier = 0.35D;
			}
		}
		else
		{
			this.speedMultiplier -= (this.speedMultiplier - 0.07D) / 35.0D;

			if(this.speedMultiplier < 0.07D)
			{
				this.speedMultiplier = 0.07D;
			}
		}

		//地面に付いているなら
		if(onGround())
		{
			motionX *= 0.5D;
			motionY *= 0.5D;
			motionZ *= 0.5D;
		}

		//移動量分だけEntityを動かす（1.7.10 の moveEntity に相当）
		this.setDeltaMovement(motionX, motionY, motionZ);
		move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

		if(horizontalCollision && lengthXZ > 0.2D)
		{
			//ボートが勢い良くぶつかったときの処理。壊れないから無視
		}
		else
		{
			motionX *= 0.99000000953674316D;
			motionY *= 0.94999998807907104D;
			motionZ *= 0.99000000953674316D;
		}

		setXRot(0.0F);
		newPosY = getYRot();
		newPosZ = this.xo - getX();
		double d10 = this.zo - getZ();

		if(newPosZ * newPosZ + d10 * d10 > 0.001D)
		{
			newPosY = (float)((Math.atan2(d10, newPosZ) * 180D) / Math.PI);
		}

		//1.7.10 の MathHelper.wrapAngleTo180_double に相当
		double angleShift = net.minecraft.util.Mth.wrapDegrees(newPosY - (double)this.getYRot());

		//変化量を上限下限に収める
		if(angleShift > 20D)
		{
			angleShift = 20D;
		}
		if(angleShift < -20D)
		{
			angleShift = -20D;
		}
		//水平角度に角度の変化量を加算する
		setYRot((float)((double)this.getYRot() + angleShift));
		//水平角度と垂直角度をセットする
		setRot(getYRot(), getXRot());

		this.setDeltaMovement(motionX, motionY, motionZ);

		if(!level().isClientSide)
		{
			List<Entity> list = level().getEntities(this, getBoundingBox().inflate(0.20000000298023224D, 0.0D, 0.20000000298023224D));

			if(list != null && !list.isEmpty())
			{
				for(Entity entity : list)
				{
					if(entity != getFirstPassenger() && entity.isPushable() && entity instanceof Boat)
					{
						entity.push(this);
					}
				}
			}

			//***********　金閣寺の一枚天井とEntityの当たり判定を取る****************//
			List<Entity> list2 = level().getEntities(this, getBoundingBox().inflate(
					net.minecraft.util.Mth.sin((float)getYRot() / 180.0F * (float)Math.PI) * 0.1D,
					0.5D,
					net.minecraft.util.Mth.cos((float)getYRot() / 180.0F * (float)Math.PI) * 0.1D));

			if(list2 != null && list2.size() > 0)
			{
				for(Entity entity : list2)
				{
					if(entity != getFirstPassenger() && entity.isPushable() && (entity instanceof EntityKinkakuzi))
					{
						entity.push(this);
					}
					if(entity != getFirstPassenger() && !(entity instanceof EntityKinkakuzi))
					{
						hitOnEntity(entity);//ダメージを与える処理とかしてる
					}
				}
			}

			//************************************************************************************//

			//誰かが乗っていて、それが死んでいるなら
			if(getFirstPassenger() instanceof LivingEntity rider && !rider.isAlive())
			{
				//乗っているEntityをいないことにする
				rider.stopRiding();
			}
		}

		//通常通り時間が進んでいるなら
		if(tickCount > lastTime)
		{
			//最後に時間が動いていたときの時間と移動量を保存する
			lastTime = tickCount;
			lastMotionX = getDeltaMovement().x;
			lastMotionY = getDeltaMovement().y;
			lastMotionZ = getDeltaMovement().z;
		}
	}

	/**
	 * AABB内に水があるか調べる（1.7.10 の isAABBInMaterial(aabb, Material.water) に相当）
	 */
	protected boolean containsWater(net.minecraft.world.phys.AABB aabb)
	{
		net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
		for(int x = net.minecraft.util.Mth.floor(aabb.minX); x < net.minecraft.util.Mth.ceil(aabb.maxX); x++)
		{
			for(int y = net.minecraft.util.Mth.floor(aabb.minY); y < net.minecraft.util.Mth.ceil(aabb.maxY); y++)
			{
				for(int z = net.minecraft.util.Mth.floor(aabb.minZ); z < net.minecraft.util.Mth.ceil(aabb.maxZ); z++)
				{
					pos.set(x, y, z);
					if(level().getFluidState(pos).is(Fluids.WATER))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	//Entityと衝突したときの処理
	protected void hitOnEntity(Entity entityHit)
	{
		//金閣寺の一枚天井が当たったEntityより上にあり、当たったEntityが、
		//金閣寺の一枚天井、アイテム、使用者、経験値オーブでないなら
		if(!level().isClientSide && entityHit != null)
		{
			if(entityHit.getY() < this.getY() &&
					!(entityHit instanceof ItemEntity)
					&& !(entityHit instanceof net.minecraft.world.entity.ExperienceOrb)
					&& !(entityHit instanceof EntityTHShot)
					&& entityHit != shootingEntity)
			{
				double yMove = this.yo - getY();
				if(yMove >= 0.0)
				{
					int damage = (int)(yMove * 40.0) + 2;//ダメージは落下速度依存　最低でもハート１個分はダメージを与える
					entityHit.hurt(level().damageSources().indirectMagic(this, shootingEntity), damage);
				}
			}
		}
	}

	/**
	 * 乗っているEntityの処理
	 * TODO K3-review: 1.7.10 は updateRiderPosition() をオーバーライドして搭乗者を posX + cos/sin の
	 * 位置に置いていた。1.21.1 は positionRider（protected）と getPassengerAttachmentPoint で
	 * 位置決めするため、getPassengerAttachmentPoint に置き換えた（y=0.3 のみ。旧版の ±0.5 の
	 * ずれは付与していない）。
	 */
	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction callback)
	{
		if(hasPassenger(passenger))
		{
			double d = Math.cos(((double)getYRot() * Math.PI) / 180D) * 0.5D;
			double d1 = Math.sin(((double)getYRot() * Math.PI) / 180D) * 0.5D;
			callback.accept(passenger, getX() + d, getY() + 0.3D, getZ() + d1);
		}
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbtTagCompound)
	{
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbtTagCompound)
	{
	}

	//プレイヤーに右クリックされたときに呼ばれる（1.7.10 の interactFirst に相当）
	@Override
	public net.minecraft.world.InteractionResult interact(Player entityPlayer, net.minecraft.world.InteractionHand hand)
	{
		if(getFirstPassenger() instanceof Player && getFirstPassenger() != entityPlayer)
		{
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
		else
		{
			if(!level().isClientSide)
			{
				entityPlayer.startRiding(this);
			}

			return net.minecraft.world.InteractionResult.SUCCESS;
		}
	}

	/**
	 * Sets the damage taken from the last hit.
	 */
	public void setDamageTaken(float par1)
	{
		this.entityData.set(DATA_DAMAGE_TAKEN, par1);
	}

	/**
	 * Gets the damage taken from the last hit.
	 */
	public float getDamageTaken()
	{
		return this.entityData.get(DATA_DAMAGE_TAKEN);
	}

	/**
	 * Sets the time to count down from since the last time entity was hit.
	 */
	public void setTimeSinceHit(int par1)
	{
		this.entityData.set(DATA_TIME_SINCE_HIT, par1);
	}

	/**
	 * Gets the time since the last hit.
	 */
	public int getTimeSinceHit()
	{
		return this.entityData.get(DATA_TIME_SINCE_HIT);
	}

	/**
	 * Sets the forward direction of the entity.
	 */
	public void setForwardDirection(int par1)
	{
		this.entityData.set(DATA_FORWARD_DIRECTION, par1);
	}

	/**
	 * Gets the forward direction of the entity.
	 */
	public int getForwardDirection()
	{
		return this.entityData.get(DATA_FORWARD_DIRECTION);
	}
}