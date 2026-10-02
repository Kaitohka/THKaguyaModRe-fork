package thkaguyamod.entity.shot;

import java.util.List;
import java.util.Optional;

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

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;

/** お空の核弾 */
public class EntityNuclearShot extends EntityTHShot
{
	/** すでに放たれたかどうかのフラグ */
	public boolean shootingFlag;

	//ワールド読み込み時に呼び出されるコンストラクト
	public EntityNuclearShot(EntityType<? extends EntityNuclearShot> type, Level level)
	{
		super(type, level);
	}

	/**
	 * 核弾を生成する。
	 * 引数はあるものすべて（ShotDataを使用しない）
	 */
	public EntityNuclearShot(EntityType<? extends EntityNuclearShot> type, Level level, LivingEntity user, Entity entity,
		Vec3 pos, Vec3 angle, float slope,
		Vec3 rotate, float rotationSpeed, int rotationEnd,
		double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
		int color, float size, float damage, int delay, int end, int special)
	{
		super(type, level, user, entity, pos, angle, slope, rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
				gravity, color, size, damage, delay, end, special);
		shootingFlag = false;
	}

	/**
	 * 核弾を生成する。
	 * 引数はあるものすべて（ShotDataを使用する）
	 */
	public EntityNuclearShot(EntityType<? extends EntityNuclearShot> type, Level level, LivingEntity user, Entity entity,
			Vec3 pos, Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
			ShotData shot)
	{
		this(type, level, user, entity, pos, angle, slope, rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
				gravity, shot.color, shot.size, shot.damage, shot.delay, shot.end, shot.special);
	}

	//ショットが存在する限り呼び出されるメソッド
	@Override
	public void tick()
	{
		if(user != null)
		{
			if(user instanceof Player player)
			{
				if(player.isUsingItem() && !shootingFlag)
				{
					Vec3 vec3 = THShotLib.getVecFromAngle(-getYRot() + 20F, -getXRot(), 0.3D + getShotSize() / 2.0D);

					setPos(user.getX() + vec3.x, THShotLib.getPosYFromEye(user, -0.1D) + vec3.y, user.getZ() + vec3.z);
					setRot(-user.getYRot(), -user.getXRot());

					if(getShotSize() < 6.00F)
					{
						setShotSize(getShotSize() + 0.06F);
					}
					if(!player.getMainHandItem().isEmpty())
					{
						if(!player.getMainHandItem().is(ModItems.NUCLEAR_CONTROL_ROD.get()))
						{
							if(!level().isClientSide)
							{
								level().explode(this, this.getX(), this.getY(), this.getZ(), getShotSize(), Level.ExplosionInteraction.BLOCK);
								discard();
							}
							return;
						}
					}
					else
					{
						if(!level().isClientSide)
						{
							level().explode(this, this.getX(), this.getY(), this.getZ(), getShotSize(), Level.ExplosionInteraction.BLOCK);
							discard();
						}
						return;
					}
					return;
				}
				else if(shootingFlag == false)
				{
					if(!player.getMainHandItem().isEmpty())
					{
						if(!player.getMainHandItem().is(ModItems.NUCLEAR_CONTROL_ROD.get()))
						{
							if(!level().isClientSide)
							{
								level().explode(this, this.getX(), this.getY(), this.getZ(), getShotSize(), Level.ExplosionInteraction.BLOCK);
								discard();
							}
							return;
						}
					}
					else
					{
						if(!level().isClientSide)
						{
							level().explode(this, this.getX(), this.getY(), this.getZ(), getShotSize(), Level.ExplosionInteraction.BLOCK);
							discard();
						}
						return;
					}
					tickCount = 1;
					setVector();
					shotAcceleration = 0.2D;
					shootingFlag = true;
				}
			}
		}
		else
		{
			if(!level().isClientSide)
			{
				discard();
				return;
			}
		}

		shotDamage = getShotSize() * 8.0F;

		super.tick();
	}

	/**
	 * 使用者との当たり判定をとるかを返す
	 * @return 使用者と当たり判定をとるならtrue
	 */
	@Override
	public boolean userHitCheck(Entity entity)
	{
		if(this.shotSpecial == SPECIAL_SPOILER02 || this.shotSpecial == SPECIAL_SPOILER01)
		{
			return true;
		}
		return !entity.is(user);
	}

	@Override
	public HitResult hitEntityCheck(HitResult hitResult, Vec3 vec3d, Vec3 vec3d1)
	{
		float hitSize = getShotSize() * 0.5F;
		//ここから移動量分の線分を作り、それに弾の大きさの２倍の肉付けをし直方体を作る。それに当たったEntityをリスト化する
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(motionX, motionY, motionZ).inflate(hitSize));//指定範囲内のEntityをリストに登録

		for (int j = 0; j < list.size(); j++)
		{
			Entity entity1 = list.get(j);//entity1にリストの先端のentityを保存
			//entity1が、当たり判定を取らない　または　entity1が使用者　ならパス
			if ( entity1.canBeCollidedWith() &&
				userHitCheck(entity1) &&
				!hitCheckEx(entity1) &&
				//TODO: EntitySakuyaWatch / EntityTHFairy 移植後に 1.7.10 の除外条件を復元する
				!(entity1 instanceof Animal) &&
				!(entity1 instanceof AbstractVillager) &&
				(entity1 instanceof LivingEntity || entity1 instanceof EntityTHShot))
			{
				//判定を弾の大きさに変更
				AABB axisalignedbb = entity1.getBoundingBox().inflate(hitSize);
				Optional<Vec3> intercept = axisalignedbb.clip(vec3d, vec3d1);

				//この判定で当たっているなら
				if (intercept.isPresent())
				{
					onImpact(new EntityHitResult(entity1));
				}
			}
		}

		//当たっているなら当たった場合の処理をする
		if (hitResult != null)
		{
			onImpact(hitResult);
		}

		return hitResult;
	}

	//ブロックやEntityに当たった時の処理
	@Override
	protected boolean onImpact(HitResult hitResult)
	{
		//当たった時の処理
		if (!level().isClientSide)
		{
			Entity hitEntity = hitResult instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;

			//当たったEntityがいるなら
			if ( hitEntity != null )
			{
				//それがEntityTHShotに属していないなら
				if(!(hitEntity instanceof EntityTHShot))
				{
					//Entityに当たった時の特殊な処理
					entityHitSpecialProcess(hitEntity);
					//指定したダメージ分の魔法ダメージを与える
					if(!hitEntity.is(user))
					{
						hitEntity.hurt(damageSources().indirectMagic(this, user), shotDamage);
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
					}
				}
			}
			else
			{
				if(hitResult instanceof BlockHitResult blockHit)
				{
					if(blockHitSpecialProcess(blockHit))
					{
						delete();//ブロックに当たったら消滅
						return true;
					}
				}
			}
		}
		return false;
	}

	//当たり判定の追加
	@Override
	public boolean hitCheckEx(Entity entity)
	{
		return false;
	}
}
