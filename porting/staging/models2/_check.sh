#!/usr/bin/env bash
# Throwaway compile check for the models2 batch: compiles the staged living-entity models
# against the NeoForge 21.1.252 merged artifact with throwaway entity stubs (the real
# thkaguyamod.entity.living.* mobs land in a parallel batch).
set -u
STAGE="D:/AI构建插件源代码/THKaguyaModRe-fork-master/porting/staging/models2"
WORK=/tmp/models2check
JAR="D:/AI构建插件源代码/THKaguyaModRe-fork-master/porting/neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.252-merged.jar"
JOML=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.joml" -name "joml-*.jar" ! -name "*sources*" | head -1)
echo "joml=$JOML"

rm -rf "$WORK"; mkdir -p "$WORK/src/thkaguyamod/client/model/living" "$WORK/src/thkaguyamod/entity/living"
cp "$STAGE/thkaguyamod/client/model/living"/*.java "$WORK/src/thkaguyamod/client/model/living/"

cat > "$WORK/src/thkaguyamod/entity/living/EntityDanmakuMob.java" <<'JAVA'
package thkaguyamod.entity.living;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
public abstract class EntityDanmakuMob extends Mob {
    public EntityDanmakuMob(EntityType<? extends EntityDanmakuMob> type, Level level) { super(type, level); }
    public byte getSpellCardMotion() { return 0; }
    public byte getFlyingHeight() { return 0; }
}
JAVA

for N in EntityCirno EntityMiko EntityReimu EntityRumia EntitySakuya EntitySanae EntityToziko EntityCursedDecoyDoll EntityWriggle; do
cat > "$WORK/src/thkaguyamod/entity/living/$N.java" <<EOF
package thkaguyamod.entity.living;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
public class $N extends EntityDanmakuMob {
    public $N(EntityType<? extends EntityDanmakuMob> type, Level level) { super(type, level); }
}
EOF
done

cat > "$WORK/src/thkaguyamod/entity/living/EntityTHFairy.java" <<'JAVA'
package thkaguyamod.entity.living;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
public class EntityTHFairy extends EntityDanmakuMob {
    public EntityTHFairy(EntityType<? extends EntityDanmakuMob> type, Level level) { super(type, level); }
}
JAVA

cat > "$WORK/src/thkaguyamod/entity/living/EntitySunFlowerFairy.java" <<'JAVA'
package thkaguyamod.entity.living;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
public class EntitySunFlowerFairy extends EntityTHFairy {
    public EntitySunFlowerFairy(EntityType<? extends EntityDanmakuMob> type, Level level) { super(type, level); }
}
JAVA

cat > "$WORK/src/thkaguyamod/entity/living/EntityRinnosuke.java" <<'JAVA'
package thkaguyamod.entity.living;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
public class EntityRinnosuke extends AbstractVillager {
    public EntityRinnosuke(EntityType<? extends AbstractVillager> type, Level level) { super(type, level); }
    @Override protected void updateTrades() {}
    @Override protected void rewardTradeXp(MerchantOffer offer) {}
    @Override public net.minecraft.world.entity.AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.AgeableMob other) { return null; }
}
JAVA

mkdir -p "$WORK/out"
find "$WORK/src" -name "*.java" | while read -r f; do cygpath -w "$f"; done > "$WORK/sources.txt"
cp=$(cygpath -w "$JAR")\;"$(cygpath -w "$JOML")"
"/c/Program Files/Java/zulu-21/bin/javac" -nowarn -d "$(cygpath -w "$WORK/out")" -cp "$cp" @"$(cygpath -w "$WORK/sources.txt")" 2>&1 | head -60
echo "javac exit: ${PIPESTATUS[0]}"
