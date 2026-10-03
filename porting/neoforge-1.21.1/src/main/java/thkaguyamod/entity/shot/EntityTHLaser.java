package thkaguyamod.entity.shot;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.EntityShotMaterial;

/** レーザー系の共通処理 */
public class EntityTHLaser extends EntityTHShot
{
	private static final EntityDataAccessor<Integer> DATA_LASER_LENGTH = SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_MAX_LASER_LENGTH = SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_MOTHER_POS_X = SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_MOTHER_POS_Y = SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_MOTHER_POS_Z = SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_MOTHER_AND_CHILD = SynchedEntityData.defineId(EntityTHLaser.class, EntityDataSerializers.INT);

	private EntityTHLaser mother;

	//ワールド読み込み時に呼び出されるコンストラクト
	public EntityTHLaser(EntityType<? extends EntityTHLaser> type, Level level)
	{
		super(type, level);
		noCulling = true;//レーザーの中心が画面から外れても描画される (1.7.10 の ignoreFrustumCheck に相当)
	}

	public EntityTHLaser(EntityType<? extends EntityTHLaser> type, Level level, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
			int color, float width, float length, float damage, int delay, int end, int special)
	{
		super(type, level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, acceleration, gravity, color, width, damage, delay, end, special);
		setLaserLength(0.0D);
		setMaxLaserLength(length);
		mother = null;
	}

