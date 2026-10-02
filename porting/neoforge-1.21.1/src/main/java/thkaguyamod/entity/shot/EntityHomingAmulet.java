package thkaguyamod.entity.shot;

import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.BLUE;
import static thkaguyamod.danmaku.DanmakuConstants.FORM_AMULET;
import static thkaguyamod.danmaku.THShotLib.rotate_Default;

/** ホーミングアミュレット */
public class EntityHomingAmulet extends EntityTHShot
{
	private static final EntityDataAccessor<String> DATA_OWNER_NAME = SynchedEntityData.defineId(EntityHomingAmulet.class, EntityDataSerializers.STRING);

	//ワールド読み込み時に呼び出されるコンストラクト
	public EntityHomingAmulet(EntityType<? extends EntityHomingAmulet> type, Level level)
	{
		super(type, level);
	}

	public EntityHomingAmulet(EntityType<? extends EntityHomingAmulet> type, Level level, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity, int color, float size, float damage, int delay, int end, int special)
	{
		super(type, level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration, gravity, color, size, damage, delay, end, special);

		if(user instanceof TamableAnimal tameable)
		{
			LivingEntity owner = tameable.getOwner();
			this.setOwner(owner != null ? owner.getName().getString() : "");
		}
		else
		{
			this.setOwner("");
		}
	}

	//Entity生成時に一度だけ呼ばれる
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_OWNER_NAME, "");
	}

	/**
	 * 弾幕の特殊な動きを記述
	 */
	@Override
	public void specialMotion()
	{
		//大きな拡散アミュレットで、１６tick目なら
		if(tickCount == 16 && this.getShotColor() == 3)
		{
			double vectorXG = 0.0D, vectorYG = 0.0D, vectorZG = 0.0D;
			if(user != null)
			{
				vectorXG = -Math.sin(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * 0.09D;
				vectorYG = -Math.sin(user.getXRot() / 180F * 3.141593F) * 0.09D;
				vectorZG =  Math.cos(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * 0.09D;
				ShotData shot = ShotData.shot(FORM_AMULET, BLUE, 0.4F, 5.0F, 0, 60);
				THShotLib.createSphereShot(user, user, THShotLib.pos_Entity(this), angle, 0F, rotate_Default(), 0F, 9999, 0.6D, 0.6D, 0.0D, THShotLib.gravity(vectorXG, vectorYG, vectorZG), shot, 32, 0.0D, random.nextFloat() * 360F);
			}

			if(!level().isClientSide)
			{
				discard();
			}
			return;
		}
		//小さな拡散アミュレットなら特殊な処理はしない
		if(getShotColor() >= 2)
		{
			return;
		}

		//追尾する動きをする
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(motionX, motionY, motionZ).inflate(24.0D));//指定範囲内のEntityをリストに登録

		LivingEntity nearEntity = null;
		double nearDistance = 999.9D;
		float nearAngle = 180F;
		double nearValue = nearDistance * THShotLib.halfAbsSin(nearAngle / 180F * (float)Math.PI);
		Vec3 shotVec = this.getShotVector();

		for (int j = 0; j < list.size(); j++)
		{
			Entity entitys = list.get(j);//entity1にリストの先端のentityを保存
			//動物や生物でないものなら反応しない
			if ( !(entitys instanceof LivingEntity) || entitys instanceof Animal || entitys instanceof AbstractVillager || entitys == source || entitys == user)
			{
				continue;
			}
			LivingEntity entity1 = (LivingEntity)entitys;
			//生物が死んでいるなら無視する
			if(!entity1.isAlive())
			{
				continue;
			}

			//始点（現在地）
			Vec3 vec3d = new Vec3(getX(), getY(), getZ());
			//終点（対象の目の位置）
			Vec3 vec3d1 = new Vec3(entity1.getX(), entity1.getY() + entity1.getEyeHeight(), entity1.getZ());
			//始点と終点からブロックとの当たりを取得
			HitResult blockHit = clipBlocks(vec3d, vec3d1);
			//何らかのブロックに当たっているなら
			if (blockHit != null)
			{
				continue;
			}

			Vec3 targetVec = new Vec3(entity1.getX() - getX(), entity1.getY() + entity1.getEyeHeight() - getY(), entity1.getZ() - getZ());
			targetVec = THShotLib.getVectorNomalize(targetVec);//単位ベクトルに変換
			float angleSpan = Math.abs(THShotLib.getVectorAndVectorAngle(shotVec, targetVec));
			double toEntity1Distance = Math.sqrt(this.distanceToSqr(entity1.getX(), entity1.getY() + entity1.getEyeHeight(), entity1.getZ()));
			double value = toEntity1Distance * THShotLib.halfAbsSin(angleSpan / 180F * (float)Math.PI);
			if(nearValue > value)
			{
				nearEntity = entity1;
				nearAngle = angleSpan;
				nearValue = value;
				nearDistance = toEntity1Distance;
			}
		}

		//追尾対象がいるならば
		if(nearEntity != null)
		{
			float homingLevel = 4.0F;
			Vec3 targetVec = new Vec3(nearEntity.getX() - getX(), nearEntity.getY() + nearEntity.getEyeHeight() - getY(), nearEntity.getZ() - getZ());
			targetVec = THShotLib.getVectorNomalize(targetVec);//単位ベクトルに変換
			Vec3 rotate = THShotLib.getOuterProduct(this.getShotVector(), targetVec);
			float rotateAngle = THShotLib.getVectorAndVectorAngle(shotVec, targetVec);
			if(rotateAngle > homingLevel)
			{
				rotateAngle = homingLevel;
			}
			else if(rotateAngle < -homingLevel)
			{
				rotateAngle = -homingLevel;
			}
			Vec3 newVec = THShotLib.getVectorFromRotation(rotate, angle, rotateAngle);
			angle = newVec;
			if(!level().isClientSide)
			{
				this.updateMotion();
				this.updateAngle();
			}
		}
	}

	public String getOwnerName()
	{
		return this.entityData.get(DATA_OWNER_NAME);
	}

	public void setOwner(String str)
	{
		this.entityData.set(DATA_OWNER_NAME, str);
	}
}
