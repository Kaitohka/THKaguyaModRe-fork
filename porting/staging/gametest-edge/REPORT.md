# gametest-edge 批次报告

范围：为 I7（`EntityTHShot` 延迟期 `getAnimationCount()` 为负 → 渲染器拿去当数组下标致客户端崩溃）
暴露的**边界值测试盲区**补 GameTest。只写 `porting/staging/gametest-edge/`，主树与既有测试
（`porting/neoforge-1.21.1/src/main/java/thkaguyamod/`）**一个字节未改**。

包名 = 最终包名 `thkaguyamod.test`，目录 `thkaguyamod/test/`（合并即拷贝）。全部沿用既有约定：
`@GameTestHolder(THKaguyaMod.MOD_ID)` + `@PrefixGameTestTemplate(false)` + `@GameTest(template = "empty")`
（模板 `thkaguyamod:empty` 已在主树 `src/main/resources/data/thkaguyamod/structure/empty.nbt`）。

---

## 1. 变更清单（新增，无修改）

| 文件（`porting/staging/gametest-edge/thkaguyamod/test/`） | 行数 | 一句话 |
|---|---|---|
| `ShotAnimationCountBoundaryTest.java` | 193 | 4 test：延迟期负计数往返（`setAnimationCount(-80)`）、各 delay 边界、延迟结束转实体化、弹 NBT 往返边界 |
| `ShotIdEncodingBoundaryTest.java` | 210 | 4 test：25 form × 8 color = 200 组 ID 编码互逆、setter 互不破坏、`getShotColor()` 恒 0..7、RANDOM/RAINBOW 在生成路径被归一化 |
| `DanmakuParamsBoundaryTest.java` | 228 | 6 test：color 0/7/越界、逐字段 int 边界、全字段异值极值、infinity 位与 VarInt 字节数、DEFAULT、未知字段忽略（JSON+NBT+ByteBuf 三路径） |
| `IntegerDataComponentBoundaryTest.java` | 285 | 5 test：CHARGE / GAP_COLOR / ORIGINAL_DAMAGE 的 0/上限/负值三路径往返、缺省 absent 语义、0 明示 vs 缺失可区分、ItemStack NBT 三组件同存、networkSynchronized 实际驱动 |
| `SpellCardDeclarationEdgeTest.java` | 203 | 4 test：无视线目标静默失败、视线被方块遮挡失败、64 格内已有符卡时二次宣言失败、CHARGE=0 失败 / CHARGE=1 成功 |
| **合计** | **1119** | **24 个 `@GameTest`**（全部 `required` 默认，`runGameTestServer` 全量跑） |

测试类命名带 `Boundary` / `Edge` 后缀，与既有 6 个类不重名；未改动 `DanmakuParamsCodecTest` /
`SpellCardDeclarationTest` 已覆盖的场景（本批第 4、5 项均刻意避开重复）。

## 2. 验证方式与结果

### 2.1 `compile_staging.sh`（本批要求优先使用的工具）→ **不适用，已绕开并说明**

```
bash porting/tools/compile_staging.sh gametest-edge   → exit 1，18 errors
bash porting/tools/compile_staging.sh items3（基线）  → exit 1，22 errors
```
两组错误**同源且与本批无关**，说明该脚本的「staging 合并」假设与当前 staging 布局已经不匹配
（多个并行批次把同一批类放进了不同子区）：

- **类重复**：`ModDispenserBehaviors`、`ItemBuddhaStoneBowl`、`ItemHisyaku`、`ItemTHSpellCard` 同时存在于
  `staging/items3/` 与主树 —— staging 子区已部分合并进主树；
- **符号缺失**：`EntityCursedDecoyDoll`（在 `tools/staging_blocked.txt` 被排除，但主树 `ModEntities` 已引用）；
- **变量重定义**：`SILVER_KNIFE` / `MIRACLE_CIRCLE`（`tools/staging_modentities_extra.txt` 的插入补丁
  与主树 `ModEntities.java:153/215` 已存在的同名注册冲突）。

### 2.2 采用的替代编译验证（等价 javac 沙盒，未跑 gradlew / 未改主树）

按脚本同样的 classpath 组装方式（`neoforge-21.1.252-merged.jar` + `clientLegacyClasspath.txt` 经
`cygpath -w`），在仓库内临时目录 `porting/.edge-build/`（**用完已删除**）做两次编译：

