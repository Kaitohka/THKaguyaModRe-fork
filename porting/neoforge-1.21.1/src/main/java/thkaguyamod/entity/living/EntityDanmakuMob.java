package thkaguyamod.entity.living;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.EntityTHItem;
import thkaguyamod.entity.effect.EntitySpellCardCircle;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;

/** 弾幕を使うMOBの定義クラス */
public abstract class EntityDanmakuMob extends PathfinderMob
{
	/** 最後に動いた時間 */
	protected int lastTime;

	public int attackCounter = 0;
	public int attackInterval;
	public int courseChangeCooldown = 0;
	public double waypointX;
	public double waypointY;
	public double waypointZ;

	public int lostTarget;

	/** 指定移動するときの方向（正規化ベクトル。速度は {@link #moveVecSpeed} 側に持つ） */
	protected Vec3 moveVec;

	/**
	 * {@link #moveVec} にかける速度。
	 *
	 * <p># perf: A12 — 1.7.10 は {@code moveVec} の成分をその場で speed 倍していた（{@code moveVec.xCoord *= speed}）。
	 * 1.21.1 の {@code Vec3} は不変なので、{@code move()} のたびにスケール済み {@code Vec3} を作ると
	 * 指示 1 回につき 2 個、指令メソッド経由だと合計 3 個の {@code Vec3} が飛んでいた。
	 * 方向（正規化ベクトル）と速度を分けて保持し、実際に必要な {@code tick()} の 1 個だけに減らす。
	 */
	protected double moveVecSpeed = 1.0D;

	/**
	 * # perf: A11 — 飛行高さの地面探索で使い回す可変ブロック座標。
	 * 根因: {@code BlockPos.containing} は毎回新しい BlockPos を作っていた（最大 8 個/tick/怪）。
	 */
	private final BlockPos.MutableBlockPos flyingHeightProbe = new BlockPos.MutableBlockPos();

	/** 指定移動する時間 */
	protected int moveTimer;

	/** 指定移動する最大時間 */
	protected int moveTimerMax;

	/** 停止中かどうか？ */
	protected boolean stop;

	/** ターゲットとの間に取る間合い */
	protected double attackDistance;

	/** 近づくと敵対化する距離 */
	protected double detectionDistance;

	/** 飛ぶ地面からの高さ */
	protected int flyingHeight;

	/** 飛行モードであるか？
	 * @true  飛行モード
	 * @false 着地モード
	 */
	public boolean isFlyingMode;

	/** 妖精などの出す弾幕のパターンの設定 */
	public int danmakuPattern;
	/** 妖精などの出す弾の発射頻度の設定 */
	public int danmakuSpan;
	/** 妖精などの出す弾の形状設定 */
	public byte shotForm;
	/** 妖精などの出す弾の色設定 */
	public byte shotColor;
	/** 妖精などの出す弾の速度設定 */
	public float speedRate;
	/** 妖精などの出す弾の特殊弾設定 */
	public short special;

	/** スペルカードモードか？
	 * @true  スペルカードを使用中
	 * @false スペルカードを使用していない
	 */
	public boolean isSpellCardMode;

	public final int EASY = 1;
	public final int NORMAL = 2;
	public final int HARD = 3;
	public final int LUNATIC = 4;

	//体力ゲージに表示される、名前の英語名
	private String danmakuMobName = "";
	/** 種族ID 1 */
	protected int species_1;
	/** 種族ID 2 */
	protected int species_2;

	//魔法陣の色
	/** 魔法陣の色　赤 */
	public static final int CIRCLE_COLOR_RED = 0;
	/** 魔法陣の色　青 */
	public static final int CIRCLE_COLOR_BLUE = 1;
	/** 魔法陣の色　緑 */
	public static final int CIRCLE_COLOR_GREEN = 2;
	/** 魔法陣の色　黄 */
	public static final int CIRCLE_COLOR_YELLOW = 3;
	/** 魔法陣の色　紫 */
	public static final int CIRCLE_COLOR_PURPLE = 4;
	/** 魔法陣の色　水色 */
	public static final int CIRCLE_COLOR_AQUA = 5;
	/** 魔法陣の色　青 */
	public static final int CIRCLE_COLOR_ORANGE = 6;
	/** 魔法陣の色　白 */
	public static final int CIRCLE_COLOR_WHITE = 7;
	/** 魔法陣の色　レインボー */
	public static final int CIRCLE_COLOR_RAINBOW = 8;

	/** 魔法陣 */
	public EntitySpellCardCircle circle;

	//弾幕MOBの種族

	//人間に属す種族
	/** 種族：人間 */
	public static final int SPECIES_HUMAN = 0;
	/** 種族：半人 */
	public static final int SPECIES_HUMAN_HALF = 1;
	/** 種族：月人 */
	public static final int SPECIES_HUMAN_LUNARIAN = 2;
	/** 種族：仙人 */
	public static final int SPECIES_HUMAN_HERMIT = 3;
	/** 種族：尸解仙*/
	public static final int SPECIES_HUMAN_SHIKAISEN = 4;
	/** 種族：天人 */
	public static final int SPECIES_HUMAN_CELESTIALS = 5;
	/** 種族：聖人 */
	public static final int SPECIES_HUMAN_SAINT = 6;
	/** 種族：小人 */
	public static final int SPECIES_HUMAN_INCHLINGS = 7;
	/** 種族：蓬莱人 */
	public static final int SPECIES_HUMAN_HOURAI = 8;

	//神に属す種族
	/** 種族：神 */
	public static final int SPECIES_GOD = 32;
	/** 種族：現人神 */
	public static final int SPECIES_GOD_ARAHITOGAMI = 33;
	/** 種族：八咫烏 */
	public static final int SPECIES_GOD_YATAGARASU = 34;

	//閻魔
	/** 種族：閻魔 */
	public static final int SPECIES_ENMA = 48;

	//妖精に属す種族
	/** 種族：妖精 */
	public static final int SPECIES_FAIRY = 64;
	/** 種族：氷精 */
	public static final int SPECIES_FAIRY_ICE = 65;

	//幽霊に属す種族
	/** 種族：幽霊 */
	public static final int SPECIES_PHANTOM = 92;
	/** 種族：半霊 */
	public static final int SPECIES_PHANTOM_HALF = 93;
	/** 種族：亡霊 */
	public static final int SPECIES_PHANTOM_GHOST = 94;
	/** 種族：神霊 */
	public static final int SPECIES_PHANTOM_DIVINESPIRIT = 95;
	/** 種族：船幽霊 */
	public static final int SPECIES_PHANTOM_SHIP = 96;
	/** 種族：キョンシー */
	public static final int SPECIES_PHANTOM_JIANGSHI = 97;

	//式神に属す種族
	/** 種族：式神 */
	public static final int SPECIES_SHIKIGAMI = 108;
	/** 種族：人形 */
	public static final int SPECIES_SHIKIGAMI_DOLL = 109;
	/** 種族：使い魔 */
	public static final int SPECIES_SHIKIGAMI_FAMILIAR = 110;

