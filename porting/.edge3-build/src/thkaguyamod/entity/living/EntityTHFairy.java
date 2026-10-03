package thkaguyamod.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.item.ItemTHShot;
import thkaguyamod.registry.DanmakuPatternRegistry;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** 妖精 */
public class EntityTHFairy extends EntityDanmakuMob
{

	public EntityTHFairy(EntityType<? extends EntityTHFairy> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(0.6F, 1.5F) に相当）
		lastTime = 0;

		xpReward = 5;//経験値の量

		setForm((byte)random.nextInt(3));
		setPattern(random.nextInt(Math.max(DanmakuPatternRegistry.pattern.size(), 1)));

		lostTarget = 0;
		this.setSpeed(0.3D);
		this.setSpecies(SPECIES_FAIRY);
		isFlyingMode = true;

		this.setAttackDistance(8.0D);
		this.setDetectionDistance(8.0D);
		this.setFlyingHeight(2);
	}

	/** 妖精の攻撃モーション（1.7.10 は dataWatcher 17 を流用していた） */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_FAIRY_ATTACK_MOTION =
			net.minecraft.network.syncher.SynchedEntityData.defineId(EntityTHFairy.class,
					net.minecraft.network.syncher.EntityDataSerializers.BYTE);

	/** 属性（1.7.10 の applyEntityAttributes に相当） */
	public static AttributeSupplier.Builder createAttributes()
	{
		return EntityDanmakuMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 2.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.25D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
				.add(Attributes.FOLLOW_RANGE, 30.0D);
	}

