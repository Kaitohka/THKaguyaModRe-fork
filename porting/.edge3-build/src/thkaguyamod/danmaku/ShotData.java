package thkaguyamod.danmaku;

/**
 * 弾の性質設定 — non-motion properties of a danmaku shot.
 * Direct port of the 1.7.10 {@code ShotData} POJO.
 */
public class ShotData {

    /** 弾の形状 (see {@link DanmakuConstants} FORM_*) */
    public int form;
    /** 弾の色 (see {@link DanmakuConstants} color constants) */
    public int color;
    /** 弾のダメージ。2.0F でハート1つ分 */
    public float damage;
    /** 弾のサイズ。見た目ではなく当たり判定 */
    public float size;
    /** 弾の遅延時間 (tick)。この間は動作なし・当たり判定なし */
    public int delay;
    /** 弾の消滅時間 (tick)。遅延時間は含まれない */
    public int end;
    /** 特殊弾 ID */
    public int special;

    public ShotData(int form, int color, float size, float damage, int delay, int end, int special) {
        this.form = form;
        this.color = color;
        this.damage = damage;
        this.size = size;
        this.delay = delay;
        this.end = end;
        this.special = special;
    }

    public static ShotData shot(int form, int color, float size, float damage, int delay, int end, int special) {
        form = form % 32;
        color = color % 16;
        return new ShotData(form, color, size, damage, delay, end, special);
    }

    public static ShotData shot(int form, int color, float size, float damage, int delay, int end) {
        return shot(form, color, size, damage, delay, end, 0);
    }

    public static ShotData shot(int form, int color, float size, float damage) {
        return shot(form, color, size, damage, 0, 80);
    }

    public static ShotData shot(int form, int color) {
        return shot(form, color, 0);
    }

    public static ShotData shot(int form, int color, int delay) {
        return shot(form, color, delay, 80);
    }

    public static ShotData shot(int form, int color, int delay, int end) {
        return shot(form, color, delay, end, 0);
    }

    public static ShotData shot(int form, int color, int delay, int end, int special) {
        float size = THShotLib.SIZE[form % 32];
        float damage = THShotLib.DAMAGE[form % 32];
        return shot(form, color, size, damage, delay, end, special);
    }
}