	//妖怪に属す種族
	/** 種族：妖怪 */
	public static final int SPECIES_YOUKAI = 128;//妖怪
	/** 種族：魔法使い */
	public static final int SPECIES_YOUKAI_MAGICIAN = 129;//魔法使い
	/** 種族：妖獣 */
	public static final int SPECIES_YOUKAI_BEAST = 130;//妖獣
	/** 種族：吸血鬼 */
	public static final int SPECIES_YOUKAI_VAMPIRE = 131;//吸血鬼
	/** 種族：鬼 */
	public static final int SPECIES_YOUKAI_ONI = 132;//鬼
	/** 種族：河童 */
	public static final int SPECIES_YOUKAI_KAPPA = 133;//河童
	/** 種族：烏天狗 */
	public static final int SPECIES_YOUKAI_TENGU_CROW = 134;//烏天狗
	/** 種族：白狼天狗 */
	public static final int SPECIES_YOUKAI_TENGU_WHITEWOLF = 135;//白狼天狗
	/** 種族：死神 */
	public static final int SPECIES_YOUKAI_SHINIGAMI = 136;//死神
	/** 種族：雪女 */
	public static final int SPECIES_YOUKAI_YUKIONNA = 137;//雪女
	/** 種族：妖蟲 */
	public static final int SPECIES_YOUKAI_BUG = 139;//妖蟲
	/** 種族：夜雀 */
	public static final int SPECIES_YOUKAI_YOSUZUME = 140;//夜雀
	/** 種族：妖怪兎 */
	public static final int SPECIES_YOUKAI_RABBIT = 142;//妖怪兎
	/** 種族：付喪神 */
	public static final int SPECIES_YOUKAI_TUKUMOGAMI = 143;//付喪神
	/** 種族：釣瓶落とし */
	public static final int SPECIES_YOUKAI_TURUBEOTOSHI = 144;//釣瓶落とし
	/** 種族：橋姫 */
	public static final int SPECIES_YOUKAI_HASHIHIME = 145;//橋姫
	/** 種族：覚 */
	public static final int SPECIES_YOUKAI_SATORI = 146;//覚
	/** 種族：入道 */
	public static final int SPECIES_YOUKAI_NYUUDOU = 147;//入道
	/** 種族：鵺 */
	public static final int SPECIES_YOUKAI_NUE = 148;//鵺
	/** 種族：人魚 */
	public static final int SPECIES_YOUKAI_MERMAID = 149;//人魚
	/** 種族：天邪鬼 */
	public static final int SPECIES_YOUKAI_AMANOJAKU = 150;//天邪鬼

	//龍神
	/** 種族：龍神 */
	public static final int SPECIES_DRAGON = 192;

	//その他の種族
	/** 種族：その他種族 */
	public static final int SPECIES_OTHERS = 255;

	//攻撃の段階
	/** 攻撃の意思がない状態を表す */
	public static final int NOT_ATTACK 	  = 0;
	/** 通常攻撃１ */
	public static final int NORMAL_ATTACK01 = 1;
	/** 通常攻撃２ */
	public static final int NORMAL_ATTACK02 = 2;
	/** 通常攻撃３ */
	public static final int NORMAL_ATTACK03 = 3;
	public static final int NORMAL_ATTACK04 = 4;
	public static final int NORMAL_ATTACK05 = 5;
	public static final int NORMAL_ATTACK06 = 6;
	public static final int NORMAL_ATTACK07 = 7;
	public static final int NORMAL_ATTACK08 = 8;
	public static final int NORMAL_ATTACK09 = 9;
	public static final int NORMAL_ATTACK10 =10;
	public static final int NORMAL_ATTACK11 =11;
	public static final int NORMAL_ATTACK12 =12;
	public static final int NORMAL_ATTACK13 =13;
	public static final int NORMAL_ATTACK14 =14;
	public static final int NORMAL_ATTACK15 =15;
	public static final int NORMAL_ATTACK16 =16;
	public static final int NORMAL_ATTACK17 =17;
	public static final int NORMAL_ATTACK18 =18;
	public static final int NORMAL_ATTACK19 =19;
	public static final int NORMAL_ATTACK20 =20;
	/** スペルカード１ */
	public static final int SPELLCARD_ATTACK01 = 101;
	/** スペルカード２ */
	public static final int SPELLCARD_ATTACK02 = 102;
	/** スペルカード３ */
	public static final int SPELLCARD_ATTACK03 = 103;
	public static final int SPELLCARD_ATTACK04 = 104;
	public static final int SPELLCARD_ATTACK05 = 105;
	public static final int SPELLCARD_ATTACK06 = 106;
	public static final int SPELLCARD_ATTACK07 = 107;
	public static final int SPELLCARD_ATTACK08 = 108;
	public static final int SPELLCARD_ATTACK09 = 109;
	public static final int SPELLCARD_ATTACK10 = 110;
	public static final int SPELLCARD_ATTACK11 = 111;
	public static final int SPELLCARD_ATTACK12 = 112;
	public static final int SPELLCARD_ATTACK13 = 113;
	public static final int SPELLCARD_ATTACK14 = 114;
	public static final int SPELLCARD_ATTACK15 = 115;
	public static final int SPELLCARD_ATTACK16 = 116;
	public static final int SPELLCARD_ATTACK17 = 117;
	public static final int SPELLCARD_ATTACK18 = 118;
	public static final int SPELLCARD_ATTACK19 = 119;
	public static final int SPELLCARD_ATTACK20 = 120;
	public static final int SPELLCARD_ATTACK21 = 121;
	public static final int SPELLCARD_ATTACK22 = 122;
	public static final int SPELLCARD_ATTACK23 = 123;
	public static final int SPELLCARD_ATTACK24 = 124;
	public static final int SPELLCARD_ATTACK25 = 125;
	public static final int SPELLCARD_ATTACK26 = 126;
	/** 攻撃の終了を表す(倒れた) */
	public static final int ATTACK_END = 127;

