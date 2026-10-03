package thkaguyamod.entity.shot;

import java.util.List;
import java.util.Optional;

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

import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.THShotLib.gravity_Zero;

/** 固定レーザー系の共通処理 */
public class EntityTHSetLaser extends EntityTHLaser
{
	private double settingLength;//設置した場所からの距離
	private double settingYOffset;//設置する高さ

	private Vec3 setPos;//設置地点

	public Entity setting;

	/** ワールド読み込み時に呼び出されるコンストラクト */
	public EntityTHSetLaser(EntityType<? extends EntityTHSetLaser> type, Level level)
	{
		super(type, level);
	}

	public EntityTHSetLaser(EntityType<? extends EntityTHSetLaser> type, Level level, LivingEntity user, Entity source,
		Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
		int color, float width, float length, float damage, int delay, int end, int special,
		Entity set, double setLength, double setYOffset)
	{
		super(type, level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				0.0D, 0.0D, 0.0D, gravity_Zero(), color, width, length, damage, delay, end, special);
		setLaserLength(length);
		setMaxLaserLength(length);
		settingLength = setLength;
		settingYOffset = setYOffset;

		setting = set;
		if(set != null)
		{
			setPos = pos;
			setPos(set.getX() + angle.x * settingLength,
					set.getY() + angle.y * settingLength + settingYOffset,
					set.getZ() + angle.z * settingLength);
		}
	}

	public EntityTHSetLaser(EntityType<? extends EntityTHSetLaser> type, Level level, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
			LaserData laser,
			Entity set, double setLength, double setYOffset)
	{
		this(type, level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
		laser.color, laser.width, laser.length, laser.damage, laser.delay, laser.end, laser.special, set, setLength, setYOffset);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		if(this.setting != null)
		{
			if(!setting.isAlive())
			{
				discard();
				return;
			}
			setPos(setting.getX() + settingLength * angle.x,
					setting.getY() + settingLength * angle.y + settingYOffset,
					setting.getZ() + settingLength * angle.z);
			//長さが限界でないなら伸ばす
			if(getLaserLength() < getMaxLaserLength())
			{
				setLaserLength(getLaserLength() + 6.0D);
			}
			else
			{
				setLaserLength(getMaxLaserLength());
			}
		}
		else
		{
			motionX = angle.x;
			motionY = angle.y;
			motionZ = angle.z;
			setPos(getX() + motionX, getY() + motionY, getZ() + motionZ);
			this.lastTime = 1;
			if(!level().isClientSide)
			{
				discard();
				return;
			}
		}
	}

	/** 衝突時の処理 */
	@Override
	public void hitCheck()
	{
		double width = getShotSize();
		double length = getLaserLength();
		Vec3 vec = THShotLib.getVecFromAngle(getYRot(), getXRot(), length - getShotSize());
		Vec3 vec2 = THShotLib.getVecFromAngle(getYRot(), getXRot(), getShotSize());
		//始点（現在地）
		Vec3 vec3d = new Vec3(getX() - vec2.x, getY() - vec2.y, getZ() + vec2.z);
		//終点（現在地に移動量を足した点）
		Vec3 vec3d1 = new Vec3(getX() - vec.x, getY() - vec.y, getZ() + vec.z);
		//始点と終点からブロックとの当たりを取得（液体も含む）
		BlockHitResult blockHit = clipBlocks(vec3d, vec3d1);
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
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(xLength, yLength, zLength).inflate(getLaserLength()));//指定範囲内のEntityをリストに登録

		for (int j = 0; j < list.size(); j++)
		{
			Entity entity1 = list.get(j);//entity1にリストの先端のentityを保存
			//entity1が、当たり判定を取らない　または　entity1が使用者　ならパス
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

	//Entityからの攻撃を受けたときの処理　要は跳ね返す処理
	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource damagesource, float amount)
	{
		return false;
	}

	//レーザーの長さを設定
	@Override
	public void setLaserLength(double length)
	{
		super.setLaserLength(length);
	}
	//レーザーの長さを返す
	@Override
	public double getLaserLength()
	{
		return super.getLaserLength();
	}

	//レーザーの最大の長さを設定
	@Override
	public void setMaxLaserLength(double length)
	{
		super.setMaxLaserLength(length);
	}
	//レーザーの最大の長さを返す
	@Override
	public double getMaxLaserLength()
	{
		return super.getMaxLaserLength();
	}
}
