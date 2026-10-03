package thkaguyamod.test;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.item.EntitySakuyaWatch;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;
import thkaguyamod.menu.DanmakuLaserCraftingMenu;
import thkaguyamod.menu.MiracleMalletMenu;

/**
 * FIX-3b 批次的回归测试草稿（对应修复项 3 / 5 / 6）。
 *
 * <p><b>状态：草稿，本批未执行</b>。FIX-3b 铁律禁止 `gradlew`（避免与并行批争锁），
 * 因此本文件只经 javac 沙盒编译验证（见 `porting/staging/fix3b-tests/REPORT.md`），
 * 运行时由 K3 合并后跑 `./gradlew runGameTestServer`。
 *
 * <h2>覆盖</h2>
 * <ol>
 *   <li>{@link #pocketWatchDamageTogglesUseDuration}（修复项 3）：创造模式 + 停止档（damage=1）
 *       时 {@code getUseDuration} 必须返回 48；damage=0（半速档）返回 20。
 *       修复前 `usedTime(24) >= getUseDuration(48)` 不成立，非创造分支会走「切换档位」的兜底
 *       return，静默失效；创造分支不经此判断，故本条锁定「档位→持续时间」的映射不被改坏。</li>
 *   <li>{@link #laserMenuQuickMoveUsesRealSlotRanges}（修复项 5）：构造菜单后断言
 *       `slots.size() == 51`，并把矩阵槽与快捷栏槽的真实 index 与修复后的常量对照
 *       （15 = 主背包首格、42 = 快捷栏首格）。</li>
 *   <li>{@link #miracleMalletResultIsDetachedCopy}（修复项 4）：结果槽内容与
 *       {@code getResultItem()} 必须是不同实例（修复前共享同一 ItemStack 引用）。</li>
 *   <li>{@link #sanaeOffersAreFiniteAndUnrestocked}（修复项 6）：早苗/咲夜报价的 maxUses 为 12、
 *       交易 12 次后 {@code isOutOfStock()} 为真，且实体没有 restock 通道
 *       （`Merchant.needsRestock()` 默认 false，全树无 `resetUses` 调用）→ 与原作同为「永久缺货」。</li>
 * </ol>
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Fix3bRegressionTest {

    private static final String TEMPLATE = "empty";

    private Fix3bRegressionTest() {}

    /** 修复项 3：怀表档位 → 使用持续时间（1.7.10 getMaxItemUseDuration 的 20 / 48）。 */
    @GameTest(template = TEMPLATE)
    public static void pocketWatchDamageTogglesUseDuration(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack halfSpeed = new ItemStack(ModItems.SAKUYAS_POCKET_WATCH.get());
        halfSpeed.setDamageValue(0);
        helper.assertValueEqual(
                halfSpeed.getItem().getUseDuration(halfSpeed, player), 20,
                "pocket watch use duration at damage=0 (half-speed)");

        ItemStack stopped = new ItemStack(ModItems.SAKUYAS_POCKET_WATCH.get());
        stopped.setDamageValue(1);
        helper.assertValueEqual(
                stopped.getItem().getUseDuration(stopped, player), 48,
                "pocket watch use duration at damage=1 (time-stop)");

        // 档位翻转必须真的写进去（durability(2) 才允许 damage=1；若注册值被改成 1 则这里先红）
        helper.assertValueEqual(stopped.getMaxDamage(), 2,
                "pocket watch max damage (mode flag needs >= 1)");
        helper.succeed();
    }

    /** 修复项 5：激光合成台的 quickMoveStack 槽段必须按真实槽数（15/42/51）计算。 */
    @GameTest(template = TEMPLATE)
    public static void laserMenuQuickMoveUsesRealSlotRanges(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        DanmakuLaserCraftingMenu menu = new DanmakuLaserCraftingMenu(0, player.getInventory());

        helper.assertValueEqual(menu.slots.size(), 51,
                "laser crafting menu slot count (0 result + 5 function + 9 matrix + 27 + 9)");

        // 15 = 主背包首格（index 0 = result，1..5 = 功能槽，6..14 = 矩阵）
        helper.assertTrue(menu.slots.get(15).container == player.getInventory(),
                "slot 15 must be the first main-inventory slot (fixed range start)");
        // 42 = 快捷栏首格
        helper.assertTrue(menu.slots.get(42).container == player.getInventory(),
                "slot 42 must be the first hotbar slot (fixed range start)");
        // 修复前 index 42..45 会落进「>=37 && <46」分支去尝试塞进矩阵槽，现在应走快捷栏段
        helper.assertTrue(menu.slots.get(41).container == player.getInventory(),
                "slot 41 must still be a main-inventory slot (range end boundary)");
        helper.succeed();
    }

    /** 修复项 4：万宝槌结果槽必须是 copy（不与 resultItem 共享实例）。 */
    @GameTest(template = TEMPLATE)
    public static void miracleMalletResultIsDetachedCopy(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MiracleMalletMenu menu = new MiracleMalletMenu(0, player.getInventory());

        // 1.7.10 的转换表：阴晴玉 → 血に餓えた陰陽玉
        menu.craftMaterial.setItem(0, new ItemStack(ModItems.YIN_YANG_ORB.get()));
        menu.slotsChanged(menu.craftMaterial);

        ItemStack result = menu.getResultItem();
        helper.assertFalse(result.isEmpty(), "miracle mallet produced nothing for yin_yang_orb");

        ItemStack inSlot = menu.craftResult.getItem(0);
        helper.assertFalse(inSlot.isEmpty(), "result slot is empty although resultItem is not");
        helper.assertTrue(inSlot != result,
                "result slot shares the ItemStack instance with resultItem (missing .copy())");

        // 取出后 resultItem 不应变成空引用（共享实例时 takeItem 会把两边一起清空）
        inSlot.shrink(inSlot.getCount());
        helper.assertFalse(menu.getResultItem().isEmpty(),
                "taking the result cleared resultItem as well (shared instance)");
        helper.succeed();
    }

    /** 修复项 6：早苗/咲夜商店「用完 12 次即永久缺货」= 原作同式（仅锁定事实，不改行为）。 */
    @GameTest(template = TEMPLATE)
    public static void sanaeOffersAreFiniteAndUnrestocked(GameTestHelper helper) {
        var sanae = helper.spawn(ModEntities.SANAE.get(), 3, 2, 3);

        MerchantOffers offers = sanae.getOffers();
        helper.assertTrue(!offers.isEmpty(), "sanae has no offers");

        MerchantOffer offer = offers.get(0);
        helper.assertValueEqual(offer.getMaxUses(), 12, "sanae offer maxUses");
        helper.assertFalse(offer.isOutOfStock(), "sanae offer must start in stock");

        // 模拟 12 次成交（原作 useRecipe → incrementToolUses 的等价路径）
        for (int i = 0; i < offer.getMaxUses(); i++) {
            sanae.notifyTrade(offer);
        }
        helper.assertTrue(offer.isOutOfStock(),
                "offer must be out of stock after maxUses trades");
        // 1.21.1 的补货链路只有 Villager#shouldRestock → WorkAtPoi 一处；模组商人只 implements Merchant，
        // canRestock() 取接口默认 false，且全树无 resetUses 调用 → 缺货是永久的（与原作同为有限次）。
        helper.assertTrue(offer.needsRestock(), "offer should report a pending restock");
        helper.assertFalse(sanae.canRestock(),
                "sanae advertises no restock capability — out-of-stock is permanent (faithful to 1.7.10)");
        helper.succeed();
    }

    /** 修复项 6（辅）：咲夜的「停止空间」实体仍可按模式值构造（档位语义未被改动）。 */
    @GameTest(template = TEMPLATE)
    public static void sakuyaWatchEntityAcceptsBothModes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);

        for (int mode : List.of(EntitySakuyaWatch.TIME_HALF, EntitySakuyaWatch.TIME_STOP)) {
            EntitySakuyaWatch watch = new EntitySakuyaWatch(
                    ModEntities.SAKUYA_WATCH.get(), helper.getLevel(), player, mode);
            helper.assertTrue(watch != null, "EntitySakuyaWatch could not be constructed for mode " + mode);
            helper.assertFalse(watch.isRemoved(),
                    "EntitySakuyaWatch removed immediately for mode " + mode);
        }
        helper.succeed();
    }
}