	//同期データ (1.7.10 の dataWatcher 16-19 に相当)
	private static final EntityDataAccessor<Byte> DATA_FORM = SynchedEntityData.defineId(EntityDanmakuMob.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> DATA_DANMAKU_PATTERN = SynchedEntityData.defineId(EntityDanmakuMob.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_SPELL_CARD_MOTION = SynchedEntityData.defineId(EntityDanmakuMob.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_FLYING_HEIGHT = SynchedEntityData.defineId(EntityDanmakuMob.class, EntityDataSerializers.BYTE);

	public EntityDanmakuMob(EntityType<? extends EntityDanmakuMob> type, Level level)
	{
		super(type, level);
		isFlyingMode = true;
		this.setFlyingHeight(3);
		this.stop = false;
		this.setSpecies(this.SPECIES_OTHERS);
	}

	/**
	 * 最初に一度だけ呼ばれる処理
	 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_FORM, (byte)0);//見た目
		builder.define(DATA_DANMAKU_PATTERN, 0);//使用中のスペルカード
		builder.define(DATA_SPELL_CARD_MOTION, (byte)0);//スペルカードモーション
		builder.define(DATA_FLYING_HEIGHT, (byte)0);//飛ぶ高さ
	}

	/**
	 * 自研AIのため 1.21.1 では Goal を一切登録しない（1.7.10 の isAIEnabled() = false に相当）。
	 * 移動・攻撃は tick() 内で直接処理する。
	 */
	@Override
	protected void registerGoals()
	{
		//意図的に空実装
	}

	/**
	 * 最大HPを設定
	 * @param hp 最大HP
	 */
	public void setMaxHP(float hp)
	{
		this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(hp);
	}

	/**
	 * 移動速度を設定
	 * @param speed 移動速度
	 */
	public void setSpeed(double speed)
	{
		this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(speed);
	}

	/**
	 * 移動速度を返す
	 *
	 * <p>1.7.10 は EntityCreature の移動速度属性（movementSpeed）を返していた。1.21.1 の
	 * {@code LivingEntity#getSpeed()} は「今の移動速度（float）」で意味が異なるため、
	 * 属性値を返すこのメソッドは別名にする（旧 getSpeed() は double を返していた）。
	 * @return 移動速度
	 */
	public double getMovementSpeed()
	{
		return this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
	}

	/**
	 * ターゲットとの間に取る間合いを設定する
	 * @param distance ターゲットとの間に取る間合い
	 */
	public void setAttackDistance(double distance)
	{
		attackDistance = distance;
	}

	/**
	 * ターゲットとの間に取る間合いを返す
	 * @return ターゲットとの間合いの距離
	 */
	public double getAttackDistance()
	{
		return attackDistance;
	}

	/**
	 * 近づいたら敵対化する距離を設定する
	 * @param distance 近づいたら敵対化する距離
	 */
	public void setDetectionDistance(double distance)
	{
		detectionDistance = distance;
	}

	/**
	 * 近づいたら敵対化する距離を返す
	 * @return 近づいたら敵対化する距離
	 */
	protected double getDetectionDistance()
	{
		return detectionDistance;
	}

	/**
	 * 周りの妖精を敵対化させるかどうか
	 * @return 敵対化させられるならtrue
	 */
	protected boolean canFairyCall()
	{
		return false;
	}

	/**
	 * 妖精を敵対化させられる最大の距離を返す
	 * @return 妖精を敵対化させられる最大の距離
	 */
	protected double getFairyCallDistance()
	{
		return 10.0D;
	}

	/**
	 * スペルカードを宣言するモーションを始める
	 */
	protected void setSpellCardAttack()
	{
		setSpellCardMotion((byte)-30);
	}

	public LivingEntity getTarget()
	{
		if(ModConfig.danmakuLevel <= 0)
		{
			return null;
		}
		return super.getTarget();
	}

	/** 座標を直接指定して、その位置ベクトルを返す
	 * @param x : X座標
	 * @param y : Y座標
	 * @param z : Z座標
	 * @return
	 */
	public Vec3 pos(double x, double y, double z)
	{
		return THShotLib.pos(x, y, z);
	}

	/**
	 * 目の位置より少し下の位置ベクトルを返す
	 * @return
	 */
	public Vec3 pos()
	{
		return THShotLib.pos_Living(this);
	}

	/**
	 * LivingEntityの目の位置より少し下の位置ベクトルを返す
	 * @param living : 位置ベクトルを知りたいLivingEntity
	 * @return
	 */
	public Vec3 pos(LivingEntity living)
	{
		return THShotLib.pos_Living(living);
	}

	/**
	 * 水平角度と垂直角度からベクトルを返す
	 * @param yaw
	 * @param pitch
	 * @return
	 */
	public Vec3 angle(float yaw, float pitch)
	{
		return THShotLib.angle(yaw, pitch);
	}

	/**
	 * 重力の影響を受けないベクトルを返す
	 * @return
	 */
	public Vec3 gravity_Zero()
	{
		return THShotLib.gravity_Zero();
	}

	/**
	 * 弾幕のパターンを返す
	 * @return 弾幕パターン
	 */
	public int getDanmakuPattern()
	{
		return this.entityData.get(DATA_DANMAKU_PATTERN);
	}

	/**
	 * 弾幕パターンを設定
	 * @param pattern 弾幕パターン
	 */
	public void setDanmakuPattern(int pattern)
	{
		this.entityData.set(DATA_DANMAKU_PATTERN, pattern);
	}

	/**
	 * スペルカード宣言モーションを返す
	 * @return スペルカード宣言時のモーションカウント
	 */
	public byte getSpellCardMotion()
	{
		return this.entityData.get(DATA_SPELL_CARD_MOTION);
	}

	/**
	 * スペルカード宣言モーションを設定
	 * @param motion モーションカウント
	 */
	public void setSpellCardMotion(byte motion)
	{
		this.entityData.set(DATA_SPELL_CARD_MOTION, motion);
	}

	/**
	 * 使用しているスペルカードNoを返す
	 * @return スペルカードNo
	 */
	public int getUsingSpellCardNo()
	{
		return -1;
	}

	/**
	 * スペルカードを使用中かどうかを返す
	 * @return スペルカード使用中ならtrue
	 */
	public boolean isSpellCardAttack()
	{
		return getUsingSpellCardNo() >= 0;
	}

	/**
	 * 飛ぶ高さを返す
	 * @return 飛行高度
	 */
	public byte getFlyingHeight()
	{
		return this.entityData.get(DATA_FLYING_HEIGHT);
	}

	/**
	 * 飛ぶ高さを設定
	 * @param height 飛行高度
	 */
	public void setFlyingHeight(int height)
	{
		this.entityData.set(DATA_FLYING_HEIGHT, (byte)height);
	}

	/**
	 * 名前を返す。返す名前は英語
	 * @return 英名を返す
	 */
	public String getDanmakuMobName()
	{
		return danmakuMobName;
	}

	/**
	 * 名前を決める。英名
	 * @param name 英名
	 */
	public void setDanmakuMobName(String name)
	{
		danmakuMobName = name;
	}

	/**
	 * 種族を設定する
	 * @param species1 種族１
	 * @param species2 種族２
	 */
	protected void setSpecies(int species1, int species2)
	{
		species_1 = species1;
		species_2 = species2;
	}

	/**
	 * 種族を設定する
	 * @param species 種族
	 */
	protected void setSpecies(int species)
	{
		setSpecies(species, -1);
	}

	/**
	 * 種族１を返す
	 *
	 * <p>TODO K3-review: 1.7.10 の実装は {@code return this.SPECIES_OTHERS;} で、setSpecies() で
	 * 設定した species_1 を返していなかった（＝常に 255）。バイトコードで確認済み。ここでは旧挙動を
	 * そのまま踏襲しているが、isYoukai()/isFairy()/isPhantom() が常に false になり、
	 * EntityTHShot 側の「妖怪・妖精・幻霊へのダメージ2倍」も効かないという旧バグを引き継ぐ。
	 * 意図的な修正（species_1 を返す）にするかは要判断。
	 * @return 種族ID
	 */
	public int getSpecies_1()
	{
		return this.SPECIES_OTHERS;
	}

	/**
	 * 種族２を返す
	 * @return 種族ID
	 */
	public int getSpecies_2()
	{
		return this.SPECIES_OTHERS;
	}

	/**
	 * 人間ならtrueを返す
	 * @return 人間ならtrue
	 */
	public boolean isHuman()
	{
		return getSpecies_1() < this.SPECIES_GOD || getSpecies_2() < this.SPECIES_GOD;
	}

	/**
	 * 幽霊ならtrueを返す
	 * @return 幽霊ならtrue
	 */
	public boolean isPhantom()
	{
		return (getSpecies_1() >= this.SPECIES_PHANTOM && getSpecies_1() < this.SPECIES_SHIKIGAMI) ||
				(getSpecies_2() >= this.SPECIES_PHANTOM && getSpecies_2() < this.SPECIES_SHIKIGAMI);
	}

	/**
	 * 妖精ならtrueを返す
	 * @return 妖精ならtrue
	 */
	public boolean isFairy()
	{
		return getSpecies_1() == this.SPECIES_FAIRY || getSpecies_2() == this.SPECIES_FAIRY;
	}

	/**
	 * 妖怪ならtrueを返す
	 * @return 妖怪ならtrue
	 */
	public boolean isYoukai()
	{
		return (getSpecies_1() >= this.SPECIES_YOUKAI && getSpecies_1() < this.SPECIES_DRAGON) ||
				(getSpecies_2() >= this.SPECIES_YOUKAI && getSpecies_2() < this.SPECIES_DRAGON);
	}

	/**
	 * ある種族であるか返す
	 * @param species 種族
	 * @return ある種族であるならtrue
	 */
	public boolean isInSpecies(int species)
	{
		return getSpecies_1() == species || getSpecies_2() == species;
	}

	/**
	 * 移動をやめ、見ている方向も一定にする
	 */
	protected void setStopStart()
	{
		stop = true;
		moveTimer = 0;
		moveTimerMax = 0;
	}

	/**
	 * 移動停止を終了させる
	 */
	protected void setStopEnd()
	{
		stop = false;
	}

	/**
	 * 停止中かどうかを返す
	 * @return 停止中ならtrue
	 */
	public boolean isStop()
	{
		return stop;
	}

	/**
	 * ベクトルで指定した方向に指定の速度で、指定の時間の間動く
	 * @param move 移動する方向を表すベクトル
	 * @param speed 移動速度
	 * @param time 移動時間
	 */
	protected void move(Vec3 move, double speed, int time)
	{
		// # perf: A12 — 1.7.10 は moveVec の成分をその場で speed 倍していた。Vec3 は不変なので
		//   ここでスケール済みベクトルを作ると指示 1 回につき 2 個の Vec3 が出る。方向と速度を
		//   分けて保持し、実使用（tick 内の move() 呼び出し）で 1 個だけ作る形にした。
		moveVec = move;
		moveVecSpeed = speed;
		moveTimer = time;
		moveTimerMax = time;
	}

	/**
	 * 右方向に指定の速度で、指定の時間の間動く
	 * @param speed 移動速度
	 * @param time 移動時間
	 */
	protected void moveRight(double speed, int time)
	{
		// # perf: A12 — 元は moveVec に代入してから move() に渡していた（move() 内で再度 moveVec を
		//   上書き）。方向ベクトルは getVecFromAngle が作る 1 個だけで済むよう直接渡す。
		move(THShotLib.getVecFromAngle(getYRot() + 90F, getXRot()), speed, time);
	}

	/**
	 * 左方向に指定の速度で、指定の時間の間動く
	 * @param speed 移動速度
	 * @param time 移動時間
	 */
	protected void moveLeft(double speed, int time)
	{
		// # perf: A12 — moveRight と同じく一時代入を削除。
		move(THShotLib.getVecFromAngle(getYRot() - 90F, getXRot()), speed, time);
	}

	/**
	 * 前方に指定の速度で、指定の時間の間動く
	 * @param speed 移動速度
	 * @param time 移動時間
	 */
	protected void moveForward(double speed, int time)
	{
		// # perf: A12 — moveRight と同じく一時代入を削除。
		move(THShotLib.getVecFromAngle(getYRot(), getXRot()), speed, time);
	}

	/**
	 * 後方に指定の速度で、指定の時間の間動く
	 * @param speed 移動速度
	 * @param time 移動時間
	 */
	protected void moveBack(double speed, int time)
	{
		// # perf: A12 — moveRight と同じく一時代入を削除。
		move(THShotLib.getVecFromAngle(getYRot() + 180, getXRot()), speed, time);
	}

	/**
	 * 攻撃を次の段階に移す処理
	 * @param nextPattern 次の攻撃パターンID
	 * @param interval 次の攻撃を開始するまでの時間
	 * @param maxHP 次の攻撃パターン時の最大HP
	 * @param invincibleTime 無敵時間
	 */
	protected void moveDanmakuAttack(int nextPattern, int interval, float maxHP, int invincibleTime)
	{
		setDanmakuPattern(nextPattern);
		attackCounter = -interval;
		deathTime = 0;
		danmakuDying = false;
		if(!level().isClientSide)
		{
			this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(maxHP);
			setHealth(maxHP);
		}
		attackInterval = invincibleTime;
		THShotLib.danmakuRemove(this, 40.0D, "Enemy", true);
		isSpellCardMode = false;
		if(circle != null)
		{
			circle.discard();
		}
	}

	/**
	 * 向いている方向ベクトルを返す。
	 * @return
	 */
	public Vec3 look()
	{
		return THShotLib.angle(this.getYRot(), this.getXRot());
	}

	public int getSpellCardCircleColor()
	{
		return CIRCLE_COLOR_RED;
	}

	/**
	 * スペルカードを宣言する
	 * @param spellCardNo
	 */
	protected void useSpellCard(int spellCardNo)
	{
		if(getTarget() != null)
		{
			THKaguyaLib.spellCardDeclaration(this.level(), this, this.getTarget(), spellCardNo, 0, ModConfig.danmakuLevel, !isSpellCardMode);
		}
		if(!isSpellCardMode)
		{
			isSpellCardMode = true;
			//スペルカードの魔法陣を出現させる
			circle = new EntitySpellCardCircle(ModEntities.SPELL_CARD_CIRCLE.get(), this.level(), this, 16 + getSpellCardCircleColor(), -100);
			if(!level().isClientSide)
			{
				level().addFreshEntity(circle);
			}
			this.setSpellCardAttack();
		}
	}

	/**
	 * 時が止まっているかどうか（1.7.10 の「ticksExisted <= lastTime なら時間停止中」の判定）
	 */
	protected boolean isTimeStopped()
	{
		return tickCount <= lastTime;
	}

	/**
	 * 毎tick呼ばれる処理（1.7.10 の onUpdate）。
	 *
	 * <p>1.7.10 は {@code onEntityUpdate()} をオーバーライドして時間停止時に super を呼ばない、
	 * という形で「時間停止中は更新しない」を実現していた。1.21.1 では Entity#baseTick() が
	 * それに相当するため、同じ場所でガードする。
	 */
	@Override
	public void baseTick()
	{
		if(tickCount > lastTime)
		{
			super.baseTick();
		}
	}

	/**
	 * 多段階ボス等が die() を遮断している間 true（1.7.10 の onDeathUpdate 時系列を再現）。
	 * moveDanmakuAttack() が段階遷移時にクリアする。
	 */
	protected boolean danmakuDying = false;

	/**
	 * @return true なら HP 0 での die() を遮断し、tickDeath() を疑似死亡として手動で回す。
	 *         1.21.1 の die() は dead 確定・戦利品放出・DYING ポーズを一括で行うが、
	 *         1.7.10 は onDeathUpdate 内で HP 復帰して次段階へ進んでいたため遮断が必要。
	 */
	protected boolean canInterceptDeath()
	{
		return false;
	}

	@Override
	public void die(DamageSource source)
	{
		if(level().isClientSide || danmakuDying || !canInterceptDeath())
		{
			super.die(source);
			return;
		}
		danmakuDying = true;
	}

	@Override
	public void tick()
	{
		if(danmakuDying)
		{
			// 疑似死亡：vanilla は dead==true の時だけ tickDeath() を回す。die() を遮断した分、ここで回す。
			tickDeath();
		}

		//ピースフルなら消滅させる
		if (!this.level().isClientSide && this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL)
		{
			this.discard();
			return;
		}

		//弾幕レベルが０なら、攻撃の対象を持たない
		if(ModConfig.danmakuLevel <= 0)
		{
			this.setTarget(null);
		}

		//時が止まっているなら処理を行わない
		if(isTimeStopped())
		{
			setDeltaMovement(0.0D, 0.0D, 0.0D);
			//1.7.10 の setPosition(prevPosX, prevPosY, prevPosZ) に相当
			this.setPos(this.xo, this.yo, this.zo);
			return;
		}
		//時が止まっていないなら通常の処理を行う
		else
		{
			super.tick();
		}

		if(getHealth() <= 0.0F)
		{
			return;
		}

		if(attackInterval > 0)
		{
			attackInterval--;
		}
		if(isSpellCardAttack())
		{
			if(attackCounter == -20)setSpellCardAttack();
		}

		//スペルカードモーション中ならモーションを進める
		if(getSpellCardMotion() < 0)
		{
			if(!level().isClientSide)
			{
				setSpellCardMotion((byte)(getSpellCardMotion() + 2));
			}
		}

		if(moveTimer > 0)
		{
			double moveRate = Math.cos((double)moveTimer / (double)moveTimerMax * Math.PI / 2.0D) * moveVecSpeed;
			if(!level().isClientSide)
			{
				// # perf: A12 — 1.7.10 は moveVec をその場で乗算していた。Vec3 は不変なので
				//   実際に動かすこの 1 箇所だけ Vec3 を作り、方向 × 速度 × 時間減衰をまとめて掛ける。
				this.move(net.minecraft.world.entity.MoverType.SELF,
						new Vec3(moveVec.x * moveRate, moveVec.y * moveRate, moveVec.z * moveRate));
			}
			moveTimer--;
		}

		if(getFlyingHeight() > 0)
		{
			//高さを調整する
			int heightCount = 0;
			//地面から何ｍ上空にいるか調べる
			// # perf: A11 — 1.7.10 の worldObj.isAirBlock(int,int,int) 相当。BlockPos.containing は
			//   呼び出しごとに新しい BlockPos を作るため、可変ブロック座標を 1 個だけ使い回す。
			int blockX = net.minecraft.util.Mth.floor(getX());
			int blockZ = net.minecraft.util.Mth.floor(getZ());
			BlockPos.MutableBlockPos probePos = this.flyingHeightProbe;
			while(level().isEmptyBlock(probePos.set(blockX, net.minecraft.util.Mth.floor(getY()) - heightCount, blockZ)) && heightCount < 8)
			{
				heightCount++;
			}

			LivingEntity target = getTarget();
			//ターゲットがいて地面から離れる高さ以上にいるなら
			if(target != null && heightCount > getFlyingHeight())
			{
				//ターゲットの高さと今の高さを比較
				double distance = target.getY() - this.getY();

				setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y + distance * 0.0006D, getDeltaMovement().z);
			}
			//ターゲットがいないななら
			else if(heightCount < getFlyingHeight())
			{
				if(heightCount >= getFlyingHeight())
				{
					setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y - 0.0006D, getDeltaMovement().z);
				}
				else if(heightCount < getFlyingHeight())
				{
					setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y + 0.006D, getDeltaMovement().z);
				}
			}
			if(!stop)
			{
				this.move(net.minecraft.world.entity.MoverType.SELF,
						new Vec3(0D, getDeltaMovement().y, 0D));
			}
		}

		//ターゲットがいるが、死んでいるなら
		if(getTarget() != null && !getTarget().isAlive())
		{
			this.setTarget(null);//ターゲットから外す
		}

		//ターゲットがいないなら
		if (this.getTarget() == null)
		{
			//敵対範囲内にいるプレイヤーを探す
			this.setTarget(this.level().getNearestPlayer(this, getDetectionDistance()));

			//プレイヤーがいるなら
			if (this.getTarget() != null)
			{
				//妖精を敵対化させられるなら、周りの妖精を敵対化させる
				if(canFairyCall())
				{
					//範囲内のEntityをリストで取得
					List<Entity> list = level().getEntities(this,
							getBoundingBox().inflate(getFairyCallDistance(), getFairyCallDistance(), getFairyCallDistance()));
					for(int i = 0; i < list.size(); i++)
					{
						Entity entity1 = list.get(i);
						//弾幕MOBなら
						if(entity1 instanceof EntityTHFairy danmakuMob)
						{
							//妖精のターゲットがいないなら
							if(danmakuMob.getTarget() == null)
							{
								//妖精のターゲットをこの弾幕MOBと同じターゲットにする
								danmakuMob.setTarget(this.getTarget());
							}
						}
					}
				}
			}
		}

		LivingEntity target = getTarget();

		//射程圏内にターゲットがいるなら
		if(target != null && !isDeadOrDying())
		{
			//ターゲットの方を向かせる
			double xDistance = target.getX() - getX();
			double yDistance = (target.getY() + target.getEyeHeight()) - (getY() + this.getEyeHeight());
			double zDistance = target.getZ() - getZ();
			float angleXZ = - ((float)Math.atan2(xDistance, zDistance)) / 3.141593F * 180F;
			float angleY  = -(float)Math.atan2( yDistance, Math.sqrt(xDistance * xDistance + zDistance * zDistance)) / 3.141593F * 180F;

			if(!stop)
			{
				setYRot(angleXZ);
				setXRot(angleY);
			}

			//ターゲットを目視できるなら
			if(this.hasLineOfSight(target))
			{
				//ターゲットの見失いカウントを０にする
				lostTarget = 0;
				//弾幕難易度が０でなければ弾幕攻撃をする
				int level = ModConfig.danmakuLevel;
				if(level != 0 && attackCounter >= 0)
				{
					danmakuPattern(level);
				}

				//攻撃カウントを増やす
				attackCounter++;
			}
			//ターゲットを目視できないなら
			else
			{
				//ターゲットの見失いカウントを増やす
				lostTarget++;
				//ターゲットを１０秒目視できないなら
				if(lostTarget > 200)
				{
					//ターゲットをなしにする
					this.setTarget(null);
				}
			}

			if(moveTimer <= 0 && lostTarget > 0)
			{
				move(THShotLib.getVecFromAngle(random.nextFloat() + 360F, random.nextFloat() * 360F), getMovementSpeed(), 30);
			}

			if(moveTimer <= 0 && getTarget() != null && getAttackDistance() > 0)
			{
				double toTargetDistance = this.distanceTo(target);
				double rate;

				if(toTargetDistance < getAttackDistance())
				{
					rate = 1.0D -  (toTargetDistance / getAttackDistance());
					moveBack(getMovementSpeed() * 0.3D * rate, 15);
				}
				else
				{
					rate = (toTargetDistance / getAttackDistance());
					moveForward(getMovementSpeed() * 0.5D * rate, 15);
				}
			}
		}

		//最後に動いた時間として、その時間を保存
		if(tickCount > lastTime)
		{
			lastTime = tickCount;
		}
	}

