package thkaguyamod.entity.living;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** ハナビーパー　爆発時に弾幕をまき散らすクリーパー */
public class EntityDanmakuCreeper extends Creeper
{
	/**
	 * Time when this creeper was last in an active state (Messed up code here, probably causes creeper animation to go
	 * weird)
	 */
	private int lastActiveTime;

	/**
	 * The amount of time since the creeper was close enough to the player to ignite
	 */
	private int timeSinceIgnited2;
	private int fuseTime2 = 30;

	/** Explosion radius for this creeper. */
	private int explosionRadius = 3;

	public EntityDanmakuCreeper(EntityType<? extends EntityDanmakuCreeper> type, Level level)
	{
		super((EntityType<? extends Creeper>)(EntityType<?>)type, level);
	}

	/**
	 * 起爆カウンタ（1.7.10 の {@code timeSinceIgnited2}）の現在値を返す。
	 *
	 * <p>FIX-3a でテスト用に公開した読み取り専用アクセサ。フィールドは 1.7.10 から
	 * private のままで、外からは見えなかった（移植でも同様）。P0-2 の回帰テストが
	 * 「起爆後の冷却窓に入ったこと」を実 tick で確認するために必要。
	 * @return 起爆カウンタ。正値は起爆までのカウント、負値は起爆後の冷却カウンタ
	 */
	public int getIgnitionCooldownTicks()
	{
		return timeSinceIgnited2;
	}

	/**
	 * 起爆カウンタを直接設定する（<b>テスト専用</b>）。
	 *
	 * <p>P0-2 の回帰テストは「冷却窓（{@code -280 < t < -100}）の最中に体力が 0 になる」という
	 * 状態を再現する必要がある。実プレイでは点火 → 起爆（30 tick）→ 冷却窓入り（さらに 20 tick）と
	 * 時間がかかり、しかも原版 {@code Creeper.tick()} の fuse（{@code swell/maxSwell}）が
	 * 同時に進行して実体が爆散してしまうため、状態を直接置く方が確実で速い。
	 * 本メソッドはそのためだけに存在し、製品コードからは呼ばれない。
	 * @param ticks 設定する起爆カウンタ（負値なら冷却窓）
	 */
	public void setIgnitionCooldownTicks(int ticks)
	{
		timeSinceIgnited2 = ticks;
	}

	/** 死亡時に毎tick呼ばれる処理（1.7.10 の onDeathUpdate） */
	@Override
	protected void tickDeath()
	{
		++this.deathTime;

		if (this.deathTime == 40)
		{
			int i;

			if (!this.level().isClientSide && this.lastHurtByPlayerTime > 0 && !this.isBaby()
					&& this.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT))
			{
				i = this.getExperienceReward((net.minecraft.server.level.ServerLevel)this.level(), this.lastHurtByPlayer);

				while (i > 0)
				{
					int j = ExperienceOrb.getExperienceValue(i);
					i -= j;
					this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY(), this.getZ(), j));
				}
			}

			this.discard();

			for (i = 0; i < 20; ++i)
			{
				double d0 = this.random.nextGaussian() * 0.02D;
				double d1 = this.random.nextGaussian() * 0.02D;
				double d2 = this.random.nextGaussian() * 0.02D;
				//1.7.10 の "explode" パーティクル
				this.level().addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
						this.getX() + (double)(this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
						this.getY() + (double)(this.random.nextFloat() * this.getBbHeight()),
						this.getZ() + (double)(this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
						d0, d1, d2);
			}
		}
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		boolean danmakuFlag = false;

		if (this.isAlive())
		{
			//1.7.10 は timeSinceIgnited2 += getCreeperState() だった。1.21.1 の相当は getSwellDir()。
			timeSinceIgnited2 += this.getSwellDir();

			if (this.timeSinceIgnited2 >= this.fuseTime2)
			{
				this.timeSinceIgnited2 = -300;//this.fuseTime;
				if (!this.level().isClientSide)
				{
					float damage = 5.0F;
					int level = ModConfig.danmakuLevel;
					float size = 0.1F + level * 0.1F;
					int ways[] = {8, 12, 20, 32, 44};
					int way = ways[Math.max(0, Math.min(ways.length - 1, level))];

					if (this.isPowered())
					{
						Vec3 angle = THShotLib.angle_Random();
						ShotData shot = ShotData.shot(FORM_LIGHT, GREEN, size, damage * 2F);
						THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 1.2D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
						shot.color = RED;
						THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.5D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
						shot.color = YELLOW;
						THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.4D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
					}
					else
					{
						Vec3 angle = THShotLib.angle_Random();
						ShotData shot = ShotData.shot(FORM_LIGHT, GREEN, size, damage);
						THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.6D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
						shot.color = RED;
						THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.5D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
						shot.color = YELLOW;
						THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.4D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
					}
					//1.7.10 の "random.explode"
					this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F,
							(1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F);
				}
				danmakuFlag = true;
			}
		}

		if(timeSinceIgnited2 >= -100 && timeSinceIgnited2 < 0)
		{
			timeSinceIgnited2 = 0;
		}

		// FIX-3a / P0-2（audit-ai）: 1.7.10 由来の同源バグを修正。
		// 旧版のこのブロックは「冷却窓（-280 < t < -100）の間だけ手動で onDeathUpdate() を回し、
		// t が [0,20) のときだけ super.onUpdate() を回す」構造だった。ところが冷却窓の間に
		// 体力が 0 になった（＝プレイヤーに倒された）場合、getHealth() <= 0 で手動死亡更新が
		// スキップされ、t < 20 も成立しないため super.tick() も呼ばれない。すると原版側の
		// 自動死亡推進（LivingEntity.baseTick() の isDeadOrDying() → tickDeath()）が
		// 一度も走らず、さらに isAlive() が false で t が増えないため冷却窓からも永久に出られない
		// → 実体が remove() されずに世界へ残留する（毎回のチャンクロードで tick し続ける）。
		// 対処は最小限: 「死んでいるなら必ず原版の tick() を通す」ことを最優先で保証する。
		// 生きている間の冷却窓の挙動（手動 tickDeath のみ・super は呼ばない）は原作どおり維持する。
		if(this.isDeadOrDying())
		{
			super.tick();
			return;
		}

		if(timeSinceIgnited2 < -100 && timeSinceIgnited2 > -280)
		{
			if(this.getHealth() > 0F)
			{
				this.tickDeath();
			}
		}

		if(timeSinceIgnited2 < 20 && timeSinceIgnited2 >= 0)
		{
			super.tick();
		}
	}

	/**
	 * Called when the mob's health reaches 0.
	 */
	@Override
	public void die(DamageSource damageSource)
	{
		super.die(damageSource);
	}

	/**
	 * 保存するデータの書き込み
	 * @param nbtTagCompound : NTBタグ
	 */
	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbtTagCompound)
	{
		super.addAdditionalSaveData(nbtTagCompound);

		//TODO K3-review: 1.7.10 は "Fuse" に固定値 200 を書き込んでいた（読み書きで一貫しない旧バグ）。
		//1.21.1 の Creeper は "Fuse" を maxSwell として読み書きするため、固定 200 を書くと
		//リロード後に起爆までの時間が変わってしまう。旧値の再現はここでは行わない。
	}
}