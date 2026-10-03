package thkaguyamod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.init.ModItems;

/**
 * 魔法の箒
 *
 * <p>1.7.10 {@code thKaguyaMod.entity.item.EntityMarisaBroom} の移植。
 *
 * <p><b>TODO K3-review: 操縦入力は未移植。</b>1.7.10 版は {@code Minecraft.getMinecraft()} を
 * 直接読み、W/A/S/D/ジャンプの押下状態を {@code @SideOnly(CLIENT)} メソッド経由で騎乗者から
 * 取得していた（サーバー側でも「常にmove_up=true」という暗黙の初期値で動いていた）。
 * 1.21.1 ではクライアント入力がサーバーへ {@code ServerboundPlayerInputPacket} として送られ、
 * {@code ServerPlayer.setPlayerInput()} で {@code xxa}/{@code zza} に反映されるが、
 * 「ジャンプ押下」は {@code LivingEntity.jumping}（protected）にしか入らず外部から読めない。
 *
 * <p>暫定実装: 騎乗者がいないときは緩やかに降下し、騎乗者がいるときは騎手の視線方向へ前進する。
 * 旧版のキー別の加減速（ジャンプで上昇・Sで後退・A/Dで横移動）は再現していない。
 * 入力経路が確定したら {@link #tick()} の移動ブロックを差し替えること。
 */
public class EntityMarisaBroom extends Entity
{
	//同期データ (1.7.10 の dataWatcher 17-19 に相当)
	private static final EntityDataAccessor<Integer> DATA_TIME_SINCE_HIT = SynchedEntityData.defineId(EntityMarisaBroom.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FORWARD_DIRECTION = SynchedEntityData.defineId(EntityMarisaBroom.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_DAMAGE_TAKEN = SynchedEntityData.defineId(EntityMarisaBroom.class, EntityDataSerializers.FLOAT);

	/** 誰も乗っていない状態か（1.7.10 の isEmpty。クライアント側の位置補間の判定に使っていた） */
	private boolean isEmpty;
	private double speedMultiplier;

	public EntityMarisaBroom(EntityType<? extends EntityMarisaBroom> type, Level level)
	{
		super(type, level);
		this.isEmpty = true;
		this.speedMultiplier = 0.07D;
		//TODO K3-review: 1.7.10 の preventEntitySpawning / yOffset = 0.8F は 1.21.1 に対応が無い
		//（y オフセットは getPassengerAttachmentPoint / スポーン位置で表現する）。
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(0.9F, 0.5F) / yOffset = 0.8F に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.9F, 0.5F);
	}

	public EntityMarisaBroom(EntityType<? extends EntityMarisaBroom> type, Level level, double x, double y, double z)
	{
		this(type, level);
		this.setPos(x, y, z);
		this.setDeltaMovement(Vec3.ZERO);
		this.xo = x;
		this.yo = y;
		this.zo = z;
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_TIME_SINCE_HIT, 0);
		builder.define(DATA_FORWARD_DIRECTION, 1);
		builder.define(DATA_DAMAGE_TAKEN, 0.0F);
	}

	/**
	 * 押すことができるか
	 */
	@Override
	public boolean isPushable()
	{
		return true;
	}

	//触れることができるかどうか
	@Override
	public boolean canBeCollidedWith()
	{
		return !isRemoved();
	}

	//照準で狙えるかどうか（1.7.10 の canBeCollidedWith が兼ねていた照準フィルタ相当。
	//これが無いと左クリックでの攻撃（破壊・回収）や右クリックでの再騎乗が届かない）
	@Override
	public boolean isPickable()
	{
		return !isRemoved();
	}

