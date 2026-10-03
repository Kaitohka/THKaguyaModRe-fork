package thkaguyamod.entity;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModBlocks;

/** 小神霊 */
public class EntityDivineSpirit extends Entity
{
	private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(EntityDivineSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(EntityDivineSpirit.class, EntityDataSerializers.INT);

	/** 色ID (DanmakuConstants の色順) に対応する小神霊ブロック。1.7.10 の divine_spirit + メタデータに相当 */
	private static final Block[] COLOR_BLOCKS = {
		ModBlocks.DIVINE_SPIRIT_RED.get(),
		ModBlocks.DIVINE_SPIRIT_BLUE.get(),
		ModBlocks.DIVINE_SPIRIT_GREEN.get(),
		ModBlocks.DIVINE_SPIRIT_YELLOW.get(),
		ModBlocks.DIVINE_SPIRIT_PURPLE.get(),
		ModBlocks.DIVINE_SPIRIT_AQUA.get(),
		ModBlocks.DIVINE_SPIRIT_ORANGE.get(),
		ModBlocks.DIVINE_SPIRIT_WHITE.get(),
	};

	private int lastTime;
	private double lastShotMotionX;
	private double lastShotMotionY;
	private double lastShotMotionZ;
	private float lastRotationYaw;
	private float lastRotationPitch;

	private double speed;

	public EntityDivineSpirit(EntityType<? extends EntityDivineSpirit> type, Level level)
	{
		super(type, level);
	}

	public EntityDivineSpirit(EntityType<? extends EntityDivineSpirit> type, Level level, double posX, double posY, double posZ, int t)
	{
		super(type, level);
		setDivineSpiritColor((short)t);
		setDivineSpiritCount(0);
		setPos(posX + getDeltaMovement().x * 0.1D,
				posY + getDeltaMovement().y * 0.1D,
				posZ + getDeltaMovement().z * 0.1D);
		xo = posX;
		yo = posY;
		zo = posZ;
		lastTime = 0;
		speed = 0.0D;
	}

	/**
	 * 押すことができるか返す
	 * @return 押せるならtrue
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	//Entity生成時に一度だけ呼ばれる
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_COUNT, 0);
		builder.define(DATA_COLOR, 0);
	}

	/**
	 * 移動速度を返す
	 * @return 移動速度
	 */
	public double getEntitySpeed()
	{
		Vec3 motion = getDeltaMovement();
		return Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		super.tick();

		//300tickで消滅
		if(getDivineSpiritCount() > 300)
		{
			if(!level().isClientSide)
			{
				discard();
			}
		}

		setDivineSpiritCount(getDivineSpiritCount() + 1);

		//***********Entityの当たり判定を取る****************//
		double effectiveBoundary = 10.0D;//有効範囲
		double distance = 999.0D;
		Player nearPlayer = null;
		//範囲内のEntityを全て取得
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary));

		for (int j1 = 0; j1 < list.size(); j1++)//取得したEntityリストの最初から最後まで見る
		{
			Entity entity = list.get(j1);
			if(entity instanceof Player player)//プレイヤーなら
			{
				if(distance > entity.distanceTo(this))
				{
					distance = entity.distanceTo(this);
					nearPlayer = player;
				}
			}
		}

		if(nearPlayer != null)
		{
			if(speed < 0.5D)
			{
				speed += 0.03D;
			}

			setRot((float)Math.atan2(nearPlayer.getX() - getX(), nearPlayer.getZ() - getZ()) / (float)Math.PI * 180F,
					(float)Math.atan2(nearPlayer.getY() + nearPlayer.getEyeHeight() - getY(), Math.sqrt((nearPlayer.getX() - getX()) * (nearPlayer.getX() - getX()) + (nearPlayer.getZ() - getZ()) * (nearPlayer.getZ() - getZ()))) / (float)Math.PI * 180F);
		}
		else
		{
			speed -= 0.03D;
			if(speed < 0.0D)
			{
				speed = 0.0D;
			}
		}

		Vec3 vec = THShotLib.getVecFromAngle(getYRot(), getXRot(), speed);
		setPos(getX() - vec.x, getY() - vec.y, getZ() + vec.z);

		if(tickCount > lastTime)
		{
			lastTime = tickCount;
			Vec3 motion = getDeltaMovement();
			lastShotMotionX = motion.x;
			lastShotMotionY = motion.y;
			lastShotMotionZ = motion.z;
			lastRotationYaw = getYRot();
			lastRotationPitch = getXRot();
		}
	}

	/**
	 * 保存するデータの書き込み
	 * @param tag : NBTタグ
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putShort("color", (short)getDivineSpiritColor());
	}

	/**
	 * 保存したデータの読み込み
	 * @param tag : NBTタグ
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		setDivineSpiritColor(tag.getShort("color"));
	}

	/**
	 * プレイヤーと接触したときの処理
	 */
	@Override
	public void playerTouch(Player player)
	{
		int color = getDivineSpiritColor();
		Block block = COLOR_BLOCKS[Math.min(Math.max(color, 0), COLOR_BLOCKS.length - 1)];
		if (player.getInventory().add(new ItemStack(block, 1)))
		{
			level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 4.0F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 0.3F);//1.7.10 の "random.pop"
			player.take(this, 1);
		}

		if(!level().isClientSide)
		{
			discard();
		}
	}

	/**
	 * 当たり判定を取るか返す
	 * @return 常に false（diag-pose）
	 *
	 * <p>小神霊は使用者の目の高さに 1.2 格の box で生まれる（三種の目/御札の右クリック）。
	 * true を返すと 1.21.1 の姿勢フィット判定がプレイヤーの立ち姿勢を弾き、
	 * 自動で {@code Pose.SWIMMING}（匍匐・眼高 0.4）に落ちる＝「右键で趴下」。
	 * 詳細は {@code EntityTHShot#canBeCollidedWith} を参照（1.7.10 には姿勢機構が無い）。
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}

	//照準・弾丸命中はこちらが担う（canBeHitByProjectile の既定実装が isPickable() を見る）
	//照準で狙えるかどうか（1.7.10 の canBeCollidedWith が兼ねていた照準フィルタ相当）
	@Override
	public boolean isPickable()
	{
		return true;
	}

	/**
	 * 小神霊のカウントを設定
	 * @param count 設定する小神霊のカウント
	 */
	public void setDivineSpiritCount(int count)
	{
		this.entityData.set(DATA_COUNT, count);
	}

	/**
	 * 小神霊のカウントを返す
	 * @return 小神霊のカウント
	 */
	public int getDivineSpiritCount()
	{
		return this.entityData.get(DATA_COUNT);
	}

	/**
	 * 小神霊の色を設定
	 * @param color 設定する小神霊の色
	 */
	public void setDivineSpiritColor(int color)
	{
		this.entityData.set(DATA_COLOR, color);
	}

	/**
	 * 小神霊の色を返す
	 * @return 小神霊の色
	 */
	public int getDivineSpiritColor()
	{
		return this.entityData.get(DATA_COLOR);
	}
}