	/**
	 * 生成時に一度だけ呼ばれる（1.7.10 の entityInit は EntityDanmakuMob 側にあるため、
	 * ここでは妖精固有の同期データのみ定義する）。
	 */
	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_FAIRY_ATTACK_MOTION, (byte)0);
	}

	/**
	 * 1.7.10 の onSpawnWithEgg に相当。旧実装は使い魔の生成がコメントアウトされていて
	 * 実質 super 呼び出しのみだったため、SpawnGroupData をそのまま返すだけにしている。
	 */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
			MobSpawnType spawnType, SpawnGroupData spawnGroupData)
	{
		return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
	}

	protected void setPattern(int patternId)
	{
		String patternKey = DanmakuPatternRegistry.pattern.get(patternId);
		int shot = DanmakuPatternRegistry.form.get(patternKey);

		setDanmakuPattern(DanmakuPatternRegistry.danmaku.get(patternKey));
		danmakuSpan = DanmakuPatternRegistry.span.get(patternKey);
		speedRate = DanmakuPatternRegistry.speed.get(patternKey);
		shotForm = (byte)((shot - (shot % 8)) / 8);
		shotColor = (byte)(shot % 8);
	}

	/**
	 * 死んでいるときに呼ばれる（1.7.10 の onDeathUpdate）
	 *
	 * <p>ハロウィン判定は {@code worldObj.getCurrentDate()} → {@link java.time.LocalDate#now()}、
	 * 地獄判定は {@code provider.isHellWorld} → ディメンションキー比較に置き換えている。
	 */
	/** ハロウィン／地獄ではゾンビ妖精として復活するため die() を遮断する */
	@Override
	protected boolean canInterceptDeath()
	{
		java.time.LocalDate calendar = java.time.LocalDate.now();
		boolean halloween = (calendar.getMonthValue() == 10 && calendar.getDayOfMonth() == 31)
				&& !(this instanceof EntitySunFlowerFairy);
		return level().dimension() == net.minecraft.world.level.Level.NETHER || halloween;
	}

	@Override
	protected void tickDeath()
	{
		java.time.LocalDate calendar = java.time.LocalDate.now();

		boolean halloween = (calendar.getMonthValue() == 10 && calendar.getDayOfMonth() == 31)
				&& !(this instanceof EntitySunFlowerFairy);

		if(level().dimension() == Level.NETHER || halloween)
		{
			if(this.getDetectionDistance() > 2.0D)
			{
				this.setFlyingHeight(0);
				this.setDetectionDistance(this.getDetectionDistance() - 0.03D);
				this.setTarget(null);
			}
			else
			{
				setHealth(2.0F);
				setDetectionDistance(8.0D);
				setAttackDistance(2.0D);
				setFlyingHeight(2);
				this.setSpeed(0.03D);
				setDanmakuPattern(0);
				danmakuSpan = 50 - (ModConfig.danmakuLevel * 10);
				speedRate = 0.2F;
			danmakuDying = false;
				shotForm = (byte)(THShotLib.LIGHT[0] / 8);
				shotColor = (byte)(THShotLib.RED);
				this.setForm((byte)-1);
			}
		}
		else
		{
			super.tickDeath();
		}
	}

	/** 毎tick呼ばれる処理（1.7.10 の onUpdate） */
	@Override
	public void tick()
	{
		//体力がないなら動かない
		if(this.getHealth() <= 0)
		{
			setDeltaMovement(0.0D, 0.0D, 0.0D);
		}

		if(isTimeStopped())
		{
			return;
		}
		super.tick();
		if(this.attackCounter > danmakuSpan)
		{
			attackCounter = 0;
		}
	}

	/**
	 * 弾を出したLivingEntityを返す（要は発射主）
	 * @return
	 */
	public net.minecraft.world.entity.LivingEntity getShooter()
	{
		return this;
	}

	//弾幕の処理
	@Override
	public void danmakuPattern(int level)
	{
		//攻撃カウンターが０でないなら弾幕を出さない
		if(attackCounter > 0)
		{
			return;
		}

		//見ている方向ベクトルを取得
		Vec3 look = THShotLib.angle(getShooter().getYRot(), getShooter().getXRot());
		//方向ベクトルにコンフィグの設定でランダム性を与える
		look = THShotLib.angle_LimitRandom(look, ModConfig.danmakuAccuracy);

		int d = getDanmakuPattern();//弾幕のパターン値を記憶

		boolean isShot = shotForm < 32;//弾ならtrue
		boolean isLaser = shotForm == 64;//レーザーならtrue
		int shot = shotForm * 8 + shotColor;//
		ShotData shotData = ShotData.shot(shotForm, shotColor, 0, 80, special);
		LaserData laserData = LaserData.laser(THShotLib.LASER_NORMAL, shotColor, 0.1F, 2.0F, 3.0F, 0, 80, special);

		//発射音を出す
		THShotLib.playShotSound(this);

		//速度差のある弾を一点に飛ばす
		if(d >= 0 && d <= 999)
		{
			int way = d % 100 + 1;
			int num = d / 100 + 1;
			double speed = speedRate;
			double slowDownRate = speed / num;
			if(isShot)
			{
				for(int i = 0; i < num; i++)
				{
					THShotLib.createSphereShot(getShooter(), pos(), look, speed, shotData, way, 0F);
					speed -= slowDownRate;
				}
			}
			else if(isLaser)
			{
				for(int i = 0; i < num; i++)
				{
					THShotLib.createSphereLaserA(getShooter(), getShooter(), pos(), look, speed, speed, 0.0D, gravity_Zero(), laserData, way, 0F, 0F);
					speed -= slowDownRate;
				}
			}
		}
		//5,10,15,20,25...165,170,175,180度に扇状の弾幕。弾数は角度÷5発
		else if(d >= 1000 && d <= 1999)
		{
			int num = d % 100 + 1;
			float span = (d - 1000) / 50F + 1F;
			if(isShot)
			{
				THShotLib.createWideShot(getShooter(), pos(), look, (double)speedRate, (double)speedRate, 0.0D, shotData, num, num * span);
			}
			else if(isLaser)
			{
				THShotLib.createWideLaserA(getShooter(), pos(), look, (double)speedRate, (double)speedRate, 0.0D, laserData, num, num * span);
			}
		}
		//2～36way全方位弾
		else if(d >= 2000 && d <= 2999)
		{
			int way = d % 100 + 1;
			int num = (d - 2000) / 100 + 1;
			double speed = speedRate;
			double slowDownRate = speed / num;
			if(isShot)
			{
				for(int i = 0; i < num; i++)
				{
					THShotLib.createCircleShot(getShooter(), pos(), look, speed, shotData, way);
					speed -= slowDownRate;
				}
			}
			else if(isLaser)
			{
				for(int i = 0; i < num; i++)
				{
					THShotLib.createCircleLaserA(getShooter(), pos(), look, speed, laserData, way);
					speed -= slowDownRate;
				}
			}
		}
		//１発の弾を０～２０度の角度にランダムに発射する
		else if(d >= 3000 && d <= 3999)
		{
			float shotSpan = d % 100 + 1F;
			int num = (d - 3000) / 100 + 1;
			if(isShot)
			{
				THShotLib.createRandomRingShot(getShooter(), pos(), look, speedRate, shotData, num, shotSpan);
			}
			else if(isLaser)
			{
				//TODO K3-review: 1.7.10 でも createRandomRingLaserA はコメントアウトされていた（レーザーのランダムリングは未実装）。
			}
		}
		else if(d >= 4000 && d <= 4999)
		{
			float shotSpan = ((d - 4000) % 100 + 1F) * 1.8F;
			int way = (d - 4000) / 100 * ((d % 100) / 10) + 3;
			THShotLib.createRingShot(getShooter(), pos(), look, speedRate, shotData, way, 0, shotSpan, random.nextFloat() * 360F);
		}
		else if(d >= 5000 && d <= 5999)
		{
			int num = d % 100 + 1;
			THShotLib.createRandomRingShot(getShooter(), pos(), look, speedRate, shotData, num, 360F);
		}
	}

	//周りの妖精を呼び出すことができるか
	@Override
	protected boolean canFairyCall()
	{
		return true;
	}

	//妖精を呼び出せる最大距離
	@Override
	protected double getFairyCallDistance()
	{
		return 30.0D;
	}

	//倒れたときに落とすアイテム（1.7.10 の dropFewItems。getDropItem は dropCustomDeathLoot に統合）
	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.damagesource.DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		super.dropCustomDeathLoot(level, damageSource, hasBeenAttackedByPlayer);

		if(getForm() < 0)
		{
			return;
		}

		//1.7.10 の getDropItem() = power_item（ダメージを受けたプレイヤーがいる場合のみ）
		if(hasBeenAttackedByPlayer)
		{
			//TODO K3-review: 1.7.10 の dropFewItems は「死亡時に倒した側のアイテム」だけでなく
			//getDropItem() の power_item も常にドロップしていた（LivingEntity.dropFewItems 経由）。
			//1.21.1 には dropFewItems が無いため、ここで代替として 1 個落とす。
			this.spawnAtLocation(new ItemStack(ModItems.POWER_UP_ITEM_SMALL.get()));

			if(this.getMaxHealth() == 10.0F)
			{
				this.dropShotItem(ItemTHShot.SMALL, random.nextInt(12), 7, 8, THShotLib.RED, 0, 0, 2);

				this.dropPowerUpItem(this.random.nextInt(20) + this.random.nextInt(3));

				this.dropPointItem(this.random.nextInt(20) + this.random.nextInt(3));
			}
			else
			{
				this.dropShotItem(ItemTHShot.SMALL, random.nextInt(2), 3, 0, THShotLib.RED, 0, 0, 2);

				this.dropPowerUpItem(this.random.nextInt(5) + this.random.nextInt(1));

				this.dropPointItem(this.random.nextInt(5) + this.random.nextInt(1));
			}
		}
	}

	//一つのチャンクに湧く最大数
	@Override
	public int getMaxSpawnClusterSize()
	{
		return 10;
	}

	/**
	 * 自然スポーンするときに呼ばれる。trueならスポーンする（1.7.10 の getCanSpawnHere）
	 */
	@Override
		public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		//P19: 自然生成の全条件（config・確率・地形・明るさ）は ModSpawns の SpawnPlacement 謂詞に一元化。
		//ここで再判定すると確率が二重に掛かる（妖精 50%→25% 等）ため、実体側は常に true。
		return true;
	}

	//妖精の攻撃モーションを返す
	public byte getFairyAttackMotion()
	{
		return this.entityData.get(DATA_FAIRY_ATTACK_MOTION);
	}

	//妖精の攻撃モーションを設定
	public void setFairyAttackMotion(byte motion)
	{
		this.entityData.set(DATA_FAIRY_ATTACK_MOTION, motion);
	}

	/**
	 * 内部名を返す。言語ファイルで使われるもの
	 *
	 * <p>TODO K3-review: 1.7.10 は EntityList.getEntityString(this) で内部名を引き、
	 * form >= 32 のとき "THGhost" に差し替えて "entity.&lt;name&gt;.name" を翻訳していた。
	 * 1.21.1 では登録名が namespaced（thkaguyamod:th_fairy）になったため、ここでは
	 * 翻訳キーを Component で組み立てる（クライアント側の表示は旧来のキー体系に合わせる必要がある）。
	 */
	@Override
	public net.minecraft.network.chat.Component getName()
	{
		String s = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();

		if(getForm() >= 32)
		{
			s = "THGhost";
		}

		return net.minecraft.network.chat.Component.translatable("entity." + s + ".name");
	}

	/**
	 * Initialize this creature.
	 */
	public void initCreature() {}
}