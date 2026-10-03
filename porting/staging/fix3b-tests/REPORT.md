# FIX-3b 批次报告

范围：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/` 下的 item / menu / entity 指定文件
（修复项 1–6）。**本批不跑 gradlew**（避免与并行批争锁），编译验证用等价 javac 沙盒。
禁改：`porting/*.md`、`test/` 目录、FIX-3a 批禁区的 6 个实体文件（未触碰）。

## 1. 编译验证（javac 等价沙盒，与 gametest-edge §2.2 同法）

`gameTestServerLegacyClasspath.txt`（原 `clientLegacyClasspath.txt` 已改名）+ `neoforge-21.1.252-merged.jar`：

```bash
# 主树 268 个 java（含本批 6 处改动）→ 0 error，280 个 class
javac -nowarn -proc:none -encoding UTF-8 -cp "$MERGED;$LEGACY" -d porting/.fix3b-build/out @files.txt
# staging/fix3b-tests 的测试草稿（classpath 再追加主树 out）→ 0 error
javac -nowarn -proc:none -encoding UTF-8 -cp "$MERGED;$LEGACY;<主树 out>" -d ... Fix3bRegressionTest.java
```
两次仅 `deprecation` / `unchecked` 提示，**0 错误**。临时目录 `porting/.fix3b-build/` 用完已删除。

## 2. 逐项改动与「原作对照结论」

| # | 文件:行 | 改动 | 原作对照结论 |
|---|---|---|---|
| 1 | `item/ItemThirdEye.java:32-33`（删字段）、`:68-71` + 新增 `report()` | 删除 Item 单例上的 `hitEntity` / `check` 实例字段，改为 `use()` 内即时 `report()`；`inventoryTick` 只保留耐久回复 | **原作同式**（1.7.10 `ItemThirdEye` 也把两个字段放在 Item 实例上）。但 Item 是注册表单例 → 1.21.1 多人下「A 右键、B 收到消息」+ Entity 强引用滞留。原作 1 tick 延迟只是「在 onUpdate 里读」的实现手段，显示内容/对象相同，故按迁移规则「旧版 bug 修复并注释」改为无状态即时显示（唯一差异：消息早 1 tick） |
| 2a | `item/ItemSpearTheGungnir.java:33-36`（注释）、`:69` | 保留 `setDamageValue(+1)`，补注释 | **无需 clamp**：1.21.1 `ItemStack#setDamageValue` → `Item.setDamage` = `Mth.clamp(damage, 0, getMaxDamage())`（NeoForge `IItemExtension.java:399-401`），超上限根本写不进去，也不会破损坏。原作 `setItemDamage(+1)` 同为直接写 |
| 2b | `item/ItemUchide.java:31-34`（注释）、`:76`/`:86` | 同上（+30 / −1） | 同上；`+30` 到 100 即止，`−1` 已有 `Math.max(0, …)` |
| 2c | `item/ItemMikoSword.java:235` | 补注释 | 同上（探知数 i 可能 > 上限，clamp 后停在 maxDamage） |
| 3 | `item/ItemSakuyaWatch.java` | **未改**（登记） | **原作同式**：原作 `onItemRightClick` 用 `getMaxItemUseDuration`（damage 0→20 / 1→48），非创造分支 `usedTime(24) >= duration` 在停止档（48）恒 false → 走「切换档位」兜底 return，静默失效。**创造模式不经此判断**（`if(player.capabilities.isCreativeMode) onEaten(...)` 直接调用），所以「创造+停止档可用」在移植里同样成立，USER_MIGRATION Q7 的记载正确。无需修复，保留原 TODO |
| 4 | `menu/MiracleMalletMenu.java:147` | `craftResult.setItem(0, produced)` → `produced.copy()` | 移植引入（`DanmakuCraftingMenu:269` 已用 copy）。`ResultContainer.setItem` 直接存引用 → 结果槽与 `resultItem` 共享实例；`inSlot.shrink(count)` 会把 `resultItem` 一起清空。已硬化 |
| 5 | `menu/DanmakuLaserCraftingMenu.java:49-56`（新增常量）、`:367-390`（quickMoveStack） | 槽段 10/37/46 → 15/42/51（真实布局：0 结果 / 1..5 功能 / 6..14 矩阵 / 15..41 主背包 / 42..50 快捷栏） | **旧版 bug（原作同式）**：1.7.10 `ContainerDanmakuCraftingLaser.transferStackInSlot` 照抄弹版常量，矩阵尾 5 格与快捷栏后半段 shift 点击静默无效。按迁移规则「旧版 bug 修复并注释」修正，并在 javadoc 写清原错位表现 |
| 6 | `entity/living/EntitySanae.java:93-101`（注释） | **未改行为**，登记结论 | **原作同式**：1.7.10 `MerchantRecipe` 也有 `toolUses/maxTradeUses` + `isRecipeDisabled()`（MCP stable 39：`func_77399_f=incrementToolUses`、`func_82784_g=isRecipeDisabled`、`field_82786_e=maxTradeUses`），原作 `EntitySanae.useRecipe` 只 `incrementToolUses()`、无补货；1.7.10 补货在 `EntityVillager.updateAITick`，自作商人不在该路径 → 「用完即永久缺货」是原作行为。1.21.1 侧补货唯一实现是 `Villager#shouldRestock → WorkAtPoi`，模组 `canRestock()` 取接口默认 false、全树无 `resetUses`。**故不改代码**；`USER_MIGRATION.md §5#22` 的「约 2 个游戏日恢复」措辞错误，由 K3 改 |

## 3. 测试草稿（`porting/staging/fix3b-tests/thkaguyamod/test/Fix3bRegressionTest.java`）

包名 = 最终包名 `thkaguyamod.test`，合并即拷贝；5 个 `@GameTest(template="empty")`，javac 沙盒 0 错，**运行时未执行**（本批禁 gradlew）。

| 测试 | 对应修复项 | 断言要点 |
|---|---|---|
| `pocketWatchDamageTogglesUseDuration` | 3 | damage 0→`getUseDuration`=20、damage 1→48、`getMaxDamage()`=2（档位标志位可写） |
| `laserMenuQuickMoveUsesRealSlotRanges` | 5 | `slots.size()`=51；slot 15/41/42 的 container 均为玩家背包（修复前 42..45 会走错分支） |
| `miracleMalletResultIsDetachedCopy` | 4 | 结果槽与 `getResultItem()` 不是同一实例；`shrink` 后 `resultItem` 仍非空 |
| `sanaeOffersAreFiniteAndUnrestocked` | 6 | maxUses=12；12 次 `notifyTrade` 后 `isOutOfStock()`；`canRestock()`=false（缺货永久） |
| `sakuyaWatchEntityAcceptsBothModes` | 3（辅） | TIME_HALF / TIME_STOP 两种 mode 均可构造且不立即消失 |

## 4. 待 K3 项

1. **合并**：`porting/staging/fix3b-tests/thkaguyamod/test/Fix3bRegressionTest.java` → `src/main/java/thkaguyamod/test/`，然后 `./gradlew runGameTestServer`（基线 50 → 应为 55）。
2. **文档**：改 `USER_MIGRATION.md §5#22` 的「约 2 个游戏日恢复」→「用完即永久缺货（与原作一致）」。
3. **裁决**：修复项 1 的「改为即时显示」是否接受（功能等价、差 1 tick）；若要求严格保留延迟语义，需把状态挂到 `ItemStack` 组件而非 Item 实例。
4. **未覆盖**：FIX-3a 禁区的 6 个实体文件、`test/` 目录（本批未触碰）；`ItemSakuyaWatch` 只做静态核对未实机复现创造/非创造两档。
