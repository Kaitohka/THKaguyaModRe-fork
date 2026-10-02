package thkaguyamod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;

/**
 * 霊撃札
 * 霊撃により相手を吹き飛ばし、弾も消し飛ばす
 */
public class EntitySpiritualStrikeTalisman extends Entity
{
	/** 使用者 */
	public LivingEntity userEntity;

	private int count;

	public EntitySpiritualStrikeTalisman(EntityType<? extends EntitySpiritualStrikeTalisman> type, Level level)
	{
		super(type, level);
		noCulling = true;//中心が画面から外れても描画される（1.7.10 の ignoreFrustumCheck に相当）
		//TODO K3-review: 1.7.10 の preventEntitySpawning / yOffset は 1.21.1 に対応が無い。
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(0.2F, 0.2F) / yOffset = 0.0F に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.2F, 0.2F);
	}

	public EntitySpiritualStrikeTalisman(EntityType<? extends EntitySpiritualStrikeTalisman> type, Level level, LivingEntity living)
	{
		this(type, level);

		userEntity = living;//使用者をuserEntityに保存
		THKaguyaLib.itemEffectFollowUser(this, userEntity, 2.0D, 30F);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	/**
	 * 押すことができるか
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/**
	 * 他のEntityと衝突するか 右クリックできるかもこれで判定
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		//10ticksで消滅
		if(tickCount > 3)
		{
			if(!level().isClientSide)
			{
				discard();
			}
		}

		//***************************//
		double effectiveBoundary = (double)tickCount * 4.0D;//有効範囲
		//効果範囲内のEntityを全て取得
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

		if(list != null && list.size() > 0)//取得したリストにEntityがいるなら
		{
			for(int j1 = 0; j1 < list.size(); j1++)//取得したEntityリストの最初から最後まで見る
			{
				Entity entity = list.get(j1);
				if(entity == null)//Entityがいるなら
				{
					continue;
				}
				//それが使用者でないなら
				if(entity != userEntity && entity.getVehicle() != userEntity)
				{
					double distance = entity.distanceTo(this);
					if(distance <= effectiveBoundary)
					{
						Vec3 angle = THShotLib.angle_ToPos(THShotLib.pos_Entity(this),
								THShotLib.pos(entity.getX(), entity.getY(), entity.getZ()));
						double effectivePower = (effectiveBoundary - distance) * 0.1D;

						if(entity instanceof LivingEntity)
						{
							//1.7.10 の EntityAnimal / EntityVillager の除外に対応
							if(!(entity instanceof Animal) && !(entity instanceof AbstractVillager))
							{
								//1.7.10 の motionX/Y/Z 直接代入に相当
								entity.setDeltaMovement(
										angle.x * effectivePower,
										(/*angle.y +*/ 0.5D) * effectivePower,
										angle.z * effectivePower);
							}
						}
						else if(entity instanceof EntityTHShot shot)
						{
							shot.shotFinishBonus();
						}
					}
				}
			}
		}

		//************************************************************************************//
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("count", (short)count);
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		count = nbtTagCompound.getShort("count");
	}
}