package thkaguyamod.entity.effect;

import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** アイシクルソードの氷ブロック */
public class EntityCirnoIceBox extends Entity
{
	//同期データ (1.7.10 の dataWatcher 17-18 = 氷の幅・高さ に相当)
	private static final EntityDataAccessor<Float> DATA_ICE_BOX_WIDTH = SynchedEntityData.defineId(EntityCirnoIceBox.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_ICE_BOX_HEIGHT = SynchedEntityData.defineId(EntityCirnoIceBox.class, EntityDataSerializers.FLOAT);

	/** 凍らせた相手 */
	public LivingEntity frozen;
	private int count;

	public EntityCirnoIceBox(EntityType<? extends EntityCirnoIceBox> type, Level level)
	{
		super(type, level);
		//TODO K3-review: 1.7.10 の preventEntitySpawning は 1.21.1 に対応が無い（MISC カテゴリで自然スポーンしない）。
		//サイズは getDimensions() で指定する（1.7.10 の setSize(width + 0.8F, height + 0.8F) に相当）。
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(getIceBoxWidth() + 0.8F, getIceBoxHeight() + 0.8F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(getIceBoxWidth() + 0.8F, getIceBoxHeight() + 0.8F);
	}

	public EntityCirnoIceBox(EntityType<? extends EntityCirnoIceBox> type, Level level, LivingEntity frozenEntity)
	{
		this(type, level);

		frozen = frozenEntity;//凍らせた相手をfrozenに保存
		setIceBoxWidth(1.0F);
		setIceBoxHeight(frozen.getBbHeight());
		refreshDimensions();//setSize 相当

		setPos(frozen.getX(), frozen.getY(), frozen.getZ());
	}

	/** 生成時に呼ばれる */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_ICE_BOX_WIDTH, 0.5F);
		builder.define(DATA_ICE_BOX_HEIGHT, 0.5F);
	}

	protected void setIceBoxWidth(float width)
	{
		this.entityData.set(DATA_ICE_BOX_WIDTH, width);
	}

	public float getIceBoxWidth()
	{
		return this.entityData.get(DATA_ICE_BOX_WIDTH);
	}

	protected void setIceBoxHeight(float height)
	{
		this.entityData.set(DATA_ICE_BOX_HEIGHT, height);
	}

	public float getIceBoxHeight()
	{
		return this.entityData.get(DATA_ICE_BOX_HEIGHT);
	}

	/**
	 * 押すことができるか
	 */
	@Override
	public boolean isPushable()
	{
		return true;
	}

	/**
	 * 他のEntityと衝突するか
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	//照準で狙えるかどうか（1.7.10 の canBeCollidedWith が兼ねていた照準フィルタ相当）
	@Override
	public boolean isPickable()
	{
		return true;
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		super.tick();

		//TODO K3-review: 1.7.10 は frozen の位置に合わせて boundingBox を手で組み立て、
		//width/height フィールドを直接書き換えていた。1.21.1 では当たり判定が
		//EntityDimensions から再計算されるため、refreshDimensions() + setPos() で代替する
		//（1.7.10 の「minY = frozen.posY を基準にする」という上寄せの扱いは
		// 標準の当たり判定中心基準になり、厳密には一致しない）。
		if(frozen != null)
		{
			if(!frozen.isAlive())
			{
				//frozen.deathTime--;
			}
			else
			{
				setIceBoxWidth(getIceBoxWidth());
				setIceBoxHeight(getIceBoxHeight());
				refreshDimensions();
				setPos(frozen.getX(), frozen.getY(), frozen.getZ());
				frozenInIce();
			}
		}

		if(tickCount > 50)
		{
			setIceBoxHeight(getIceBoxHeight() - 0.08F);
		}
		//TODO K3-review: 1.7.10 は frozen が EntityCirno の場合さらに高さを縮めていた。
		//EntityCirno はまだ 1.21.1 に移植されていないため、クラス参照を避けて未移植のままにする。
		//（移植後: if(frozen instanceof EntityCirno) setIceBoxHeight(getIceBoxHeight() - 0.16F);）

		if(getIceBoxHeight() <= -0.7F)
		{
			if(!level().isClientSide)
			{
				discard();
			}
		}
	}

	//EntityのデータをNBTに書き込む
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("count", (short)count);
	}

	//NBTからEntityのデータを読み込む
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		// # fix (audit-nbt-zero): 缺键即 0 会把构造期写好的 count 覆盖掉。
		//   count 本身不参与逻辑（移植已改用 tickCount），但保持「只在有键时覆盖」与
		//   EntityDanmakuMob 的 I19 修法一致，避免 0 覆盖噪声。
		if(nbtTagCompound.contains("count"))
		{
			count = nbtTagCompound.getShort("count");
		}
	}

	//時間操作空間に入っていた時の処理（氷漬けの処理）
	protected void frozenInIce()
	{
		//Entityを１フレーム前の状態に、できる限りもっていく処理
		frozen.setPos(frozen.xo, frozen.yo, frozen.zo);
		//motionX/Y/Z *= 0.000000001 に相当
		Vec3 motion = frozen.getDeltaMovement();
		frozen.setDeltaMovement(motion.x * 0.000000001, motion.y * 0.000000001, motion.z * 0.000000001);
		frozen.setYRot(frozen.yRotO);
		frozen.setXRot(frozen.xRotO);
		frozen.tickCount--;//誕生してからのカウントを増やさない
		frozen.fallDistance -= 0.0F;//落下距離を変動しないように

		frozen.setYHeadRot(frozen.yHeadRotO);

		//TODO K3-review: 1.7.10 の EntityLivingBase.attackTime++（int）に相当するフィールドは
		//1.21.1 に無い（attackAnim は float の描画用）。attackAnim を1増やす近似に留めた。
		frozen.attackAnim += 1.0F;
		if(!frozen.isAlive())
		{
			frozen.deathTime = 0;
		}

		if(frozen instanceof Creeper creeper)//EntityCreeperに属しているなら
		{
			creeper.setSwellDir(-1);//爆発のカウントを戻す
			//TODO K3-review: timeSinceIgnited は 1.21.1 では SwellGoal 内に隠蔽され外部から戻せない。
		}
		else if(frozen instanceof Ghast)
		{
			//TODO K3-review: 1.7.10 の EntityGhast.attackCounter は 1.21.1 では
			//GhastShootFireballGoal.chargeTime に移動しており外部から減算できない。未移植。
		}
		if(frozen instanceof TamableAnimal)
		{
			//座っている動物は重力がなくなると立つみたい　わからない程度に重力をかける処理
			Vec3 m = frozen.getDeltaMovement();
			frozen.setDeltaMovement(m.x, m.y - 0.000001D, m.z);
		}
		//プレイヤーの動きを止める
		if(frozen instanceof ServerPlayer player)
		{
			player.connection.teleport(player.xo, player.yo, player.zo, player.getYRot(), player.getXRot(), Set.of());
		}
	}
}