package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.init.ModItems;

/**
 * 阶段 5 质量基建：残机（extend_item）复活逻辑测试。
 *
 * <p>原计划「构造玩家持 extend_item 死亡场景太重」——实际不需要让玩家真的死：
 * 复活逻辑挂在 {@link thkaguyamod.event.PlayerDeathEventHandler#onLivingDeath}，
 * 它只依赖 {@link LivingDeathEvent}。用 {@link GameTestHelper#makeMockPlayer} 造一个
 * 不入世界的玩家、把 extend_item 放进物品栏、向 {@code NeoForge.EVENT_BUS} 发一个事件即可。
 * 这样测的是<b>真实接线</b>（mod 构造期注册的监听器），而不是重新 new 一个 handler。
 *
 * <p>覆盖：
 * <ol>
 *   <li>extend_item 已在 {@code BuiltInRegistries.ITEM} 注册，且 {@code PlayerDeathEventHandler} 可加载/实例化；</li>
 *   <li>带 extend_item 的玩家收到 LivingDeathEvent → 事件被取消、道具 -1、血量回满；</li>
 *   <li>不带 extend_item 的玩家收到 LivingDeathEvent → 事件不被取消（不误复活）。</li>
 * </ol>
 *
 * <p>注意：mock player 没有真的「死亡」，所以 {@code event.setCanceled(true)} 只体现在事件对象上；
 * 这正是本测试要断言的东西（事件被取消 = 原版死亡流程会被拦截）。
 *
 * <p>模板见 {@link RegistryCompletenessTest} 的说明（{@code minecraft:empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtendReviveTest {

    private static final String TEMPLATE = "empty";

    private ExtendReviveTest() {}

    @GameTest(template = TEMPLATE)
    public static void extendItemAndHandlerExist(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "extend_item");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id),
                "Missing item " + id);
        helper.assertTrue(ModItems.EXTEND_ITEM.isBound(), "ModItems.EXTEND_ITEM was never bound");

        try {
            Class.forName("thkaguyamod.event.PlayerDeathEventHandler", false,
                    ExtendReviveTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            helper.fail("PlayerDeathEventHandler is not loadable: " + e);
            return;
        }
        helper.assertTrue(new thkaguyamod.event.PlayerDeathEventHandler() != null,
                "PlayerDeathEventHandler could not be instantiated");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deathWithExtendItemIsCancelled(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(1.0F);
        player.getInventory().setItem(0, new ItemStack(ModItems.EXTEND_ITEM.get(), 2));

        LivingDeathEvent event = new LivingDeathEvent(player, player.damageSources().generic());
        NeoForge.EVENT_BUS.post(event);

        helper.assertTrue(event.isCanceled(),
                "LivingDeathEvent was not cancelled although the player carried an extend_item");
        helper.assertValueEqual(player.getInventory().getItem(0).getCount(), 1,
                "extend_item count after revive (one charge must be consumed)");
        helper.assertValueEqual(player.getHealth(), player.getMaxHealth(),
                "player health after revive");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deathWithoutExtendItemIsNotCancelled(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(1.0F);

        LivingDeathEvent event = new LivingDeathEvent(player, player.damageSources().generic());
        NeoForge.EVENT_BUS.post(event);

        helper.assertFalse(event.isCanceled(),
                "LivingDeathEvent was cancelled although the player carried no extend_item");
        helper.assertValueEqual(player.getHealth(), 1.0F, "player health (must be untouched)");
        helper.succeed();
    }
}
