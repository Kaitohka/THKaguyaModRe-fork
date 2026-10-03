# P10 VARIANTS — 御柱 `onbashira` 挖掘速度：两案

> 两案**同一个类名同一包**（`thkaguyamod.event.OnbashiraDigSpeedHandler`），
> **只能二选一**（不要同时拷贝）。`ItemOnbashira.java`（仅 javadoc 变更）两案共用、逐字节相同。
> 接线完全相同：`THKaguyaMod` 构造函数加 1 行 `NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());`

## 0. 两案速览

| | **VARIANT-A：字面恒 3.0F** | **VARIANT-B：1.7.10 实机（适正ツール分岐）** |
|---|---|---|
| 代码 | `event.setNewSpeed(3.0F)` | `event.setNewSpeed(player.hasCorrectToolForDrops(state, level, pos) ? 3.0F : 1.0F)` |
| クモの巣（cobweb） | 3.0 | 3.0（`Tool` 只对 COBWEB 声明 `correct_for_drops`） |
| 石头/矿石/土/木… | **3.0**（1.7.10 实机是 1.0） | **1.0** |
| `#sword_efficient`（草/叶/作物） | 3.0（1.7.10 实机 1.0） | 1.0 |
| 与 1.7.10 一致度 | **字面（源码）一致**；玩家体感偏快 | **实机体感一致**（见 §2 证据与 §3 的不确定性声明） |
| 与 1.21.1 原版剑的差 | 全部 3.0（比剑默认的 1.0/1.5 快，比剑本来的蜘蛛网 15.0 慢） | 蜘蛛网 3.0（剑默认 15.0，本变体会**降到 1.7.10 的 3.0**）、草叶 1.0（剑默认 1.5） |
| 依赖别的 MOD | 无 | 会连动 `PlayerEvent.HarvestCheck`（有人改这个事件时效速一起变） |
| 代码复杂度 | 6 行核心 | 8 行核心 + 1 次 `hasCorrectToolForDrops`（内部发 1 个事件） |

## 1. 1.7.10 原作确认（先读原作再落码，本变体的前置步骤）

`src/main/java/thKaguyaMod/item/ItemOnbashira.java`（只读参考，第 30-34 行）：

```java
//ブロックに対する強さ
@Override
public float getDigSpeed(ItemStack itemStack, Block block, int metadata)
{
    return 3F;
}
```

- **源码层面**：无条件返回 `3F`，不区分方块 → VARIANT-A 是字面移植。
- **注册**：`new ItemOnbashira(ToolMaterial.WOOD)`（`THKaguyaItems.java:451`）→ 覆写前的值是 WOOD 的 2.0。
- 同一模组里另有 3 个同类覆写可作旁证（说明作者确实用 `getDigSpeed` 表达“按方块不同”的意图）：
  - `ItemRoukanken`：蜘蛛网/叶 30F、羊毛 20F、**其他 `super.getDigSpeed(...) * 3F`**
  - `ItemHisou`：叶 15F、羊毛 10F、其他 `super`
  - `ItemBuddhaStoneBowl`：恒 4F
  → 御柱的“恒 3F”与佛御石钵的“恒 4F”是同一种写法：**故意给一个不随方块变化的常数**。

## 2. 实机语义（1.7.10 = バニラ 1.7.10 逻辑 + Forge 1.7.10 的 ForgeHooks）

原始设计里，物品的 `getDigSpeed` 只是“材料效率”，最终破坏速率还要过バニラ的适正ツール分岐。
本机没有 1.7.10 Forge 二进制，但**拿到了同代 1.12.2 的 Forge 反编译类**（`forge-1.12.2-14.23.5.2860`）作为该逻辑的可靠旁证：

| 环节 | 1.12.2 实测（本机反编译） | 1.21.1（主树同代反编译源） |
|---|---|---|
| 适正ツール判定 | `ItemSword.canHarvestBlock(IBlockState)`：**只有 `Blocks.WEB` 返回 true**（`javap -c` 反汇编证实；代码里就一个 `getBlock() == Blocks.WEB` 比较） | `Item.isCorrectToolForDrops(stackstate)` → `Tool.isCorrectForDrops`：御柱的 `Tool` 只在 `COBWEB` 上写 `correct_for_drops=true`（`SwordItem.createToolProperties()`）→ **同样只有クモの巣 true** |
| 实际破坏速率公式 | `ForgeHooks.blockStrength`：`digSpeed / hardness / (canHarvest ? 30.0 : 100.0)` | `BlockBehaviour.getDestroyProgress`：`player.getDigSpeed(state,pos) / hardness / (harvestCheck ? 30 : 100)` |
| `getDigSpeed` 的时机 | `EntityPlayer.getDigSpeed` = `inventory.getDestroySpeed` + 效率附魔 + 急速/疲劳 + 水中/空中 → **最后** `ForgeEventFactory.getBreakSpeed`，ped 全在 **30/100 除法之前** | `Player.getDigSpeed` = `inventory.getDestroySpeed` + `MINING_EFFICIENCY` + 药水 + `BLOCK_BREAK_SPEED` + 水中/空中 → **最后** `EventHooks.getBreakSpeed`，同样在 **30/100 除法之前** |