	/**
	 * 無敵状態を取得する
	 * 攻撃移行時の時間の間は無敵
	 * @return [ true : 無敵状態 ] [ false : 無敵ではない]
	 */
	@Override
	public boolean isInvulnerableTo(DamageSource damageSource)
	{
		if(attackInterval > 0)
		{
			return true;
		}
		return super.isInvulnerableTo(damageSource);
	}

	/**
	 * ダメージを受けたときの処理
	 *
	 * <p>1.7.10 の {@code attackEntityFrom} は「ダメージ計算 → 成功したらノックバックをほぼ無効化し、
	 * プレイヤーからの攻撃ならターゲットにする」だった。1.21.1 では {@code hurt()} が入口で、
	 * ダメージ計算本体は {@code actuallyHurt()}。旧コードの適用ポイントに合わせるため、
	 * 軽減計算は {@code actuallyHurt}、反撃・ターゲット付けは {@code hurt} で行う。
	 */
	@Override
	public boolean hurt(DamageSource damageSource, float damage)
	{
		if (!super.hurt(damageSource, damage))
		{
			return false;
		}
		if (damageSource.getEntity() instanceof LivingEntity entity)
		{
			//ノックバック耐性が高い
			setDeltaMovement(getDeltaMovement().multiply(0.0001D, 0.0001D, 0.0001D));
			if (this.getVehicle() != entity)
			{
				if (entity instanceof Player)
				{
					this.setTarget(entity);
				}
				return true;
			}
			return true;
		}
		return true;
	}