	public EntityTHLaser(EntityType<? extends EntityTHLaser> type, Level level, LivingEntity user, Entity source,
		Vec3 pos, Vec3 angle, float slope,
		Vec3 rotate, float rotationSpeed, int rotationEnd,
		double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
		LaserData laser)
	{
		this(type, level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, acceleration, gravity,
				laser.form * 8 + laser.color, laser.width, laser.length, laser.damage, laser.delay, laser.end, laser.special);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_LASER_LENGTH, 0);
		builder.define(DATA_MAX_LASER_LENGTH, 0);
		builder.define(DATA_MOTHER_POS_X, 0);
		builder.define(DATA_MOTHER_POS_Y, 0);
		builder.define(DATA_MOTHER_POS_Z, 0);
		builder.define(DATA_MOTHER_AND_CHILD, 0);
	}

	//角度の更新処理
	@Override
	public void updateAngle()
	{
		float f = (float)Math.sqrt(angle.x * angle.x + angle.z * angle.z);
		float yaw = getYRot();
		float pitch = getXRot();
		if(!level().isClientSide)
		{
			yaw = (float)((Math.atan2(angle.x, angle.z) * 180D) / 3.1415927410125732D);
			if(yaw >= 180F)
			{
				yaw = -180F + (yaw - 180F);
			}
			else if(yaw <= -180F)
			{
				yaw = 180F + (yaw + 180F);
			}
		}
		if(!level().isClientSide)
		{
			pitch = (float)((Math.atan2(angle.y, f) * 180D) / 3.1415927410125732D);
		}
		setRot(yaw, pitch);
	}

	/** レーザーを消す。同時にレーザーが弾の素になる */
	@Override
	public void shotFinishBonus()
	{
		double zPos = 0.0D;
		while(zPos < getLaserLength())
		{
			EntityShotMaterial shotMaterial = new EntityShotMaterial(thkaguyamod.init.ModEntities.SHOT_MATERIAL_ENTITY.get(), level(), getX() + angle.x * zPos, getY() + angle.y * zPos, getZ() + angle.z * zPos);
			if(!level().isClientSide)
			{
				level().addFreshEntity(shotMaterial);
			}
			zPos += 1.5D;
		}
		if(!level().isClientSide)
		{
			discard();
		}
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		if(!(this instanceof EntityTHSetLaser))
		{
			double laserLength = getLaserLength() + getSpeed() * 2.0D;
			if(laserLength > getMaxLaserLength())
			{
				setLaserLength(getMaxLaserLength());
			}
			else
			{
				setLaserLength(laserLength);
			}
		}

		if(mother != null)
		{
			motionY += 0.4D;

			setMotherAndChild(1);
			setMotherPosX(mother.getX());
			setMotherPosY(mother.getY());
			setMotherPosZ(mother.getZ());
		}
		else
		{
			setMotherAndChild(0);
		}
	}

	/** 衝突時の処理 */
	@Override
	public void hitCheck()
	{
		double width = getShotSize();
		double length = getLaserLength() - getShotSize();
		//始点（現在地）
		Vec3 vec3d = new Vec3(getX() + angle.x * width, getY() + angle.y * width, getZ() + angle.z * width);
		//終点（現在地に移動量を足した点）
		Vec3 vec3d1 = new Vec3(getX() + motionX + angle.x * length, getY() + motionY + angle.y * length, getZ() + motionZ + angle.z * length);
		//始点と終点からブロックとの当たりを取得（液体も含む）
		BlockHitResult blockHit = clipBlocks(vec3d, vec3d1);
		HitResult hitResult = blockHit;
		//何らかのブロックに当たっているなら
		if (blockHit != null)
		{
			//終点を当たった点に変更
			vec3d1 = blockHit.getLocation();
			onImpact(blockHit);
		}

		//ここから移動量分の線分を作り、それに弾の大きさの２倍の肉付けをし直方体を作る。それに当たったEntityをリスト化する
		double xLength = motionX + Math.sin(getYRot() / 180F * 3.141593F) * Math.cos(getXRot() / 180F * 3.141593F) * getLaserLength();
		double yLength = motionY + Math.sin(getXRot() / 180F * 3.141593F) * getLaserLength();
		double zLength = motionZ + Math.cos(getYRot() / 180F * 3.141593F) * Math.cos(getXRot() / 180F * 3.141593F) * getLaserLength();
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(xLength, yLength, zLength).inflate(width));//指定範囲内のEntityをリストに登録

		for (int j = 0; j < list.size(); j++)
		{
			Entity entity1 = list.get(j);//entity1にリストの先端のentityを保存
			//entity1が、当たり判定を取らない　または　entity1が使用者　または　飛んで25カウント以下？　ならパス
			if ( entity1.canBeHitByProjectile() &&
				!entity1.is(user) &&
				!entity1.is(source) &&
				!hitCheckEx(entity1) &&
				//TODO: EntitySakuyaWatch / EntityTHFairy 移植後に 1.7.10 の除外条件を復元する
				!(entity1 instanceof Animal) &&
				!(entity1 instanceof AbstractVillager) &&
				entity1 instanceof LivingEntity)
			{
				//判定を弾の大きさに変更
				AABB axisalignedbb = entity1.getBoundingBox().inflate(width);
				Optional<Vec3> intercept = axisalignedbb.clip(vec3d, vec3d1);
				//この判定で何も当たっていないならパス
				if (intercept.isPresent())
				{
					HitResult hitEntityResult = new EntityHitResult(entity1);
					if (entity1 instanceof Player player)
					{
						if (player.getAbilities().invulnerable || source instanceof Player)
						{
							hitEntityResult = null;
						}
					}
					if(hitEntityResult != null)
					{
						onImpact(hitEntityResult);
					}
				}
			}
		}
	}

	/** ブロックやEntityに当たった時の処理
	 * @param hitResult : 移動しているものに関する情報
	 * @return trueを返す（trueしか返さない不具合)
	 */
	@Override
	protected boolean onImpact(HitResult hitResult)
	{
		Entity hitEntity = hitResult instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
		if(hitEntity != null)
		{
			//当たったEntityがいるなら
			//それがEntityTHShotに属していないなら
			if(!(hitEntity instanceof EntityTHShot))
			{
				//Entityに当たった時の特殊な処理
				entityHitSpecialProcess(hitEntity);
				//指定したダメージ分の魔法ダメージを与える
				if(!level().isClientSide)
				{
					hitEntity.hurt(damageSources().indirectMagic(this, user), this.getShotDamageWithDifficulty());
				}
				if(hitEntity.invulnerableTime == 0)//1.7.10 の hurtResistantTime に相当
				{
					shotDamage--;
				}
				if(shotDamage <= 0)
				{
					shotDamage = 1;
				}
			}
			//EntityTHShotに属しているなら
			else
			{
				EntityTHShot entityTHShot = (EntityTHShot)hitEntity;

				if(user != entityTHShot.user)//使用者の違う弾同士は打ち消し合う
				{
					//弾同士の相殺
					//お互い弾のダメージ分だけ小さくする
					float shotDamageA = this.shotDamage;
					this.shotDamage -= entityTHShot.shotDamage;
					entityTHShot.shotDamage -= shotDamageA;
					if(this.shotDamage < 0.0F)
					{
						shotDamage = 0.0F;
					}
					if(entityTHShot.shotDamage < 0.0F)
					{
						entityTHShot.shotDamage = 0.0F;
					}
				}
			}
		}
		else
		{
			if(hitResult instanceof BlockHitResult blockHit)
			{
				blockHitSpecialProcess(blockHit);
				double hitDistance = Math.sqrt(this.distanceToSqr(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z));
				if(hitDistance > 0.3D)
				{
					if(!level().isClientSide)
					{
						setLaserLength(hitDistance);
						setMaxLaserLength(hitDistance);
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
		}

		if(shotDamage <= 0)
		{
			if(!level().isClientSide)
			{
				this.discard();//ブロックに当たったら消滅
			}
		}

		return true;
	}

	//独自の動きを追加するためのもの
	@Override
	public void specialMotion()
	{
		super.specialMotion();

		switch(shotSpecial)
		{
			case DanmakuConstants.GUNGNIR:
				if(tickCount <= 10)
				{
					Vec3 move = THShotLib.getVectorFromRotation(rotate, angle, 18F);
					if(this.getLaserLength() < this.getMaxLaserLength())
					{
						this.setLaserLength(this.getLaserLength() + 1.0D);
					}
					else
					{
						this.setLaserLength(this.getMaxLaserLength());
					}
					angle = move;
					updateAngle();
				}
				if(tickCount == 10)
				{
					user.setDeltaMovement(user.getDeltaMovement().x - angle.x, user.getDeltaMovement().y, user.getDeltaMovement().z - angle.z);
					this.shotLimitSpeed = 2.0D;
					this.shotAcceleration = 0.6D;
				}
				if(tickCount > 10)
				{
					ShotData shot = ShotData.shot(DanmakuConstants.FORM_CRYSTAL, DanmakuConstants.RED, 0.45F, 6.0F, 0, 40);
					THShotLib.createRingShot(this.user, this, THShotLib.pos_Entity(this), angle, 0F, 9999, 1.4D, 0.1D, -0.1D, THShotLib.gravity_Zero(), shot, 4, 0.3D, 240F, tickCount * 10F);
				}
				break;
			default:
				break;
		}
	}

	//Entityからの攻撃を受けたときの処理　要は跳ね返す処理
	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource damagesource, float amount)
	{
		return false;
	}

	/**
	 * レーザーの長さを設定
	 * @param length レーザーの長さ
	 */
	public void setLaserLength(double length)
	{
		this.entityData.set(DATA_LASER_LENGTH, (int)(length * 10000.0D));
	}

	/**
	 * レーザーの長さを取得する
	 * @return レーザーの長さを返す
	 */
	public double getLaserLength()
	{
		return (double)this.entityData.get(DATA_LASER_LENGTH) / 10000.0D;
	}

	/**
	 * レーザーの最大の長さを設定
	 * @param maxLength レーザーの最大の長さ
	 */
	public void setMaxLaserLength(double maxLength)
	{
		this.entityData.set(DATA_MAX_LASER_LENGTH, (int)(maxLength * 10000.0D));
	}

	/**
	 * レーザーの最大の長さを取得
	 * @return レーザーの最大の長さを返す
	 */
	public double getMaxLaserLength()
	{
		return (double)this.entityData.get(DATA_MAX_LASER_LENGTH) / 10000.0D;
	}

	/**
	 * 未実装メソッド
	 * 親のレーザーのX座標を設定する
	 * @param x
	 */
	@Deprecated
	public void setMotherPosX(double x)
	{
		this.entityData.set(DATA_MOTHER_POS_X, (int)(x * 10000.0D));
	}

	/**
	 * 未実装メソッド
	 * 親のレーザーのX座標を返す
	 * @return
	 */
	@Deprecated
	public double getMotherPosX()
	{
		return this.entityData.get(DATA_MOTHER_POS_X) / 10000.0D;
	}

	/**
	 * 未実装メソッド
	 * 親のレーザーのY座標を設定する
	 * @param y
	 */
	@Deprecated
	public void setMotherPosY(double y)
	{
		this.entityData.set(DATA_MOTHER_POS_Y, (int)(y * 10000.0D));
	}

	/**
	 * 未実装メソッド
	 * 親のレーザーのY座標を返す
	 * @return
	 */
	@Deprecated
	public double getMotherPosY()
	{
		return this.entityData.get(DATA_MOTHER_POS_Y) / 10000.0D;
	}

	/**
	 * 未実装メソッド
	 * 親のレーザーのZ座標を設定する
	 * @param z
	 */
	@Deprecated
	public void setMotherPosZ(double z)
	{
		this.entityData.set(DATA_MOTHER_POS_Z, (int)(z * 10000.0D));
	}

	/**
	 * 未実装メソッド
	 * 親のレーザーのZ座標を返す
	 * @return
	 */
	@Deprecated
	public double getMotherPosZ()
	{
		return this.entityData.get(DATA_MOTHER_POS_Z) / 10000.0D;
	}

	/**
	 * 未実装メソッド
	 * 親子がいるかを設定する
	 * @param b
	 */
	@Deprecated
	public void setMotherAndChild(int b)
	{
		this.entityData.set(DATA_MOTHER_AND_CHILD, b);
	}

	/**
	 * 未実装メソッド
	 * 親子がいるかを返す
	 * @return
	 */
	@Deprecated
	public int isMotherAndChild()
	{
		return this.entityData.get(DATA_MOTHER_AND_CHILD);
	}
}
