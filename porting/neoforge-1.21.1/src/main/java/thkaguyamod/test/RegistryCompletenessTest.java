package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.init.ModBlocks;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModMenus;

/**
 * 阶段 5 质量基建：注册完整性冒烟测试。
 *
 * <p>目的：任何一次「注册表悄悄少一项 / 注册名拼错 / BlockItem 漏注册」都在 GameTest
 * 阶段被抓住，而不是等到玩家在游戏里发现。
 *
 * <h2>期望值来源（2026-10-02 对主树逐文件核对）</h2>
 * <ul>
 *   <li>{@code ModItems.ITEMS} 自身注册 <b>126</b> 项 —— 与阶段 3 验收文档一致
 *       （54 原物品 + metadata 变体拆分）。</li>
 *   <li>{@code BuiltInRegistries.ITEM} 中 thkaguyamod 命名空间共 <b>136</b> 项 =
 *       126 + 10 个 BlockItem（{@link ModBlocks#register} 把 10 个方块注册进
 *       {@code ModItems.ITEMS}，见 ModBlocks.java:80-83）。简报写的「126 个物品」
 *       指的是前者；注册表总数必须是 136，否则说明 BlockItem 丢了。</li>
 *   <li>{@code ModBlocks.BLOCKS} 注册 <b>10</b> 项（2 弹幕工作台 + 8 小神灵）。</li>
 *   <li>{@code ModEntities.ENTITY_TYPES} 注册 <b>46</b> 项。简报写 48，逐行核对
 *       {@code ENTITY_TYPES.register} 调用点与字段声明（ModEntities.java）确为 46；
 *       若后续补注册实体，这里的期望值需要同步更新。</li>
 *   <li>{@code ModMenus.MENU_TYPES} 注册 <b>4</b> 项。</li>
 * </ul>
 *
 * <p>三个断言层次，任一失败都能立刻定位：
 * <ol>
 *   <li>DeferredRegister 自身的条目数（编译期常量级，抓「少写一行 register」）；</li>
 *   <li>真实注册表里 thkaguyamod 命名空间的 id 数量（抓「注册事件没跑 / 被吞」）；</li>
 *   <li>抽查具体 id 存在（抓「注册名被改名」）。</li>
 * </ol>
 *
 * <p>模板：{@code thkaguyamod:empty}（= {@link GameTestHolder} 的命名空间 + {@code template} 名）。
 * NeoForge 不自带空模板，仓库里也找不到现成的，因此随测试一起提供
 * {@code resources/data/thkaguyamod/structure/empty.nbt}（7×7×7 空结构，
 * 由 tools/generate_empty_structure.py 生成）。详见 porting/staging/gametest/README.md。
 *
 * <p>{@code @PrefixGameTestTemplate(false)}：让结构名就是干净的 {@code thkaguyamod:empty}，
 * 而不是 {@code thkaguyamod:registrycompletenesstest.empty}，五个测试类共用同一个模板。
 * 副作用：测试名不带类名前缀（{@code itemsarecomplete} 而非
 * {@code registrycompletenesstest.itemsarecomplete}），因此 {@code /test runthese} 这类
 * 按类名过滤的子命令不会命中 —— 这些测试是给 {@code runGameTestServer} 全量跑的。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistryCompletenessTest {

    /** 结构名 = {@code thkaguyamod:empty}（见 {@code data/thkaguyamod/structure/empty.nbt}）。 */
    private static final String TEMPLATE = "empty";

    private static final int EXPECTED_MOD_ITEMS = 136; // 126 items + 10 BlockItems（ModBlocks 的方块物品也注册进 ModItems.ITEMS）
    private static final int EXPECTED_ITEM_REGISTRY_ENTRIES = EXPECTED_MOD_ITEMS; // BuiltInRegistries.ITEM 的 thkaguyamod 条目 = 同一批，不重复计数
    private static final int EXPECTED_BLOCKS = 10;
    private static final int EXPECTED_ENTITY_TYPES = 46;
    private static final int EXPECTED_MENUS = 4;

    private static final List<String> BLOCK_IDS = List.of(
            "danmaku_crafting_table", "danmaku_laser_crafting_table",
            "divine_spirit_red", "divine_spirit_blue", "divine_spirit_green", "divine_spirit_yellow",
            "divine_spirit_purple", "divine_spirit_aqua", "divine_spirit_orange", "divine_spirit_white");

    private static final List<String> MENU_IDS = List.of(
            "danmaku_crafting", "danmaku_laser_crafting", "merchant_sanae", "miracle_mallet");

    private RegistryCompletenessTest() {}

    @GameTest(template = TEMPLATE)
    public static void itemsAreComplete(GameTestHelper helper) {
        helper.assertValueEqual(ModItems.ITEMS.getEntries().size(), EXPECTED_MOD_ITEMS,
                "ModItems.ITEMS registered entry count");

        List<ResourceLocation> registered = idsIn(BuiltInRegistries.ITEM);
        helper.assertValueEqual(registered.size(), EXPECTED_ITEM_REGISTRY_ENTRIES,
                "thkaguyamod entries in BuiltInRegistries.ITEM (126 ModItems + 10 BlockItems), ids=" + registered);

        assertPresent(helper, BuiltInRegistries.ITEM, "jeweled_branch_of_hourai");
        assertPresent(helper, BuiltInRegistries.ITEM, "small_shot");
        assertPresent(helper, BuiltInRegistries.ITEM, "spell_card_musou_fuuin");
        // 每个方块都必须有对应的 BlockItem，否则创造栏/配方都会缺件
        for (String blockId : BLOCK_IDS) {
            assertPresent(helper, BuiltInRegistries.ITEM, blockId);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blocksAreComplete(GameTestHelper helper) {
        helper.assertValueEqual(ModBlocks.BLOCKS.getEntries().size(), EXPECTED_BLOCKS,
                "ModBlocks.BLOCKS registered entry count");

        List<ResourceLocation> registered = idsIn(BuiltInRegistries.BLOCK);
        helper.assertValueEqual(registered.size(), EXPECTED_BLOCKS,
                "thkaguyamod entries in BuiltInRegistries.BLOCK, ids=" + registered);

        for (String blockId : BLOCK_IDS) {
            assertPresent(helper, BuiltInRegistries.BLOCK, blockId);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void entityTypesAreComplete(GameTestHelper helper) {
        helper.assertValueEqual(ModEntities.ENTITY_TYPES.getEntries().size(), EXPECTED_ENTITY_TYPES,
                "ModEntities.ENTITY_TYPES registered entry count");

        List<ResourceLocation> registered = idsIn(BuiltInRegistries.ENTITY_TYPE);
        helper.assertValueEqual(registered.size(), EXPECTED_ENTITY_TYPES,
                "thkaguyamod entries in BuiltInRegistries.ENTITY_TYPE, ids=" + registered);

        assertPresent(helper, BuiltInRegistries.ENTITY_TYPE, "normal_shot");
        assertPresent(helper, BuiltInRegistries.ENTITY_TYPE, "th_fairy");
        // 1.7.10 时代命名为 EntitySakuyaWatch，移植后注册名按 clean snake_case 定为 private_square
        assertPresent(helper, BuiltInRegistries.ENTITY_TYPE, "private_square");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void menusAreComplete(GameTestHelper helper) {
        helper.assertValueEqual(ModMenus.MENU_TYPES.getEntries().size(), EXPECTED_MENUS,
                "ModMenus.MENU_TYPES registered entry count");

        List<ResourceLocation> registered = idsIn(BuiltInRegistries.MENU);
        helper.assertValueEqual(registered.size(), EXPECTED_MENUS,
                "thkaguyamod entries in BuiltInRegistries.MENU, ids=" + registered);

        for (String menuId : MENU_IDS) {
            assertPresent(helper, BuiltInRegistries.MENU, menuId);
        }

        helper.succeed();
    }

    private static <T> List<ResourceLocation> idsIn(Registry<T> registry) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation id : registry.keySet()) {
            if (THKaguyaMod.MOD_ID.equals(id.getNamespace())) {
                ids.add(id);
            }
        }
        ids.sort(ResourceLocation::compareTo);
        return ids;
    }

    private static <T> void assertPresent(GameTestHelper helper, Registry<T> registry, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, path);
        helper.assertTrue(registry.containsKey(id), "Missing entry " + id + " in " + registry.key().location());
    }
}
