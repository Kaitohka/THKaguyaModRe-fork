# GameTest 冒烟测试（阶段 5 质量基建 · 第一块砖）

只包含测试代码与测试用资源，**不改主树**。合并方式见文末「合并到主树」。

## 文件清单

```
porting/staging/gametest/
├── README.md                              ← 本文件
├── thkaguyamod/test/                      ← 包名 thkaguyamod.test（最终包名，合并=拷贝）
│   ├── RegistryCompletenessTest.java      # 4 test：ITEM 136 / BLOCK 10 / ENTITY_TYPE 46 / MENU 4
│   ├── DanmakuParamsCodecTest.java        # 3 test：CODEC / STREAM_CODEC / 数据组件往返
│   ├── RecipeValidityTest.java            # 3 test：配方数 110 / 抽查 / result 物品已注册
│   ├── DanmakuSpawnTest.java              # 2 test：小弹生成+移动 / 超 endTime 消亡
│   └── ExtendReviveTest.java              # 3 test：extend_item 存在 / 带残机死亡被取消 / 不带不取消
├── resources/data/thkaguyamod/structure/
│   └── empty.nbt                          ← 测试用空结构模板（7×7×7），已生成并校验
└── tools/
    └── generate_empty_structure.py        ← 生成上面那个 nbt 的脚本（可重复执行）
```

共 **15 个 @GameTest**，全部 `required`（默认），`runGameTestServer` 会全量跑并以其退出码反映成败。

## 模板（结构）怎么来的

GameTest 必须先加载一个结构模板。原版成品 jar 里**没有**可供 mod 复用的空模板，NeoForge 也不提供，
仓库里也没有现成的 —— 所以自带一个 `thkaguyamod:empty`：

- 位置：`resources/data/thkaguyamod/structure/empty.nbt` → 合并到 `src/main/resources/data/thkaguyamod/structure/empty.nbt`
- 内容：`size=[7,7,7]`、`palette=[{Name:"minecraft:air"}]`、`blocks=[]`、`entities=[]`（gzip 压缩的未压缩布局 NBT，110 字节）
- 重新生成：`python porting/staging/gametest/tools/generate_empty_structure.py`

测试注解写法：

```java
@GameTestHolder(THKaguyaMod.MOD_ID)          // 自动注册（NeoForge 扫 @GameTestHolder）
@PrefixGameTestTemplate(false)               // 结构名就是 thkaguyamod:empty，不加类名前缀
public final class XxxTest {
    @GameTest(template = "empty")            // templateNamespace 缺省取 @GameTestHolder 的值 = thkaguyamod
    public static void caseName(GameTestHelper helper) { ... helper.succeed(); }
}
```

**已知副作用**：`@PrefixGameTestTemplate(false)` 让测试名不带类名前缀（`itemsarecomplete`
而不是 `registrycompletenesstest.itemsarecomplete`），因此 `/test runthese` 这类按类名过滤的
子命令不会命中这些测试；它们的目标入口是 `gradlew runGameTestServer` 全量跑。

## 怎么跑

```bash
cd porting/neoforge-1.21.1
./gradlew runGameTestServer      # 全量跑，退出码 = 失败的必要测试数
```

`build.gradle` 已配置：`neoforge.enabledGameTestNamespaces=thkaguyamod`（client / server / clientSmoke 运行配置），
`gameTestServer` 运行配置也已存在。

> 注意：`clientSmoke` / `serverSmoke` 目前**没跑** gametest（只有 client/server 配了命名空间属性），
> 且冒烟脚本只关心启动到世界，与 gametest 无关。要不要把 gametest 挂进冒烟脚本由 K3 裁决。

## 测试设计要点 / 与简报的差异

| 项 | 简报 | 实际（已按代码核对） | 处理 |
|---|---|---|---|
| ITEM 数量 | 126 | `ModItems.ITEMS` = 126；`BuiltInRegistries.ITEM` 里 thkaguyamod = **136**（126 + 10 个 BlockItem） | 两个数都断言 |
| BLOCK 数量 | 10 | 10 | 一致 |
| ENTITY_TYPE 数量 | 48 | **46**（`ModEntities.ENTITY_TYPES.register` 调用点与字段声明都是 46） | 断言 46，注释说明 |
| MENU 数量 | 4 | 4 | 一致 |
| 配方抽查 | `icicle_sword` | 配方 id 是 `icicle_sword_bonus`（产物才是 `icicle_sword`） | 抽查 `*_bonus`，并断言产物物品存在 |
| 配方总数 | — | 110（`data/thkaguyamod/recipe/*.json`） | 断言 110 |
| 空模板 | 若需显式模板写 TODO | 自带 `thkaguyamod:empty` + 生成脚本 | 已解决，无 TODO |