**结论**：`PlayerEvent.BreakSpeed`（两代都在同一位置）给的是**除 30/100 之前的“材料效率”**，
所以它等价于旧版的 `Item#getDigSpeed(block)`，而**不是**旧版的 `getEntityPlayerRelativeBlockHardness`。
于是：

- 忠实的映射 =「适正 → 3.0，不適正 → 1.0」（VARIANT-B），因为旧版不適正路径里 `getDigSpeed` 的贡献也是常数 1.0。
- VARIANT-A 会在不適正方块上给出 3.0，**比 1.7.10 实机快 3 倍**（石头：1.7.10 约 1 秒 → API 约 0.33 秒）。

## 3. 需要 K3 裁决的事实与不确定项（全部列在此）

1. **两案都能编译、都能独立套用**（§4 已验证）；差别只在玩家体感与“忠于字面/忠于实机”。
2. **一项无法本地证实的不确定点**：1.7.10 的 Forge `blockStrength` 是否**仍然是 `canHarvest ? 30 : 100`**，
   还是当年把 `getDigSpeed` 的返回值直接当成最终速度（那样“恒 3 vs 1 常数”才是关键差异）。
   1.12.2 的对应实现已逐字节证实（见 §2），1.7.10 的 Forge 二进制本机不可得。
   - 若 1.7.10 也是 30/100 分岐 → **VARIANT-B 正确**。
   - 若 1.7.10 的 `getDigSpeed` 结果被拿去和常数 1 比较（有些老 Forge 版本这么干）→ A/B 的差异就只是“3 vs 1”的绝对值，
     VARIANT-B 依然复现了“1.0”的那一半，仍然更接近实机。
   → **建议按 VARIANT-B 合并**；若要 100% 断言，需有人拿 1.7.10 Forge dev 环境反编译一次 `forge/.../ForgeHooks.class` 核对。
3. **`#sword_efficient` 的取舍**：B 案把草/叶/作物从 1.21.1 的 1.5 拉到 1.0（= 1.7.10 实机）。
   若不希望“比原版剑更慢”，可在 B 案基础上把 else 分支写成
   `event.getOriginalSpeed()`（保留原版 1.5/15.0 等）而不是 1.0 —— 这会变成第三种取向：“只把蜘蛛网压到 3.0”。
4. **副手**：两案都认副手（`getOffhandItem`）；1.7.10 只看主手 `getCurrentItem`。若要严格化，删掉副手判定。
5. **创造模式**：事件在创造模式也会发火，但原版创造模式走 `destroyAndAck` 的瞬破路径，不受影响。
6. **位置参数**：`event.getPosition()` 是 `Optional`（可能空），B 案用 `orElse(player.blockPosition())` 兜底；
   由于 `hasCorrectToolForDrops` 只依赖 `state` 与 `Tool`，位置的精确值不影响结果（`doPlayerHarvestCheck` 把它透传给 `HarvestCheck` 事件供其它 MOD 使用）。

## 4. 合并指令

```bash
# 二选一（类名相同，切勿同时拷）
X=A        # 或 B
cp porting/staging/polish-trio/variants/p10/VARIANT-$X/src/main/java/thkaguyamod/event/OnbashiraDigSpeedHandler.java \
   porting/neoforge-1.21.1/src/main/java/thkaguyamod/event/
cp porting/staging/polish-trio/variants/p10/VARIANT-$X/src/main/java/thkaguyamod/item/ItemOnbashira.java \
   porting/neoforge-1.21.1/src/main/java/thkaguyamod/item/
# THKaguyaMod.java 构造函数（PlayerDeathEventHandler 那行旁边）加：
#   NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());
#   以及 import thkaguyamod.event.OnbashiraDigSpeedHandler;
```

## 5. 验证

- 两案各自在隔离沙盒做过全树 javac（overlay `neoforge-1.21.1/src/main/java` 共 **261** 个 `.java`）：
  `p10/VARIANT-A -> exit=0 / errors=0`、`p10/VARIANT-B -> exit=0 / errors=0`（B 案的 `hasCorrectToolForDrops` 三参重载已实际参与编译）。
- 未实机验证：破坏时间比对。建议实机测 4 组：石头 / 土 / クモの巣 / 草，分别在 A、B 案下计时，
  并与 1.7.10 旧版录屏对照（这是唯一能最终定案 A/B 的手段）。