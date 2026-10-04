package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;

/**
 * 自然生成「群系名单」闸门 — biome_modifier 静默失效的唯一低成本探针。
 *
 * <h2>为什么需要这道闸门</h2>
 * <p>NeoForge 的 biome modifier 是**数据包注册表**，文件必须放在
 * {@code data/<ns>/neoforge/biome_modifier/*.json}（目录名由
 * {@code CommonHooks.prefixNamespace("neoforge:biome_modifier")} 推出，见
 * {@code RegistryDataLoader#loadContentsFromManager}）。放错路径时
 * {@code RegistryDataLoader} 只是「没有元素可读」——**不报错、不告警**，于是：
 * 注册表为空 → 群系 spawn 列表里没有任何模组条目 → {@code NaturalSpawner} 永远
 * 抽不到模组怪，而 smoke 的「0 ERROR / 0 recipe 错」闸门完全看不到。
 * 同类静默失效还有 {@code biomes} 字段的写法（字符串数组不被 {@code HolderSetCodec} 接受）。
 *
 * <p>本测试直接从「平原 / 森林 / 沙漠」三个群系读回**真实的** {@code MobSpawnSettings}，
 * 断言里面至少有 thkaguyamod 实体。任何路径/语法问题都会在这里变红，而不是等玩家
 * 逛遍地图发现没有妖精。
 *
 * <p>实现注意：{@code EntityType#toString()} 返回的是 {@code entity.thkaguyamod.th_fairy}
 * 这种带点的显示名，不是注册 id。必须走
 * {@code BuiltInRegistries.ENTITY_TYPE.getKey(type)} 取真正的
 * {@code thkaguyamod:th_fairy} —— 首版就是踩了这个坑而误报。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BiomeSpawnGateTest {

    private static final String TEMPLATE = "empty";

    /** 抽查群系：三者由不同的 biome_modifier 文件覆盖，可顺带证明多文件都生效。 */
    private static final List<String> PROBE_BIOMES = List.of("plains", "forest", "desert");

    private BiomeSpawnGateTest() {}

    /** 主闸门：三个代表群系的 spawn 列表里必须出现 thkaguyamod 实体。 */
    @GameTest(template = TEMPLATE)
    public static void biomeModifierSpawnsAreApplied(GameTestHelper helper) {
        for (String biomeName : PROBE_BIOMES) {
            List<String> modSpawns = modSpawnTypesOf(helper, biomeName);
            helper.assertTrue(!modSpawns.isEmpty(),
                    "群系 minecraft:" + biomeName + " 的 spawn 列表里没有任何 thkaguyamod 实体 —— "
                            + "biome_modifier 未生效（文件必须放在 data/<ns>/neoforge/biome_modifier/，"
                            + "且 biomes 字段不能写成字符串数组）");
        }

        helper.succeed();
    }

    /**
     * 反向抽查：沙漠里必须同时有 th_fairy 与 danmaku_creeper
     * （= 至少两个不同的 biome_modifier JSON 都被加载并应用）。
     */
    @GameTest(template = TEMPLATE)
    public static void multipleBiomeModifierFilesAreApplied(GameTestHelper helper) {
        List<String> modSpawns = modSpawnTypesOf(helper, "desert");

        helper.assertTrue(modSpawns.contains("thkaguyamod:th_fairy"),
                "沙漠缺少 th_fairy（th_fairy.json 未生效）: " + modSpawns);
        helper.assertTrue(modSpawns.contains("thkaguyamod:danmaku_creeper"),
                "沙漠缺少 danmaku_creeper（danmaku_creeper.json 未生效）: " + modSpawns);

        helper.succeed();
    }

    /** 该群系全部 MobCategory 里 thkaguyamod 命名空间的 spawn 条目注册 id。 */
    private static List<String> modSpawnTypesOf(GameTestHelper helper, String biomeName) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(biomeName)));

        List<String> ids = new ArrayList<>();
        Stream.of(MobCategory.values())
                .flatMap(category -> biome.getMobSettings().getMobs(category).unwrap().stream())
                .map(data -> data.type)
                .map(BiomeSpawnGateTest::idOf)
                .filter(id -> id != null && id.startsWith(THKaguyaMod.MOD_ID + ":"))
                .distinct()
                .sorted()
                .forEach(ids::add);
        return ids;
    }

    private static String idOf(EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key == null ? null : key.toString();
    }
}
