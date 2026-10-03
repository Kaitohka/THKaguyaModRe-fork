package thkaguyamod.danmaku;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.entity.shot.EntityTHShot;

/**
 * 特殊弾を定義するインターフェース.
 * Ported from 1.7.10 {@code ISpecialShot}; {@code World}→{@code Level},
 * {@code MovingObjectPosition}→{@code BlockHitResult}.
 */
public interface ISpecialShot {

    /**
     * 毎tick呼ばれる特殊弾の処理
     * @param id 特殊弾ID
     * @param shot 特殊動作をする弾またはレーザー
     */
    void specialShot_move(Level level, int id, EntityTHShot shot);

    /**
     * ブロックに当たったときの特殊弾の処理
     * @return ブロックに当たって消滅するなら false、そのまま存在し続けるなら true
     */
    boolean specialShot_hitBlock(Level level, int id, EntityTHShot shot, BlockHitResult hitResult);

    /**
     * Entityに当たったときの特殊弾の処理
     * @return Entityに当たって消滅するなら false、そのまま存在し続けるなら true
     */
    boolean specialShot_hitEntity(Level level, int id, EntityTHShot shot, Entity hitEntity);
}
