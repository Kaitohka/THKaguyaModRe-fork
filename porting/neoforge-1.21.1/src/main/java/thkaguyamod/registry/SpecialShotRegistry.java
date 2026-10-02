package thkaguyamod.registry;

import java.util.HashMap;
import java.util.Map;

import thkaguyamod.danmaku.ISpecialShot;

/**
 * 特殊弾の登録レジストリ.
 *
 * <p>The 1.7.10 version stored {@code Class<? extends ISpecialShot>} and called
 * {@code Class.newInstance()} on every lookup — every tick, per bullet. The port
 * stores live instances (special-shot handlers are stateless in practice), which
 * removes the reflection hotspot entirely.
 */
public final class SpecialShotRegistry {

    private static final Map<Integer, ISpecialShot> SPECIAL_SHOT_MAP = new HashMap<>();

    private SpecialShotRegistry() {}

    public static void registerSpecialShot(ISpecialShot specialShot, int id) {
        SPECIAL_SHOT_MAP.put(id, specialShot);
    }

    /** @return the handler, or {@code null} when no special shot is registered for the id */
    public static ISpecialShot getSpecialShot(int id) {
        return SPECIAL_SHOT_MAP.get(id);
    }

    public static boolean isSpecialShot(int id) {
        return SPECIAL_SHOT_MAP.containsKey(id);
    }

    public static int getNumberOfSpecialShot() {
        return SPECIAL_SHOT_MAP.size();
    }
}
