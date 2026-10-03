package thkaguyamod.spellcard;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.effect.EntitySpellCardCircle;
import thkaguyamod.init.ModEntities;
import thkaguyamod.registry.SpellCardRegistry;

/** スペルカードのEntity */
public class EntitySpellCard extends Entity
{
	/** スペルカードを宣言したEntityLiving */
	public LivingEntity user;
	/** 攻撃対象のEntityLiving */
	public LivingEntity tgEntity;
	/** スペルカードを宣言してからの時間 */
	public int count;
	private int spellCardNumber;//スペルカード番号
	/** ターゲットへのベクトル */
	protected Vec3 tgVec;
	private int spellCardUsedTime;
	private int lastTime;
	public int level;//スペルカードのレベル。プレイヤー使用時は基本ノーマルであるレベル２固定

	public EntitySpellCardCircle circle;

	public boolean canMoveInTimeStop;

	/*
	 * 1.7.10 では Class<?> spellcard / THSpellCard useSpellCard をフィールドに保持していた。
	 * 1.21.1 ではリフレクション Class を保持する必要が無いため、spellcard_0 / spellcard は移植しない。
	 */
	/** 使用中のスペルカードのインスタンス（1.7.10 の useSpellCard） */
	THSpellCard useSpellCard;

	public static final int SC_REIMU_MusouFuuin = 0;
	public static final int SC_MARISA_MasterSpark = 1;
	public static final int SC_YUYUKO_Kasho_no_Eimin = 2;
	public static final int SC_MARISA_MeteonicShower = 3;
	public static final int SC_YUKARI_Nami_to_Tubu_no_Kyoukai = 4;
	public static final int SC_YUKARI_NijuuKokushichou = 5;
	public static final int SC_REMILIA_ScarletShoot = 6;
	public static final int SC_TENSHI_Zenzinrui_no_Hisouten = 7;
	public static final int SC_SAKUYA_SatuzinDoll = 8;
	public static final int SC_CIRNO_PerfectFreeze = 9;
	public static final int SC_YUKARI_HikouchuuNest = 10;
	public static final int SC_NITORI_Kappa_no_Pororoca = 11;
	public static final int SC_MARISA_StardustReverie = 12;
	public static final int SC_SUWAKO_Kerochan_Fuu_ni_Makezu = 13;
	public static final int SC_SANAE_MiracleFruit = 14;
	public static final int SC_SANAE_Fafrotskies_no_Kiseki = 15;
	public static final int SC_SANAE_YouryokuSpoiler = 16;
	public static final int SC_SANAE_Moses_no_Kiseki = 17;
	public static final int SC_SANAE_Yasaka_no_Kamikaze = 18;
	public static final int SC_CIRNO_IcicleFall = 19;
	public static final int SC_FLANDRE_StarbowBreak = 20;
	public static final int SC_FLANDRE_Catadioptric = 21;
	public static final int SC_SUWAKO_MishagujiSama = 22;
	public static final int SC_REMILIA_RedMagic = 23;
	public static final int SC_SAKUYA_EternalMeek = 24;
	public static final int SC_MARISA_NonDirectionalLaser = 25;
	public static final int SC_YUUKA_Kachoufuugetu_Shoufuurougetu = 26;
	public static final int SC_REMILIA_Spear_the_Gungnir = 27;
	public static final int SC_RUMIA_MoonlightRay = 28;
	public static final int SC_MEIRIN_Houkakenran = 29;
	public static final int SC_MEIRIN_Saikouranbu = 30;
	public static final int SC_PATCHOULI_PrincessUndine = 31;

	//同期データ (1.7.10 の dataWatcher 18 = スペルカード番号 に相当)
	private static final EntityDataAccessor<Integer> DATA_SPELL_CARD_NUMBER = SynchedEntityData.defineId(EntitySpellCard.class, EntityDataSerializers.INT);

	/** スペルカードのコンストラクタ */
	public EntitySpellCard(EntityType<? extends EntitySpellCard> type, Level level)
	{
		super(type, level);
		//TODO K3-review: 1.7.10 は preventEntitySpawning = true を設定していたが、1.21.1 に対応する
		//フラグが無い（このEntityは非生体のMISCカテゴリで自然スポーンしないため実害は無いと判断）。
		//1.7.10 の blocksBuilding は LivingEntity が立てるフラグで用途が異なるため設定しない。
		//サイズは getDimensions() で指定する（1.7.10 の setSize(0.4F, 0.4F) に相当）
	}

