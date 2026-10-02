package thkaguyamod.entity.shot;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.init.ModItems;

/** へにょりレーザー系の共通処理 */
public class EntityTHHenyoriLaser extends EntityTHLaser
{

	/** ワールド読み込み時に呼び出されるコンストラクト */
	public EntityTHHenyoriLaser(EntityType<? extends EntityTHHenyoriLaser> type, Level level)
	{
		super(type, level);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
	}

	/** レーザーを消す。同時にレーザーが弾の素になる */
	@Override
	public void shotFinishBonus()
	{
		double zPos = 0.0D;
		while(zPos < getLaserLength())
		{
			ItemEntity itemEntity = new ItemEntity(level(), getX() - angle.x * zPos, getY() - angle.y * zPos, getZ() - angle.z * zPos, new ItemStack(ModItems.SHOT_MATERIAL.get(), 1));
			level().addFreshEntity(itemEntity);
			itemEntity.lifespan = 300;//アイテムがすぐ消滅するよう設定（15秒で消える。1.7.10 の age = 5700 に相当）
			zPos += 0.3D;
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

	/** 衝突時の処理 */
	@Override
	public void hitCheck()
	{
		double width = getShotSize();

		float angleSpanY = (float)Math.PI / 30F;
		float baseAngle = (float)getAnimationCount() * angleSpanY;
		double moveLength = 0.0D;

		double nowX, nowY, nowZ, nextX, nextY, nextZ;
		nowX = getX();
		nowY = getY();
		nowZ = getZ();

		for(int i = 0; i < 13; i++)
		{
			baseAngle += angleSpanY;
			int flontCount = i + 1;
			moveLength = flontCount * getLaserLength() / 13.0D;
			nextX = nowX + angle.x * moveLength - Math.sin(baseAngle) * 3.0D + motionX;
			nextY = nowY + angle.y * moveLength + motionY;
			nextZ = nowZ + angle.z * moveLength + Math.cos(baseAngle) * 3.0D + motionZ;

			//始点（現在地）
			Vec3 vec3d = new Vec3(nowX, nowY, nowZ);
			//終点（現在地に移動量を足した点）
			Vec3 vec3d1 = new Vec3(nextX, nextY, nextZ);
			//始点と終点からブロックとの当たりを取得（液体を含む）
			BlockHitResult blockHit = clipBlocks(vec3d, vec3d1);
			//何らかのブロックに当たっているなら
			if (blockHit != null)
			{
				//終点を当たった点に変更
				vec3d1 = blockHit.getLocation();
				onImpact(blockHit);
			}

			//ここから移動量分の線分を作り、それに弾の大きさの２倍の肉付けをし直方体を作る。それに当たったEntityをリスト化する
			double xLength = motionX + Math.sin(getYRot() / 180F * 3.141593F) * Math.cos(getXRot() / 180F * 3.141593F) * moveLength;
			double yLength = motionY + Math.sin(getXRot() / 180F * 3.141593F) * moveLength;
			double zLength = motionZ + Math.cos(getYRot() / 180F * 3.141593F) * Math.cos(getXRot() / 180F * 3.141593F) * moveLength;
			//性能上の負債を返済: 1.7.10 はここで毎セグメント dummyEntity を new して getEntitiesWithinAABBExcludingEntity
			// に渡していたが、除外対象が dummy（未登録Entity）なので実質何も除外していなかった。純粋計算に置き換え、this を除外する。
			List<Entity> list = level().getEntities(this, this.getBoundingBox().expandTowards(xLength, yLength, zLength).inflate(width));//指定範囲内のEntityをリストに登録

			for (int j = 0; j < list.size(); j++)
			{
				Entity entity1 = list.get(j);//entity1にリストの先端のentityを保存
				//entity1が、当たり判定を取らない　または　EntityTHShotならパス
				if ( entity1.canBeCollidedWith() &&
					!hitCheckEx(entity1) &&
					!(entity1 instanceof EntityTHShot) &&
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
			nowX = nowX + angle.x * moveLength - Math.sin(baseAngle) * 3.0D;
			nowY = nowY + angle.y * moveLength;
			nowZ = nowZ + angle.z * moveLength + Math.cos(baseAngle) * 3.0D;
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
					hitEntity.hurt(damageSources().indirectMagic(this, user), shotDamage);
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
			//EntityTHShotに属しているなら（1.7.10 でも未実装。弾同士の相殺はしない）
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
			delete();
		}

		return true;
	}
}
