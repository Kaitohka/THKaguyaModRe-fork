package thkaguyamod.entity.shot;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
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

		// # perf: B1 — 24 格立方の全 Entity 取得 → 候補全員に DDA、という 1.7.10 の毎 tick スキャンを、
		//   親クラス EntityTHShot の共通実装（4 tick ごとの再選択 + 値の小さい順の二段式）に寄せた。
		//   ここでしか使わない nearDistance/nearAngle は選別に使われていなかったため削除した。
		LivingEntity nearEntity = getHomingTarget(false);

		//追尾対象がいるならば
		if(nearEntity != null)
		{
			float homingLevel = 4.0F;
			Vec3 targetVec = new Vec3(nearEntity.getX() - getX(), nearEntity.getY() + nearEntity.getEyeHeight() - getY(), nearEntity.getZ() - getZ());
			targetVec = THShotLib.getVectorNomalize(targetVec);//単位ベクトルに変換
			Vec3 shotVec = this.getShotVector();
			Vec3 rotate = THShotLib.getOuterProduct(shotVec, targetVec);
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
