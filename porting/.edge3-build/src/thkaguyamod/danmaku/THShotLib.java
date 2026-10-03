package thkaguyamod.danmaku;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.entity.shot.EntityNuclearShot;
import thkaguyamod.entity.shot.EntityTHLaser;
import thkaguyamod.entity.shot.EntityTHSetLaser;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

/**
 * 弾幕関連の汎用関数群
 *
 * 1.7.10 の THShotLib の移植。Vec3 は不変になったため、生成した Vec3 はすべて新規割り当てになる。
 * 性能上の負債を返済: 1.7.10 は乱数のたびに new Random() していたが、ThreadLocalRandom を再利用する
 * （どちらも非決定的なので意味論は変わらない）。日本語メソッド名の別名（弾生成 等）は移植しない。
 */
public class THShotLib extends DanmakuConstants
{

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		定義	Definition																										*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 弾形状		Shot Forms
	 */
	/** 小弾 */
	public static final int[] SMALL = {  0,  1,  2,  3,  4,  5,  6,  7};
	/** 粒弾 */
	public static final int[] TINY = {  8,  9, 10, 11, 12, 13, 14, 15};
	/** 中弾 */
	public static final int[] MEDIUM = { 16, 17, 18, 19, 20, 21, 22, 23};
	/** 真珠弾 */
	public static final int PEARL[]		= { 24, 25, 26, 27, 28, 29, 30, 31};
	/** 輪弾 */
	public static final int CIRCLE[]	= { 32, 33, 34, 35, 36, 37, 38, 39};
	/** 光弾 */
	public static final int LIGHT[]		= { 40, 41, 42, 43, 44, 45, 46, 47};
	/** 鱗弾 */
	public static final int SCALE[]		= { 48, 49, 50, 51, 52, 53, 54, 55};
	/** 蝶弾 */
	public static final int BUTTERFLY[]	= { 56, 57, 58, 59, 60, 61, 62, 63};
	/** 小星弾 */
	public static final int SMALLSTAR[]	= { 64, 65, 66, 67, 68, 69, 70, 71};
	/** 星弾 */
	public static final int STAR[]		= { 72, 73, 74, 75, 76, 77, 78, 79};
	/** 米弾 */
	public static final int RICE[]		= { 80, 81, 82, 83, 84, 85, 86, 87};
	/** 結晶弾 */
	public static final int CRYSTAL[]	= { 88, 89, 90, 91, 92, 93, 94, 95};
	/** ハート弾 */
	public static final int HEART[]		= { 96, 97, 98, 99,100,101,102,103};
	/** クナイ弾 */
	public static final int KUNAI[]		= {104,105,106,107,108,109,110,111};
	/** 札弾 */
	public static final int TALISMAN[]	= {112,113,114,115,116,117,118,119};
	/** 大光弾 */
	public static final int BIGLIGHT[]	= {120,121,122,123,124,125,126,127};
	/** 楕円弾 */
	public static final int OVAL[]		= {128,129,130,131,132,133,134,135};
	/** 使い魔 */
	public static final int FAMILIAR[]	= {136,137,138,139,140,141,142,143};
	/** 矢弾 */
	public static final int ARROW[]		= {144,145,146,147,148,149,150,151};
	/** アミュレット */
	public static final int AMULET[]	= {216,217,218,219,220,221,222,223};
	/** ナイフ弾 */
	public static final int KNIFE[]		= {224,225,226,227,228,229,230,231};
	/** 風弾 */
	public static final int WIND[]		= {232,233,234,235,236,237,238,239};
	/** 大弾 */
	public static final int BIG[]		= {240,241,242,243,244,245,246,247};
	/** 気質弾 */
	public static final int KISHITU[]	= {248,249,250,251,252,253,254,255};
	/** レーザー */
	public static final int LASER[]		= {512,513,514,515,516,517,518,519};

	/**
	 * 弾の色の名前		Shot Color Names(JP)
	 */
	public static final String COLOR_NAME_JP[]	={
		"赤", "青", "緑", "黄", "紫", "水色", "橙", "白", "虹色", "ランダム", "暖色", "寒色", "", "", "", ""};

	/**
	 * 弾の色の名前		Shot Color Names(EN)
	 */
	public static final String COLOR_NAME[]	={
		"Red", "Blue", "Green", "Yellow", "Purple", "Aqua", "Orange", "White", "Rainbow", "Random", "Hot", "Cold", "", "", "", ""};

	/**
	 * 弾の名前		Shot Form Names
	 */
	public static final String SHOT_NAME_JP[]	={
		  "小弾",  "粒弾",  "中弾","真珠弾",  "輪弾",  "光弾",  "鱗弾",  "蝶弾",
		"小星弾",  "星弾",  "米弾","結晶弾","ハート","クナイ",  "札弾","大光弾",
		"楕円弾","使い魔","　矢弾","未定義","未定義","未定義","未定義","未定義",
		"未定義","未定義","未定義","未定義","ナイフ",  "風弾",  "大弾","気質弾"};

	/**
	 * 形状ごとの弾のダメージを定義		Shot Damage by Form
	 */
	public static final float DAMAGE[] = {	2.4F, 1.0F, 2.8F, 5.0F, 2.0F, 3.4F, 1.6F, 2.8F,
											1.6F, 3.0F, 1.2F, 1.8F, 3.2F, 2.6F, 1.0F, 6.8F,
											3.0F, 1.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
											0.0F, 0.0F, 0.0F, 5.0F, 2.6F, 4.0F, 5.0F, 5.0F};

	/**
	 * 形状ごとの弾の大きさを定義		Shot Size by Form
	 */
	public static final float SIZE[] = {0.30F, 0.15F, 0.50F, 0.30F, 0.30F, 0.30F, 0.15F, 0.30F,
										0.25F, 0.50F, 0.15F, 0.15F, 0.50F, 0.15F, 0.15F, 0.60F,
										0.50F, 1.00F, 0.15F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F,
										0.00F, 0.00F, 0.00F, 0.40F, 0.30F, 1.00F, 0.90F, 0.90F};

	private static int ways[][] ={	{ 0},

			{ 1},
			{ 1, 1},
			{ 1, 0, 2, 0},
			{ 1, 0, 3, 0},
			{ 1, 3, 1},
			{ 1, 4, 1},
			{ 1, 5, 1},
			{ 1, 3, 3, 1},
			{ 1, 5, 3, 0},
			{ 1, 4, 4, 1},

			{ 1, 6, 4, 0},
			{ 1, 5, 5, 1},
			{ 1, 3, 5, 3, 1},
			{ 1, 3, 6, 3, 1},
			{ 1, 4, 5, 4, 1},
			{ 1, 4, 6, 4, 1},
			{ 1, 4, 7, 4, 1},
			{ 1, 3, 5, 5, 3, 1},
			{ 1, 3, 6, 6, 3},
			{ 1, 3, 6, 6, 3, 1},

			{ 1, 5, 9, 5, 1},
			{ 1, 3, 7, 7, 3, 1},
			{ 1, 6, 9, 6, 1},
			{ 1, 4, 7, 7, 4, 1},
			{ 1, 3, 5, 7, 5, 3, 1},
			{ 1, 3, 5, 8, 5, 3, 1},
			{ 1, 3, 5, 9, 5, 3, 1},
			{ 1, 3, 6, 8, 6, 3, 1},
			{ 1, 3, 6, 9, 6, 3, 1},
			{ 1, 4, 6, 8, 6, 4, 1},

			{ 1, 3, 6, 11, 6, 3, 1},
			{ 1, 3, 6, 12, 6, 3, 1},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},

