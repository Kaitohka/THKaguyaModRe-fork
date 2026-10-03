package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;

/**
 * 阶段 5 质量基建：配方加载有效性测试。
 *
 * <p>为什么值得测：1.21.1 的配方是 datapack 资源，解析失败<b>不会崩游戏</b>，只在日志里
 * 打一行 error 然后静默丢弃。阶段 3 验收时靠人工翻日志确认「配方解析错误 0」，之后任何一次
 * 改注册名 / 改目录结构（{@code recipe/} 单数 vs {@code recipes/} 复数）都会悄悄让配方消失。
 * 这个测试把「日志里的 error」变成「GameTest 失败」。
 *
 * <p>覆盖：
 * <ol>
 *   <li>配方管理器里 thkaguyamod 命名空间的配方数 = 110（{@code src/main/resources/data/thkaguyamod/recipe/*.json} 计数）；</li>
 *   <li>抽查 {@code shot_material} / {@code icicle_sword_bonus} / {@code spell_card_master_spark} 三条配方存在；
 *       简报里写的是 {@code icicle_sword}，但冰剑的配方 id 实际是 {@code icicle_sword_bonus}
 *       （阶段 3 把 8 条 config 门控配方转成了常规 {@code _bonus} 配方，产物是 {@code icicle_sword}）；</li>
 *   <li>遍历全部 thkaguyamod 配方，断言 result 非空且 result 物品确实在 {@code BuiltInRegistries.ITEM} 里注册
 *       （抓「配方指向了不存在的物品」）。</li>
 * </ol>
 *
 * <p>模板见 {@link RegistryCompletenessTest} 的说明（{@code minecraft:empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeValidityTest {

    private static final String TEMPLATE = "empty";

    private static final int EXPECTED_RECIPES = 110;

    private static final List<String> SPOT_CHECK_RECIPE_IDS = List.of(
            "shot_material",
            "icicle_sword_bonus",
            "spell_card_master_spark");

    /** 抽查配方必须产出的物品（顺带验证 recipe 的 result 解析正确）。 */
    private static final List<String> SPOT_CHECK_RESULT_ITEMS = List.of(
            "shot_material",
            "icicle_sword",
            "spell_card_master_spark");

    private RecipeValidityTest() {}

    @GameTest(template = TEMPLATE)
    public static void modRecipesAreLoaded(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        helper.assertFalse(recipes.hadErrorsLoading(), "RecipeManager reported parse errors on load");

        List<ResourceLocation> modRecipes = modRecipeIds(recipes);
        helper.assertValueEqual(modRecipes.size(), EXPECTED_RECIPES,
                "thkaguyamod recipes loaded by the RecipeManager, ids=" + modRecipes);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void spotCheckedRecipesExist(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        for (String path : SPOT_CHECK_RECIPE_IDS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, path);
            helper.assertTrue(recipes.byKey(id).isPresent(), "Missing recipe " + id);
        }
        for (String path : SPOT_CHECK_RESULT_ITEMS) {
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, path);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(itemId), "Missing result item " + itemId);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyRecipeResultIsARegisteredItem(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        int checked = 0;

        for (ResourceLocation id : modRecipeIds(recipes)) {
            Optional<RecipeHolder<?>> holder = recipes.byKey(id);
            if (holder.isEmpty()) {
                // getRecipeIds() 与 byKey() 都是同一张表，走到这里说明 RecipeManager 内部不一致
                helper.fail("RecipeManager lists " + id + " but byKey() cannot find it");
                return;
            }
            ItemStack result = holder.get().value().getResultItem(helper.getLevel().registryAccess());
            if (result.isEmpty()) {
                helper.fail("Recipe " + id + " has an empty result (unresolved item id?)");
                return;
            }
            ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId == null) {
                helper.fail("Recipe " + id + " produces an unregistered item " + result.getItem());
                return;
            }
            checked++;
        }

        helper.assertValueEqual(checked, EXPECTED_RECIPES,
                "recipes whose result item resolved to a registered item");
        helper.succeed();
    }

    private static List<ResourceLocation> modRecipeIds(RecipeManager recipes) {
        List<ResourceLocation> ids = new ArrayList<>();
        recipes.getRecipeIds()
                .filter(id -> THKaguyaMod.MOD_ID.equals(id.getNamespace()))
                .forEach(ids::add);
        ids.sort(ResourceLocation::compareTo);
        return ids;
    }
}
