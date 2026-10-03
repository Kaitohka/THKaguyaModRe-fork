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
 * <p>操舵は W/A/S/D/ジャンプの押下ビットを {@link #steerMask} として受け取り、
 * {@link BroomSteer} の純関数で移動量を計算する（原作 {@code onUpdate} 344-687 に相当）。
 * 1.7.10 は {@code Minecraft.getMinecraft()} を公共コードから直接読んでいたが、
 * 1.21.1 ではクライアント専用の {@code thkaguyamod.client.input.BroomSteerClient} が
 * {@code KeyMapping} を読んでビットを書き込む（{@code MIGRATION_RULES.md} R3 準拠）。
 *
 * <h2>どちら側が動かすか（原作との最重要の差異）</h2>
 * <p>1.7.10 は両側で同じ計算をしていた（サーバー側は「常に W 押下」の暗黙初期値、
 * 旧箒 365-379）。1.21.1 ではバニラの乗り物と同じく
 * <b>{@link #isControlledByLocalInstance()} が true の側だけが積分</b>し、
 * もう一方は移動量を 0 にする（{@code Boat} と同じ扱い）。
 * <ul>
 *   <li>乗員がローカルプレイヤー → クライアントが積分し、毎 tick
 *       {@code ServerboundMoveVehiclePacket} を送る（{@code LocalPlayer#tick}）。</li>
 *   <li>サーバー側の同じ実体 → 送られてきた位置を受け取るだけ（二重積分しない）。</li>
 *   <li>他のプレイヤーから見た実体 → 位置パケットを {@code lerpTo} で補間。</li>
 *   <li>無人 → サーバーだけが積分（原作 665-687 の緩降下）。</li>
 * </ul>
 * この分岐が成立するには {@link #getControllingPassenger()} の実装が必須で、
 * これが無いと {@code handleMoveVehicle} の乗員チェックも
 * {@code ClientboundMoveEntityPacket} の補間ガードも通らない。
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

	/**
	 * 操舵入力ビット（{@link BroomSteer#FORWARD} 等の OR）。
	 *
	 * <p>制御側（ローカル騎乗者を持つ側）でだけ意味を持つ。書き込みは
	 * クライアントの {@code BroomSteerClient}、またはサーバー側の
	 * {@code BroomSteerPayload} ハンドラ（フェーズ 2、任意）から。
	 * 同期データにはしない（毎 tick 変化させると {@code SynchedEntityData} が
	 * 汚れ、{@code ServerEntity} の送信判定が毎 tick 成立してしまう）。
	 */
	private int steerMask;

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

	/**
	 * 操舵者（1.7.10 の {@code riddenByEntity instanceof EntityPlayer} 判定に相当）。
	 *
	 * <p>1.21.1 ではこのメソッドが「操縦できる乗り物」の入口になる。
	 * 実装すると次の連鎖が有効になる:
	 * <ul>
	 *   <li>{@code Entity#isControlledByLocalInstance()} が乗員の {@code isLocalPlayer()} を見る
	 *       → ローカル騎乗中のクライアントだけが積分・送信する。</li>
	 *   <li>{@code ServerGamePacketListenerImpl#handleMoveVehicle} が
	 *       {@code getControllingPassenger() == player} を要求する
	 *       → サーバーがこの箒の位置パケットを受け付ける。</li>
	 *   <li>{@code ClientPacketListener#handleMoveEntity} が
	 *       {@code !isControlledByLocalInstance()} を要求する
	 *       → 自分が操縦中の箒がサーバー位置で上書きされない。</li>
	 *   <li>{@code Entity#getKnownMovement()} が騎手の移動量を返す（原作の「箒の移動量＝騎手の移動量」相当）。</li>
	 * </ul>
	 *
	 * <p>原作はプレイヤーのみを操舵者として扱っていた（旧箒 360）ので、
	 * {@code Player} 以外（村人やゾンビ等）は {@code null} を返す＝操舵されない。
	 */
	@Override
	public LivingEntity getControllingPassenger()
	{
		return this.getFirstPassenger() instanceof Player player ? player : null;
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

		if(this.isControlledByLocalInstance())
		{
			//この側だけが運動を積分する（バニラの乗り物と同じ）。
			//注意: BroomSteer に渡す角度は「前 tick の箒の角度」でなければならない
			//（原作も入力演算 395-479 の後に角度を更新する 481-507）。
			float prevYaw = this.getYRot();
			float prevPitch = this.getXRot();

			if(this.getFirstPassenger() instanceof Player rider)
			{
				//原作 483。読み出しの無い死コードだが、フィールド互換のため維持する。
				this.speedMultiplier = 0.6D;

				Vec3 next = BroomSteer.step(this.getDeltaMovement(), this.steerMask, prevYaw, prevPitch, this.isInWater());
				this.setDeltaMovement(next);

				//原作 486-487 / 507: 箒の向きは騎手に合わせる。
				//pitch のクランプと減衰（原作 489-504）はローカル変数への代入で無効なため再現しない
				//（BroomSteer のクラス javadoc §2.2 参照）。
				this.setXRot(rider.getXRot());
				this.setYRot(rider.getYRot() - 90.0F);
			}
			else
			{
				//原作 665-687: 無人の箒は緩やかに減速・降下する。
				//move() より前に移動量へ反映させること（後段でローカル変数に書くだけでは
				//setDeltaMovement に届かず、下騎後も動き続けていた）。
				//接地中の垂直成分は主樹 I13 の修正（motionY を -0.03 に固定）を
				//BroomSteer.stepEmpty 内で反映している（EMPTY_GROUNDED_VERTICAL の javadoc 参照）。
				Vec3 next = BroomSteer.stepEmpty(this.getDeltaMovement(), this.onGround(), this.isInWater());
				this.setDeltaMovement(next);
				this.setXRot(BroomSteer.decayEmptyPitch(this.getXRot()));
			}

			this.move(MoverType.SELF, this.getDeltaMovement());

			//原作 584-587: move() の後の減衰（常に実行される）
			this.setDeltaMovement(BroomSteer.dampAfterMove(this.getDeltaMovement()));
			this.setRot(this.getYRot(), this.getXRot());
		}
		else
		{
			//非制御側は動かさない。位置は位置パケット（サーバー）または
			//lerpTo（クライアント）で決まる。ここで積分すると二重積分になる。
			this.setDeltaMovement(Vec3.ZERO);
		}

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
	 * 操舵入力ビットを設定する。制御側からのみ呼ばれる。
	 */
	public void setSteerMask(int mask)
	{
		this.steerMask = mask;
	}

	/** 操舵入力ビットを取得する（{@link BroomSteer#FORWARD} 等の OR）。 */
	public int getSteerMask()
	{
		return this.steerMask;
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
