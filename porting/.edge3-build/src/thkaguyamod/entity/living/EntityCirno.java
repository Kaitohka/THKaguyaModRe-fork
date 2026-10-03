package thkaguyamod.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.item.ItemTHShot;
import thkaguyamod.spellcard.EntitySpellCard;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** チルノ */
public class EntityCirno extends EntityDanmakuMob
{
	private int random2;

	/** チルノのコンストラクタ */
	public EntityCirno(EntityType<? extends EntityCirno> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当。寸法は登録側で指定）

		this.xpReward = 250;					// 経験値の量

		this.setDanmakuMobName("Cirno");		// 体力バーに表示される名前を設定
		this.setSpecies(this.SPECIES_FAIRY, this.SPECIES_FAIRY_ICE);
												// 種族の設定

		this.setDanmakuPattern(NORMAL_ATTACK01);	// 最初の弾幕パターン
		this.setMaxHP(38.0F);					// 最初の最大HP
		this.setHealth(38.0F);					// 最初のHP
		this.setSpeed(0.5F);					// 移動速度
		this.setAttackDistance(14.0D);			// 攻撃時取る間合い
		this.setDetectionDistance(30.0D);		// 索敵範囲
		this.setFlyingHeight(2);				// 飛行高度
		this.isFlyingMode = true;				// 飛行モードのONOFF

		this.isSpellCardMode = false;			// スペルカードモード判定
		this.attackInterval = 0;				// 次の攻撃パターンへの移行時間
		this.random2 = 0;						// ランダム値
	}

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
	 * 使用しているスペルカードNoを返す
	 * @return 使用しているスペルカードNo
	 */
	@Override
	public int getUsingSpellCardNo()
	{
		switch(getDanmakuPattern())
		{
		case SPELLCARD_ATTACK01:
			return EntitySpellCard.SC_CIRNO_IcicleFall;
		case SPELLCARD_ATTACK02:
			return EntitySpellCard.SC_CIRNO_PerfectFreeze;
		default:
			return -1;
		}
	}

	//死んでいるときに呼ばれる（1.7.10 の onDeathUpdate）
	/** 多段階ボス：HP 0 では真に死なず、tickDeath() が moveDanmakuAttack で次段階へ進む */
	@Override
	protected boolean canInterceptDeath()
	{
		return getDanmakuPattern() != NOT_ATTACK && getDanmakuPattern() != ATTACK_END;
	}

