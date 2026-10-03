package thkaguyamod.entity.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModSounds;

/** ミニ八卦炉 */
public class EntityMiniHakkero extends Entity
{
	/** 使用者 */
	public LivingEntity user;
	private LivingEntity tgEntity;
	public int count;
	private float circleAngle;
	public int num;
	private int damage;
	private int lastTime;
	private boolean isSpellCard;

	public EntityMiniHakkero(EntityType<? extends EntityMiniHakkero> type, Level level)
	{
		super(type, level);
		//TODO K3-review: 1.7.10 の preventEntitySpawning / yOffset は 1.21.1 に対応が無い。
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(0.4F, 0.4F) / yOffset = 0.0F に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.4F, 0.4F);
	}

	public EntityMiniHakkero(EntityType<? extends EntityMiniHakkero> type, Level level, LivingEntity entityLivingBase, int da)
	{
		this(type, level);

		user = entityLivingBase;//使用者をuserに保存
		setPos(user.getX() - Math.sin(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F),
				user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) + (double)user.getEyeHeight() - 0.10000000149011612D,
				user.getZ() + Math.cos(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F));//初期位置を設定(x,y,z)
		setYRot(user.getYRot());
		setXRot(user.getXRot());
		tgEntity = null;
		count = 0;
		circleAngle = 0F;
		damage = da;
		isSpellCard = false;

		level().playSound(null, blockPosition(), ModSounds.MASTER_SPARK.get(), SoundSource.NEUTRAL, ModConfig.MasterSparkVol, 1.0F);
		lastTime = 0;

		int special = DanmakuConstants.EXPLOSION01;
		Vec3 look = THShotLib.getVecFromAngle(getYRot(), getXRot());
		//TODO K3-review: 1.7.10 は LaserData.laser(24, 4.2F, 40.0F, 8.0F, 30, 120, special) を渡していた。
		//24 は 1.7.10 の LaserData の色引数としては範囲外（0-7 想定）の値だが、
		//旧コードの値をそのまま踏襲する（見た目/当たり判定の再現性を優先）。
		THShotLib.createLaserB(user, this, THShotLib.pos_Entity(this), look,
				THShotLib.rotate_Default(), 0.0F, 9999, LaserData.laser(24, 4.2F, 40.0F, 8.0F, 30, 120, special), this, 1.0D, 0.0F);
	}

	public EntityMiniHakkero(EntityType<? extends EntityMiniHakkero> type, Level level, LivingEntity entityLivingBase, LivingEntity target)
	{
		this(type, level);

		user = entityLivingBase;//使用者をuserに保存
		setPos(user.getX() - Math.sin(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F),
				user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) + (double)user.getEyeHeight() - 0.10000000149011612D,
				user.getZ() + Math.cos(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F));//初期位置を設定(x,y,z)
		setYRot(user.getYRot());
		setXRot(user.getXRot());
		tgEntity = target;
		count = 0;
		circleAngle = 0F;
		damage = 1;
		level().playSound(null, blockPosition(), ModSounds.MASTER_SPARK.get(), SoundSource.NEUTRAL, ModConfig.MasterSparkVol, 1.0F);
		lastTime = 0;

		isSpellCard = true;

		Vec3 look = THShotLib.getVecFromAngle(getYRot(), getXRot());
		THShotLib.createLaserB(user, this, THShotLib.pos_Entity(this), look,
				THShotLib.rotate_Default(), 0.0F, 9999, LaserData.laser(24, 4.2F, 60.0F, 8.0F, 30, 120, 0), this, 1.0D, 0.0F);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	/**
	 * 押すことができるか？
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	//他のEntityと当たり判定を取るか
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

	//毎tick呼ばれる処理
	@Override
	public void tick()
	{
		super.tick();

		if(!level().isClientSide && user == null)
		{
			if(!isSpellCard)
			{
				THKaguyaLib.itemEffectFinish(this, user, ModItems.MINI_HAKKERO.get());
			}
			else
			{
				discard();
			}
			return;
		}

		if(tickCount <= lastTime)
		{
			return;
		}

		circleAngle += 4.7F;

		/*
		 * TODO K3-review: 1.7.10 のこの箇所には、コメントアウトされた旧マスタースパーク実装
		 * （EntityMasterSpark の生成とレイ判定によるダメージ処理）が大量に残っていた。
		 * 当該コードは 1.7.10 時点ですでにブロックコメントで無効化されていた
		 * （＝実際には動いていない）ため、1.21.1 でも移植せず、生成時の
		 * THShotLib.createLaserB（設置レーザー）だけでマスタースパークを表現する。
		 * これは旧版の実行時挙動と同一である。
		 */

		if(tickCount > lastTime)
		{
			lastTime = tickCount;
		}
		//時間で消滅
		if(tickCount >= 109)
		{
			if(!level().isClientSide)
			{
				if(!isSpellCard)
				{
					THKaguyaLib.itemEffectFinish(this, user, ModItems.MINI_HAKKERO.get());
				}
				else
				{
					discard();
				}
			}
		}
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("count", (short)count);
		nbtTagCompound.putShort("damage", (short)damage);
		nbtTagCompound.putBoolean("isSpellCard", tgEntity != null);
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		count = nbtTagCompound.getShort("count");
		damage = nbtTagCompound.getShort("damage");
		isSpellCard = nbtTagCompound.getBoolean("isSpellCard");
	}

	public float getCircleAngle()
	{
		return circleAngle;
	}
}