	/**
	 * ダメージの最終適用（1.7.10 の attackEntityFrom 内 applyPotionDamageCalculations 相当）
	 */
	@Override
	protected void actuallyHurt(DamageSource damageSource, float damage)
	{
		super.actuallyHurt(damageSource, applyPotionDamageCalculations(damageSource, damage));
	}

	/**
	 * 無敵状態のときに受けるダメージを 5% にする（1.7.10 の applyPotionDamageCalculations）。
	 */
	protected float applyPotionDamageCalculations(DamageSource damageSource, float damage)
	{
		if (attackInterval > 0)
		{
			damage = (float)((double)damage * 0.05D);
		}
		return damage;
	}

	/**
	 * Entityとの当たり判定をとる（1.7.10 の hitEntityCheck）
	 */
	public HitResult hitEntityCheck(HitResult hitResult, Vec3 vec3d, Vec3 vec3d1)
	{
		Entity entity = null;//実際に当たったことにするEntity
		double d = 0.0D;//そのEntityまでの仮の距離
		float hitSize = 0.9F;
		//ここから移動量分の線分を作り、それに弾の大きさの２倍の肉付けをし直方体を作る。それに当たったEntityをリスト化する
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(hitSize));

		for (int j = 0; j < list.size(); j++)
		{
			Entity entity1 = list.get(j);
			//entity1が当たり判定を取らないならパス
			if (entity1.canBeHitByProjectile())
			{
				//判定を弾の大きさに変更
				AABB axisalignedbb = entity1.getBoundingBox().inflate(hitSize, hitSize, hitSize);
				java.util.Optional<Vec3> intercept = axisalignedbb.clip(vec3d, vec3d1);
				//この判定で当たっているなら
				if (intercept.isPresent())
				{
					//当たっているならここからその点までの距離を取得
					double d1 = vec3d.distanceTo(intercept.get());
					//今までの一番近くにいるなら、一番近いEntityを更新する
					if (d1 < d || d == 0.0D)
					{
						entity = entity1;
						d = d1;
					}
				}
			}
		}

