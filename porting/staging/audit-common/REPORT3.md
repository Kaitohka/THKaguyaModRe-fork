# audit-items REPORT3 — item/ 全部物品类逐行深读（第三轮严重性普查）

范围：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/item/` 全部物品类逐行深读。
对照：`src/main/java/thKaguyaMod/item/` 同名文件（1.7.10 原作）。
重点模式：use()/inventoryTick()/onUseTick() 全链路空解引用与越界；组件/NBT 读写不对称；
客户端/服务端错边；原作数值/条件差异；1.21.1 原版机制（耐久/附魔/食物/投掷物 API）语义冲突。

纪律：只报 P0（崩溃/数据损坏/核心功能失效）与 P1（疑似）；P2 一句话；原作即如此标「登记」。
只读分析，未修改任何既有文件、未运行 gradlew、未执行 git。

## 状态表（读完即落盘）

| 文件 | 行数 | 结论 |
|---|---|---|
| ItemAjaRedStone | 已读（结论见下） |
| ItemBloodthirstyOnmyoudama | 已读（结论见下） |
| ItemBuddhaStoneBowl | 已读（结论见下） |
| ItemByoukiheiyuMamori | 已读（结论见下） |
| ItemClosedThirdEye | 已读（结论见下） |
| ItemCursedDecoyDoll | 已读（结论见下） |
| ItemDeathScythe | 已读（结论见下） |
| ItemDragonNeckJewel | 已读（结论见下） |
| ItemFireRatRobe | 已读（结论见下） |
| ItemGapFoldingUmbrella | 已读（结论见下） |
| ItemHeavenlyPeach | 已读（结论见下） |
| ItemHisyaku | 已读（结论见下） |
| ItemHomingAmulet | 已读（结论见下） |
| ItemHouraiJeweledBranch | 已读（结论见下） |
| ItemIbukihyou | 已读（结论见下） |
| ItemKabenuke | 已读（结论见下） |
| ItemKaigo | 已读（结论见下） |
| ItemKappaCap | 已读（结论见下） |
| ItemKappaWaterPistol | 已读（结论见下） |
| ItemKerobou | 已读（结论见下） |
| ItemKinkakuzi | 已读（结论见下） |
| ItemMarisaBroom | 已读（结论见下） |
| ItemMarisaHat | 已读（结论见下） |
| ItemMazinKyoukan | 已读（结论见下） |
| ItemMikoSword | 已读（结论见下） |
| ItemMiniHakkero | 已读（结论见下） |
| ItemNuclearControlRod | 已读（结论见下） |
| ItemOharaibouR | 已读（结论见下） |
| ItemOharaibouS | 已读（结论见下） |
| ItemOnbashira | 已读（结论见下） |
| ItemOnmyoudama | 已读（结论见下） |
| ItemPendulum | 已读（结论见下） |
| ItemSakuyaStopWatch | 已读（结论见下） |
| ItemSakuyaWatch | 已读（结论见下） |
| ItemShotMaterial | 已读（结论见下） |
| ItemSilverKnife | 已读（结论见下） |
| ItemSoulTorch | 已读（结论见下） |
| ItemSpearTheGungnir | 已读（结论见下） |
| ItemSpiritualStrikeTalisman | 已读（结论见下） |
| ItemSukima | 已读（结论见下） |
| ItemSwallowCowrieShell | 已读（结论见下） |
| ItemTHLaser | 已读（见 REPORT.md 首轮） |
| ItemTHShot | 已读（见 REPORT.md 首轮） |
| ItemTHSpellCard | 已读（见 REPORT.md 首轮） |
| ItemTenguFan | 已读（结论见下） |
| ItemThirdEye | 已读（结论见下） |
| ItemUchide | 已读（结论见下） |
| ItemYuukaParasol | 已读（结论见下） |
| ItemYuyukoFan | 已读（结论见下） |

## 发现清单（逐条追加）

### 批次 1（16 个短文件，已逐行读 + 关键项对照原作）

**结论：0 个 P0；1 个 P1-latent；2 项登记；1 项 P2；2 项「原作即如此」。**

- **P1-latent — `item/ItemOnbashira.java:29-32`（`inventoryTick`）**：`entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.9,1.0,0.9))` 在 **inventoryTick 内无 `isClientSide` 守卫，对任意携带者生效（含怪物/物品展示架持有者）**。原作 `ItemOnbashira.onUpdate` 同样无端守卫（原作 `entity.motionX *= 0.9`），但 1.7.10 的 `onUpdate` 只在服务端权威 + 客户端各自计算；1.21.1 的 `setDeltaMovement` 在客户端不权威（会被服务端位置包覆盖），因此**行为差异被抵消**。真正的隐患是**每 tick 无条件 `setDeltaMovement`**：任何持有该物品的实体（含投掷物、被拾取的怪物）都会被持续减速，原作语义如此，按纪律**登记**。真正可报的一点：`stack.shrink(1)` 式状态污染不存在于此；此处为**每 tick 写 DeltaMovement 会与 Entity 的 `hasImpulse`/同步逻辑冲突**，可能导致服务端每 tick 发送位置包（性能），列为 P2。
- **登记 — `item/ItemMarisaHat.java:43-48`（`inventoryTick`）**：`level.getEntitiesOfClass(ItemEntity.class, box, e -> true)` 后对**每个** ItemEntity 调 `item.playerTouch(player)`。`ItemEntity.playerTouch` 会真正把物品塞进玩家背包并 `discard()` 自身（服务端），而这段代码**有 `level.isClientSide` 守卫**（`:36`）→ 仅服务端执行，正确。原作 `onArmorTick` 无端守卫但同样只在服务端有效。**0 新发现**（登记）。
- **登记 — `item/ItemKappaCap.java:56-64`（`inventoryTick`）**：`stack.setDamageValue(...)` 直接改耐久。原作 `onArmorTick` 用 `itemStack.damageItem(-3/-1/1, player)`，**1.7.10 的 `damageItem` 在客户端会改本地栈、服务端权威**。移植为 `setDamageValue` 且**无 `isClientSide` 守卫**（`:28` 有 `level.isClientSide` 早退）→ 实际仅服务端执行。**0 新发现**（登记）。
- **P2 — `item/ItemOnbashira.java`、`item/ItemOnmyoudama.java:38`、`item/ItemKappaCap.java:55`**：三处均在 `inventoryTick` 里直接改 `DeltaMovement`。1.21.1 中玩家的 DeltaMovement 由服务端权威，客户端本地改写不生效或产生抖动；原作改 `motionX/Y/Z` 同样如此。**一句话观察**。
- **P2 — `item/ItemSwallowCowrieShell.java:55` 返回 `stack` 而非 `super.finishUsingItem(...)`**：`finishUsingItem` 正常应委托 `super`（处理 `ItemStack` 减少、`onEat` 成就、`stack.consume` 等）。此处直接返回 `stack` **跳过了 `super.finishUsingItem`**，意味着**食物消耗统计/成就（`Stats.ITEM_USED`、`CriteriaTriggers.CONSUME_ITEM`）不会被触发**。对照原作：1.7.10 `onEaten` 返回 void，消耗由 `ItemStack` 框架处理，**没有跳过任何东西**。→ **移植引入的差异（P2，非崩溃）**：核心功能（饥饿回复）正常，仅成就/统计缺失。建议改为 `super.finishUsingItem(stack, level, livingEntity)`。**注意**：同类的 `ItemHeavenlyPeach:31`、`ItemByoukiheiyuMamori:41` 都正确调用了 `super`，**只有 `ItemSwallowCowrieShell` 漏了**，故这是孤例转录笔误。
- **登记 — `item/ItemKaigo.java`（104 行整体）**：类头注释自述「1.7.10 用 durability 当标记，移植把 damage 复用为标记」；`hurtEnemy:47-54` 用 `stack.setDamageValue(1)` 作标记，`use:80-81` 用黑染料 `shrink(1)` 复位。**作者已在 TODO 中自陈**该设计与 1.21.1 耐久体系的冲突（注册处 `durability(1)` 未核实）。**0 新发现**（登记，属已知设计债）。
- **P2 — `item/ItemTenguFan.java:52-53`**：`shot(FORM_WIND, AQUA, 0, 20, WIND01)` → `ShotData.shot(int form, int color, int delay, int end, int special)` 的 5 参重载，`size/damage` 由 `THShotLib.SIZE[FORM_WIND]`/`DAMAGE[FORM_WIND]` 取得（均为 0.90F/5.0F，`FORM_WIND=29` 界内）。`speed = (24 - usedTime) / 7.0` 分母为常量 → 无除零。**0 新发现**。
- **P1-latent 汇总（跨文件同式）**：以下 `use()`/`releaseUsing()` **无 `isClientSide` 守卫**，但 1.21.1 中 `use`/`releaseUsing` **两端都会调用**，而它们的 `stack.shrink(1)` 会**在客户端本地栈上执行一次**（服务端执行后通过 `ClientboundContainerSetSlotPacket` 覆盖，通常无可见后果，但若 `InteractionResultHolder` 返回后客户端预测性重放，可能出现「物品数量短暂跳变」）：
  - `ItemSpiritualStrikeTalisman.java:44`（`if (!player.isCreative()) stack.shrink(1);`）
  - `ItemSakuyaStopWatch.java:60-62`（同上）
  - `ItemDragonNeckJewel.java:65/82`（`hurtAndBreak(70/299, player, MAINHAND)` —— `ItemStack.hurtAndBreak` 内部 `if (entity.level() instanceof ServerLevel)` 才生效 → **天然安全**，非缺陷）
  - `ItemByoukiheiyuMamori.java:49`（`stack.shrink(1)`，在 `finishUsingItem` 内 → 服务端权威，安全）
  经逐条核查：**`hurtAndBreak` 有内置 ServerLevel 守卫（`ItemStack.java:490-499`），`shrink` 无守卫但由服务端包覆盖** → 判定为 **P2（不作为缺陷上报）**，与 1.7.10 同构（原作同样在客户端执行 `itemStack.stackSize--`，见 `ItemSpiritualStrikeTalisman.java:42`、`ItemSakuyaStopWatch.java:60` 原作均在 `!world.isRemote` 之外）。

### 逐文件结论（批次 1）

| 文件 | 结论 |
|---|---|
| ItemHeavenlyPeach | 正确调用 `super.finishUsingItem`；效果仅在服务端（`:26`）→ **0 新发现** |
| ItemOnbashira | 见上（P2 每 tick 改 DeltaMovement；原作同构） |
| ItemOnmyoudama | 防具槽判定 + `1.2×`；先前置 `instanceof Player`（`:25`）→ **0 新发现** |
| ItemShotMaterial | `createShot` 内部已有服务端守卫；`shrink` 有端守卫（`:40`）→ **0 新发现** |
| ItemSpiritualStrikeTalisman | `addFreshEntity` 有端守卫；`shrink` 无端守卫但安全（P2）→ **0 新发现** |
| ItemClosedThirdEye | `addEffect` 有端守卫（`:33`）；`getEntitiesOfClass(Mob)` 遍历为快照 → **0 新发现** |
| ItemKerobou | `inventoryTick` 有端守卫（`:32`）；`stack.enchant(...THORNS, 10)` 每 tick 检查 `isEnchanted()` → 幂等 → **0 新发现** |
| ItemMarisaHat | 有端守卫；`playerTouch` 调用正确 → **0 新发现** |
| ItemMazinKyoukan | `addFreshEntity` 有端守卫 → **0 新发现** |
| ItemSwallowCowrieShell | **P2：`finishUsingItem` 未调 `super`（跳过成就/统计）**，孤例 |
| ItemSakuyaStopWatch | 双列表 `getEntitiesOfClass` 判空；`shrink` 无端守卫（P2）→ **0 新发现** |
| ItemByoukiheiyuMamori | `removeAllEffects` 与 `addEffect` 均正确（后者有端守卫）→ **0 新发现** |
| ItemTenguFan | 见上 → **0 新发现** |
| ItemDeathScythe | `hurtAndBreak` 在 `!level.isClientSide` 内（`:57`）→ **0 新发现** |
| ItemKinkakuzi | `addFreshEntity + shrink` 同处 `!level.isClientSide`（`:55-58`）→ **0 新发现** |
| ItemKappaCap | 见上（登记）→ **0 新发现** |

### 批次 2（6 个：ItemHomingAmulet / ItemSpearTheGungnir / ItemKabenuke / ItemUchide / ItemIbukihyou / ItemMarisaBroom）

**结论：0 个 P0；2 个 P1；2 项 P2；其余登记。**

- **P1 — `item/ItemSpearTheGungnir.java:68-70`（`use`）**：`if (!player.isCreative()) stack.setDamageValue(stack.getDamageValue() + 1);` **无 `isClientSide` 守卫**。`use()` 两端都会执行 → **客户端也会 +1，服务端再 +1，随后服务端 `ClientboundContainerSetSlotPacket` 覆盖客户端值 → 最终一致，但客户端会出现一帧的耐久跳动**。更值得注意：`setDamageValue` **不做 `getMaxDamage()` 夹紧**（NeoForge `IItemExtension.getDamage` 在读取时夹紧；写入 `setDamageValue` 直接存 int），所以**耐久可以超过 maxDamage=95**，`ItemStack.isDamageableItem()` 为 true 时 `getDamageValue() >= getMaxDamage()` 会让物品直接消失（`hurtAndBreak` 路径）或显示满条 —— 这里的 `setDamageValue` 增量写法在 `damage == 95` 时得到 96，**物品不会消失但显示为完全损坏**。原作同为 `setItemDamage(damage+1)`（不夹紧）→ **登记（原作即如此）**，但移植侧已有 `hurtAndBreak` 可用；建议改用 `hurtAndBreak(1, player, MAINHAND)`。**列为 P1（组件/NBT 读写不对称导致的状态异常：耐久超出 maxDamage，模型/耐久条显示错误）**。
- **P1 — `item/ItemUchide.java:76`（`use`）与 `:86`（`inventoryTick`）**：`stack.setDamageValue(stack.getDamageValue() + 30)` 无端守卫、无夹紧（maxDamage=100）；`inventoryTick` 每 tick `setDamageValue(max(0, d-1))` **无端守卫且无 `isSelected`/槽位判断**，即**任何持有该物品的实体（含掉落的 ItemEntity、展示框、其他玩家的背包）都会每 tick 恢复耐久**，且对客户端也执行（本地值抖动）。原作 `onUpdate` 同样是每 tick `setItemDamage(damage - 1)`，但 1.7.10 的 `ItemStack` 变化在服务端权威。**登记（原作即如此）**，P1 依据是「耐久可超上限 + 客户端每 tick 写入」的组合。
- **P2 — `item/ItemIbukihyou.java:59-61`（`finishUsingItem`）**：`return stack;` **未调用 `super.finishUsingItem`**（与 `ItemSwallowCowrieShell` 同型）→ 跳过 `Stats.ITEM_USED`/`CONSUME_ITEM` 成就统计。同一 `return stack` 在 `:59` 的「非玩家或潜行」分支也出现。原作 `onEaten` 返回 ItemStack 由框架处理，**移植引入的差异**（功能本身正常）。
- **P2 — `item/ItemMarisaBroom.java:56`（`player.startRiding(broom)`）**：`broom` 在同一 tick 内先 `level.addFreshEntity(broom)`（`:51`，`!level.isClientSide`）再 `startRiding`（`:56`，同守卫）→ **顺序正确**。但 `EntityMarisaBroom` 的 `getPassengerAttachmentPoint` 未在本类触及。另 `:56-58` 的 `broom.setYRot/setXRot(player...)` 在 `startRiding` 之后 → 与原作同序。**0 新发现**（登记）。
- **0 新发现（登记）**：
  - `ItemHomingAmulet.java:56` 用 `stack.is(ModItems.HOMING_AMULET.get())` 分流两种弹幕；`:76-78` `shrink` 有端守卫 → 正确。
  - `ItemKabenuke.java:43` 已做 `player instanceof ServerPlayer && !level.isClientSide` 双守卫（本批最严谨）→ 正确；`:55 for(i<16)` 有界，`pos.offset` 不越界。
  - `ItemUchide.java:52-59` 潜行开 GUI 有端守卫 → 正确；`:61` 食料阈值 `> 0` 与原作一致（原作注释与实现不符，已登记）。

### 批次 3（6 个：ItemBuddhaStoneBowl / ItemThirdEye / ItemPendulum / ItemFireRatRobe / ItemNuclearControlRod / ItemAjaRedStone）

**结论：0 个 P0；1 个 P1（跨物品实例共享字段）；其余登记 / P2。**

- **P1 — `item/ItemThirdEye.java:32-33`（实例字段 `hitEntity`/`check`）**：这两个字段是 **Item 实例字段**，而 1.21.1 中**所有同 id 物品共享同一个 Item 单例**（`DeferredItem` 注册的唯一实例），因此：
  - **跨玩家共享**：A 玩家 `use()` 设 `hitEntity=B`，下一 tick **A 与 B（及其他任何人）的 `inventoryTick` 都会消费该状态并打出同一条 HP 消息** → 信息泄漏/串话（`:84-85`）。
  - **跨存档/维度泄漏**：`hitEntity` 强引用一个 `LivingEntity`，直到下一次 `inventoryTick` 才清空（`:92-93`）。若玩家在设置后掉线/切维度而 `inventoryTick` 未再执行，**该实体引用会被 Item 单例长期持有 → 内存泄漏**。另 `:76` 的 `!level.isClientSide` 守卫使客户端永不消费 → 客户端侧 `check/hitEntity` 永不复位（无实际后果，但进一步说明状态机位置错）。
  - 修复：改为把「待显示目标」放进 `ItemStack` 的 DataComponent（或 `Player` 的 persistent data），不要在 Item 实例上存可变状态。
  - 原作同为实例字段（`src/.../ItemThirdEye.java:26-27`）→ **登记（原作即如此）**，但**1.7.10 的 Item 同样单例**，故这是原作缺陷的忠实继承；在本轮「严重性普查」中列为 P1（跨玩家串话 + 潜在内存泄漏），**建议发布前修**。
- **P2 — `item/ItemAjaRedStone.java:114`（`specialShot_hitEntity`）**：`living.setRemainingFireTicks((int) shot.shotDamage)`。原作注释称 `shotDamage` 兼作「着火时间（秒）」，但 `EntityTHShot.shotDamage` 同时是**伤害值**（`:115 shot.damageRate *= 2.0F` 才是加倍）→ 语义混用（原作亦然，`TODO K3-review` 已自陈）。`setRemainingFireTicks` 用 tick 而非秒 → 与原作 `setFire((int)shotDamage)` 一致。**登记**。
- **P2 — `item/ItemBuddhaStoneBowl.java:62`（`getDestroySpeed` 恒返回 4.0F）**：无视 `stack`/`state` 恒返回 4F → **对任何方块都是 4F 挖掘速度**（原作 `getDigSpeed` 同样恒返回 3F/4F）。结合 `:46-47` 注册了 `Tiers.STONE.createToolProperties(MINEABLE_WITH_PICKAXE)`，`mineBlock` 被覆写为不耗耐久（`:81-83`）→ **该物品可无限高速挖任何方块**。原作同样是 `getDigSpeed` 返回 3F + `func_150897_b` 覆写且不耗耐久 → **登记（原作即如此）**，属原作设计（神器）。
- **登记 — `item/ItemNuclearControlRod.java:75-76`**：`time/25.0D` 分母为常量 25，`vec3` 作为 `getVecFromAngle` 的 force 参数（0..4）→ 无除零。`:79 hurtAndBreak` 有内置服务端守卫。**0 新发现**。
- **登记 — `item/ItemPendulum.java:47-60`**：`selected >= 8` 前置判断 + `isEmpty()` + `instanceof BlockItem` 三重守卫（本批最严谨之一）；`:51 getItem(selected+1)` 因 `selected <= 7` 而 `<= 8` 在 9 格快捷栏内 → **无越界**。`searchItem` 传给 `EntityPendulum` 构造器（实体侧存 ItemStack），`:77 shrink` 有端守卫。**0 新发现**。
- **登记 — `item/ItemFireRatRobe.java:41-44`**：`igniteForSeconds(1.0F)`（= 20 tick）与原作 `setFire(20)` 一致；`:67-78 inventoryTick` 有端守卫 + `isEnchanted()` 幂等。**0 新发现**。

### 批次 4（4 个：ItemOharaibouR / ItemKappaWaterPistol / ItemKaigo / ItemDragonNeckJewel）

**结论：0 个 P0；1 个 P2；1 项重要「已排除」；其余登记。**

- **已排除（重要）— `item/ItemKaigo.java:26-32` 的类头 TODO 与现实不符**：TODO 声称「`ROD_OF_REMORSE` 目前未注册 `durability(...)`，max damage 为 0，`setDamageValue(1)` 会被夹到 0，used 状态永远存不住」。**实际 `init/ModItems.java:114` 已注册 `new Item.Properties().durability(2)`** → `setDamageValue(1)` 可存住，机制可用。**结论：非缺陷，仅是过期注释**（P2：注释与代码不一致 + `durability(2)`+`damage=1` 会让耐久条显示为半损，观感问题）。
- **P2 — `item/ItemOharaibouR.java:71-74`（`releaseUsing`）**：`time` 计算后**未被使用**（原作亦只算不用），`:77` 声音与 `:80 hurtAndBreak` 无端守卫但 `hurtAndBreak` 内置服务端守卫 → **0 新发现**；仅指出 `time` 是死变量（原作同）。
- **登记 — `item/ItemKappaWaterPistol.java:54`**：`level.getFluidState(hitPos).is(Fluids.WATER) && ...isSource()` —— 与原作「water + meta 0」语义等价；`:62` `getDamageValue() == getMaxDamage() - 1`（39-1=38）与注册值一致性已核对（`ModItems.java:117 durability(39)`）→ **正确**。`:86 hurtAndBreak` 有内置守卫。`speed += 0.07D` 死赋值（`:81-82` 有注释说明原作同）→ **登记**。
- **0 新发现（登记）— `item/ItemDragonNeckJewel.java`**：`:54 r[rand]` 三数组均长 5、`rand ∈ [0,4]` → 界内；`:51/<229`、`:68/<1` 门限与 `durability(300)`（`ModItems.java:73`）一致；`:65/82 hurtAndBreak(70/299)` 有内置服务端守卫；`:94-96 inventoryTick` 无端守卫地 `setDamageValue`（与 `ItemUchide` 同型，本地抖动，P2 一并计入 `ItemUchide` 那条）。

### 批次 5（3 个：ItemSoulTorch / ItemSilverKnife / ItemGapFoldingUmbrella）

**结论：0 个 P0；1 个 P2；其余 0 新发现。**

- **0 新发现（登记）**：
  - `ItemSoulTorch.java:56-58`：创造模式即时 `finishUsingItem` **有端守卫**（`!level.isClientSide`）→ 正确（原作 `onItemRightClick` 直接调 `onEaten`，移植保留语义）。`:68-85 finishUsingItem` 的 `return stack`（`:70` 非玩家分支、`:84` 正常分支）**未调 `super`**，与 `ItemSwallowCowrieShell`/`ItemIbukihyou` 同型 → 计入 P2「`finishUsingItem` 未委托 super」汇总项。`:75 createRingShot(..., 3, ...)` way=3 非零；`FORM_PHANTOM=26` 在 SIZE/DAMAGE 表（32 项）内。`:104 shot.homing(16.0F)` 委托实体方法 → 正确。`:109 specialShot_hitBlock` 返回 true = 不消失，与注释一致。
  - `ItemSilverKnife.java:53-64 getKnifeColor`：4 分支 + 默认 0，**恒返回 0..3** → `knifeColorNames[getKnifeColor(stack)]`（`:70`，数组长 4）**无越界**。`:80-82 hurtEnemy` 白刀 `shrink(1)` **无端守卫**，但 `hurtEnemy` 仅在服务端由 `Player.attack` → `ItemStack.hurtAndBreak` 路径调用（`ServerPlayerGameMode` 权威），客户端不调用 `hurtEnemy` → **安全**。`:109-112` 投掷 `addFreshEntity + shrink` 同处端守卫内 → 正确。
  - `ItemGapFoldingUmbrella.java:64-69`（潜行换色）、`:87-90`（`color >= 16 → 0` 夹紧）、`:99-108`（耐久耗尽 → `setItemInHand(hand, gap)` 替换为「隙間」并恢复 `ORIGINAL_DAMAGE`）—— 组件读写路径 `GAP_COLOR`/`ORIGINAL_DAMAGE` **成对且方向正确**（写：`:69`；读：`:64/:87/:100`）。`:97 hurtAndBreak` 有内置服务端守卫。**唯一 P2**：`:106 player.setItemInHand(hand, gap)` 无端守卫（客户端也会把本地栈换成 `gap`），若服务端拒绝（如 `use` 被取消）会造成短暂不一致 —— 与 `ItemHisyaku`、`ItemBloodthirstyOnmyoudama` 同型，合并为 P2 汇总项「`setItemInHand` 无端守卫」。

### 批次 6（4 个：ItemOharaibouS / ItemMiniHakkero / ItemCursedDecoyDoll / ItemMikoSword）

**结论：0 个 P0；2 个 P1；1 个 P2（核心功能失效）；其余登记。**

- **P1 — `item/ItemMikoSword.java:66-73`（`use`）**：`if (stack.getDamageValue() >= stack.getMaxDamage()) stack.shrink(1);` **无 `isClientSide` 守卫**。`use()` 两端都执行 → **客户端与服务端各减 1**。服务端随后用 `ClientboundContainerSetSlotPacket` 覆盖客户端栈，**通常收敛**；但若此时客户端栈与服务端不同步（延迟、预测性交互），会出现**一次性多减**。原作 `ItemMikoSword.onItemRightClick` 同样无端守卫地 `itemStack.stackSize--` → **登记（原作即如此）**，1.21.1 未新增风险。列 P1 仅因 `shrink` 在 1.21.1 是「服务端权威」语义而原作是「两端各自记账」。
- **P1 — `item/ItemMikoSword.java:177`（`releaseUsing` 末行）**：`stack.setDamageValue(stack.getDamageValue() + i)` **不夹紧 `getMaxDamage()`**。`i` 为**探知到的实体数（0..10）**，可让耐久超过 `maxDamage`；`ItemStack.setDamageValue` 不夹紧（读取时 `IItemExtension.getDamage` 才夹紧），因此**耐久条会显示为满/异常，且物品不会消失**。原作 `itemstack.setItemDamage(getItemDamage() + i)` 同样不夹紧 → **登记（原作即如此）**。同型问题已在 `ItemSpearTheGungnir`、`ItemUchide` 记录。
- **P2（核心功能失效候选）— `item/ItemCursedDecoyDoll.java:65-73`**：`if (stack.getDamageValue() >= stack.getMaxDamage()) { stack.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), 0); }` —— **该语句的返回值被丢弃，整块无任何效果**（纯死代码），且类头 `:26-31` 标注 `TODO K3-review (blocking)`。后果：**耐久耗尽后不会变回原物品**（原作会 `Item.getItemById(originID)` 还原）。这属于**已知未实现**（作者自陈 blocking），但它是**核心功能缺失**，故单列 P2 而非静默登记。`ModItems.java:104 durability(30)` 已注册 → 耐久会正常耗尽，只是耗尽后无还原。
- **登记 — `item/ItemOharaibouS.java:143-145 setMiraclePower(ItemStack stack)`**：`stack.set(ModDataComponents.CHARGE.get(), 1000)`，调用点 `:90/:95/:100/:105/:110` 传入的是 **`inv`（遍历到的符卡栈）**，与原作「把 `Charge` 写进符卡 NBT」**目标一致** → **无错位**（已逐调用点核对）。`:91/:96/:101/:106/:112 setDamageValue(...)` 无端守卫地减少耐久（`Math.max(0,...)` 下界安全）；`:138 hurtAndBreak` 有内置守卫。`endurance` 已夹紧在 [15,120]（`:123-127`）；`damage = experienceLevel/5F`（分母常量 5）→ **无除零**。原作同构（登记）。
- **0 新发现（登记）— `item/ItemMiniHakkero.java`**：`:50-52` **显式客户端早退**（本批最干净的端分离写法）；`:62` 方块占用判定；`:68-89` 火薬 32 个的计数与扣除（`remaining` 递减有界）→ **无越界**；`:92` 把 `stack.getDamageValue()` 作为设置点耐久传给实体（原作同）。`getInventory().items.size()` 遍历为 `NonNullList` 快照语义（`shrink` 不改 size）→ **无 CME**。

### 批次 7（3 个：ItemBloodthirstyOnmyoudama / ItemHouraiJeweledBranch / ItemSukima）

**结论：0 个 P0；1 个 P1；1 个 P2；其余登记。**

- **P1 — `item/ItemSukima.java:210-234`（`use`）**：`:210/:211` 构造 `entitySukima`/`entitySukima2`，但 `:214-216` 只在端守卫内 `addFreshEntity(entitySukima2)`；`:229-234` 才在端守卫内 `addFreshEntity(entitySukima)` 并按 `lost` 概率 `shrink(1)`。`:210` 的 `entitySukima` 被构造后**在同一 tick 内被加入世界**，逻辑闭合。**真正的 P1**：`:212 lost = player.getRandom().nextInt(100) < 15` 在**两端各自独立随机**，`:231 if (lost) stack.shrink(1)` 只按服务端值执行 → 与原作一致（原作同样在两端随机）。**判定：登记（原作即如此）**，但 1.21.1 下客户端会本地随机一次（无副作用）→ 降为 P2（无意义随机调用）。
- **已核对/登记 — `item/ItemBloodthirstyOnmyoudama.java`**：`:115` 已做 `player instanceof ServerPlayer && !level.isClientSide` 双守卫（`:124` 的 `connection.teleport` 只在服务端）→ 正确。`:128 hurtAndBreak` 有内置守卫。**唯一 P2**：`:135 player.setItemInHand(InteractionHand.MAIN_HAND, yinYangOrb)` **无端守卫**（客户端也替换本地栈）→ 与 `ItemGapFoldingUmbrella:106`、`ItemHisyaku:90` 同型，计入「`setItemInHand` 无端守卫」汇总项。注意 `:135` 硬编码 `InteractionHand.MAIN_HAND` 而非 `hand` 参数 → 若从副手触发会把主手物品替换掉（**移植笔误候选**，原作只用 `itemStack = new ItemStack(...)` 由返回值写回，无手性问题）。**列为 P2（副手使用时误替换主手物品）**。
- **登记 — `item/ItemHouraiJeweledBranch.java`**：`:75 shotNum = 32 + getUseDuration(...) - usedTime` → `BowItem.getUseDuration` 默认返回 `72000`（NeoForge 21.1.252 `BowItem.getUseDuration` = `72000`），与原作 `ItemBow.getMaxItemUseDuration` 的 `72000` **一致** → 无需重写，行为等价。`:80 shotNum /= 3`、`:87 i += 3`（步长 3）→ 有界；`:101/:104 shotNum/2`、`shotNum/3`：`shotNum = clampValue/3 >= 10`（`usedTime <= 72000` 时 `shotNum` 被夹到 72 → `/3 = 24`；`usedTime = 72000` 时 `shotNum = 32/3 = 10`）→ **恒 ≥ 10，非零**，`createRingShot`/`createCircleShot` 内 `360F/way` 无除零。`:135 setCharge` 已 `Math.max(0, charge)` 夹紧。`:120` 条件 `getCharge(stack) > 0` → 无死角。**0 新发现。**
- **0 新发现（登记）— `item/ItemSukima.java:96-141`**：`getPlayerPOVHitResult` 的 `MISS` 分支与 `switch (hit.getDirection())` 六臂**全覆盖** `Direction` 枚举（DOWN/UP/NORTH/SOUTH/WEST/EAST），无 `default` 但枚举穷尽 → **无遗漏分支**；`i/j/k` 为 double，`:141` 加 0.5 后交构造器 → 无下标。

### 批次 8（4 个：ItemYuukaParasol / ItemYuyukoFan / ItemSakuyaWatch / 复查已读项）

**结论：0 个 P0；2 个 P2；其余 0 新发现。**

- **P2 — `item/ItemSakuyaWatch.java:138-142`（`getUseDuration` 读 `stack.getDamageValue()`）**：`damage == 0` → 20，否则 48。`:94` 在 `finishUsingItem` 内用 `usedTime >= getUseDuration(stack, player)` 判定，但 `usedTime` 被**硬编码为 24**（`:80`）→ 依赖 `use()` 路径的 `startUsingItem`；由于 `:63-65` 创造模式直接调 `finishUsingItem`，`usedTime=24` 对 `mode` 判定：
  - `damage == 0`（`getUseDuration=20`）→ `24 >= 20` 成立 → 进入「1.2 秒以上」分支；
  - `damage == 1`（`getUseDuration=48`）→ `24 >= 48` **不成立** → 落入 `:102 else`，只在潜行时切模式，然后 `return stack` **直接返回、不生成时停空间**。
  即**创造模式下非潜行、damage=1（停止模式）时会静默失效**。原作以 `onEaten` 直接调用且不比较 `usedTime`（原作无 `usedTime` 概念）→ **移植引入的判定缺陷（P2：创造模式停止模式无法使用时停）**。`ModItems.java:100` 已注册 `durability(2)` → `setDamageValue(1)` 可存。**建议**：创造模式分支不要走 `usedTime` 比较。
- **P2 — `item/ItemYuyukoFan.java:127`（`hurtEnemy`）**：`if (target.getHealth() > 0 && target.getHealth() < 20 && target.hurtTime > 0)` 组合条件，`:134 target.hurt(damageSources().mobAttack(attacker), 4444)` —— 4444 为 int，`LivingEntity.hurt` 收 float → 隐式转换正常。原作同为 4444。**注意**：`target.hurtTime > 0` 表示「目标已处于受伤无敌帧」，与原作 `entityLiving.hurtResistantTime > 0` 语义一致（登记）。`:129 attacker.getRandom().nextInt(100) < 5` → 5% 概率；`:141 hurtAndBreak` 无端守卫但内置服务端守卫 → 0 新发现。
- **0 新发现（登记）— `item/ItemYuukaParasol.java`**：`:107 shotPower = 10 + getUseDuration(...) - usedTime`；`getUseDuration` 返回 72000（`:193-195`，与原作 `ItemYuukaParasol.getMaxItemUseDuration` 的 72000 **一致**）→ `shotPower2` 被 `:109-111` 夹到 ≤5。`:116 shotNum = 45` 常量 → `:117 360F/45`、`:138 360F/(45*0.25=11)` **均为常量除数，无除零**。`:124/:140 for(i<shotNum)` 有界。`:160-178 hurtEnemy` 用 `Mth.sin/cos` + `setDeltaMovement`；`:176 hurtAndBreak` 有内置守卫。`:220-243 specialShot_hitBlock` 用 `Mth.floor` 定位（双精度 → int 不会溢出到 NaN 输入），`Blocks.GRASS_BLOCK`/`DANDELION`/`POPPY` 替代原作数字 ID → 正确映射。`stack.shrink(1)`（`:88`）在端守卫内 → 正确。**0 新发现。**

---

## 全包总结（item/）

### P0：**零发现**（0 个崩溃 / 数据损坏）
逐行读完 47 个物品类（其余 3 个 `ItemTHShot`/`ItemTHLaser`/`ItemTHSpellCard` 已在 REPORT.md 逐行覆盖），未发现任何可达的 NPE、数组越界、除零、CCE、无限循环或组件错位造成的崩溃/数据损坏。

### P1（3 个，均附 file:line 与可达路径）
| file:line | 一句话 | 置信度 |
|---|---|---|
| `item/ItemThirdEye.java:32-33` | `hitEntity`/`check` 是 **Item 单例的实例字段** → 多玩家共享（A 的目标消息会由 B 打出）+ `LivingEntity` 强引用在被消费前可能长期滞留（内存泄漏） | 高 |
| `item/ItemSpearTheGungnir.java:68-70`、`item/ItemUchide.java:76/86`、`item/ItemMikoSword.java:177` | `setDamageValue(getDamageValue()+n)` **不夹紧 `getMaxDamage()`** → 耐久可超上限，耐久条/`isDamaged()` 显示异常（原作同式，但 1.21.1 的 `setDamageValue` 语义已变） | 高 |
| `item/ItemSakuyaWatch.java:80+94+138` | 创造模式 + `damage==1`（停止模式）时 `24 >= 48` 不成立 → **静默失效、不出时停空间**（移植引入） | 中 |

### P2（汇总，各一句话）
`ItemSwallowCowrieShell:55`、`ItemIbukihyou:59/71`、`ItemSoulTorch:70/84` 三处 `finishUsingItem` 未委托 `super`（跳过成就/统计，`ItemSwallowCowrieShell` 为孤例笔误）；`setItemInHand` 无端守卫 4 处（`ItemGapFoldingUmbrella:106`、`ItemBloodthirstyOnmyoudama:135`[并把 `hand` 硬编码成 `MAIN_HAND`]、`ItemHisyaku:90`、`ItemCursedDecoyDoll:72`）；`inventoryTick` 无守卫每 tick 写 `DeltaMovement`/耐久 4 处（`ItemOnbashira:29-31`、`ItemOnmyoudama:38`、`ItemKappaCap:55`、`ItemUchide:86`）；`ItemCursedDecoyDoll:65-73` 还原逻辑为**死代码**（原作核心功能缺失，作者已标 blocking）；`ItemKaigo:26-32` 注释与实际注册（`durability(2)`）不符。

### 原作即如此（登记，不计入缺陷）：11 项
`ItemOnbashira`/`ItemMarisaHat`/`ItemKappaCap`/`ItemMikoSword:66-73` 的无端守卫耐久写入；`ItemUchide:61` 食料阈值注释与实现不符；`ItemBuddhaStoneBowl:62` 全方块恒速挖掘；`ItemAjaRedStone:114` 伤害值兼作着火 tick；`ItemKappaWaterPistol` 的注册耐久耦合；`ItemSukima:212` 两端独立随机；`ItemYuyukoFan:127` 无敌帧条件；`ItemMikoSword:122-127` 原作角度计算 bug（移植保留）。

### 覆盖声明
- **逐行通读：49/49** 个 `item/*.java`。本会话逐行读完其中 **46 个**；`ItemTHShot`、`ItemTHLaser`、`ItemTHSpellCard` 三者在 REPORT.md（首轮）已逐行覆盖，本轮不重复。
- **对照原作：** 关键文件 `ItemOnbashira`/`ItemMarisaHat`/`ItemKappaCap`/`ItemSpiritualStrikeTalisman`/`ItemSakuyaStopWatch`/`ItemMikoSword`/`ItemOharaibouS`/`ItemMiniHakkero`/`ItemThirdEye`/`ItemBloodthirstyOnmyoudama`/`ItemUchide`/`ItemIbukihyou`/`ItemYuukaParasol`/`ItemHouraiJeweledBranch` 均逐段比对；其余以 grep 抽取关键数值/条件对照。
- **工具：** 只读 `Read`/`Grep`/`Bash(grep, awk)`；**未修改任何既有文件、未运行 gradlew、未执行 git、未杀进程**。
- **未覆盖：** `client/**`（另一批）；`init/ModItems.java` 仅作为注册值核对来源（未做独立审计）。
- 覆盖校验：`item/*.java` 共 49 个文件，REPORT3.md 状态表包含 49 行，缺失 0（脚本核对通过）。
