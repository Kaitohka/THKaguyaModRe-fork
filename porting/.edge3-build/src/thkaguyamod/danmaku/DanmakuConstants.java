package thkaguyamod.danmaku;

/**
 * 弾幕に関する定数定義 (Danmaku constants).
 * Direct port of the 1.7.10 {@code DanmakuConstants}; the dead bit-packing
 * variants (MASK_COLOR / MASK_FORM era) are dropped.
 *
 * <p>1.7.10 では THShotLib がこのクラスを継承して定数を直参照していたため、
 * 継承できるよう final / private コンストラクタは外してある（定数の値は一切変更なし）。
 */
public class DanmakuConstants {

    /* 難易度（弾幕難易度） */
    public static final int EASY = 1;
    public static final int NORMAL = 2;
    public static final int HARD = 3;
    public static final int LUNATIC = 4;

    /* 弾の色 Shot Colors */
    public static final int RED = 0;
    public static final int BLUE = 1;
    public static final int GREEN = 2;
    public static final int YELLOW = 3;
    public static final int PURPLE = 4;
    public static final int AQUA = 5;
    public static final int ORANGE = 6;
    public static final int WHITE = 7;
    public static final int RANDOM = 8;
    public static final int RAINBOW = 9;

    /* 弾の形状 Shot Forms */
    public static final int FORM_SMALL = 0;
    public static final int FORM_TINY = 1;
    public static final int FORM_MEDIUM = 2;
    public static final int FORM_PEARL = 3;
    public static final int FORM_CIRCLE = 4;
    public static final int FORM_LIGHT = 5;
    public static final int FORM_SCALE = 6;
    public static final int FORM_BUTTERFLY = 7;
    public static final int FORM_SMALLSTAR = 8;
    public static final int FORM_STAR = 9;
    public static final int FORM_RICE = 10;
    public static final int FORM_CRYSTAL = 11;
    public static final int FORM_HEART = 12;
    public static final int FORM_KUNAI = 13;
    public static final int FORM_TALISMAN = 14;
    public static final int FORM_BIGLIGHT = 15;
    public static final int FORM_OVAL = 16;
    public static final int FORM_FAMILIAR = 17;
    public static final int FORM_ARROW = 18;
    public static final int FORM_PHANTOM = 26;
    public static final int FORM_AMULET = 27;
    public static final int FORM_KNIFE = 28;
    public static final int FORM_WIND = 29;
    public static final int FORM_BIG = 30;
    public static final int FORM_KISHITU = 31;

    /* レーザーの形状 Laser Forms */
    public static final int LASER_NORMAL = 0;
    public static final int LASER_GUNGNIR = 1;
    public static final int LASER_LAEVATAIN = 2;

    /* 特殊弾 Special Shot IDs */
    public static final int HOMING01 = 10;
    public static final int HOMING02 = 11;
    public static final int DIFFUSION01 = 20;
    public static final int FALL01 = 30;
    public static final int BOUND01 = 40;
    public static final int BOUND02 = 41;
    public static final int BOUND03 = 42;
    public static final int BOUND04 = 43;
    public static final int BOUNDFALL01 = 50;
    public static final int FIRE = 60;
    public static final int EXPLOSION01 = 70;
    public static final int EXPLOSION02 = 71;
    public static final int BRAKE01 = 150;
    public static final int WIND01 = 180;
    public static final int GUNGNIR = 285;

    /** 標準の重力値 Default gravity */
    public static final double GRAVITY_DEFAULT = -0.03D;

    protected DanmakuConstants() {}
}