	//Entityに攻撃されたときに呼ばれる
	@Override
	public boolean hurt(DamageSource damageSource, float par2)
	{
		//無敵ならfalseを返す
		if(this.isInvulnerableTo(damageSource))
		{
			return false;
		}
		//無敵でないなら
		else if(!this.level().isClientSide && !this.isRemoved())
		{
			this.setForwardDirection(-this.getForwardDirection());
			this.setTimeSinceHit(10);
			this.setDamageTaken(this.getDamageTaken() + par2 * 10.0F);
			this.markHurt();
			boolean flag = damageSource.getEntity() instanceof Player player && player.getAbilities().instabuild;

			if(flag || this.getDamageTaken() > 40.0F)//相手がプレイヤーでクリエイティブ、または、４以上のダメージを受けているなら
			{
				if(this.getFirstPassenger() != null)
				{
					this.getFirstPassenger().stopRiding();
				}

				if(!flag)
				{
					//1.7.10 の dropItem(THKaguyaItems.magic_bloom, 1) に相当
					this.spawnAtLocation(ModItems.MAGIC_BROOM.get());
				}

				this.discard();
			}

			return true;
		}
		else
		{
			return true;
		}
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		//ダメージアニメーション関連
		if(this.getTimeSinceHit() > 0)
		{
			this.setTimeSinceHit(this.getTimeSinceHit() - 1);
		}

		if(this.getDamageTaken() > 0.0F)
		{
			this.setDamageTaken(this.getDamageTaken() - 1.0F);
		}

		//前のポジションを今のポジションにする
		this.xo = this.getX();
		this.yo = this.getY();
		this.zo = this.getZ();

		Vec3 motion = this.getDeltaMovement();
		double motionX = motion.x;
		double motionY = motion.y;
		double motionZ = motion.z;

		double motionXZ = Math.sqrt(motionX * motionX + motionZ * motionZ);//平面方向の移動量

		//誰か乗っているなら
		if(this.getFirstPassenger() instanceof Player living)
		{
			//TODO K3-review: 旧版はここでキー入力（W/A/S/D/ジャンプ）を読んでいた。未移植。
			//暫定: 騎手の視線方向へ前進する。
			Vec3 look = living.getLookAngle();
			double acceleration = 0.04D;
			motionX += look.x * acceleration;
			motionY += look.y * acceleration;
			motionZ += look.z * acceleration;

			//Entityの移動量に箒の移動量を合わす
			float yaw = living.getYRot();
			speedMultiplier = 0.6D;

			float pitch = living.getXRot();
			this.setXRot(pitch);

			if(pitch > 90F)
			{
				pitch = 90F;
			}
			else if(pitch < -90F)
			{
				pitch = -90F;
			}

			if(pitch < 0F)
			{
				pitch *= 0.1F;
			}

			this.setYRot(yaw - 90F);

			double moveSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
			if(moveSpeed > 0.8D)
			{
				motionX *= moveSpeed;
				motionY *= moveSpeed;
				motionZ *= moveSpeed;
			}
		}
		else
		{
			//無人の箒は緩やかに減速・降下する（1.7.10 と同じ挙動。
			//move() より前に移動量へ反映させること。後段でローカル変数に書くだけでは
			//setDeltaMovement に届かず、下騎後も動き続けていた）
			motionX *= 0.9D;
			motionY -= 0.03D;
			motionZ *= 0.9D;
			if(this.getXRot() > 1.0F)
			{
				this.setXRot(this.getXRot() - 1.0F);
			}
			else if(this.getXRot() < -1.0F)
			{
				this.setXRot(this.getXRot() + 1.0F);
			}
			if(this.onGround())
			{
				motionY += 0.06D;
			}
		}

		if(this.isInWater())
		{
			motionX *= 0.5D;
			motionY *= 0.5D;
			motionZ *= 0.5D;
		}

		this.setDeltaMovement(motionX, motionY, motionZ);
		this.move(MoverType.SELF, this.getDeltaMovement());

		{
			motionX *= 0.9900000095367432D;
			motionY *= 0.949999988079071D;
			motionZ *= 0.9900000095367432D;
		}

		this.setDeltaMovement(motionX, motionY, motionZ);
		this.setRot(this.getYRot(), this.getXRot());

		if(!this.level().isClientSide)
		{
			List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.20000000298023224D, 0.0D, 0.20000000298023224D));

			if(list != null && !list.isEmpty())
			{
				for(Entity entity : list)
				{
					if(entity != this.getFirstPassenger() && entity.isPushable() && entity instanceof EntityMarisaBroom)
					{
						entity.push(this);
					}
				}
			}

			//乗っているEntityがおり、それが死んでいるなら
			if(this.getFirstPassenger() instanceof LivingEntity rider && !rider.isAlive())
			{
				rider.stopRiding();//乗っているEntityをいないことにする
			}

			if(this.getFirstPassenger() != null)
			{
				this.getFirstPassenger().fallDistance = 0.0F;
				this.fallDistance = 0.0F;
			}
		}

		this.isEmpty = this.getFirstPassenger() == null;
	}

	/**
	 * 乗っているEntityの位置を更新
	 */
	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction callback)
	{
		if(this.hasPassenger(passenger))
		{
			//1.7.10 の updateRiderPosition2（d0/d1 はどちらも 0D、getMountedYOffset = -0.15D）に相当
			callback.accept(passenger, this.getX(), this.getY() - 0.15D, this.getZ());
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

	//プレイヤーに右クリックされたときに呼ばれる（1.7.10 の interactFirst に相当）
	@Override
	public InteractionResult interact(Player entityPlayer, InteractionHand hand)
	{
		if(this.getFirstPassenger() instanceof Player && this.getFirstPassenger() != entityPlayer)
		{
			return InteractionResult.SUCCESS;
		}
		else
		{
			if(!this.level().isClientSide)
			{
				entityPlayer.startRiding(this);
			}

			return InteractionResult.SUCCESS;
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

	/**
	 * 誰も乗っていないかどうか
	 * TODO K3-review: 1.7.10 の func_70270_d(boolean) はクライアント側の位置補間フラグを
	 * 設定するパケット由来の setter だった。1.21.1 は lerpTo による補間になったため
	 * 同名setterは用意せず、フィールドは tick() 内で更新している。
	 */
	public boolean isEmpty()
	{
		return this.isEmpty;
	}

	public double getSpeedMultiplier()
	{
		return speedMultiplier;
	}
}