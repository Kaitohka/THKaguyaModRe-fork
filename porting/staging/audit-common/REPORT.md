# audit-common — 逻辑侧（common + 共享代码）同类隐患普查

**总结（前 3 行）**
- **P0 = 1 项**：`DanmakuPatternRegistry` 从未初始化 → `EntityTHFairy` 构造器必然 `IndexOutOfBoundsException`（已在浸泡测试日志中实锤，18 次）；这是与 I7 同源的「移植遗漏调用点」类 bug，不是奇偶下标 bug。
- **P1 = 4 项**：`DanmakuParams` 不夹紧负值→下游数组下标（含 `dropShotItem` 负取模）、`EntitySpellCard.specialProcessInTimeStop()` 潜在 NPE、`EntitySakuyaWatch/StopWatch` 解引用可能为 null 的 `spellCard.user`、`THSC_Fafurotskies` 把速度当模式编号（原作即如此，登记待裁决）。
- **P2 = 11 项**（已排除项与观察项，见下表与「P2 — 观察」，不阻塞发布）。

范围：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/` 下全部 **非 `client/`** 包（entity/、item/、danmaku/、spellcard/、component(init/ModDataComponents)、network/、menu/、init/、config/、registry/、block/、blockentity/、event/、test/）。
只读审计，未修改任何既有文件；未运行 gradlew/git；未杀进程。

---

## P0 — 崩溃/数据损坏

| 文件:行 | 模式 | 可达路径 | 严重度 | 置信度 | 最小修复建议 | 对照原作结论 |
|---|---|---|---|---|---|---|
| `entity/living/EntityTHFairy.java:96`（`setPattern` 的 `DanmakuPatternRegistry.pattern.get(patternId)`）；调用点 `:45`、`EntityFamiliar.java:29`、`EntityTHPhantom.java:33` | 数组/List 下标来自**未初始化的静态注册表**（模式 1 变体：下标来源为空表） | `DanmakuPatternRegistry.setDanmakuPattern()` 在整个移植树（含 `resources/`、`build/` 产物、`test/`）**无任何调用点**（grep 证据见下）。因此 `pattern` 永远为空 `ArrayList`，`pattern.get(0)` 必抛 `IndexOutOfBoundsException`。凡构造妖精系实体即崩：`/summon thkaguyamod:th_fairy`、自然生成（`data/thkaguyamod/biome_modifier/th_fairy.json` 权重 30）、刷怪蛋、`EntitySunFlowerFairy`/`EntityTHPhantom`/`EntityFamiliar` 子类同理 | **P0** | **极高（实锤）** | 在 `THKaguyaMod` 构造器（或 `FMLCommonSetupEvent`）加 `DanmakuPatternRegistry.setDanmakuPattern();`，位置必须在任何实体实例化之前（原作于 `THKaguyaCore.preInit` 的 `proxy.registerDanmakuPattern()` 调用）。另建议把 `EntityFamiliar:29` 的 `random.nextInt(pattern.size())` 改为 `Math.max(size,1)`（`EntityTHFairy:45` 已有此保护，但保护不了 `pattern.get(id)` 本身） | 原作 `THKaguyaCore.java:95 proxy.registerDanmakuPattern()` → `CommonProxy.java:681 DanmakuPatternRegistry.setDanmakuPattern()`；移植漏了这一步 |

**实锤证据（浸泡测试日志，非推测）** — `porting/neoforge-1.21.1/run-smoke/logs/latest.log`：
```
138: [Server thread/WARN] [net.minecraft.world.entity.EntityType/]: Exception loading entity:
139: java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
145:   at ...thkaguyamod.entity.living.EntityTHFairy.setPattern(EntityTHFairy.java:96)
146:   at ...thkaguyamod.entity.living.EntityTHFairy.<init>(EntityTHFairy.java:45)
```
`grep -c "EntityTHFairy.setPattern" latest.log` = **18**（roster 中每个妖精/花妖/幽灵 summon 均失败）。日志时间 2026-10-03 10:41，属当前发布候选构建。

**结构性证据（不依赖日志）**
```
$ grep -rn "DanmakuPatternRegistry.setDanmakuPattern" porting/neoforge-1.21.1/src/   → 无
$ grep -rn "registerDanmakuPattern" porting/neoforge-1.21.1/src/                       → 仅 DanmakuPatternRegistry.java 自身
$ javap -p -c build/libs/thkaguyamod-2.90-1.21.1.jar!/thkaguyamod/THKaguyaMod.class | grep -i DanmakuPattern  → 0 命中
$ javap -c DanmakuPatternRegistry.class 的 static{} 只 new 空集合，无 registerXxx 调用
```
`pattern.size()` 恒为 0 → `RandomSource.nextInt(0)` 还会额外抛 `IllegalArgumentException("Bound must be positive")`（`BitRandomSource.nextInt`），即 FTFairy:45 的 `Math.max(...,1)` 只是把异常从 `nextInt` 推迟到 `pattern.get(0)`。

---

## P1 — 疑似（有 file:line 与可达路径，但需裁决/复现）

| 文件:行 | 模式 | 可达路径 | 严重度 | 置信度 | 最小修复建议 | 对照原作结论 |
|---|---|---|---|---|---|---|
| `spellcard/EntitySpellCard.java:135` + `item/EntitySanaeWind*.` 路径 | 实体构造器内 `user/` 参数解引用（模式 3 变体：构造期 NPE/异常面） | `EntitySpellCard` 构造器在 `useSpellCard.getEndTime()`…正常；但同文件 `:236 specialProcessInTimeStop()` 直接 `useSpellCard.specialProcessInTimeStop()` 无 null 检查，而 `:122-141` 明确允许 `factory == null` 时 `useSpellCard` 保持 null（此时 `:146` 只 `discard()` 且仅在服务端）。客户端侧 factory 为 null 时 `tick()` 的 `:277 if(useSpellCard == null) return;` 已挡住，故暂列 P1 需确认 | P1 | 中 | `specialProcessInTimeStop()` 加 `if (useSpellCard != null)` | 原作同样无检查（`useSpellCard` 由反射失败时也可能为 null），**原作即如此但原作 `EntitySpellCard` 走不到该分支**（1.7.10 无时间停止实体遍历调用该方法的路径），移植新增了调用点，故登记 |
| `entity/item/EntitySakuyaWatch.java:238` / `entity/item/EntitySakuyaStopWatch.java:179` | NPE：`spellCard.user.equals(...)` 直接解引用 `public LivingEntity user` | `EntitySpellCard.user` 是 public 字段且构造器允许为 null（`spellCardDeclaration` 要求 target 非 null，但 `user` 来自 `user` 参数）；时间停止 tick 遍历 40 格内全部 `EntitySpellCard` 时执行 `spellCard.user.equals(this.userEntity)` | P1 | 中 | 改为 `this.userEntity.equals(spellCard.user)` 或加 null 检查 | 原作 `spellCard.user.equals(this.userEntity)` 同样写法 → **原作即如此**；但 1.7.10 中 `user` 由 `EntitySpellCard` 构造保证非 null，移植的 `THKaguyaLib.spellCardDeclaration` 仍保证非 null，故实际不可达，降级为观察 |
| `menu/DanmakuCraftingMenu.java:397-398`（`slotUseProcess(player, this.craftCopy, copyNum - 1)`）与 `:305-313` 的 `copyNum` 计算 | 容器越界/负数量（模式 6） | `copyNum` 被夹在 `[1, oriNum]`，故 `copyNum-1 >= 0`；但 `slotUseProcess` 内部 `container.removeItem(0, useNum)`，`useNum` 来自 `craftCopy.getItem(0).getCount()` 累加，已达 64 上限（槽 `getMaxStackSize()=64`），不会越界。**已排除**，保留在表内仅为记录裁决 | P2 | 高（排除） | 无 | 原作同构 |
| `menu/MiracleMalletMenu.java` / `SlotMiracleMalletResult.java:69-96` | slot 内 `container.setItem(0, remainder)` 在 `removeItem` 之后（模式 6） | 素材槽只有 1 格，`container.getItem(0)` 在 `removeItem(0,1)` 后可能为空 → 走 `setItem(0, remainder)` 分支；语义上是「返还物放回槽位」，`SimpleContainer` 无越界风险 | P2 | 高 | 无 | 原作 `slotUseProcess` 同构 |
| `entity/living/EntityDanmakuMob.java:1751-1776`（`readAdditionalSaveData`）与 `:1537-1545`（`dropShotItem`）| NBT→组件迁移后读写不对称（模式 4/2） | 写入端 `addAdditionalSaveData:1797 (short) danmakuSpan`、`:1798 putByte("ShotForm")`…；读回端 `:1773 if(shotColor < 0 \|\| shotColor >= 10) shotColor = 8;` 已夹紧。`dropShotItem:1540 form % 8`、`:1541 speed % 64`、`:1542 gravity % 16`、`:1544 color % 16`：当入参为**负**（如 `form=-1`）时 Java 取模结果仍为负（`-1 % 8 == -1`），写入 `DanmakuParams(form=-1,…)` → 之后 `ShotData.shot(form=-1,…)` 中 `form % 32 == -1` → `THShotLib.SIZE[-1]` / `DAMAGE[-1]` **ArrayIndexOutOfBounds**。可达路径：掉落表调用点传常量 `form=2`（`EntityCirno:278`、`EntityRumia:177`、`EntitySanae:272`、`EntityToziko:162`、`EntityWriggle:227`…）**全部为正**，故当前不可达 | P1（潜伏） | 中（路径已核对为不可达） | 在 `dropShotItem` 内改用 `Math.floorMod(form, 8)` 等，或在 `DanmakuParams` 构造器统一夹紧 | 原作 `nbt.setByte("DanmakuForm",(byte)(form%8))` 同构造；原作调用点同样全为正 → **原作即如此**，登记为「同类隐患模板」供 I7 家族排查参考 |
| `danmaku/DanmakuParams.java:27-56` + `init/ModDataComponents.java:21-25` | Codec/StreamCodec 读写对（模式 2） | `CODEC` 与 `STREAM_CODEC` 均用无界 `Codec.INT` / `writeVarInt`，读写**对称**；无 byte 截断、无精度丢失。**已排除**对称性缺陷。但组件值无任何范围约束，一旦被负值/超范围值写入（见上一行），下游数组下标会炸 | P1（与上一行合并） | 高 | 同上一行：在 `DanmakuParams` compact 构造器加 `number/form/speed/gravity/special/color` 夹紧 | 1.7.10 用 short/byte 存储天然截断，移植改用 int 后**失去了隐式截断保护**，属移植引入的语义变化 |
| `item/ItemTHShot.java:147`、`init/ModDispenserBehaviors.java:109`、`item/ItemTHShot.java:302` | 数组下标 `shotTypeTrans[...]` / `SIZE[...]` 由 `itemTypeIndex`/`shotID` 决定 | `itemTypeIndex` 由 `ModItems` 注册时写死 0..18（`ModItems.java:141-159`），`shotTypeTrans` 长度 19 → 恒安全。**已排除** | P2 | 高（排除） | 无 | 原作 meta 0..18 同构 |
| `spellcard/THSC_MeteorOnEarth.java:44,60` / `THSC_StardustReverie.java:44` | 数组下标 `way[level]`，`level` 来自 `THSpellCard.level`（`init(...,spLevel)`） | `level` 由 `THKaguyaLib.spellCardDeclaration(..., level, ...)` 提供：玩家路径恒 `DanmakuConstants.NORMAL(2)`；Boss 路径为 `ModConfig.danmakuLevel`（config 范围 `defineInRange(...,0,4)`，**允许 0**）。`way[]` 长度 5 → `way[0]` 合法；`way[4]` 合法。**已排除越界**（0..4 均在界内） | P2 | 高（排除） | 无（若要更严可 `Mth.clamp`） | 原作 `way[level]` 同构，且原作 `THKaguyaConfig.danmakuLevel` 亦允许 0 |
| `spellcard/THSpellCard.java:444-445`（`SIZE[form]` / `DAMAGE[form]`） | 数组下标由 form 决定（模式 1） | 仅被 `shot(form,color,delay,end,special)` 重载调用，各调用点 form 均为 `FORM_*` 常量（0..31），`SIZE/DAMAGE` 长度 32 | P2 | 高（排除） | 无 | 原作 `SIZE[form]` 同构；原作有自递归 bug，移植已修（见 `:430-435` 注释） |
| `danmaku/THShotLib.java:366 / 947 / 580 / 1064` 等 | 除零：`wideAngle / (way - 1)`、`360F / way` | `way` 由调用点提供。已核对全部调用点（清单见「覆盖声明」）：`way >= 2` 至少有 1 处例外——`THSC_StardustReverie:44 way[2]=7`、`THSC_MeteorOnEarth` `way[0]=0` 但该值只在 `level==0` 时使用，而 `level==0` 时 `EntityDanmakuMob.getTarget()` 恒返回 null（`:463-466`）→ 不会进入 `spellcard_main`。`createWideShot(way=1)` 在 `ItemYuyukoFan` 等处不存在。**未发现可达除零** | P2 | 中（排除，但依赖 `way[0]==0` 与 `level==0` 不共存的推理） | 可选：`Math.max(way,2)` | 原作同为 `wideAngle / (way - 1)`、`360F / way` → **原作即如此** |
| `entity/effect/EntitySpellCardCircle.java:153` | 除零：`(endTime2 - animationCount) / endTime2` | `endTime2 = (float)getEndTime()`，来自构造器 `setEndTime(end)`：玩家符卡传 `useSpellCard.getEndTime()`（各符卡 `setEndTime(50..200)`，已 grep 全部 35 张均非 0）；Boss 符卡传 `-100`（`EntityDanmakuMob.java:846`）→ `endTime2 = -100` 非零。**已排除除零**（`getCircleType() < 16` 且 `end >= 0` 时才会计算） | P2 | 高（排除） | 无 | 原作同构（`EntitySpellCardCircle` 原代码同表达式） |
| `spellcard/THSC_Fafurotskies_no_Kiseki.java:69` (`int pattern = (int)shot.shotLimitSpeed`) 与 `:104/116/124/134` | 类型窄化 / 语义错误（模式 2） | 把「速度」当「模式编号」用：`shotLimitSpeed` 由 `ShotData`…实际由 `THShotLib.createLaserA(..., limitSpeed=0.8D ...)` 传入 → `(int)0.8 == 0`，恒落 `pattern <= 70` 分支。原作同写法 | P1 | 中（原作即如此，但移植后 `shotLimitSpeed` 由 config 参与赋值，值域可能变化） | 与主控确认是否为已知设计 | **原作即如此**，不报为缺陷，仅登记 |
| `spellcard/THSC_GagoujiCyclone.java:127` | `shot.shotLimitSpeed = 0.2D + ModConfig.danmakuLevel * 0.1D` 与上一行是同一语义混用 | 同上 | P2 | 中 | 无 | 原作同构 |

---

## P2 — 观察（一句话带过）

| 位置 | 一句话 |
|---|---|
| `entity/EntityDivineSpirit.java:197` `COLOR_BLOCKS[Math.min(Math.max(color,0), len-1)]` | 已做双向夹紧，**良好实践**，登记为对照样板。 |
| `entity/living/EntityDanmakuCreeper.java:101` `ways[Math.max(0,Math.min(ways.length-1, level))]` | 已夹紧，同上。 |
| `item/ItemTHLaser.java:212` `ItemTHShot.sellectbleColor[meta % 16]` | `meta` 来自 `switch(DyeColor)` 返回 0..15，恒非负且 <16，安全。 |
| `entity/shot/EntityTHShot.java:1838` `getShotId() % 8` | `DATA_SHOT_ID` 由 `form*8+color` 写入、`form` 来自 `ShotData.shot` 的 `% 32`；`ShotIdEncodingBoundaryTest` 已固定「结果恒 0..7」。I7 的奇偶 bug 在于**渲染器**把 `color%2` 当颜色下标，本次 common 侧未发现第二处同类。 |
| `danmaku/ShotData.java:35-36` `form % 32` / `color % 16` | 负值仍为负（Java 语义）；当前全部调用点为正，登记为同类隐患模板（与 `dropShotItem` 同一模式）。 |
| `registry/SpellCardRegistry.java:222-240 sortSpellCard()` | `String array[] = new String[256]`，`spellcardID` 仅 35 项；`array[0]=list.get(0)` 要求 list 非空——`getSpellCardList()` 在任一 factory 为 null 时返回 null 会致 NPE，但该方法在移植树内**无调用点**（仅 `client/` 外无引用），不可达。 |
| `registry/SpecialShotRegistry.java` | 无并发保护（`HashMap`），但注册全部发生在 `ModSpellCards.registerSpellCards()` 单线程 mod 构造阶段、读取在 tick 中，无「注册与读取并发」路径；**未发现 CME**（模式 8）。 |
| `entity/living/EntityDanmakuMob.java:1040-1049`、`EntitySakuyaWatch.java:136-250`、`THSC_PerfectFreeze.java:63-77`、`THSC_SatuzinDoll.java:83-111` | 遍历 `level().getEntities(...)` 返回的**快照 List**（`EntityGetter.getEntities` 每次新建 ArrayList，非实时视图），遍历中 `discard()` 不会 CME。**未发现 ConcurrentModificationException**（模式 8）。 |
| 反射残留（模式 7） | `grep -rn "Class.forName\|getDeclaredField\|getDeclaredMethod\|setAccessible\|getDeclaredConstructor\|ReflectionHelper\|MethodHandles"` 在**全部非 client 源码**中仅命中 `test/ExtendReviveTest.java:54`（GameTest 内的类可加载性断言，非生产路径）。`SpecialShotRegistry`/`SpellCardRegistry` 已从 `Class.newInstance()` 迁到 `Supplier` 工厂。**P1 级反射残留：零发现。** |

---

## 覆盖声明（grep 证据，替代逐行通读）

### 已逐文件读完（35 个）
`danmaku/DanmakuParams.java`、`danmaku/ShotData.java`、`danmaku/LaserData.java`、`danmaku/DanmakuConstants.java`、`danmaku/THShotLib.java`（头部+全部 `ways[][]` 表+全部 `create*` 生成器+尾部）、`init/ModDataComponents.java`、`init/ModMenus.java`、`init/ModNetworking.java`、`init/ModSpawns.java`、`init/ModSpellCards.java`、`config/ModConfig.java`、`network/SelectSanaeTradePayload.java`、`network/UpdateSanaeOffersPayload.java`、`menu/MerchantSanaeMenu.java`、`menu/DanmakuCraftingMenu.java`、`menu/DanmakuLaserCraftingMenu.java`、`menu/MiracleMalletMenu.java`、`menu/SlotDanmakuCraftingA.java`、`menu/SlotDanmakuResult.java`、`menu/SlotMiracleMalletResult.java`、`registry/SpecialShotRegistry.java`、`registry/DanmakuPatternRegistry.java`、`registry/SpellCardRegistry.java`、`spellcard/EntitySpellCard.java`、`spellcard/THSpellCard.java`（含全部 shot 工厂与 endTime 访问器）、`entity/shot/EntityTHShot.java`（tick/animationCount/构造器/命中/特殊弹/bound/homing/扩散+全部访问器与 Codec 相关）、`entity/item/EntitySakuyaWatch.java`、`entity/item/EntityCursedDecoyDoll.java`、`entity/EntityDivineSpirit.java`、`entity/EntityShotMaterial.java`、`entity/EntityTHItem.java`、`entity/living/EntityTHFairy.java`、`entity/living/EntityFamiliar.java`、`entity/living/EntityTHPhantom.java`、`entity/living/EntitySunFlowerFairy.java`、`item/ItemTHShot.java`、`item/ItemTHLaser.java`、`item/ItemTHSpellCard.java`、`item/ItemGapFoldingUmbrella.java`、`item/ItemBloodthirstyOnmyoudama.java`、`item/ItemCursedDecoyDoll.java`、`item/ItemDragonNeckJewel.java`、`item/ItemMikoSword.java`、`item/ItemKaigo.java`、`item/ItemHouraiJeweledBranch.java`、`item/ItemHisyaku.java`、`block/DanmakuCraftingTableBlock.java`、`blockentity/DivineSpiritBlockEntity.java`、`event/PlayerDeathEventHandler.java`。

### 模式扫描覆盖（未逐行读，但已用下列 grep 全量筛过）
| 模式 | grep 表达式 | 结果 |
|---|---|---|
| 1 数组/List 下标来自组件/实体数据/配置/算术（含负数 `%`） | `grep -rnE "\[[a-zA-Z_][a-zA-Z0-9_]*( *[-+*/%] *[…]?)?\]"` 于 entity/item/danmaku/spellcard/menu/init/registry/block/ | 全部命中已逐条核对，见上表；**除 P0 外无新增** |
| 2 Codec/StreamCodec 读写不对称 | 人工核对 `DanmakuParams`、`ModDataComponents`(CHARGE/GAP_COLOR/ORIGINAL_DAMAGE 均 `Codec.INT`+`VAR_INT`)、`SelectSanaeTradePayload`(VAR_INT)、`UpdateSanaeOffersPayload`(`MerchantOffers.STREAM_CODEC`) | 读写对称；**无 truncation 缺陷** |
| 3 组件 absent/默认缺失 NPE | `grep -rn "\.get(ModDataComponents\|stack.get("` \| grep -v getOrDefault | 仅 5 处裸 `.get()`：`DanmakuCraftingMenu:168,329,354,370,386`、`DanmakuLaserCraftingMenu:133,257,282`，**全部由前置 `mat.has(...)` 守卫**（已逐行确认），无 NPE |
| 4 NBT↔同步默认值一致性 | 对 23 个类的 `defineSynchedData` 与 `addAdditionalSaveData/readAdditionalSaveData` 对照 | 未发现不一致；`EntityHisou:213-226` 与 `EntityMiniHakkero:189-202` 的 short/byte 往返与原作同构 |
| 5 除零 | `grep -rnE "/ *[a-zA-Z_]"` 全量 | 全部逐条核对，见上表；**零可达除零** |
| 6 Menu/slot/Payload 越界与 CCE | 逐读 4 个 Menu + 4 个 Slot + 2 个 Payload；`quickMoveStack` 三段索引与 `slots` 总数核对（Merchant 39、Danmaku 46、Laser 46、Mallet 38） | 与 `addSlot` 数量一致；`MerchantSanaeMenu` 的 `slots.get(index)` 有 `slot != null` 检查；**无 CCE/越界** |
| 7 反射残留 | 见上「反射残留」行 | **零发现** |
| 8 CME | `grep -rn "ArrayList\|List<Entity>\|HashMap\|HashSet"` + 逐条看遍历体内是否有 `discard/remove` | 所有遍历源为快照；**零发现** |

### 未扫描/明确排除
- `client/**`（按任务约定由另一批负责）。
- `test/**`：仅作为「已存在的断言覆盖」引用，未做缺陷审计。
- `THEven`/`THShotLib` 的**数学函数段**（`getVectorFromRotation`、`getOuterProduct`、`angle_*`、`gravity_*`）做逐行 diff 对比后只发现 `Vec3` 可变→不可变与 `Random`→`ThreadLocalRandom` 两类机械替换，未逐行复核浮点精度。
- `build/**`、`run-smoke/**`（构建产物与运行数据，非源码；仅把 `logs/latest.log` 当作 P0 的运行时证据引用）。

---

## 对照原作（1.7.10 `src/main/java/thKaguyaMod/`）的差异判定记录
1. **P0 是纯移植遗漏**：原作 `THKaguyaCore.java:95` 显式调用 `proxy.registerDanmakuPattern()`；移植树无对应调用。**不是「原作即如此」。**
2. `DanmakuParams` 由 short/byte 改 int：原作靠 NBT 类型天然截断，移植失去该保护 → 属**移植引入的语义变化**，建议补夹紧（P1）。
3. 其余 P1/P2 项经 diff 核对**均为「原作即如此」**（已逐条标注），按纪律不作为缺陷上报，仅登记为同类隐患模板。
4. 反射：原作 `SpecialShotRegistry` 每 tick 每弹 `Class.newInstance()`，移植已彻底移除 → **改进**。

---

## 建议处置顺序（给主控）
1. **立即**（阻塞发布）：补 `DanmakuPatternRegistry.setDanmakuPattern()` 调用点，位置在 mod 构造期、早于任何实体实例化。可加 GameTest 断言 `DanmakuPatternRegistry.pattern.size() > 0`（现有 `RegistryCompletenessTest` 是可挂载处）。
2. **发布前可选**：`DanmakuParams` 构造器夹紧 6 个 int 字段（1 行 compact 构造器即可覆盖全部写入路径，性价比最高）。
3. **发布后**：`dropShotItem` 换 `Math.floorMod`；`EntitySpellCard.specialProcessInTimeStop()` 加 null 检查。