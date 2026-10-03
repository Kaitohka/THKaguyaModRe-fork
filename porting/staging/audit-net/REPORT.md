# audit-net — 第三轮严重性普查：network / menu / client Screen 数据流

**范围**：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/`
- `network/`（2 个双向 Payload）
- `menu/`（4 Menu + 4 Slot）
- `client/gui/screen/`（4 Screen）

**只报**：P0（崩溃 / 数据损坏 / 物品复制 dupe / 核心功能失效）与 P1（疑似，需复现）。
**纪律**：只读，不改任何既有文件；禁 gradlew / git；不杀进程。
**对照基线**：1.7.10 原作 `src/main/java/thKaguyaMod/gui/`（Container*/Slot*/Gui*）。
**方法**：逐行读移植文件 + 对照 1.7.10 同名类 + 反查 1.21.1 vanilla（`/tmp/allnf` 解包的 `sourcesAndCompiledWithNeoForge` 全量源码）的 Slot / AbstractContainerMenu#doClick / MerchantContainer / MerchantResultSlot / moveItemStackTo / clearContainer / MainThreadPayloadHandler / PayloadRegistrar 语义。

## 覆盖声明

已逐行读完（13/13 文件）：

| 文件 | 结论 |
|---|---|
| `network/SelectSanaeTradePayload.java` | 无 P0。侧别正确（`playToServer`）；主线程（`PayloadRegistrar` 默认 `HandlerThread.MAIN` → `MainThreadPayloadHandler` 包一层 `enqueueWork`，见 `PayloadRegistrar.java:166`）；`menu.stillValid(player)` + `instanceof MerchantSanaeMenu` 双校验；只写 selectionHint 不改物品。 |
| `network/UpdateSanaeOffersPayload.java` | 无 P0。`playToClient` + `context.enqueueWork`；containerId 比对后才写入；`MerchantOffers.STREAM_CODEC` 走 RegistryFriendlyByteBuf（含 RegistryAccess）。 |
| `menu/MerchantSanaeMenu.java` | **P1-a**（见下）。其余与 1.7.10 `ContainerMerchantSanae` 逐段对齐：槽坐标 (36,53)/(62,53)/(120,53)、背包 (8,84)/(8,142)、quickMoveStack 槽段 0..2/3..29/30..38、removed 无条件 drop 两个支付槽（原作笔误双调 super 未复刻，已在注释声明）。 |
| `menu/DanmakuCraftingMenu.java` | **P1-c**（见下）。updateResult / removeResult / slotUseProcess 与 1.7.10 `ContainerDanmakuCrafting` + `SlotDanmakuResult` 的 way/speed/gravity/special/copy 累加与上限逐条对齐（含 `usePoint--` 分支、`BOUND01` 逻辑、`form==4 && way>32` 无效判定）。`craftSpecial` 字段存在但无槽位（原作亦被注释）→ 第 380-393 行读它恒为空，等价于原作，非缺陷。 |
| `menu/DanmakuLaserCraftingMenu.java` | 无 P0（含一个「忠实照抄原作」的槽段错位，见 P1-d）。 |
| `menu/MiracleMalletMenu.java` | **P1-b**（见下）。 |
| `menu/SlotDanmakuCraftingA.java` | 无 P0。type 0..5 与 1.7.10 `SlotDanmakuCraftingA.isItemValid` 逐条对应；`getMaxStackSize()=64` 同原作。 |
| `menu/SlotDanmakuResult.java` | 无 P0。`remove()` 累计 amountCrafted、`onTake` 调 `menu.removeResult`，与 1.7.10 `onPickupFromSlot` 结构一致；`isFake()` 只影响渲染 API 选择（`AbstractContainerScreen:301`），不影响逻辑。 |
| `menu/SlotLaserResult.java` | 无 P0。同构于 SlotDanmakuResult。 |
| `menu/SlotMiracleMalletResult.java` | 无 P0。`slotUseProcess` 忠实移植（含 `hasCraftingRemainingItem` → `getCraftingRemainingItem` 的现代等价）。 |
| `client/gui/screen/MerchantSanaeScreen.java` | 无 P0。翻页越界已有防御（`renderBg:147`/`render:178` 双重 `selectionIndex` 边界检查，比 1.7.10 更稳）；发包链路 `setSelectionHint` + `PacketDistributor.sendToServer` 与 1.7.10 `C17PacketCustomPayload("MC|TrSel")` 等价。 |
| `client/gui/screen/DanmakuCraftingScreen.java` | 无 P0。纯绘制，坐标与 1.7.10 一致。 |
| `client/gui/screen/DanmakuLaserCraftingScreen.java` | 无 P0。纯绘制。 |
| `client/gui/screen/MiracleMalletScreen.java` | 无 P0。三个 SWING 按钮无动作 = 1.7.10 原样（原作无 actionPerformed 覆写）；`renderLabels` 里每帧禁用第一个按钮亦照抄。 |

未覆盖（超出本批范围，仅记录）：`block/DanmakuCraftingTableBlock.java`、`item/ItemUchide.java`、`entity/living/EntitySanae.java` 的 Merchant 实现 —— 只在核对调用链时读过相关片段，未做逐行普查。

## P0 清单

**P0 零发现。** 未发现崩溃、数据损坏、物品复制、核心功能失效级别的缺陷。理由摘要：
- 所有 menu 的 `quickMoveStack` 槽段虽然常量沿用了 1.7.10（与 1.21.1 实际槽数不符），但经逐条推演：越界目标段不会被真实索引命中（唯一可能命中目标段的是结果槽 index=0 与功能槽 index 1..9，见 P1-c/P1-d），最坏后果是「shift 点击无反应」，不会 dupe、不会崩溃（`slots.get(i)` 的 i 恒在 `slots.size()` 内）。
- 两个 Payload 的侧别、线程、校验均正确；服务端 handler 在主线程执行且带 `stillValid` 校验。
- 未发现 `onTake` 与组件写回路径的复制/丢失：`SlotDanmakuResult.onTake → menu.removeResult` 的扣减顺序（先矩阵、后各功能槽、最后素材/复制槽）与 1.7.10 完全一致。

## P1 清单（疑似，需复现）

| # | 位置 | 严重性 | 结论 |
|---|---|---|---|
| P1-a | `menu/MerchantSanaeMenu.java:163-165`（`setOffers`）＋`init/ModNetworking.java:29`（唯一发送点） | P1 疑似（已降级，见「补查」） | offers 只在 `EntitySanae.mobInteract` 开界面时下发一次（`sendSanaeOffers` 全树仅 1 个调用点）；服务端 `EntitySanae.overrideOffers` 为空实现，故**没有「交易后重新下发」的通道**（vanilla 村民靠 `Villager.resendOffersToTradingPlayer`，此处不存在）。补查结论：客户端 `MerchantResultSlot.onTake` 里的 merchant 是 `ClientSideMerchant`，其 `notifyTrade` 会 `offer.increaseUses()`，所以单机/单玩家场景下客户端 uses 与服务端同步自增，**不出现可见偏差**。仅当「服务端拒绝但客户端接受」（材料不足、并发）或「同一实体两个玩家同时开界面」时才会 diverges，属显示层陈旧，不 dupe、不丢物。 |
| P1-b | `menu/MiracleMalletMenu.java:159-165`（`clicked`） | P1 疑似 | `clicked()` 每次点击都无条件走 `super.clicked` 再在 `experienceLevel>=30` 时 `updateResult()`。1.7.10 `ContainerMiracleMallet.slotClick` 同样如此，但 1.7.10 的 `onCraftMatrixChanged` 会把结果写进 `craftResult`；移植版 `updateResult()` 用 `craftResult.setItem(0, produced)`（`:147`）传的是**未 copy 的同一个 ItemStack 引用**（对比 `DanmakuCraftingMenu:269` 用的是 `produced.copy()`）。`ResultContainer.setItem` 直接存引用 → 结果槽与 `resultItem` 字段共享实例，取出时 `Slot.remove → ResultContainer.removeItem → ContainerHelper.takeItem` 会把容器槽置空，`resultItem` 仍指向同一对象，后续若有人再 `setItem` 会复用该对象。当前无第二处写入，暂不构成实害；但一旦补上「取出后重建结果」逻辑即为 dupe 温床。建议改为 `produced.copy()`（一行）。 |
| P1-c | `menu/DanmakuCraftingMenu.java:500-508` 与 `menu/DanmakuLaserCraftingMenu.java:372-380` | P1 疑似 | 槽段常量直接沿用 1.7.10 的 10/37/46（弹版实际功能槽 0..9、背包 10..36、快捷栏 37..45；激光版实际功能槽 0..14、背包 15..41、快捷栏 42..50）。激光版尤其错位：`index 10..14`（矩阵后 5 格）落入 `>=10 && <37` 分支 → 只会尝试移入 37..45（快捷栏），跳过 15..36；`index 42..45`（快捷栏末 4 格）落入 `>=37 && <46` 分支 → 尝试移入 10..36（含矩阵/功能槽），但矩阵槽 `mayPlace(type 0)` 只收 POWER_UP_ITEM_SMALL，功能槽同理，绝大多数物品会被拒 → 静默无反应。最坏后果为「shift 点击无效」，非 dupe/崩溃。1.7.10 同样错位（原作注释已指出），属忠实移植；建议按现代槽数修正。 |
| P1-d | `client/gui/screen/MerchantSanaeScreen.java:183-201` | P1 疑似（有意保留） | 交易预览三格为硬编码（costA=`aja_red_stone`×1、result=`illness_recovery_charm`×5），只有 costB 来自 `offer.getCostB()`。1.7.10 `GuiMerchantSanae.drawScreen` 完全同款（`getItemToBuy/getItemToSell` 被注释）。属「显示与实际报价不符」，玩家可能照预览放错材料（服务端按真实 offers 结算，不会给错物品）。已在源码注释声明为刻意保留。 |

## 已排除（查证后判定为非缺陷）

1. **`SelectSanaeTradePayload` 网络线程直接碰世界数据**：排除。`ModNetworking.java:22` 用 `registrar.playToServer(...)`，`PayloadRegistrar` 默认 `thread = HandlerThread.MAIN`，`register()` 里 `if (this.thread == HandlerThread.MAIN) handler = new MainThreadPayloadHandler<>(handler)`，后者即 `context.enqueueWork(...)`。handler 内直接读 `player.containerMenu` 是安全的。
2. **serverbound 未签名校验 / 越界索引**：排除。索引只传给 `MerchantContainer.setSelectionHint`（写 int 字段 + `updateSellItem`），不参与数组下标；`getRecipeFor` 对 `index>0 && index<size` 之外走全表扫描，负数/越界均安全。
3. **商店空 offers 越界**：排除。`MerchantContainer.updateSellItem` 内 `if (!merchantoffers.isEmpty())` 保护；Screen 侧 `renderBg`/`render` 都有 `offers.isEmpty()` + 索引边界双检查。
4. **早苗/万宝槌 Menu 的 dataSlots 同步截断（int→short）**：排除。`grep addDataSlot|DataSlot menu/` = 0 命中，两个菜单都没有 dataSlots，不存在截断。
5. **弹幕工作台参数在 shift 取出/关界面时丢失或复制**：排除。`DanmakuCraftingMenu.updateResult` 每次都用 `new DanmakuParams(...)` 重新构造组件（`:264-265`），`removeResult` 的扣减顺序与 1.7.10 一致；`removed()` 对 8 个容器逐个 `clearContainer`（现代 API：存活时 `placeItemBackInInventory`，对应原作 `dropPlayerItemWithRandomChoice`，行为差异已在注释声明）。
6. **`SlotDanmakuCraftingA` 允许把成品弹放回素材槽导致自复制**：排除。`type 2` 的 `isShotItem` 就是 19 种弹物品，1.7.10 亦如此；复制数量受 `craftCopy` 与素材堆叠数双重钳制，且每次取出都真实扣减 `craftMaterial`。
7. **`MiracleMalletMenu.canTakeItemForPickAll` 允许结果槽参与双击拾取**：排除。`canTakeItemForPickAll` 的调用点是 `ClickType.PICKUP_ALL` 分支且要求 `slot8.mayPickup(player)`；结果槽 `SlotMiracleMalletResult.mayPlace` 恒 false，且该分支只搬运携带物（`safeTake`）不触发 `onTake` 的素材消耗，与 1.7.10 `func_94530_a` 语义等价。
8. **`MerchantSanaeMenu.removed` 与客户端 `trader.isClientSide()` 判断导致支付物丢失**：排除。客户端提前 return，服务端才 drop；`trader` 在服务端是真实 `EntitySanae`（`isClientSide()` 返回 `level().isClientSide`），在客户端是 `ClientSideMerchant`（返回 `source.level().isClientSide`），两者判断都正确。
9. **`MerchantSanaeMenu.stillValid` 恒 true 导致他人共用界面**：排除（与 1.7.10 一致，原作 `canInteractWith` 恒 true，`getCustomer()==player` 被注释）。

## 不确定项 / 待补

**本批唯一补查（已完成）**：offers 下发/重下发链路 —— 全树 `grep -rn "sendSanaeOffers"` = 2 命中（定义 `init/ModNetworking.java:29` + 唯一调用 `entity/living/EntitySanae.java:346`），无任何「交易后重发」；结合 `ClientSideMerchant.notifyTrade` 会 `increaseUses()`，得出 P1-a 的降级结论（单玩家不 diverges）。

其余待办：
- P1-b 需确认 `ResultContainer.setItem` 是否真的会与 `resultItem` 共享引用（`ResultContainer` 是 `NonNullList.withSize(1, EMPTY)`，`setItem(0, stack)` 直接存引用，无 copy），并评估后续是否会新增第二次写入。
- P1-c 建议按现代槽数重算常量（弹版 10/37/46 恰好与 1.21.1 的 46 槽总数巧合一致，激光版 46 应为 51），需主控裁决「忠实移植 vs 修正」。
- P1-d 属有意保留，仅需主控确认是否修正预览为 `offer.getCostA()/getResult()`。

## 置信度

- 覆盖：13/13 目标文件逐行读完（100%）。
- P0 零发现：高置信（每条候选都反查过 vanilla 调用点与 1.7.10 原作）。
- P1-a 降级判定：中高（依赖 `ClientSideMerchant.notifyTrade` 语义，已读源码确认）。
- P1-b/P1-c：中（静态推演，未实机复现）。