```bash
# ① 基线：主树 253 个 java → 0 error
javac -nowarn -proc:none -encoding UTF-8 -cp "$MERGED;$LEGACY" -d out @files.txt
# ② 主树 + 本批 staging（259 个 java）→ 0 error，产出：
#    thkaguyamod/test/{ShotAnimationCountBoundaryTest,ShotIdEncodingBoundaryTest,
#                     DanmakuParamsBoundaryTest,IntegerDataComponentBoundaryTest,
#                     SpellCardDeclarationEdgeTest}.class
```
两次输出仅 `deprecation` / `unchecked` 提示，**0 错误**。`-Xlint:deprecation` 复查确认本批 0 警告
（已把 `RegistryFriendlyByteBuf` 的 deprecated 构造器换成带 `ConnectionType.NEOFORGE` 的三参重载）。

**结论：编译层面已通过（javac 等价沙盒）；运行时 GameTest 未执行**（铁律禁止 gradlew，
需要 K3 在合并后跑 `./gradlew runGameTestServer`）。

### 2.3 静态自查要点（编译覆盖不到的部分）

- 全部 `@GameTest` 方法为 `public static void ...(GameTestHelper)`，返回前必调 `helper.succeed()`；
  延迟用例用 `helper.runAtTickTime` 回调里 `succeed()`（与 `DanmakuSpawnTest` 同构）。
- 弹实体的构造走 `EntityTHShot` 的既有 public 20 参构造器（与 1.7.10 生成处理同参数序），
  位置用 `helper.absoluteVec(...)`；不加入世界的弹避免 tick 干扰（只有「延迟结束」用例进世界）。
- 「延迟结束」用例的 tick 预算：`delay=5` 等 10 tick 再断言（`tickCount > lastTime` 推进条件 +
  延迟期内每 tick `+1`，到第 6 tick 起 `getAnimationCount()` 转为 `lastTime`），留够余量。
- 视线遮挡用例：结构是 7³ 空壳，射线为 y=4.62 水平 +X，在 (3,4,3)/(3,3,3) 放石保证遮挡。

## 3. 假定 API 清单（全部经 `javap` / 主树源码核对，沙盒编译通过）

| 符号 | 签名要点 | 用途 |
|---|---|---|
| `EntityTHShot(EntityType,Level,LivingEntity,Entity,Vec3,Vec3,float,Vec3,float,int,double,double,double,Vec3,int,float,float,int,int,int)` | public 20 参 ctor（第 15 参 = shotId） | 直接构造不落世界的弹 |
| `EntityTHShot#setAnimationCount(int)` / `getAnimationCount()` | 存 `count+1000`；服务端 raw<1000 → raw-1000，否则 `lastTime` | 负值往返 |
| `EntityTHShot#setShotId/Form/Color`、`getShotId/Form/Color` | `id = form*8+color`；`color = id%8` | ID 编码互逆 |
| `EntityTHShot#canBeCollidedWith()` | `getAnimationCount() >= 1` | 延迟/实体化判定 |
| `Entity#saveWithoutId(CompoundTag)` / `load(CompoundTag)` | — | NBT 往返（无既有辅助方法可复用） |
| `THShotLib.createShot(LivingEntity,Vec3,Vec3,double,ShotData)`、`angle`、`rotate_Default`、`gravity_Zero` | static | 走真实生成路径 |
| `DanmakuParams`（record）+ `CODEC` / `STREAM_CODEC` / `DEFAULT` | 7 成分全 int/bool | 编解码边界 |
| `ModDataComponents.CHARGE / GAP_COLOR / ORIGINAL_DAMAGE` | `DeferredHolder<..., DataComponentType<Integer>>`，`persistent(Codec.INT)` + `networkSynchronized(ByteBufCodecs.VAR_INT)` | 组件边界 |
| `DataComponentType#codecOrThrow()/streamCodec()` | `StreamCodec#encode/decode` | 实际驱动网络编解码 |
| `ItemStack#set/get/getOrDefault/has/remove/save/parse` | — | absent 语义 + 实物品 NBT |
| `RegistryFriendlyByteBuf(ByteBuf,RegistryAccess,ConnectionType)` | 三参重载（二参已 deprecated） | networkSynchronized 驱动 |
| `GameTestHelper#makeMockPlayer/spawn/absoluteVec/setBlock/getLevel/assertValueEqual/assertTrue/assertFalse/runAtTickTime/succeed` | — | 测试脚手架 |
| `GameTestAssertException`（`net.minecraft.gametest.framework`） | `extends RuntimeException` | `getOrThrow` 的错误工厂 |