			{ 0},
			{ 0},
			{ 0},
			{ 1, 3, 6, 12, 12, 6, 3, 1}

};

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		弾生成補助	Assistant Create Shots 																						*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 弾IDから形状IDを返す
	 * @param shotID	: 弾の形状と色を含めたID。THShotLib.SMALLSTAR[THShotLib.GREEN]と表される。
	 * @return 形状ID。色を含まない形状単位のID。形状によるデフォルトのサイズやダメージ量を求めるのに必要。
	 */
	public static final int getFormID(int shotID)
	{
		return (shotID - (shotID % 8)) / 8;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		弾生成系	Create Shots																								*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * すべての弾の基礎となる生成処理	Create Shots Base of All
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param slope		 : 弾の進行方向を軸とした傾き。度数で指定（180にすると弾が上下ひっくり返る）
	 * @param rotate        : 弾が角度を変えるときに利用する軸のベクトル（回転軸）
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）。回転の軸はrotateVectorX,Y,Z
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float slope,
											Vec3 rotate, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity, ShotData shot)
	{
		Level level = source.level();

		EntityTHShot entityTHShot;
		ShotData shot2 = shot;
		boolean random = false;
		boolean rainbow = false;
		if(shot2 != null)
		{
			if(shot2.color == RAINBOW)
			{
				shot2.color = ThreadLocalRandom.current().nextInt(7);
				rainbow = true;
			}
			else if(shot2.color == RANDOM)
			{
				shot2.color = ThreadLocalRandom.current().nextInt(8);
				random = true;
			}
		}

		entityTHShot = new EntityTHShot(ModEntities.NORMAL_SHOT.get(), level, user, source, pos,
										angle, slope,
										rotate, rotationSpeed, rotationEnd,
										firstSpeed, limitSpeed, acceleration,
										gravity, shot2);
		if(!level.isClientSide)
		{
			level.addFreshEntity(entityTHShot);
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
		return entityTHShot;
	}

	/**
	 * 軌道変化しない弾の基礎となる生成処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param slope		 : 弾の進行方向を軸とした傾き。度数で指定（180にすると弾が上下ひっくり返る）
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float slope,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot)
	{
		float yaw = getYawFromVector(angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 rotate = getVecFromAngle(yaw, pitch + 90F);
		return createShot(user, source, pos, angle, slope, rotate, 0F, 9999,
										firstSpeed, limitSpeed, acceleration, gravity, shot);
	}

	/**
	 * 加減速を指定できる一般的な弾の生成
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, ShotData shot)
	{
		return createShot(user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), shot);
	}

	/**
	 * 加減速と重力加速度を指定できる弾の生成
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity, ShotData shot)
	{
		return createShot(user, user, pos, angle, 0F,
						firstSpeed, limitSpeed, acceleration, gravity, shot);
	}

	/**
	 * 最も単純な弾の生成
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param speed    	 : 弾の速度
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot)
	{
		return createShot(user, pos, angle, speed, speed, 0.0D, shot);
	}

	/**
	 * すべての扇状弾の基礎となる処理	Create Wide Shots Base
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle	 : 弾の角度のズレ
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, float wideAngle, double distance, float baseAngle)
	{
		//弾の発射方向に対して上方に９０度回転させたベクトル（回転軸）を取得する
		float yaw = getYawFromVector(angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 rotateVec = THShotLib.getVecFromAngle(-yaw, - pitch + 90F, 1.0F);
		//扇の成す角度の半分、取得した回転軸を回転させる
		float angle2 = -wideAngle / 2F;
		float span = wideAngle / (way - 1);
		angle2 += baseAngle;

		//色関連
		int color = ThreadLocalRandom.current().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		ShotData shot2 = shot;
		if(shot2.color == RANDOM)
		{
			random = true;
		}
		else if(shot2.color == RAINBOW)
		{
			rainbow = true;
		}

		if(random)
		{
			shot2.color = ThreadLocalRandom.current().nextInt(8);
		}

		//扇の角度になるまで回転軸に沿って弾を配置する
		for(int i = 0; i < way; i++)
		{
			if(rainbow)
			{
				shot2.color = rainbowPattern[color % 7];
				color++;
			}
			Vec3 angleVec = getVectorFromRotation(rotateVec.x, rotateVec.y, rotateVec.z, angle.x, angle.y, angle.z, angle2);
			createShot(user, source, new Vec3(pos.x + angleVec.x * distance, pos.y + angleVec.y * distance, pos.z + angleVec.z * distance), angleVec,
					 pitch * (float)Math.sin(angle2 / 180F * 3.141593F), rotateVec, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration, gravity, shot);
			angle2 += span;
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
	}

	/**
	 * 旧すべての扇状弾の基礎となる処理
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, float wideAngle, double distance)
	{
		createWideShot(user, source, pos, angle, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
				gravity, shot, way, wideAngle, distance, 0F);
	}

	/**
	 * 軌道変化しない扇状弾の基礎となる処理
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, float wideAngle, double distance, float baseAngle)
	{
		createWideShot(user, source, pos, angle, 0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity, shot, way, wideAngle, distance, baseAngle);
	}

	/**
	 * 旧軌道変化しない扇状弾の基礎となる処理
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, float wideAngle, double distance)
	{
		createWideShot(user, source, pos, angle, 0F, 9999, firstSpeed, limitSpeed, acceleration, gravity, shot, way, wideAngle, distance);
	}

	/**
	 * 最も一般的な扇状弾に加減速が加わったもの
	 */
	public static final void createWideShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, ShotData shot, int way, float wideAngle)
	{
		createWideShot(user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(),
				shot, way, wideAngle, 0.0D);
	}

	/**
	 * 最も一般的な扇状弾
	 */
	public static final void createWideShot(LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot, int way, float wideAngle)
	{
		createWideShot(user, pos, angle, speed, speed, 0.0D, shot, way, wideAngle);
	}

	/**
	 * すべての全方位弾の基礎となる処理	Create Circle Shots Base
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle	 : 弾の角度のズレ
	 */
	public static final void createCircleShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, double distance, float baseAngle)
	{
		if(way % 2 == 0)
		{
			baseAngle += 360F / (way * 2F);
		}

		createWideShot(user, source, pos,
				angle, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, acceleration,
				gravity,
				shot,  way, 360F - (360F / way), distance, baseAngle);
	}

	/**
	 * 旧すべての全方位弾の基礎となる処理
	 */
	public static final void createCircleShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, double distance)
	{
		createCircleShot(user, source, pos,
				angle, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, acceleration,
				gravity,
				shot,  way, distance, 0F);
	}

	/**
	 * 角度連れのない全方位弾の基礎となる処理
	 */
	public static final void createCircleShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, double distance, float baseAngle)
	{
		createCircleShot(user, source, pos,
				angle, 0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity,
				shot, way, distance, baseAngle);
	}

	/**
	 * 最も基本的な全方位弾を発射する
	 */
	public static final void createCircleShot(LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot, int way)
	{
		createCircleShot(user, pos, angle, speed, speed, 0.0D, shot, way);
	}

	/**
	 * 最も基本的な全方位弾を発射するに加速度遅延を指定できるもの
	 */
	public static final void createCircleShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, ShotData shot, int way)
	{
		createCircleShot(user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), shot, way, shot.size, 0F);
	}

	/**
	 * 目の前にリング状の弾幕を発射する基礎処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : リングの広がり。大きいほどリングが大きくなり中央ががら空きになる
	 * @param ringBaseAngle : リングの起点となる角度
	 */
	public static final void createRingShot(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle, float rotationSpeed, int rotationEnd,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		ShotData shot, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		ringSpan /= 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(-angle.x, -angle.y, angle.z);
		Vec3 baseVec = getVecFromAngle(yaw, pitch + ringSpan, 1.0D);
		Vec3 baseOverVec = getVecFromAngle(yaw, pitch + ringSpan + 90F, 1.0D);
		Vec3 overVec = getVecFromAngle(yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2 = ringBaseAngle;
		float span = 360F / way;

		Vec3 movingVec;

		int color = ThreadLocalRandom.current().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		ShotData shot2 = shot;

		if(shot2 != null)
		{
			if(shot2.color == RANDOM)
			{
				random = true;
			}
			else if(shot2.color == RAINBOW)
			{
				rainbow = true;
			}
		}

		if(random)
		{
			shot2.color = ThreadLocalRandom.current().nextInt(8);
		}

		for(int i = 0; i < way; i++)
		{
			if(rainbow)
			{
				shot2.color = rainbowPattern[color % 7];
				color++;
			}
			movingVec = getVectorFromRotation(angle.x, angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(angle.x, angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);
			Vec3 overVec2 = getVectorFromRotation(angle, baseOverVec, angle2);
			createShot(user, source, new Vec3(pos.x + extendVec.x * distance, pos.y + extendVec.y * distance, pos.z + extendVec.z * distance), new Vec3(movingVec.x, movingVec.y, movingVec.z),
					angle2, overVec2, rotationSpeed, rotationEnd,
					firstSpeed, limitSpeed, acceleration, gravity,
					shot2);
			angle2 += span;
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
	}

	/**
	 * 最も基本的な、目の前にリング状の弾幕を発射する
	 */
	public static final void createRingShot(
			LivingEntity user, Vec3 pos,
			Vec3 angle, double speed,
			ShotData shot, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		ShotData shot2 = shot;
		createRingShot(user, user, pos, angle, 0F, 9999,
				speed, speed, 0.0D, gravity_Zero(),
				shot2,
				way, distance, ringSpan, ringBaseAngle);
	}

	/**
	 * 目の前の輪状空間にランダムに弾を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param num           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : 輪状空間の広がり。大きいほど輪状空間が大きくなりばらつく
	 */
	public static final void createRandomRingShot(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle, float rotationSpeed, int rotationEnd,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		ShotData shot, int num, double distance, float ringSpan)
	{
		float ringMaxSpan = ringSpan / 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 baseVec;
		Vec3 overVec = getVecFromAngle(-yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2;
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		Vec3 movingVec;

		int color = ThreadLocalRandom.current().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		if(shot != null)
		{
			if(shot.color == RANDOM)
			{
				random = true;
			}
			else if(shot.color == RAINBOW)
			{
				rainbow = true;
			}
		}

		if(random)
		{
			shot.color = ThreadLocalRandom.current().nextInt(8);
		}

		for(int i = 0; i < num; i++)
		{
			if(rainbow)
			{
				shot.color = rainbowPattern[color % 7];
				color++;
			}

			angle2 = rand.nextFloat() * 360F;
			baseVec = getVecFromAngle(-yaw, pitch + rand.nextFloat() * ringMaxSpan, 1.0D);
			movingVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);
			createShot(user, source, new Vec3(pos.x - extendVec.x * distance, pos.y - extendVec.y * distance, pos.z + extendVec.z * distance), new Vec3(-movingVec.x, -movingVec.y, movingVec.z),
					angle2, angle, rotationSpeed, rotationEnd,
					firstSpeed, limitSpeed, acceleration, gravity,
					shot);
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
	}

	/**
	 * 目の前の輪状空間にランダムに弾を発射する
	 */
	public static final void createRandomRingShot(
			LivingEntity user, Vec3 pos,
			Vec3 angle, double speed, ShotData shot, int num, float ringSpan)
	{
		createRandomRingShot(user, pos, angle,
				speed, speed, 0.0D, shot, num, 0.0D, ringSpan);
	}

	/**
	 * 目の前の輪状空間にランダムに弾を発射する
	 */
	public static final void createRandomRingShot(
			LivingEntity user, Vec3 pos,
			Vec3 angle,
			double firstSpeed, double limitSpeed, double acceleration,
			ShotData shot, int num, double distance, float ringSpan)
	{
		createRandomRingShot(user, user, pos, angle, 0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity_Zero(), shot, num, distance, ringSpan);
	}

	/**
	 * 球状の弾幕を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向（ここでは基準となる弾の方向）
	 * @param rotate		 : 弾が角速度を持つときの回転軸
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle     : 基準となる点からの角度のずれ
	 */
	public static final void createSphereShot(
			LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, float angleZ,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, double distance, float baseAngle)
	{
		//45発以上は未実装
		if(way >= ways.length)
		{
			return;
		}

		Vec3 frontPos = pos(pos.x + angle.x * distance, pos.y + angle.y * distance, pos.z + angle.z * distance);
		Vec3 backPos = pos(pos.x - angle.x * distance, pos.y - angle.y * distance, pos.z - angle.z * distance);

		float angleBase = 0F;
		float angleSpan = 0F;
		if(ways[way].length >= 2)
		{
			angleSpan = 360F / (float)(ways[way].length - 1);
		}
		boolean flagFB = false;
		float slope = 0F;

		ShotData shot2 = shot;
		if(shot2.color == RANDOM)
		{
			shot2.color = ThreadLocalRandom.current().nextInt(8);
		}

		for(int i = 0; i < ways[way].length; i++)
		{
			if(ways[way][i] == 1)
			{
				if(!flagFB)
				{
					createShot(user, source, frontPos, angle, angleZ,
						rotate, rotationSpeed, rotationEnd,
						firstSpeed, limitSpeed, acceleration, gravity,
						shot2);
					flagFB = true;
				}
				else
				{
					createShot(user, source, backPos, angle(-angle.x, -angle.y, -angle.z), angleZ,
							rotate, rotationSpeed, rotationEnd,
							firstSpeed, limitSpeed, acceleration, gravity,
							shot2);
				}
			}
			else
			{
				createRingShot(user, source, pos, angle, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration, gravity, shot2, ways[way][i], distance, angleBase, baseAngle + slope);
				slope += 180F / ways[way].length;
			}

			angleBase += angleSpan;
		}
	}

	/**
	 * 球状の弾幕を発射する
	 */
	public static final void createSphereShot(
			LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, float angleZ,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, double distance, float baseAngle)
	{
		createSphereShot(user, source, pos, angle, angleZ, rotate_Default(), 0.0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity, shot, way, distance, baseAngle);
	}

	/**
	 * 球状の弾幕を発射する
	 */
	public static final void createSphereShot(
			LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot, int way, float baseAngle)
	{
		createSphereShot(user, user, pos, angle, 0F, rotate_Default(), 0.0F, 9999, speed, speed, 0.0D, gravity_Zero(),
				shot, way, 0.0D, baseAngle);
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		レーザー生成系	Create Lasers																					　　　　*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * すべての移動レーザー発射の基礎となる処理		Create Laser Base of Can Move
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : レーザーの発射元。あまり意味は無い
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 未実装　gravity_Zero()とお願いします
	 * @param laser         : レーザーの動作以外のデータ
	 * @return
	 */
	public static final EntityTHLaser createLaserA(	LivingEntity user, Entity source, Vec3 pos, Vec3 angle,
												double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
												LaserData laser)
	{
		if(laser.color == RANDOM )
		{
			laser.color = ThreadLocalRandom.current().nextInt(8);
		}
		else if(laser.color == RAINBOW)
		{
			laser.color = ThreadLocalRandom.current().nextInt(7);
		}
		EntityTHLaser entityLaser = new EntityTHLaser(ModEntities.NORMAL_LASER.get(), source.level(), user, source, pos, angle, 0F, rotate_Default(), 0F, 9999,
												firstSpeed, limitSpeed, acceleration, gravity,
												laser);
		if(!source.level().isClientSide)
		{
			//レーザーを生成する
			source.level().addFreshEntity(entityLaser);
		}
		return entityLaser;
	}

	/**
	 * 最も単純な移動レーザー発射を出す処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param speed         : レーザーの速度
	 * @param gravity       : 未実装　gravity_Zero()とお願いします
	 * @param laser         : レーザーの動作以外のデータ
	 * @return
	 */
	public static final EntityTHLaser createLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double speed, LaserData laser)
	{
		return createLaserA(	user, pos, angle, speed, speed, 0.0D, laser);
	}

	/**
	 * 移動レーザーを出す処理。最も一般的なもの
	 */
	public static final EntityTHLaser createLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, LaserData laser)
	{
		return createLaserA(	user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), laser);
	}

	/**すべてのn-wayレーザーの基礎となる処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : レーザーの発射元。あまり意味は無い
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 未実装　gravity_Zero()とお願いします
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 */
	public static final void createWideLaserA(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											LaserData laser, int way, float wideAngle, double distance)
	{
		//弾の発射方向に対して上方に９０度回転させたベクトル（回転軸）を取得する
		float yaw = getYawFromVector(angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 rotateVec = THShotLib.getVecFromAngle(-yaw, - pitch + 90F, 1.0F);
		//扇の成す角度の半分、取得した回転軸を回転させる
		float angle2 = -wideAngle / 2F;
		float span = wideAngle / (way - 1);

		boolean rainbow = false;
		int color = ThreadLocalRandom.current().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		if(laser.color == RANDOM)
		{
			random = true;
		}
		else if(laser.color == RAINBOW)
		{
			rainbow = true;
		}
		//扇の角度になるまで回転軸に沿って弾を配置する
		for(int i = 0; i < way; i++)
		{
			if(random)
			{
				laser.color = ThreadLocalRandom.current().nextInt(8);
			}
			else if(rainbow)
			{
				laser.color = rainbowPattern[color % 7];
				color++;
			}
			Vec3 angleVec = getVectorFromRotation(rotateVec.x, rotateVec.y, rotateVec.z, angle.x, angle.y, angle.z, angle2);
			createLaserA(user, source, pos(pos.x + angleVec.x * distance, pos.y + angleVec.y * distance, pos.z + angleVec.z * distance), angleVec,
					firstSpeed, limitSpeed, acceleration, gravity, laser);
			angle2 += span;
		}
	}

	/**最も単純なn-wayレーザーを出す処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param speed         : レーザーの初速度
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 */
	public static final void createWideLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double speed,
			LaserData laser, int way, float wideAngle)
	{
		createWideLaserA(user, pos, angle, speed, speed, 0.0D, laser, way, wideAngle);
	}

	/**一般的なn-wayレーザーを出す処理
	 */
	public static final void createWideLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
			LaserData laser, int way, float wideAngle)
	{
		createWideLaserA(user, user, pos, angle, firstSpeed, limitSpeed, acceleration,
				gravity_Zero(), laser, way, wideAngle, 0.0D);
	}

	/**すべての全方位レーザーの基礎となる処理
	 */
	public static final void createCircleLaserA(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											LaserData laser, int way, double distance)
	{
		createWideLaserA(user, source, pos,
				angle, firstSpeed, limitSpeed, acceleration,
				gravity,
				laser, way + 1, 360F, distance);
	}

	/**最も単純な全方位レーザーを出す処理
	 */
	public static final void createCircleLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double speed,
			LaserData laser, int way)
	{
		createCircleLaserA(user, pos, angle, speed, speed, 0.0D, laser, way);
	}

	/**一般的な全方位レーザーを出す処理
	 */
	public static final void createCircleLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
			LaserData laser, int way)
	{
		createCircleLaserA(user, user, pos, angle, firstSpeed, limitSpeed, acceleration,
				gravity_Zero(), laser, way, 0.0D);
	}

	/**
	 * 目の前にリング状のレーザーを発射する基礎処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param laser		 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : リングの広がり。大きいほどリングが大きくなり中央ががら空きになる
	 * @param ringBaseAngle : リングの起点となる角度
	 */
	public static final void createRingLaserA(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		LaserData laser, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		ringSpan /= 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 baseVec = getVecFromAngle(-yaw, pitch + ringSpan, 1.0D);
		Vec3 overVec = getVecFromAngle(-yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2 = ringBaseAngle;
		float span = 360F / way;

		Vec3 movingVec;

		int color = ThreadLocalRandom.current().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		LaserData laser2 = laser;
		if(laser2.color == RANDOM)
		{
			random = true;
		}
		else if(laser2.color == RAINBOW)
		{
			rainbow = true;
		}

		if(random)
		{
			laser2.color = ThreadLocalRandom.current().nextInt(8);
		}

		for(int i = 0; i < way; i++)
		{
			if(rainbow)
			{
				laser2.color = rainbowPattern[color % 7];
				color++;
			}

			movingVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);
			createLaserA(user, source, pos(pos.x - extendVec.x * distance, pos.y - extendVec.y * distance, pos.z + extendVec.z * distance), angle(-movingVec.x, -movingVec.y, movingVec.z),
					firstSpeed, limitSpeed, acceleration, gravity,
					laser2);
			angle2 += span;
		}
		if(random)
		{
			laser.color = RANDOM;
		}
		else if(rainbow)
		{
			laser.color = RAINBOW;
		}
	}

	/**
	 * 目の前にリング状のレーザーを発射する基礎処理
	 */
	public static final void createRingLaserA(
		LivingEntity user, Vec3 pos,
		Vec3 angle,
		double firstSpeed, double limitSpeed, double acceleration,
		LaserData laser, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		createRingLaserA(user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), laser,
				way, distance, ringSpan, ringBaseAngle);
	}

	/**
	 * 目の前の輪状空間にランダムにレーザーを発射する
	 */
	public static final void createRandomRingLaser(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		LaserData laser, int num, double distance, float ringSpan)
	{
		float ringMaxSpan = ringSpan / 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 baseVec;
		Vec3 overVec = getVecFromAngle(-yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2;
		ThreadLocalRandom rand = ThreadLocalRandom.current();

		Vec3 movingVec;
		for(int i = 0; i < num; i++)
		{
			angle2 = rand.nextFloat() * 360F;
			baseVec = getVecFromAngle(-yaw, pitch + rand.nextFloat() * ringMaxSpan, 1.0D);
			movingVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);

			createLaserA(user, source, pos(pos.x - extendVec.x * distance, pos.y - extendVec.y * distance, pos.z + extendVec.z * distance),angle( -movingVec.x, -movingVec.y, movingVec.z),
					firstSpeed, limitSpeed, acceleration, gravity,
					laser);
		}
	}

	/**
	 * 球状のレーザーを発射する
	 */
	public static final void createSphereLaserA(
			LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			LaserData laser, int way, double distance, float baseAngle)
	{
		//45発以上は未実装
		if(way >= ways.length)
		{
			return;
		}

		Vec3 frontPos = pos(pos.x + angle.x * distance, pos.y + angle.y * distance, pos.z + angle.z * distance);
		Vec3 backPos = pos(pos.x - angle.x * distance, pos.y - angle.y * distance, pos.z - angle.z * distance);

		float angleBase = 0F;
		float angleSpan = 0F;
		if(ways[way].length >= 2)
		{
			angleSpan = 360F / (float)(ways[way].length - 1);
		}
		boolean flagFB = false;
		float slope = 0F;

		LaserData laser2 = laser;
		if(laser2.color == RANDOM)
		{
			laser2.color = ThreadLocalRandom.current().nextInt(8);
		}

		for(int i = 0; i < ways[way].length; i++)
		{
			if(ways[way][i] == 1)
			{
				if(!flagFB)
				{
					createLaserA(user, source, frontPos, angle,
						firstSpeed, limitSpeed, acceleration, gravity,
						laser2);
					flagFB = true;
				}
				else
				{
					createLaserA(user, source, frontPos, angle(-angle.x, -angle.y, -angle.z),
							firstSpeed, limitSpeed, acceleration, gravity,
							laser2);
				}
			}
			else
			{
				createRingLaserA(user, source, pos, angle, firstSpeed, limitSpeed, acceleration, gravity, laser2, ways[way][i], distance, angleBase, baseAngle + slope);
				slope += 180F / ways[way].length;
			}

			angleBase += angleSpan;
		}
	}

	/**
	 * すべての設置レーザーの基礎となる生成処理
	 * 仕様変更がある可能性が大であり、不具合があるので設置レーザーを使うのは非推奨
	 */
	public static final EntityTHSetLaser createLaserB(LivingEntity user, Entity setting, Vec3 pos,
			Vec3 angle, Vec3 rotate, float rotateYawSpeed, int rotationEnd,
			LaserData laser, Entity set, double setLength, double setYOffset)
	{
		EntityTHSetLaser entityLaser = new EntityTHSetLaser(ModEntities.SET_LASER.get(), setting.level(), user, setting,
				pos, angle, 0F,
				rotate, rotateYawSpeed, rotationEnd, laser, set, setLength, setYOffset);

		if(!setting.level().isClientSide)
		{
			//レーザーを生成する
			setting.level().addFreshEntity(entityLaser);
		}
		return entityLaser;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		特殊弾の生成	Create Special Shot																									*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 核弾を生成する基礎処理
	 */
	public static EntityNuclearShot createNuclearShot(LivingEntity user, Entity source, Vec3 pos, Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd, double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
			ShotData shot)
	{
		EntityNuclearShot nuclearShot = new EntityNuclearShot(ModEntities.NUCLEAR_SHOT.get(), source.level(), user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, firstSpeed, gravity, shot);
		if(!source.level().isClientSide)
		{
			source.level().addFreshEntity(nuclearShot);//核弾を出現させる
		}
		return nuclearShot;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		エフェクト系	Effect																									*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * ショット発射音を出す
	 * @param entity : 音を出すEntity
	 */
	public static void playShotSound(Entity entity)
	{
		entity.level().playSound(null, entity.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 2.0F, 1.2F);//音を出す (1.7.10 の "random.bow"。1.21.1 では entity.arrow.shoot に相当)
	}

	/**
	 * 爆発の音とパーティクルを出す
	 * @param level
	 * @param posX
	 * @param posY
	 * @param posZ
	 * @param explosionSize
	 */
	public static void explosionEffect(Level level, double posX, double posY, double posZ, float explosionSize)
	{
		level.playSound(null, posX, posY, posZ, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 2.0F, 3.0F);//1.7.10 の "random.explosion"

		if (level instanceof ServerLevel serverLevel)
		{
			if (explosionSize >= 2.0F)
			{
				serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, posX, posY, posZ, 1, 1.0D, 0.0D, 0.0D, 0.0D);//1.7.10 の "hugeexplosion"
			}
			else
			{
				serverLevel.sendParticles(ParticleTypes.EXPLOSION, posX, posY, posZ, 1, 1.0D, 0.0D, 0.0D, 0.0D);//1.7.10 の "largeexplode"
			}
		}
	}

	/**
	 * 軽い爆発の音とパーティクルを出す
	 * @param level
	 * @param posX
	 * @param posY
	 * @param posZ
	 * @param explosionSize
	 */
	public static void explosionEffect2(Level level, double posX, double posY, double posZ, float explosionSize)
	{
		level.playSound(null, posX, posY, posZ, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 2.0F, 3.0F);//1.7.10 の "fireworks.blast"

		if (level instanceof ServerLevel serverLevel)
		{
			if (explosionSize >= 2.0F)
			{
				serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, posX, posY, posZ, 1, 1.0D, 0.0D, 0.0D, 0.0D);
			}
			else
			{
				serverLevel.sendParticles(ParticleTypes.EXPLOSION, posX, posY, posZ, 1, 1.0D, 0.0D, 0.0D, 0.0D);
			}
		}
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		弾幕関連処理系	Danmaku Handle																							*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 周囲から指定の、もしくはすべての弾幕を取り除く
	 * @param centerEntity : 弾消しの中心となるEntity
	 * @param range        : 弾消しの有効距離
	 * @param target       : "ALL"ですべての弾、"Enemy"で敵弾、"Player"でプレイヤーの弾、"Other"で中心となるEntityの所有する弾以外をすべて消す
	 * @param isDropBounus : 弾消しボーナスを出すかどうか。出した場合弾の元に変換され、出さない場合完全に消滅だけする
	 * @return       : 消した弾の数
	 */
	public static int danmakuRemove(Entity centerEntity, double range, String target, boolean isDropBounus)
	{
		int count = 0;
		List<Entity> list = centerEntity.level().getEntities(centerEntity, centerEntity.getBoundingBox().inflate(range));
		for(int k = 0; k < list.size(); k++)
		{
			Entity entity = list.get(k);
			if(entity instanceof EntityTHShot)
			{
				EntityTHShot entityTHShot = (EntityTHShot)entity;
				if(target.equals("ALL"))
				{
					if(isDropBounus)
					{
						entityTHShot.shotFinishBonus();
					}
					else
					{
						entityTHShot.discard();
					}
					count++;
				}
				else if(target.equals("Enemy"))
				{
					if(!(entityTHShot.user instanceof Player))
					{
						if(isDropBounus)
						{
							entityTHShot.shotFinishBonus();
						}
						else
						{
							entityTHShot.discard();
						}
						count++;
					}
				}
				else if(target.equals("Player"))
				{
					if(entityTHShot.user instanceof Player)
					{
						if(isDropBounus)
						{
							entityTHShot.shotFinishBonus();
						}
						else
						{
							entityTHShot.discard();
						}
						count++;
					}
				}
				else if(target.equals("Other"))
				{
					if(entityTHShot.user != centerEntity)
					{
						if(isDropBounus)
						{
							entityTHShot.shotFinishBonus();
						}
						else
						{
							entityTHShot.discard();
						}
						count++;
					}
				}
			}
		}
		return count;
	}

	/**
	 * 倒れたときに周囲の妖精にダメージを与えて消える。距離があるほどダメージが少なくなる
	 * TODO: 1.7.10 は EntityTHFairy（EntityFamiliar を除く）のみを対象としていた。
	 *       EntityTHFairy 移植後に対象を妖精に限定すること。現在は LivingEntity 全般が対象。
	 * @param deadEntity   : 倒れたEntity
	 * @param range        : 周囲にダメージを与える有効距離
	 * @param maxDamage    : 最も接近していた時のダメージ
	 */
	public static void banishExplosion(Entity deadEntity, double range, float maxDamage)
	{
		List<Entity> list = deadEntity.level().getEntities(deadEntity, deadEntity.getBoundingBox().inflate(range));
		for(int k = 0; k < list.size(); k++)
		{
			Entity entity = list.get(k);

			if(entity instanceof LivingEntity fairy)
			{
				double distance = entity.distanceTo(deadEntity);

				if(distance <= range)
				{
					float damage = maxDamage * (1.0F - (float)(distance / range));
					if(damage < 2.0F)
					{
						damage = 2.0F;
					}
					fairy.hurt(deadEntity.level().damageSources().indirectMagic(entity, deadEntity), damage);
				}
			}
		}
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*		計算系	Mathematics																										*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 各成分の位置から位置ベクトルを返す。
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static Vec3 pos(double x, double y, double z)
	{
		return new Vec3(x, y, z);
	}

	/**
	 * Entityの位置ベクトルを返す。
	 * この座標のY座標はEntityによっては足元になる。弾は基本的に中心になる
	 * @param entity
	 * @return
	 */
	public static Vec3 pos_Entity(Entity entity)
	{
		return pos(entity.getX(), entity.getY(), entity.getZ());
	}

	/**
	 * Entityの位置ベクトルを返す。
	 * heightでｙ座標を調整できる
	 * @param entity
	 * @param height ｙ座標の調整値
	 * @return
	 */
	public static Vec3 pos_Entity(Entity entity, double height)
	{
		return pos(entity.getX(), entity.getY() + height, entity.getZ());
	}

	/**
	 * 生物の位置ベクトルを返す。
	 * この座標のY座標はその生物の目より少し下になる
	 * @param living
	 * @return
	 */
	public static Vec3 pos_Living(LivingEntity living)
	{
		return pos(living.getX(), getPosYFromEye(living), living.getZ());
	}

	/**
	 * ある地点からある方向に一定距離離れた位置ベクトルを返す
	 * @param pos 起点となる位置ベクトル
	 * @param angle 起点からの方向
	 * @param distance 起点からの距離
	 * @return
	 */
	public static Vec3 pos_Distance(Vec3 pos, Vec3 angle, double distance)
	{
		return pos(pos.x + angle.x * distance, pos.y + angle.y * distance, pos.z + angle.z * distance);
	}

	/**
	 * ある位置ベクトルからランダムな方向に一定の距離離れた位置ベクトルを返す
	 * @param pos
	 * @param range
	 * @return
	 */
	public static Vec3 pos_Random(Vec3 pos, double range)
	{
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		Vec3 randPos = angle(rand.nextFloat() * 360F, rand.nextFloat() * 180F - 90F);
		return pos.add(randPos);
	}

	/**
	 * 各成分から方向ベクトルを返す
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static Vec3 angle(double x, double y, double z)
	{
		return new Vec3(x, y, z);
	}

	/**
	 * 水平角度と垂直角度から方向ベクトルを返す
	 * @param yaw 水平角度
	 * @param pitch 垂直角度
	 * @return
	 */
	public static Vec3 angle(float yaw, float pitch)
	{
		return getVecFromAngle(yaw, pitch);
	}

	/**
	 * A地点からB地点への方向ベクトルを返す（単位ベクトルとして返す）
	 * @param posA A地点の位置ベクトル
	 * @param posB B地点の位置ベクトル
	 * @return A地点からB地点の方向ベクトル
	 */
	public static Vec3 angle_ToPos(Vec3 posA, Vec3 posB)
	{
		return getVectorNomalize(pos(posB.x - posA.x, posB.y - posA.y, posB.z - posA.z));
	}

	/**
	 * 全方向からランダムな方向ベクトルを返す
	 * @return
	 */
	public static Vec3 angle_Random()
	{
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		return angle(rand.nextFloat() * 360F, rand.nextFloat() * 180F - 90F);
	}

	/**
	 * 基準の方向ベクトルから指定の角度内のランダムな方向ベクトルを返す
	 * @param angle 基準となる方向ベクトル
	 * @param limitAngle ランダムに変化する限界の角度（度数）
	 * @return
	 */
	public static Vec3 angle_LimitRandom(Vec3 angle, float limitAngle)
	{
		Vec3 rotate = rotate_Random();
		return getVectorFromRotation(rotate, angle, ThreadLocalRandom.current().nextFloat() * limitAngle - (limitAngle / 2.0F));
	}

	/**
	 * 各成分の値から軸となるベクトルを返す
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static Vec3 rotate(double x, double y, double z)
	{
		return new Vec3(x, y, z);
	}

	/**
	 * 特に指定のない真上方向に伸びる回転軸のベクトルを返す
	 */
	public static Vec3 rotate_Default()
	{
		return new Vec3(0.0D, 1.0D, 0.0D);
	}

	/**
	 * ランダムな回転軸ベクトルを返す
	 * @return
	 */
	public static Vec3 rotate_Random()
	{
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		return angle(rand.nextFloat() * 360F, rand.nextFloat() * 180F - 90F);
	}

	/**
	 * 各成分から重力のベクトルを返す
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static Vec3 gravity(double x, double y, double z)
	{
		return new Vec3(x, y, z);
	}

	/**
	 * y軸方向にのみ働く重力のベクトルを返す
	 * @param gravityY y軸方向の重力。マイナスで上、プラスで下に向かう
	 * @return
	 */
	public static Vec3 gravity(double gravityY)
	{
		return gravity(0.0D, gravityY, 0.0D);
	}

	/**
	 * 重力の影響を受けない重力のベクトルを返す
	 */
	public static Vec3 gravity_Zero()
	{
		return gravity(0.0D, 0.0D, 0.0D);
	}

	/**
	 * 重力を受ける場合の標準的な重力のベクトルを返す
	 * @return
	 */
	public static Vec3 gravity_Default()
	{
		return gravity(GRAVITY_DEFAULT);
	}

	/**
	 * 水平方向の方向ベクトルを単位ベクトルにして返す
	 * @param vec
	 * @return
	 */
	public static Vec3 getVecOutY(Vec3 vec)
	{
		return getVectorNomalize(new Vec3(vec.x, 0.0D, vec.z));
	}

	/**
	 * 任意のベクトルの真上にあたる方向ベクトルを返す
	 * @param vec
	 * @return
	 */
	public static Vec3 getOverVector(Vec3 vec)
	{
		float yaw, pitch;
		vec = getVecOutY(vec);
		yaw = getYawFromVector(vec.x, vec.z);
		pitch = getPitchFromVector(vec.x, vec.y, vec.z);
		return getVecFromAngle(yaw, pitch + 90F);
	}

	/**
	 * DegreeをRadianに変換する
	 * @param deg : 変換するdegree
	 * @return 変換されたradian
	 */
	public static final float radTrans(float deg)
	{
		return (float)((double)deg / 180.0D * Math.PI);
	}

	/**
	 * DegreeをRadianに変換する
	 * @param deg : 変換するdegree
	 * @return 変換されたradian
	 */
	public static final float radTrans(double deg)
	{
		return (float)(deg / 180.0D * Math.PI);
	}

	/**
	 * 角度を-180～180度に変換する
	 * @param angle 変換する角度
	 * @return -180～180度に変換した角度
	 */
	public static final float getAngleMax180(float angle)
	{
		angle %= 360F;
		if(angle > 180F)
		{
			angle -= 360F;
		}
		else if(angle < -180)
		{
			angle += 360F;
		}
		return angle;
	}

	/**
	 * 単位ベクトルを返す
	 * @param vec 単位ベクトルにするベクトル
	 * @return 単位ベクトル
	 */
	public static final Vec3 getVectorNomalize(Vec3 vec)
	{
		double length = Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);
		length = 1.0D / length;
		return new Vec3(vec.x * length, vec.y * length, vec.z * length);
	}

	/**
	 * ベクトルの各要素を加算したベクトルを返す
	 * @param originalVec  : 各要素を乗算するベクトル
	 * @param addVec       : 加算するベクトル
	 * @return
	 */
	public static final Vec3 getVectorAdd(Vec3 originalVec, Vec3 addVec)
	{
		return new Vec3(originalVec.x + addVec.x, originalVec.y + addVec.y, originalVec.z + addVec.z);
	}

	/**
	 * ベクトルの各要素を乗算したベクトルを返す
	 * @param vec       : 各要素を乗算するベクトル
	 * @param multiply  : 乗算する値
	 * @return
	 */
	public static final Vec3 getVectorMultiply(Vec3 vec, double multiply)
	{
		return new Vec3(vec.x * multiply, vec.y * multiply, vec.z * multiply);
	}

	/**
	 * ２つのベクトル、ベクトルA、ベクトルBの成分から内積を求める
	 * @return ２つのベクトルの内積
	 */
	public static final double getInnerProduct(double vectorAX, double vectorAY, double vectorAZ, double vectorBX, double vectorBY, double vectorBZ)
	{
		return vectorAX * vectorBX + vectorAY * vectorBY + vectorAZ * vectorBZ;
	}

	/**
	 * ２つのベクトル、ベクトルA、ベクトルBの成分から内積を求める
	 * @return ２つのベクトルの内積
	 */
	public static final double getInnerProduct(Vec3 vectorA, Vec3 vectorB)
	{
		return vectorA.x * vectorB.x + vectorA.y * vectorB.y + vectorA.z * vectorB.z;
	}

	/**
	 * ２つのベクトル、ベクトルA、ベクトルBの間の角度を返す（-180～180度）
	 * @return ２つのベクトルの成す角度
	 */
	public static final float getVectorAndVectorAngle(Vec3 vectorA, Vec3 vectorB)
	{
		double inner = getInnerProduct(vectorA, vectorB);
		return getAngleMax180((float)(Math.acos(inner / (vectorA.length() * vectorB.length())) / Math.PI * 180.0D));
	}

	/**
	 * ２つのベクトル、ベクトルA、ベクトルBの成分から外積を求める。
	 * ２つのベクトルに対して垂直なベクトルが求められる。
	 * @return ２つのベクトルの外積
	 */
	public static final Vec3 getOuterProduct(double vectorAX, double vectorAY, double vectorAZ, double vectorBX, double vectorBY, double vectorBZ)
	{
		return new Vec3(	vectorAY * vectorBZ - vectorAZ * vectorBY,
							vectorAZ * vectorBX - vectorAX * vectorBZ,
							vectorAX * vectorBY - vectorAY * vectorBX);
	}

	/**
	 * ２つのベクトル、ベクトルA、ベクトルBから外積を求める
	 * ２つのベクトルに対して垂直なベクトルが求められる。
	 * @return ２つのベクトルの外積
	 */
	public static final Vec3 getOuterProduct(Vec3 vectorA, Vec3 vectorB)
	{
		return getOuterProduct(vectorA.x, vectorA.y, vectorA.z, vectorB.x, vectorB.y, vectorB.z);
	}

	/**
	 * ２つの角度の差を求める。-180°～180°の間で返す。
	 * @param angleA : 基準となる角度。これを０°としてangleBとどれだけ差があるかを返す
	 * @param angleB : 基準の角度angleAと比較する角度。
	 * @return angleAに対するangleBの角度（-180～180度で返る）
	 */
	public static final float getAngleAndAngleSpan(float angleA, float angleB)
	{
		angleA = getAngleMax180(angleA);
		angleB = getAngleMax180(angleB);

		if(angleA == 0.0F)
		{
			return angleB;
		}
		//angleAが右よりなら
		else if(angleA > 0.0F)
		{
			if(angleB > angleA - 180F)
			{
				return angleB - angleA;
			}
			else
			{
				return (angleB - angleA) + 360F;
			}
		}
		//angleAが左よりなら
		else
		{
			if(angleB < angleA + 180F)
			{
				return angleB - angleA;
			}
			else
			{
				return (angleB - angleA) - 360F;
			}
		}
	}

	/**
	 * EntityAが向いている方向と、EntityAに対してEntityBがいる方向の差を求める。水平方向版。（度数）
	 * @return EntityAの向いている方向と、EntityBのいる方向の差（-180～180度で返る）
	 */
	public static final float getEntityAndEntityAngleSpanYaw(Entity entityA, Entity entityB)
	{
		float angleA = getAngleMax180(entityA.getYRot());
		float angleB = (float)Math.atan2(entityB.getX() - entityA.getX(), entityB.getZ() - entityA.getZ()) / 3.141593F * 180F;
		return getAngleAndAngleSpan(angleA, angleB);
	}

	/**
	 * EntityAが向いている方向と、EntityAに対してEntityBがいる方向の差を求める。垂直方向版。（度数）
	 * @return EntityAの向いている方向と、EntityBのいる方向の差（-180～180度で返る）
	 */
	public static final float getEntityAndEntityAngleSpanPitch(Entity entityA, Entity entityB)
	{
		double distanceX = entityB.getX() - entityA.getX();
		double distanceY = entityB.getY() - entityA.getY();
		double distanceZ = entityB.getZ() - entityA.getZ();
		double distanceXZ = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);
		float angleA = getAngleMax180(entityA.getXRot());
		float angleB = (float)Math.atan2(distanceY, distanceXZ) / 3.141593F * 180F;
		return getAngleAndAngleSpan(angleA, angleB);
	}

	/**
	 * ０度で最低、１８０，－１８０度で最大値を取る特殊なsin
	 * @param angle
	 * @return
	 */
	public static final double halfAbsSin(float angle)
	{
		angle = angle * 0.5F;
		return Math.abs(Math.sin(angle));
	}

	/**
	 * 目の高さも含めたY座標を返す
	 * @param living      : 目の高さを含めたY座標を取得するLivingEntity
	 * @param yAdjustment : 目の高さを基準に、高さの調整をさせる
	 * @return：目の高さに調整したY座標
	 */
	public static final double getPosYFromEye(LivingEntity living, double yAdjustment)
	{
		return living.getY() + living.getEyeHeight() + Math.sin((double)living.getXRot() / 180.0D * Math.PI) * yAdjustment + yAdjustment;
	}

	/**
	 * 目の高さも含めたY座標を返す。目より少し下から出す標準的な設定
	 * @param living      : 目の高さを含めたY座標を取得するLivingEntity
	 * @return ：目の高さに調整したY座標
	 */
	public static final double getPosYFromEye(LivingEntity living)
	{
		return living.getY() + living.getEyeHeight() + Math.sin((double)living.getXRot() / 180.0D * Math.PI) * -0.5D + Math.cos((double)living.getXRot() / 180.0D * Math.PI) * -0.5D;
	}

	/**
	 * 角度（度数）と強さからベクトルを取得する
	 * @param yaw   : 水平方向の角度（度数）
	 * @param pitch : 垂直方向の角度（度数）
	 * @param force : ベクトルの強さ
	 * @return ：角度と強さから求まった３次元ベクトル
	*/
	public static final Vec3 getVecFromAngle(float yaw, float pitch, double force)
	{
		double yawRad = (double)yaw / 180.0D * Math.PI;
		double pitchRad = (double)pitch / 180.0D * Math.PI;
		double vectorX = -Math.sin(yawRad) * Math.cos(pitchRad) * force;//X方向　水平方向
		double vectorY = -Math.sin(pitchRad) * force;//Y方向　上下
		double vectorZ =  Math.cos(yawRad) * Math.cos(pitchRad) * force;//Z方向　水平方向
		return new Vec3(vectorX, vectorY, vectorZ);
	}

	/**
	 * 角度（度数）から単位ベクトルを取得する
	 * @param yaw   : 水平方向の角度（度数）
	 * @param pitch : 垂直方向の角度（度数）
	 * @return ：角度から求まった３次元の単位ベクトル
	*/
	public static final Vec3 getVecFromAngle(float yaw, float pitch)
	{
		return getVecFromAngle(yaw, pitch, 1.0D);
	}

	/**
	 * ベクトルから水平方向の角度を返す
	 * @param vectorX : 垂直方向の角度を求めるベクトルのX成分
	 * @param vectorZ :
	 * @return ベクトルから求まった水平方向の角度
	 */
	public static final float getYawFromVector(double vectorX, double vectorZ)
	{
		return  (float)((Math.atan2(vectorX, vectorZ) * 180D) / 3.1415927410125732D);
	}

	/**
	 * ベクトルから垂直方向の角度を返す
	 * @param vectorX : 垂直方向の角度を求めるベクトルのX成分
	 * @param vectorY :
	 * @param vectorZ :
	 * @return ベクトルから求まった垂直方向の角度
	 */
	public static final float getPitchFromVector(double vectorX, double vectorY, double vectorZ)
	{
		double f = Math.sqrt(vectorX * vectorX + vectorZ * vectorZ);
		return  (float)((Math.atan2(vectorY, f) * 180D) / 3.1415927410125732D);
	}

	/**
	 * 特定の角度から水平方向に特定の角度ずれたベクトルを取得する
	 * 要はどんな方向を見ていても、左に３０度ずれた弾の進行方向が変わらないようなベクトルを取得するってもの
	 * @param vectorX : 元のベクトルのX成分
	 * @param vectorY :
	 * @param vectorZ :
	 * @param angle   : 元のベクトルからどれだけ横に角度をずらすか
	 * @param force   : ベクトルの強さ
	 * @return 特定の角度から水平方向に特定の角度ずれたベクトル
	 */
	public static final Vec3 getRotationVector(double vectorX, double vectorY, double vectorZ, float angle, double force)
	{
		float disXZ = (float)Math.sqrt( vectorX * vectorX + vectorZ * vectorZ );
		float angleYaw = (float)Math.atan2(-vectorX, vectorZ);
		float anglePitch  = (float)Math.atan2(-vectorY, disXZ);
		float baseYaw = angleYaw;
		float basePitch = anglePitch;
		double X1, X2, Z1, Z2, vecX, vecY, vecZ;
		float anglePitchRad;
		Vec3 lookAt = new Vec3(vectorX, vectorY, vectorZ);
		angle = angle / 180.0F * (float)Math.PI;

		//飛ぶ方向を設定
		angleYaw = angle;//横の角度　0=正面　+1ごとに左にずれていき360で正面に戻る
		anglePitch = 0;//縦の角度　0=正面　+1ごとに上にずれていき360で正面に戻る
		anglePitchRad = radTrans(anglePitch);

		X1 =  Math.sin(angleYaw) * Math.cos(baseYaw);
		Z1 =  Math.sin(angleYaw) * Math.sin(baseYaw);
		X2 =  Math.cos(angleYaw) * Math.sin(anglePitchRad) * Math.sin(basePitch) * Math.sin(baseYaw);
		Z2 =  Math.cos(angleYaw) * Math.sin(anglePitchRad) * Math.sin(basePitch) * Math.cos(baseYaw);

		vecY = -Math.cos(angleYaw) * Math.sin(basePitch - anglePitchRad);//Y方向　上下
		vecX =  Math.cos(angleYaw) * Math.cos(anglePitchRad) * lookAt.x + X1 - X2;//X方向　水平方向
		vecZ =  Math.cos(angleYaw) * Math.cos(anglePitchRad) * lookAt.z + Z1 + Z2;//Z方向　水平方向

		return new Vec3(vecX * force, vecY * force, vecZ * force);
	}

	/**
	 * 特定の角度から水平方向に特定の角度ずれたベクトルを取得する
	 * 要はどんな方向を見ていても、左に３０度ずれた弾の進行方向が変わらないようなベクトルを取得するってもの
	 * @param yaw   : 元のベクトルの表す水平方向の角度（度数）
	 * @param pitch : 元のベクトルが表す垂直方向の角度（度数）
	 * @param angle   : 元のベクトルからどれだけ横に角度をずらすか
	 * @param force   : ベクトルの強さ
	 * @return 特定の角度から水平方向に特定の角度ずれたベクトル
	 */
	public static final Vec3 getRotationVectorFromAngle(float yaw, float pitch, float angle, double force)
	{
		Vec3 vec3 = getVecFromAngle(yaw, pitch, 1.0D);
		return getRotationVector(vec3.x, vec3.y, vec3.z, angle, force);
	}

	/**
	 * 任意の回転軸rotateVectorに対して、任意のベクトルangleVectorがangle度回転したベクトルを返す
	 * @param rotateVectorX 回転の軸にしたいベクトルのX成分
	 * @param rotateVectorY
	 * @param rotateVectorZ
	 * @param angleVectorX 回転したいベクトルのX成分
	 * @param angleVectorY
	 * @param angleVectorZ
	 * @param angle 回転させたい角度（度数）
	 * @return 任意の回転軸rotateVectorに対して、任意のベクトルangleVectorがangle度回転したベクトル。
	 */
	public static Vec3 getVectorFromRotation(double rotateVectorX, double rotateVectorY, double rotateVectorZ,
											double angleVectorX, double angleVectorY, double angleVectorZ, float angle)
	{
		double angleRad = (double)angle / 180.0D * (double)Math.PI;
		double sinA = Math.sin(angleRad);
		double cosA = Math.cos(angleRad);
		double returnVectorX = (rotateVectorX * rotateVectorX * (1 - cosA) + cosA)              * angleVectorX + (rotateVectorX * rotateVectorY * (1 - cosA) - rotateVectorZ * sinA) * angleVectorY + (rotateVectorZ * rotateVectorX * (1 - cosA) + rotateVectorY * sinA) * angleVectorZ;
		double returnVectorY = (rotateVectorX * rotateVectorY * (1 - cosA) + rotateVectorZ * sinA) * angleVectorX + (rotateVectorY * rotateVectorY * (1 - cosA) + cosA)              * angleVectorY + (rotateVectorY * rotateVectorZ * (1 - cosA) - rotateVectorX * sinA) * angleVectorZ;
		double returnVectorZ = (rotateVectorZ * rotateVectorX * (1 - cosA) - rotateVectorY * sinA) * angleVectorX + (rotateVectorY * rotateVectorZ * (1 - cosA) + rotateVectorX * sinA) * angleVectorY + (rotateVectorZ * rotateVectorZ * (1 - cosA) + cosA)              * angleVectorZ;

		return new Vec3(returnVectorX, returnVectorY, returnVectorZ);
	}

	/**
	 * 任意の回転軸rotateVectorに対して、任意のベクトルangleVectorがangle度回転したベクトルを返す
	 * @param rotate 回転の軸にしたいベクトル
	 * @param angleVector 回転したい方向ベクトル
	 * @param angle 回転させたい角度（度数）
	 * @return 任意の回転軸rotateVectorに対して、任意のベクトルangleVectorがangle度回転したベクトル。
	 */
	public static Vec3 getVectorFromRotation(Vec3 rotate, Vec3 angleVector, float angle)
	{
		return getVectorFromRotation(rotate.x, rotate.y, rotate.z,
				angleVector.x, angleVector.y, angleVector.z, angle);
	}
}