	@Override
	protected void tickDeath()
	{
		if(tickCount <= lastTime)
		{
			return;
		}

		switch(getDanmakuPattern())
		{
			case NORMAL_ATTACK01:
				moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 46.0F, 60);
				break;
			case SPELLCARD_ATTACK01:
				moveDanmakuAttack(NORMAL_ATTACK02, 60, 32.0F, 20);
				break;
			case NORMAL_ATTACK02:
				moveDanmakuAttack(SPELLCARD_ATTACK02, 40, 58.0F, 60);
				break;
			case SPELLCARD_ATTACK02:
				moveDanmakuAttack(ATTACK_END, 30, 0F, 60);
				break;
			default:
				if(deathTime % 9 == 0)
				{
					THShotLib.explosionEffect(level(), getX(), getY(), getZ(), 1.0F + deathTime * 0.1F);
				}
				super.tickDeath();
		}
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();
	}

	/** 弾幕のパターンを記述
	 * @param level : EASY～LUNATICの難易度
	 */
	@Override
	public void danmakuPattern(int level)
	{
		Vec3 angle;
		angle = THShotLib.angle(getYRot(), getXRot());

		switch(getDanmakuPattern())
		{
			case NORMAL_ATTACK01:
				danmaku01(angle, level);
				break;
			case SPELLCARD_ATTACK01:
				spellcard01(angle, level);
				break;
			case NORMAL_ATTACK02:
				danmaku02(angle, level);
				break;
			case SPELLCARD_ATTACK02:
				spellcard02(angle, level);
				break;
			default:
				break;
		}
	}

	//通常１
	private void danmaku01(Vec3 angle, int level)
	{
		if(attackCounter == 40)
		{
			angle = this.getLookAngle();
			for(int i = 0; i < 7; i++)
			{
				THShotLib.createRingShot(this, this, pos(), angle, 0F, 9999, 0.6D + level * 0.15D, 0.6D + level * 0.15D, 0.1D, gravity_Zero(), ShotData.shot(FORM_CRYSTAL, AQUA, 0.2F, 4.0F, i, 120, 0), i + 2, 0.0D, i * (level), this.random.nextFloat() * 360F);
			}
			//1.7.10 の "random.bow"
			THShotLib.playShotSound(this);
		}
		if(attackCounter >= 50)
		{
			attackCounter = 0;
		}
	}

	//スペルカード１　アイシクルフォール
	private void spellcard01(Vec3 angle, int level)
	{
		if(attackCounter == 1)
		{
			useSpellCard(getUsingSpellCardNo());
		}
		else if(attackCounter > 30 && attackCounter < 170 && attackCounter % 17 == 0 && level >= 2)
		{
			float size = THShotLib.SIZE[THShotLib.SMALL[0] / 8];
			if(level >= 3)
			{
				size = THShotLib.SIZE[THShotLib.MEDIUM[0] / 8];
			}
			THShotLib.createWideShot(this, this, pos(), angle, 0.3D + level * 0.1D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_SMALL, YELLOW, size, 4.0F, 3, 80), 3, 70F, 0.2D);
			//1.7.10 の "random.bow"
			THShotLib.playShotSound(this);
		}
		else if(attackCounter >= 240)
		{
			attackCounter = 0;
		}
	}

	//通常２
	private void danmaku02(Vec3 angle, int level)
	{
		if(attackCounter < 50 && attackCounter % 6 == 0)
		{
			THShotLib.createCircleShot(this, pos(), angle, 0.2D, ShotData.shot(FORM_TINY, WHITE), 5 + level * 2);
			THShotLib.createCircleShot(this, pos(), angle, 0.3D + level * 0.1D, ShotData.shot(FORM_CIRCLE, BLUE), 5 + level * 2);
			//1.7.10 の "random.bow"
			THShotLib.playShotSound(this);
		}
		else if(attackCounter == 55 || attackCounter == 65 || attackCounter == 75)
		{
			THShotLib.createWideLaserA(this, this.pos(), getLookAngle(), 0.3D + level * 0.2D, LaserData.laser(WHITE, 0.1F, 4.0F, 4.0F), 3, 30F);
		}
		if(attackCounter > 128)
		{
			attackCounter = 0;
		}
	}

	//スペルカード２　パーフェクトフリーズ
	private void spellcard02(Vec3 angle, int level)
	{
		if(attackCounter == 1)
		{
			random2 = this.random.nextInt(2);
			useSpellCard(getUsingSpellCardNo());
		}
		else if(attackCounter >= 70 && attackCounter <= 90 && attackCounter % 3 == 0)
		{
			int way = 4 + random2;
			for(int i = 0; i < 3 + level; i++)
			{
				THShotLib.createWideShot(this, this, pos(), angle, 0.3D + level * 0.1D, 0.5D, 1.0D, gravity_Zero(), ShotData.shot(FORM_CIRCLE, BLUE, 0.3F, 4.0F, i, 80), way, 70F, 0.2D);
			}
			//1.7.10 の "random.bow"
			THShotLib.playShotSound(this);
		}
		else if(attackCounter >= 170)
		{
			attackCounter = 0;
		}
	}

	//周りの妖精を呼び出すことができるか
	@Override
	protected boolean canFairyCall()
	{
		return false;//チルノは周りの妖精の助けを得ない
	}

	//倒れたときに落とすアイテム
	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.damagesource.DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		super.dropCustomDeathLoot(level, damageSource, hasBeenAttackedByPlayer);

		if( hasBeenAttackedByPlayer && this.isSpellCardAttack() )
		{
			int j = 40;
			int k;
			Vec3 vec3;
			float yaw, pitch;

			for (k = 0; k < j; k+=2)
			{
				yaw = 360F / (float)j * (float)k;
				pitch = Mth.sin( yaw / 180F * 3.141593F * 4F) * 20F - 60F;
				vec3 = THShotLib.getVecFromAngle(yaw, pitch, 1.0F);
				this.dropPointItem(this.pos(), vec3);
				yaw = 360F / (float)j * (float)(k + 1);
				pitch = Mth.cos( yaw / 180F * 3.141593F * 4F) * 20F - 60F;
				vec3 = THShotLib.getVecFromAngle(yaw, pitch, 1.0F);
				this.dropPowerUpItem(this.pos(), vec3);
			}

			this.dropShotItem(ItemTHShot.CRYSTAL, 17 + this.random.nextInt(2), 5, 32, AQUA, 0, 0, 2);
		}
		if( hasBeenAttackedByPlayer && getDanmakuPattern() == SPELLCARD_ATTACK02)
		{
			//1.7.10 の dropItem(THKaguyaItems.icicle_sword, 1)
			this.spawnAtLocation(new net.minecraft.world.item.ItemStack(thkaguyamod.init.ModItems.ICICLE_SWORD.get()));
			this.dropExtendItem(this.pos(), this.angle(this.getYRot(), -90F));
		}
	}

	/**
	 * Return whether this entity should NOT trigger a pressure plate or a tripwire.
	 * （1.7.10 の doesEntityNotTriggerPressurePlate）
	 */
	@Override
	public boolean isIgnoringBlockTriggers()
	{
		return true;
	}

	@Override
	public int getMaxSpawnClusterSize()
	{
		return 1;
	}

	/**
	 * 自然スポーンするときに呼ばれる
	 * @return trueならスポーン成功
	 */
	@Override
		public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		//P19: 自然生成の全条件（config・確率・地形・明るさ）は ModSpawns の SpawnPlacement 謂詞に一元化。
		//ここで再判定すると確率が二重に掛かる（妖精 50%→25% 等）ため、実体側は常に true。
		return true;
	}
}