## 4. TODO 与降级点（相对简报的偏差，均已在测试注释里写明）

1. **简报第 3 项的「size 0 / 极大 float」无法按字面测**：`DanmakuParams` 的 record 成分是
   `(int number,int form,int speed,int gravity,int special,int color,boolean infinity)`，
   **没有 size / float 字段**（旧 NBT 键只有 Number/DanmakuForm/Speed/Gravity/Special/Color/Infinity；
   弹尺寸由 `ShotData.size` 与 `THShotLib.SIZE[form]` 决定）。
   降级为：① 逐字段 int 极值（含 `Integer.MIN/MAX_VALUE`、VarInt 边界 127/128/16383/16384）；
   ② 「未知字段（`size`/`damage` 浮点）被忽略且不报错」——等价于明确记录该字段不属于本组件。
2. **弹 NBT 往返是「边界」而非「值保留」**：`EntityTHShot#addAdditionalSaveData()` /
   `readAdditionalSaveData()` 是 1.7.10 同款的空实现，`ModEntities.NORMAL_SHOT` 又是 `.noSave()`，
   同步槽（含动画计数）**本来就不进 NBT**。测试按现状固定为：往返不抛异常、UUID/POS 保真、
   读回后计数为 0、读回后负值仍可正常 set/get；并把这个行为写成断言，未来若补持久化会先红。
3. **没有复用既有 NBT 辅助方法**：既有 20 个测试中无 NBT 往返工具，直接调 `saveWithoutId`/`load`；
   未使用 `EntityType.create(tag, level)`，因为它要求 `saveWithoutId` 不写的 `"id"` 字段。
4. **符卡「充能为 0」覆盖为 CHARGE 明示 0**（既有测试只覆盖「组件缺失」）；「无视线目标」用例
   通过「不放任何目标实体」构造；「二次宣言」用例走真实 `ItemStack.use()` 双次调用。

## 5. 需 K3 接线项

1. **合并**：把 `porting/staging/gametest-edge/thkaguyamod/test/*.java` 拷入
   `porting/neoforge-1.21.1/src/main/java/thkaguyamod/test/`（最终包名一致，合并即拷贝）。
   测试类无需任何注册代码——`@GameTestHolder` 由 NeoForge 扫描自动注册。
2. **运行**：`cd porting/neoforge-1.21.1 && ./gradlew runGameTestServer`（`build.gradle` 已配
   `neoforge.enabledGameTestNamespaces=thkaguyamod`；全量应为 **46** = 主树现有 22 + 本批 24）。
   注意 `@PrefixGameTestTemplate(false)` ⇒ 测试名不带类名前缀，`/test runthese` 按类名过滤命中不了。
3. **建议**（非必须）：把 GameTest 挂进 `clientSmoke`/`serverSmoke`，或至少在批次账本里登记本批 24 条；
   另建议顺手修 `tools/staging_blocked.txt` 与 `items3` 的重复（见 §2.1），否则 `compile_staging.sh`
   对后续批次持续返回假失败。

## 6. 未决风险

- **未在真实 GameTest 环境跑过**：所有断言仅经 javac 与源码/字节码核对。最可能在运行期暴露的两处：
  ① `SpellCardDeclarationEdgeTest.blockedLineOfSightFails` 的方块遮挡（依赖 eye 高度与包围盒膨胀，
  与 `SpellCardDeclarationTest` 同结构尺寸，风险低但非零）；
  ② `nbtSaveLoadRoundTripDoesNotCarryAnimationCount` 的位置断言用了 `1.0E-6` 容差（正常坐标应精确相等）。
- **`DanmakuParams` 越界值（color=8/9/16/负数）现在能往返**：本测试刻意固定了「编解码不校验值域」这一现状；
  若将来引入校验（如 `Codec.intRange`），第 1、2 条用例会转红——那是预期的设计变更信号，不是回归。
- **`getShotForm()` 对非法组合仍会给出负数**（`(id-color)/8` 在 id 落在别的色槽时），
  本批只固定了生成/setter 的合法路径与 `getShotColor()` 的值域；未把非法组合的负数行为写成断言
  （避免把一个未决的语义冻进测试）。渲染器假设「form 来自合法路径」这一点仍靠调用方保证。
- 报告未覆盖的：GameTest 的 batch 并发/时序（例如同一结构被多个非 `required` 用例复用）不在本批范围。