	/** スペルカードのコンストラクタ */
	public EntitySpellCard(EntityType<? extends EntitySpellCard> type, Level world, LivingEntity spUser, LivingEntity target, int num, int spLevel)
	{
		this(type, world);

		setPos(spUser.getX() - (double)Mth.sin((spUser.getYRot() + 30F) / 180F * 3.141593F) * (double)Mth.cos(spUser.getXRot() / 180F * 3.141593F),
				spUser.getY() - (double)Mth.sin(spUser.getXRot() / 180F * 3.141593F) + (double)spUser.getEyeHeight() - 0.10000000149011612D ,
				spUser.getZ() + (double)Mth.cos((spUser.getYRot() + 30F) / 180F * 3.141593F) * (double)Mth.cos(spUser.getXRot() / 180F * 3.141593F));//初期位置を設定(x,y,z)
		setYRot(spUser.getYRot());
		setXRot(spUser.getXRot());
		setRot(getYRot(), getXRot());
		tgVec = spUser.getLookAngle();
		user = spUser;//使用者をuserに保存
		tgEntity = target;//ターゲットした相手
		spellCardNumber = num;
		setSpellCardNumber(num);
		spellCardUsedTime = 0;
		count = 0;
		lastTime = 0;
		//level().playSound(null, spUser.blockPosition(), ModSounds.SPELLCARD.get(), SoundSource.PLAYERS, ModConfig.SpellCardVol, 1.0F);

		level = spLevel;

		//1.7.10 は Class.newInstance() で符卡インスタンスを生成していた（リフレクション）。
		//1.21.1 では SpellCardRegistry が Supplier ファクトリを保持するので factory.get() に置き換える。
		Supplier<? extends THSpellCard> factory = SpellCardRegistry.getSpellCardFactory(num);
		if(factory != null)
		{
			useSpellCard = factory.get();

			if (useSpellCard != null)
			{
				useSpellCard.init(world, this, user, tgEntity, level);
				canMoveInTimeStop = useSpellCard.canMoveInTimeStop();
			}
			if(spUser instanceof Player)
			{
		    	//スペルカードの魔法陣を出現させる
		    	circle = new EntitySpellCardCircle(ModEntities.SPELL_CARD_CIRCLE.get(), world, spUser, useSpellCard != null ? useSpellCard.getSpellCardCircleColor() : 0, useSpellCard != null ? useSpellCard.getEndTime() : 0);
		    	if(!world.isClientSide)
		    	{
		    		world.addFreshEntity(circle);
		    	}
			}
		}
		else
		{
			if(!world.isClientSide)
			{
				discard();
			}
		}
	}

    /*public EntitySpellCard(World world,EntityLivingBase spUser, EntityLivingBase target, int num, int spLevel)
    {
        this(world, spUser, target, num, level);

    	level = spLevel;
    }*/

    /** 生成時に一度だけ呼ばれるメソッド */
	@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
		builder.define(DATA_SPELL_CARD_NUMBER, 0);
    }

	/**
	 * 当たり判定の大きさを返す（1.7.10 の setSize(0.4F, 0.4F) / yOffset = 0.0F に相当）
	 */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.4F, 0.4F);
	}

    /**
     * 押すことができるかを返す
     */
    @Override
    public boolean isPushable()
    {
        return false;
    }

    /**
     * 他のEntityと接触できるかを返す
     */
    @Override
    public boolean canBeCollidedWith()
    {
    	return false;
    }

	/**
	 * Entityの消滅処理
	 */
	private void finish()
	{
		if(!this.level().isClientSide)
		{
			this.discard();
			if(circle != null)
			{
				circle.discard();
			}
		}
	}

	/**
	 * 指定した時間になったらスペルカードを終了する
	 */
	private void spellCardEnd(int time)
	{
		if(this.tickCount >= time)
		{
			finish();
		}
	}

	private void playerSpellCardDanmakuRemove(int time)
	{
		if(user instanceof Player && tickCount < time)
		{
			THShotLib.danmakuRemove(user, 40.0F, "Other", true);
		}
	}

	private void playerSpellCardDanmakuRemove(int start, int end)
	{
		if(user instanceof Player && tickCount >= start && tickCount < end)
		{
			THShotLib.danmakuRemove(user, 40.0F, "Other", true);
		}
	}

	public void specialProcessInTimeStop()
	{
		useSpellCard.specialProcessInTimeStop();
	}

	/**
	 * Entityが存在する限り毎フレーム呼び出されるメソッド
	 */
	@Override
    public void tick()
    {
    	//時間が進んでいないなら
    	if(tickCount <= lastTime)
    	{
    		return;//処理を終了させる
    	}

        super.tick();

        //使用者がいないか、死んでいるなら消す
    	if(!level().isClientSide && (user == null || !user.isAlive()))
    	{
    		finish();
    		return;
    	}

    	//使用者がいるなら
    	if(user != null)
    	{
    		//使用者が弾幕MOBの場合、スペルカードモードでなければ消滅させる（1.7.10 同様）
    		if(user instanceof thkaguyamod.entity.living.EntityDanmakuMob danmakuMob)
    		{
    			if(!danmakuMob.isSpellCardMode)
    			{
    				finish();
    				return;
    			}
    		}
    	}


    	if(tickCount > spellCardUsedTime/* || spellCardNumber == 8*/)
    	{
    		if(useSpellCard == null)
    		{
    			return;
    		}

    		useSpellCard.onUpdate();

    		playerSpellCardDanmakuRemove(useSpellCard.getRemoveTime());

    		if(useSpellCard.time >= useSpellCard.getEndTime())
    		{
    			finish();
    			return;
    		}
    	}

    	if(spellCardUsedTime < tickCount)
    	{
    		spellCardUsedTime = tickCount;
    	}

		//時間で消滅
		if(tickCount >= 9999)
		{
			if(!level().isClientSide)
			{
				finish();
			}
		}

    	//通常通り時間が進んでいるなら
    	if(tickCount > lastTime)
    	{
    		//最後に時間が動いていたときの時間と移動量を保存する
    		lastTime = tickCount;
    	}
	}


	/**
	 * 保存するデータの書き込み
	 * @param nbtTagCompound : NTBタグ
	 */
	@Override
    protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
    {
    }

	/**
	 * 保存したデータの読み込み
	 * @param nbtTagCompound : NBTタグ
	 */
    @Override
    protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
    {
    }

	/**
	 * スペルカードNoを設定する
	 */
	public void setSpellCardNumber(int number)
	{
		this.entityData.set(DATA_SPELL_CARD_NUMBER, number);
	}

	/**
	 * スペルカードNoを返す
	 */
	public int getSpellCardNumber()
	{
		return this.entityData.get(DATA_SPELL_CARD_NUMBER);
	}

    /**
     * Gets how bright this entity is.
     */
    @Override
    public float getLightLevelDependentMagicValue()
    {
        return 0.5F;
    }

}