### DanmakuSpawnTest 的物理假设（改结构尺寸时同步改）

- 结构 7×7×7，GameTest 会给四周（含 x/z 四个面）自动加屏障墙，弹幕不能飞太远。
- 运动用例：从中心 (3.5,3.5,3.5) 出发，速度 0.25/格每 tick，只 tick 8 次 = 移动 2 格，离墙还有 1 格余量。
- 发射者用 `GameTestHelper.makeMockPlayer`：不入世界，所以不会被弹幕打到，也不会自己走动；
  弹幕的 `user == null → discard()` 守卫仍能通过（非 null 即可）。
- 消亡用例：速度压到 0.02/tick，保证在 endTime=10 之前撞不到墙；
  消亡判定是 `getAnimationCount() > getShotEndTime()`，所以在第 20+5 tick 断言 `isRemoved()`。

### ExtendReviveTest 为什么没降级

简报说「构造玩家持 extend_item 死亡场景太重，可降级为轻断言」。实际上不需要真的让玩家死：
`PlayerDeathEventHandler#onLivingDeath` 只依赖 `LivingDeathEvent`。用 mock player + 往
`NeoForge.EVENT_BUS` 发一个 `LivingDeathEvent` 就能测到**真实接线**（mod 构造期注册的那个监听器），
断言事件被取消、道具 -1、血量回满。保留一个轻断言用例（物品注册 + handler 可加载）。

## 假定 API 清单（NeoForge 21.1.252 / Mojmap，均已在合并 jar 上 `javap` 核对）

- `net.minecraft.gametest.framework.GameTest`（`template` / `templateNamespace` / `timeoutTicks`…）
- `net.minecraft.gametest.framework.GameTestHelper`：
  `getLevel()` / `succeed()` / `fail(String)` / `assertTrue` / `assertFalse` / `assertValueEqual` /
  `runAtTickTime(long, Runnable)` / `absoluteVec(Vec3)` / `makeMockPlayer(GameType)`
- `net.neoforged.neoforge.gametest.GameTestHolder`（值 = templateNamespace）
- `net.neoforged.neoforge.gametest.PrefixGameTestTemplate`
- `net.minecraft.core.Registry#keySet/containsKey/key()`
- `net.minecraft.core.component.DataComponentHolder#get/has`、`DataComponentType#codecOrThrow()`
- `net.minecraft.world.item.crafting.RecipeManager#hadErrorsLoading/getRecipeIds/byKey`
- `net.minecraft.world.item.crafting.Recipe#getResultItem(HolderLookup.Provider)`
- `net.minecraft.network.FriendlyByteBuf(io.netty.buffer.ByteBuf)` + `Unpooled.buffer()`
  （netty 已在 mod 编译/运行 classpath 上：`FriendlyByteBuf` 是 mod 现有 API 的一部分）
- `com.mojang.serialization.DataResult#getOrThrow(Function<String,E>)`（无参重载在 8.0.16 里是 default 方法）
- `net.neoforged.neoforge.event.entity.living.LivingDeathEvent`（`ICancellableEvent`）
- `net.neoforged.neoforge.common.NeoForge.EVENT_BUS#post`
- `net.neoforged.neoforge.registries.DeferredRegister#getEntries/getNamespace`、`DeferredHolder#isBound`

## 未跑验证 / 风险

- **没有跑 `gradlew`**（主树诊断中，简报要求）。已做的验证：用 `javac 21` + 真实 classpath
  （`build/moddev/serverSmokeLegacyClasspath.txt` + `neoforge-21.1.252-merged.jar` + `build/classes/java/main`
  + `build/resources/main`）把 5 个测试类全部编译通过（0 error），产物已删除、临时文件已清理。
- 空模板 nbt 的字节结构已用独立解析脚本逐字节校验（消费完 114 字节无残留），但**没在游戏里加载过**。
- 实体数量 46 / 物品总数 136 / 配方 110 都是按当前主树静态核对的结果；主树若变动，测试期望值要同步。
- `RegistryCompletenessTest` 里 `Registry.key().location()` 只在失败信息里用到（避免导入 `Registries`）。

## 合并到主树（给 K3）

```bash
cp -r porting/staging/gametest/thkaguyamod/test            porting/neoforge-1.21.1/src/main/java/thkaguyamod/test
cp -r porting/staging/gametest/resources/*                  porting/neoforge-1.21.1/src/main/resources/
```

合并后：`./gradlew compileJava` → `./gradlew runGameTestServer`。