		//当たったEntityがいるなら、当たったEntityをHitResultで登録
		if (entity != null)
		{
			hitResult = new EntityHitResult(entity);
		}

		return hitResult;
	}

	/**
	 * 衝突処理（1.7.10 の hitCheck）
	 */
	public void hitCheck(float damage)
	{
		//始点（現在地）
		Vec3 vec3d = new Vec3(getX(), getY(), getZ());
		//終点（現在地に移動量を足した点）
		Vec3 vec3d1 = new Vec3(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
		//始点と終点からブロックとの当たりを取得（液体も含む）
		BlockHitResult blockHit = level().clip(new ClipContext(vec3d, vec3d1,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		HitResult movingObjectPosition = blockHit.getType() == HitResult.Type.MISS ? null : blockHit;

		//何らかのブロックに当たっているなら
		if (movingObjectPosition != null)
		{
			THShotLib.playShotSound(this);
			//終点を当たった点に変更
			vec3d1 = blockHit.getLocation();
		}

		movingObjectPosition = hitEntityCheck(movingObjectPosition, vec3d, vec3d1);

		if (movingObjectPosition != null)
		{
			//当たった場合の処理をする
			onImpact(movingObjectPosition, damage);
		}
	}

	/**
	 * ブロックやEntityに当たった時の処理
	 */
	protected void onImpact(HitResult movingObjectPosition, float damage)
	{
		//当たった時の処理
		if (!level().isClientSide)
		{
			if (movingObjectPosition instanceof EntityHitResult entityHit)
			{
				Entity hitEntity = entityHit.getEntity();
				//当たったEntityがいるなら
				if (hitEntity != null)
				{
					hitEntity.hurt(damageSources().mobAttack(this), damage);
				}
			}
		}
	}

	//弾幕パターンの記述
	protected void danmakuPattern(int level)
	{

	}

	/**
	 * 死んでいるときに呼ばれる（1.7.10 の onDeathUpdate）
	 */
	@Override
	protected void tickDeath()
	{
		super.tickDeath();

		//周囲の妖精を巻き込む
		if(this.deathTime == 7)
		{
			THShotLib.explosionEffect2(level(), getX(), getY(), getZ(), 1.0F + deathTime * 0.1F);
			THShotLib.banishExplosion(this, 5.0F, 5.0F);
		}
	}

	/**
	 * 倒れたときに落とすアイテム（1.7.10 の dropFewItems）
	 */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		super.dropCustomDeathLoot(level, damageSource, hasBeenAttackedByPlayer);

		//スペルカードを使用していたなら、そのスペルカードを落とす
		if(getUsingSpellCardNo() >= 0 && hasBeenAttackedByPlayer)
		{
			this.spawnAtLocation(spellCardStack(getUsingSpellCardNo()));
		}
	}

	/**
	 * 使用中スペルカードNoからドロップする符卡アイテムを返す。
	 *
	 * <p>TODO K3-review: 1.7.10 は {@code new ItemStack(THKaguyaItems.spell_card, 1, spellCardNo)} と
	 * いう単一アイテム + damage 値だった。1.21.1 では符卡が 35 個の独立アイテム
	 * （{@code spell_card_<name>}）に分割されたため、No → アイテムの表を明示的に持つ。
	 * 表に無い No（31 = PrincessUndine など 1.7.10 でも未登録）はドロップしない。
	 */
	protected ItemStack spellCardStack(int spellCardNo)
	{
		net.minecraft.world.item.Item item = spellCardItem(spellCardNo);
		if(item == null)
		{
			return ItemStack.EMPTY;
		}
		return new ItemStack(item);
	}

	/**
	 * スペルカードNo → 1.21.1 の符卡アイテム（{@code spell_card_<name>}）対応表。
	 * 対応は SpellCardRegistry の name（ModSpellCards 登録順）→ ModItems の snake_case 定数。
	 */
	private static net.minecraft.world.item.Item spellCardItem(int spellCardNo)
	{
		switch(spellCardNo)
		{
			case 0:  return ModItems.SPELL_CARD_MUSOU_FUUIN.get();
			case 1:  return ModItems.SPELL_CARD_MASTER_SPARK.get();
			case 2:  return ModItems.SPELL_CARD_KASHO_NO_EIMIN.get();
			case 3:  return ModItems.SPELL_CARD_METEONIC_SHOWER.get();
			case 4:  return ModItems.SPELL_CARD_NAMI_TO_TUBU_NO_KYOUKAI.get();
			case 5:  return ModItems.SPELL_CARD_NIJUU_KOKUSHICHOU.get();
			case 6:  return ModItems.SPELL_CARD_SCARLET_SHOOT.get();
			case 7:  return ModItems.SPELL_CARD_ZENZINRUI_NO_HISOUTEN.get();
			case 8:  return ModItems.SPELL_CARD_SATUZIN_DOLL.get();
			case 9:  return ModItems.SPELL_CARD_PERFECT_FREEZE.get();
			case 10: return ModItems.SPELL_CARD_HIKOUCHUU_NEST.get();
			case 11: return ModItems.SPELL_CARD_KAPPAS_POROROCA.get();
			case 12: return ModItems.SPELL_CARD_STARDUST_REVERIE.get();
			case 13: return ModItems.SPELL_CARD_KEROCHAN_FUUU_NI_MAKEZU.get();
			case 14: return ModItems.SPELL_CARD_MIRACLE_FRUIT.get();
			case 15: return ModItems.SPELL_CARD_FAFROTSKIES_NO_KISEKI.get();
			case 16: return ModItems.SPELL_CARD_YOURYOKU_SPOILER.get();
			case 17: return ModItems.SPELL_CARD_MOSES_NO_KISEKI.get();
			case 18: return ModItems.SPELL_CARD_YASAKA_NO_KAMIKAZE.get();
			case 19: return ModItems.SPELL_CARD_ICICLE_FALL.get();
			case 20: return ModItems.SPELL_CARD_STARBOW_BREAK.get();
			case 21: return ModItems.SPELL_CARD_CATADIOPTRIC.get();
			case 22: return ModItems.SPELL_CARD_MISHAGUJISAMA.get();
			case 23: return ModItems.SPELL_CARD_RED_MAGIC.get();
			case 24: return ModItems.SPELL_CARD_ETERNAL_MEEK.get();
			case 25: return ModItems.SPELL_CARD_NON_DIRECTIONAL_LASER.get();
			case 26: return ModItems.SPELL_CARD_KACHOUFUUGETU_SHOUFUUROUGETU.get();
			case 27: return ModItems.SPELL_CARD_SPEAR_THE_GUNGNIR.get();
			case 28: return ModItems.SPELL_CARD_MOONLIGHT_RAY.get();
			case 29: return ModItems.SPELL_CARD_HOUKA_KENRAN.get();
			case 30: return ModItems.SPELL_CARD_SAIKOU_RANBU.get();
			case 33: return ModItems.SPELL_CARD_FUJIYAMA_VOLCANO.get();
			case 34: return ModItems.SPELL_CARD_LITTLE_BUG_STORM.get();
			case 35: return ModItems.SPELL_CARD_METEOR_ON_EARTH.get();
			case 40: return ModItems.SPELL_CARD_GAGOUJI_CYCLONE.get();
			//TODO K3-review: 31 (PrincessUndine) は 1.7.10 でも未登録（ModSpellCards にも無い）。ドロップしない。
			default: return null;
		}
	}

	/**
	 * 弾形番号（旧 meta 0-18）→ 1.21.1 の弾アイテムの対応表。
	 * 1.7.10 は単一の shot_item + damage 値だったが、1.21.1 では 19 個の独立アイテムに分割されている。
	 */
	private static final java.util.function.Supplier<net.minecraft.world.item.Item>[] SHOT_ITEMS = new java.util.function.Supplier[] {
			ModItems.SMALL_SHOT, ModItems.TINY_SHOT, ModItems.MEDIUM_SHOT, ModItems.BIG_SHOT,
			ModItems.STAR_SHOT, ModItems.SMALL_STAR_SHOT, ModItems.CIRCLE_SHOT, ModItems.SCALE_SHOT,
			ModItems.BUTTERFLY_SHOT, ModItems.LIGHT_SHOT, ModItems.KNIFE_SHOT, ModItems.HEART_SHOT,
			ModItems.KUNAI_SHOT, ModItems.TALISMAN_SHOT, ModItems.BIG_LIGHT_SHOT, ModItems.RICE_SHOT,
			ModItems.OVAL_SHOT, ModItems.CRYSTAL_SHOT, ModItems.ARROW_SHOT
	};

	public void dropItem(int type, Vec3 pos, Vec3 angle)
	{
		EntityTHItem item = new EntityTHItem(ModEntities.TH_ITEM.get(), level(), pos.x, pos.y, pos.z, angle, (byte)type);

		if(!level().isClientSide)
		{
			level().addFreshEntity(item);
		}
	}

	/**
	 * パワーアップアイテム 小を落とす
	 * @param pos   : 落とす場所
	 * @param angle : 落とす方向
	 */
	public void dropPowerUpItem(Vec3 pos, Vec3 angle)
	{
		this.dropItem(EntityTHItem.ITEM_ID_SMALLPOWER, pos, angle);
	}

	/**
	 * パワーアップアイテム 小を落とす
	 * @param num 落とす数
	 */
	public void dropPowerUpItem(int num)
	{
		for(int i = 1; i <= num; i++)
		{
			dropPowerUpItem(this.pos(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
		}
	}

	/**
	 * 得点アイテムを落とす
	 * @param pos   : 落とす場所
	 * @param angle : 落とす方向
	 */
	public void dropPointItem(Vec3 pos, Vec3 angle)
	{
		this.dropItem(EntityTHItem.ITEM_ID_POINT, pos, angle);
	}

	/**
	 * 得点アイテムを落とす
	 * @param num 落とす数
	 */
	public void dropPointItem(int num)
	{
		for(int i = 1; i <= num; i++)
		{
			dropPointItem(this.pos(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
		}
	}

	/**
	 * スペルカードアイテムを落とす
	 * @param pos   : 落とす場所
	 * @param angle : 落とす方向
	 */
	public void dropSpellCardItem(Vec3 pos, Vec3 angle)
	{
		this.dropItem(EntityTHItem.ITEM_ID_BOMB, pos, angle);
	}

	/**
	 * スペルカードアイテムを落とす
	 * @param num 落とす数
	 */
	public void dropSpellCardItem(int num)
	{
		for(int i = 1; i <= num; i++)
		{
			dropSpellCardItem(this.pos(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
		}
	}

	/**
	 * エクステンドアイテムを落とす
	 * @param pos   : 落とす場所
	 * @param angle : 落とす方向
	 */
	public void dropExtendItem(Vec3 pos, Vec3 angle)
	{
		this.dropItem(EntityTHItem.ITEM_ID_EXTEND, pos, angle);
	}

	/**
	 * エクステンドアイテムを落とす
	 * @param num 落とす数
	 */
	public void dropExtendItem(int num)
	{
		for(int i = 1; i <= num; i++)
		{
			dropExtendItem(this.pos(), THShotLib.angle_LimitRandom(THShotLib.angle(0.0D, 1.0D, 0.0D), 90F));
		}
	}

	/**
	 * 弾アイテムを落とす。弾幕形状の設定もできる
	 *
	 * <p>1.7.10 は単一の shot_item + damage 値 + NBT（Number/Speed/Color/Gravity/Special/DanmakuForm）。
	 * 1.21.1 では弾形が独立アイテム（{@code ModItems.SMALL_SHOT} … {@code ARROW_SHOT}）、
	 * パラメータは {@link thkaguyamod.danmaku.DanmakuParams} DataComponent。
	 *
	 * @param shotID : 弾ID。ItemTHShot.SMALL 等で指定（旧 meta 0-18）
	 * @param stackNumber : スタック数
	 * @param number : 弾幕の弾数
	 * @param speed : 弾速加算値
	 * @param color : THShotLib.RED などで指定
	 * @param gravity : 重力加速度
	 * @param special : 特殊な弾
	 * @param form : 弾幕形状。0で一点、1で前方ランダム、2で扇状、3で全方位、4で球状、5でリング
	 * @return 作成されたItemEntity
	 */
	public ItemEntity dropShotItem(int shotID, int stackNumber, int number, int speed, int color, int gravity, int special, int form)
	{
		if(shotID < 0 || shotID >= SHOT_ITEMS.length)
		{
			return null;
		}
		//1.7.10 の NBT 相当を DanmakuParams コンポーネントに載せる
		ItemStack shot = new ItemStack(SHOT_ITEMS[shotID].get(), stackNumber);
		shot.set(thkaguyamod.init.ModDataComponents.DANMAKU.get(),
				new thkaguyamod.danmaku.DanmakuParams(
						number,
						form % 8,
						speed % 64,
						gravity % 16,
						special,
						color % 16,
						false));
		return this.spawnAtLocation(shot, 0.0F);
	}

	/**
	 * 落下ダメージ処理（1.7.10 の fall）
	 */
	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos)
	{
		if(getFlyingHeight() > 0)
		{
			super.checkFallDamage(y, onGround, state, pos);
		}
	}

	//Entityの発する音の大きさ
	@Override
	protected float getSoundVolume()
	{
		return 0.3F;
	}

	//発する音のピッチ
	@Override
	public float getVoicePitch()
	{
		return super.getVoicePitch() * 1.95F;
	}

	//生きてるときに出す音
	@Override
	protected SoundEvent getAmbientSound()
	{
		return this.random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT;
	}

	//攻撃を受けたときの音
	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource)
	{
		return SoundEvents.BAT_HURT;
	}

	//倒れたときの音
	@Override
	protected SoundEvent getDeathSound()
	{
		return SoundEvents.BAT_DEATH;
	}

	//一つのチャンクに湧く最大数
	@Override
	public int getMaxSpawnClusterSize()
	{
		return 1;
	}

	/**
	 * 見た目を返す
	 * @return
	 */
	public byte getForm()
	{
		return this.entityData.get(DATA_FORM);
	}

	/**
	 * 見た目を設定する
	 * @param type
	 */
	public void setForm(byte type)
	{
		this.entityData.set(DATA_FORM, type);
	}

	/*エンダーマンのテレポート処理*/

	/**
	 * エンダーマンのテレポート。ランダムに近くへテレポートする
	 */
	protected boolean teleportRandomly(double range)
	{
		double warpX;
		double warpY;
		double warpZ;

		int count = 0;
		do
		{
			warpX = this.getX() + (this.random.nextDouble() - 0.5D) * range;
			warpY = this.getY() + (double)(this.random.nextInt((int)range) - (int)(range / 2.0D));
			warpZ = this.getZ() + (this.random.nextDouble() - 0.5D) * range;
			count++;
		}while(!this.danmakuTeleportTo(warpX, warpY, warpZ) && count < 10);
		return true;
	}

	/**
	 * エンダーマンのテレポート。他のエンティティのところへテレポートする
	 */
	protected boolean teleportToEntity(Entity par1Entity)
	{
		Vec3 vec3 = new Vec3(this.getX() - par1Entity.getX(),
				this.getBoundingBox().minY + (double)(this.getBbHeight() / 2.0F) - par1Entity.getY() + (double)par1Entity.getEyeHeight(),
				this.getZ() - par1Entity.getZ());
		vec3 = vec3.normalize();
		double d0 = 16.0D;
		double d1 = this.getX() + (this.random.nextDouble() - 0.5D) * 8.0D - vec3.x * d0;
		double d2 = this.getY() + (double)(this.random.nextInt(16) - 8) - vec3.y * d0;
		double d3 = this.getZ() + (this.random.nextDouble() - 0.5D) * 8.0D - vec3.z * d0;
		return this.danmakuTeleportTo(d1, d2, d3);
	}

	/**
	 * エンダーマンのテレポート
	 *
	 * <p>1.7.10 は Forge の {@code EnderTeleportEvent} を投げていた。1.21.1 の対応イベントは
	 * {@link EntityTeleportEvent}（EnderTeleportEvent は削除された）。
	 */
	private boolean danmakuTeleportTo(double par1, double par3, double par5)
	{
		EntityTeleportEvent event = new EntityTeleportEvent(this, par1, par3, par5);
		if (NeoForge.EVENT_BUS.post(event).isCanceled()){
			return false;
		}
		double d3 = this.getX();
		double d4 = this.getY();
		double d5 = this.getZ();
		double targetX = event.getTargetX();
		double targetY = event.getTargetY();
		double targetZ = event.getTargetZ();
		boolean flag = false;
		int i = net.minecraft.util.Mth.floor(targetX);
		int j = net.minecraft.util.Mth.floor(targetY);
		int k = net.minecraft.util.Mth.floor(targetZ);

		if (this.level().hasChunkAt(new BlockPos(i, j, k)))
		{
			boolean flag1 = false;

			while (!flag1 && j > this.level().getMinBuildHeight())
			{
				BlockState state = this.level().getBlockState(new BlockPos(i, j - 1, k));

				if (state.blocksMotion())
				{
					flag1 = true;
				}
				else
				{
					targetY--;
					j--;
				}
			}

			if (flag1)
			{
				this.setPos(targetX, targetY, targetZ);

				if (this.level().noCollision(this) && !this.level().containsAnyLiquid(this.getBoundingBox()))
				{
					flag = true;
				}
			}
		}

		if (!flag)
		{
			this.setPos(d3, d4, d5);
			return false;
		}
		else
		{
			short short1 = 128;

			for (int l = 0; l < short1; ++l)
			{
				double d6 = (double)l / ((double)short1 - 1.0D);
				float f = (this.random.nextFloat() - 0.5F) * 0.2F;
				float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
				float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
				double d7 = d3 + (targetX - d3) * d6 + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
				double d8 = d4 + (targetY - d4) * d6 + this.random.nextDouble() * (double)this.getBbHeight();
				double d9 = d5 + (targetZ - d5) * d6 + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
				//1.7.10 の "portal" パーティクル
				this.level().addParticle(ParticleTypes.PORTAL, d7, d8, d9, (double)f, (double)f1, (double)f2);
			}

			//1.7.10 の "mob.endermen.portal"
			this.level().playSound(null, d3, d4, d5, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
			this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
			return true;
		}
	}

	/**
	 * 保存したデータを読み込む（1.7.10 の readEntityFromNBT）
	 */
	@Override
	public void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		super.readAdditionalSaveData(nbtTagCompound);
		if(nbtTagCompound != null && !nbtTagCompound.isEmpty())
		{
			//弾幕MOBの攻撃パターンを読み込み
			setForm(nbtTagCompound.getByte("Form"));
			setDanmakuPattern(nbtTagCompound.getInt("Danmaku"));
			danmakuSpan = nbtTagCompound.getShort("Span");
			shotForm = nbtTagCompound.getByte("ShotForm");
			shotColor = nbtTagCompound.getByte("ShotColor");
			speedRate = nbtTagCompound.getFloat("Speed");
			setAttackDistance(nbtTagCompound.getByte("AttackRange"));
			setDetectionDistance(nbtTagCompound.getByte("DetectRange"));
			setFlyingHeight(nbtTagCompound.getByte("Flying"));
			special = nbtTagCompound.getShort("Special");
			if(getFlyingHeight() <= 0)
			{
				isFlyingMode = false;
			}
			else
			{
				isFlyingMode = true;
			}
			if(shotForm < 0 || shotForm >= 32)
			{
				shotForm = 0;
			}
			if(shotColor < 0 || shotColor >= 10)
			{
				shotColor = 8;
			}
		}
		else
		{
			setDanmakuPattern(0);
			setAttackDistance(14.0D);
			setDetectionDistance(30.0D);
			setFlyingHeight(3);
		}
	}

	/**
	 * 保存するデータを書き込む（1.7.10 の writeEntityToNBT）
	 */
	@Override
	public void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		super.addAdditionalSaveData(nbtTagCompound);
		//弾幕MOBの攻撃パターンを書き込む
		nbtTagCompound.putByte("Form", getForm());
		nbtTagCompound.putInt("Danmaku", getDanmakuPattern());
		nbtTagCompound.putShort("Span", (short) danmakuSpan);
		nbtTagCompound.putByte("ShotForm", shotForm);
		nbtTagCompound.putByte("ShotColor", shotColor);
		nbtTagCompound.putFloat("Speed", speedRate);
		nbtTagCompound.putByte("AttackRange", (byte)getAttackDistance());
		nbtTagCompound.putByte("DetectRange", (byte)getDetectionDistance());
		nbtTagCompound.putByte("Flying", getFlyingHeight());
		nbtTagCompound.putShort("Special", special);
	}

	/**
	 * 自然スポーン判定の入口。
	 *
	 * <p>1.7.10 の {@code getCanSpawnHere()} は各サブクラスでオーバーライドされていた。
	 * 1.21.1 の対応は {@code checkSpawnRules(LevelAccessor, MobSpawnType)}。
	 * サブクラスはこのメソッドをオーバーライドする。
	 */
	@Override
	public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		return true;
	}
}
