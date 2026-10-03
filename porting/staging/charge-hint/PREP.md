# charge-hint 批次 PREP — 早苗符卡「奇迹之力」不足提示接线

> 批次：charge-hint（子代理）
> 范围：**只读**主树与语言文件；产出全部落在 `porting/staging/charge-hint/`
> 对应台账：`porting/integration-notes.md` **P13**
> 结论：**守卫已定位、提示键 5 语言齐全（无缺失、lang 零写入）、补丁草稿已编译验证**；
> 默认开关/配置门控的建议已给出，最终裁决留 `// TODO K3-review`。

## 0. 产出物清单

| 文件 | 用途 |
|---|---|
| `porting/staging/charge-hint/PREP.md` | 本文件（主交付） |
| `porting/staging/charge-hint/charge-hint.patch` | 最小补丁（unified diff，`patch -p1` 可套；已实测套用后与暂存文件逐字节相同） |
| `porting/staging/charge-hint/thkaguyamod/danmaku/THKaguyaLib.java` | 补丁后的完整文件（最终包名 `thkaguyamod.danmaku`，与主树同路径，合并=拷贝覆盖） |
| `porting/staging/charge-hint/lang/*.fragment.json` × 5 | 键值权威快照（**非补丁**，见该目录 README.md） |
| `porting/staging/charge-hint/lang/README.md` | lang 所有权说明 + 键现状核对表 |

---

## 1. 充能守卫与静默失败点定位

### 1.1 唯一守卫点

`porting/neoforge-1.21.1/src/main/java/thkaguyamod/danmaku/THKaguyaLib.java:199`

```java
175  public static boolean checkSpellCardDeclaration(Level world, ItemStack itemStack, LivingEntity user,
                                                      int spellCardNumber, int level, boolean firstAttack)
181      if(user instanceof Player player)
184          if(!player.isCreative())
189              if(spellCardNumber >= SANAE_CARD_MIN && spellCardNumber <= SANAE_CARD_MAX)   // 14..18
192                  if(!(itemStack.getItem() instanceof ItemTHSpellCard card && card.getSpellCardID() == spellCardNumber))
194                      logFail(...); return false;      // 失败① 手持物与宣言卡不一致
199                  if(itemStack.getOrDefault(ModDataComponents.CHARGE.get(), 0) <= 0)
205                      logFail(...); return false;      // ★失败② 充能不足 —— 本批次目标分支（静默）
235          if((player.experienceLevel + bombItemNum) < needLevel)
237              logFail(...); return false;              // 失败③ 等级不足
```

相关常量与调用方：

| 位置 | 内容 |
|---|---|
| `THKaguyaLib.java:46-47` | `SANAE_CARD_MIN = 14` / `SANAE_CARD_MAX = 18`（与 `ItemTHSpellCard.java:44-45` 同值，**两处各写一份**） |
| `ItemTHSpellCard.java:91` | `use()` → `checkSpellCardDeclaration(level, stack, player, this.spellCardId, NORMAL, true)`（**唯一的玩家符卡入口**） |
| `ItemSpearTheGungnir.java:52` | 硬编码 `spellCardNumber = 27` → 在 14..18 之外，**不受充能守卫影响** |
| `ModDataComponents.java:28-32` | `CHARGE` = `DataComponentType<Integer>`（persistent `Codec.INT` + network `VARINT`），1.7.10 的 NBT `Charge` |
| `ModItems.java:177-181` | 5 张早苗卡各自带 `spellCardId` 14/15/16/17/18 |

### 1.2 充能的生产与消耗（守卫的输入从哪来）

| 环节 | 位置 | 行为 |
|---|---|---|
| 充能写入 | `ItemOharaibouS.java:143-145` `setMiraclePower()` | `stack.set(CHARGE, 1000)`（满值 1000） |
| 充能触发条件 | `ItemOharaibouS.java:89-113` | 按 `time`（五芒星数）与 `experienceLevel` 分 5 档，分别对应 id 18/17/16/15/14 的卡；未达标则**什么都不发生**（另一处静默点，见 §1.4） |
| 充能衰减 | `ItemTHSpellCard.java:102-111` `inventoryTick` | 每 tick `-1`，降到 ≤1 时整体移除组件；1000 → 约 1000 tick ≈ **50 秒** |
| 充能显示 | `ItemTHSpellCard.java:123-130` `appendHoverText` | 仅早苗卡显示 `thKaguya.msg.sneSc4 + (miracleTime/10) + "%"` |
| 发光反馈 | `ItemTHSpellCard.java:137-143` `isFoil` | 仅早苗卡且 `CHARGE > 0` 时发光 |

### 1.3 完整条件链（玩家右键一张早苗符卡）

```
ItemTHSpellCard.use()                        ItemTHSpellCard.java:87
 └─ THKaguyaLib.checkSpellCardDeclaration()  THKaguyaLib.java:175
     ├─ user instanceof Player ?                                   :181  否 → 跳过整段守卫（LivingEntity 非玩家无充能限制）
     ├─ !player.isCreative() ?                                     :184  创造模式 → 跳过守卫（**无充能也能宣言**）
     ├─ spellCardNumber ∈ [14,18] ?                                :189  否 → 跳过守卫
     ├─ 手持物 = 该 spellCardId 的 ItemTHSpellCard ?                :192  否 → logFail 失败①
     ├─ CHARGE 组件存在且 > 0 ?                                     :199  否 → ★静默失败②（1.7.10 同）
     ├─ (experienceLevel + bomb_item 总数) ≥ needLevel ?            :235  否 → logFail 失败③
     ├─ 64 格内本玩家无已展开符卡 ?                                  :304  否 → logFail 失败④
     ├─ 32 格视线命中合格生物（非 Animal/AbstractVillager）?         :311  否 → logFail 失败⑤
     └─ itemProssessionCheck()（恒 true，1.7.10 遗留空壳）           :449  恒真
          └─ !world.isClientSide → spellCardDeclaration()            :324
```

**要点**：①③④⑤ 已在 agent-32 批次补了 `debug` 日志（`logFail`，`THKaguyaLib.java:342`），
**只有 ②（本批次目标）玩家侧毫无反馈**，而它恰好是「右键没反应」最常见的成因之一
（玩家拿到卡但没先用驱魔棒充能）。

### 1.4 静默失败点全量清点（供 K3 判断"是否只修 ②"）

| # | 位置 | 玩家可见 | 现状 |
|---|---|---|---|
| ① 手持物不匹配 | `THKaguyaLib.java:194` | 静默 | debug 日志；正常玩法几乎不会触发（`ItemTHSpellCard.use` 传的就是自身 id） |
| ② **充能不足** | `THKaguyaLib.java:199-206` | **静默** | **本批次接线** |
| ③ 等级/炸弹不足 | `THKaguyaLib.java:235-239` | 静默 | debug 日志（有 §6 Q1 文档解释） |
| ④ 64 格内已展开 | `THKaguyaLib.java:304` | 静默 | debug 日志 |
| ⑤ 32 格内无合格目标 | `THKaguyaLib.java:311-317` | 静默 | debug 日志（**最高频**成因，§6 Q1 有文档） |
| ⑥ 驱魔棒蓄力未达标 | `ItemOharaibouS.java:79-115` | 静默（不充能） | 旧版即如此；`time < 30` 会退化为普通攻击「風起し」，玩家有可见反馈 |

本批次**只动 ②**（P13 裁决范围）。⑥ 属于另一个可选改进方向，见 §6「遗留」。

---

## 2. `thKaguya.msg.sneScCharge` 键核实

### 2.1 结论：**5 个语言全部存在，无缺失语言，lang 目录零写入**

| 语言 | 文件:行 | 值 | 与 `check_lang.py` 的 `GUI_NEW` 表（:134-136）比对 |
|---|---|---|---|
| en_us | `lang/en_us.json:273` | `Not enough Miracle Power!` | 一致 |
| ja_jp | `lang/ja_jp.json:273` | `奇跡の力が足りない！` | 一致 |
| ko_kr | `lang/ko_kr.json:273` | `기적의 힘이 부족합니다!` | 一致 |
| zh_cn | `lang/zh_cn.json:273` | `奇迹之力不足！` | 一致 |
| zh_tw | `lang/zh_tw.json:273` | `奇蹟之力不足！` | 一致 |

佐证（均为实测）：

- `python porting/tools/check_lang.py` → **exit 0**：`en_us/ja_jp/ko_kr/zh_cn/zh_tw 各 274 键、union 274`，
  键集完全相同，46 实体 / 126 物品 / 10 方块 / 35 符卡与所有 Java 字面量键覆盖。
- 1.7.10 原始 lang（`porting/assets-1.7.10/assets/thkaguyamod/lang/*.lang`）**只有** `sneSc1..sneSc4`，
  **没有** `sneScCharge` → 该键确实是移植期新写的，`check_lang.py` 的 `GUI_NEW` 表就是它的
  单一事实来源（与台账 P13「提示文案键缺失」的表述**不符**，见 §4.5）。
- `sneScCharge` 在 1.21.1 Java 树中**目前零引用**（唯一出现是 `THKaguyaLib.java:204` 的注释行）；
  补丁落地后它成为 `Component.translatable` 字面键，会被 `check_lang.py:249 literal_java_keys()`
  纳入检查——该键已在 5 文件中，**不会新增失败**。

### 2.2 为什么仍落了 5 个片段文件

按简报要求「缺哪个语言就落哪个语言的增补片段」，**实际无缺失**，故
`porting/staging/charge-hint/lang/*.fragment.json` 只作为键值快照与人工核对材料存在，
**不是待合并补丁**，合并方**不要**把它们 merge 进 lang JSON：

- 所有权约定（`check_lang.py` 头部注释 + 台账 **P28**）：lang JSON 由
  `build_phase3_assets.py` 生成，审计补齐内容由 `python porting/tools/check_lang.py --apply` 维护；
  该脚本 `apply_overlay()` 用 `setdefault` 写入（幂等、不覆盖既有值、`sort_keys=True` 重排）。
- 若某次生成器重跑把该键冲掉，**正确恢复方式**是 `python porting/tools/check_lang.py --apply`，
  而不是手工/脚本 merge 片段。
- 详细说明见 `porting/staging/charge-hint/lang/README.md`。

---

## 3. 最小补丁草稿

### 3.1 合并点（1 处）

| 项 | 值 |
|---|---|
| 文件 | `porting/neoforge-1.21.1/src/main/java/thkaguyamod/danmaku/THKaguyaLib.java` |
| 方法 | `checkSpellCardDeclaration(Level, ItemStack, LivingEntity, int, int, boolean)` |
| 分支 | `if(itemStack.getOrDefault(ModDataComponents.CHARGE.get(), 0) <= 0)`（当前第 199 行） |
| 替换范围 | 原第 201-204 行（4 行注释，其中 1 行是被注释掉的调用）→ 新 11 行 |
| 新增 import | **无**（`Component` 已在 `:7` 导入；`Player` 已在 `:14`） |
| 新增成员 | **无** |

### 3.2 补丁内容（`charge-hint.patch`）

```diff
--- a/src/main/java/thkaguyamod/danmaku/THKaguyaLib.java
+++ b/src/main/java/thkaguyamod/danmaku/THKaguyaLib.java
@@ -198,10 +198,19 @@
 					//旧版は「NBT が無い」「Charge <= 0」の双方を宣言失敗にしていたので、getOrDefault(…, 0) <= 0 で同値。
 					if(itemStack.getOrDefault(ModDataComponents.CHARGE.get(), 0) <= 0)
 					{
-						//1.7.10 には宣言失敗時のメッセージが無かったため、専用キーは存在しない。
-						//TODO K3-review: lang キー "thKaguya.msg.sneScCharge"（「奇跡之力」不足の警告）が未登録。
-						//キー追加までは旧版どおり無言で失敗させる（下の 1 行を有効化すれば表示に切り替わる）。
-						//player.displayClientMessage(Component.translatable("thKaguya.msg.sneScCharge"), true);
+						//1.7.10 はこの分岐を無言で通していた（旧版にメッセージ用キーは無い）。
+						//移植では「右键が無反応」の最頻原因がここだと判明したため、アクションバーへ
+						//文言キー thKaguya.msg.sneScCharge を出す。overlay=true なので通常チャットは汚さない。
+						//このメソッドは両側で走る（use() が両側で呼ばれる）ため、!isClientSide で
+						//サーバー側の 1 回だけに絞る＝L377/L404 の送信規約と同型、二重表示も防げる。
+						//専用サーバーでも効く: Player#displayClientMessage は ServerPlayer が override して
+						//actionBar パケットを送る（共通コードに client-only 参照は無い＝R3 準拠）。
+						//TODO K3-review: 既定 ON か配置門控かは未決。推奨=既定 ON で Other 節に切替キーを追加
+						//（理由と代替案: porting/staging/charge-hint/PREP.md §3.3）。下の if が門控の差込点。
+						if(!world.isClientSide)
+						{
+							player.displayClientMessage(Component.translatable("thKaguya.msg.sneScCharge"), true);
+						}
 						logFail(user, spellCardNumber, "no miracle charge on this Sanae card");
 						return false;//宣言失敗
 					}
```

API 对照（已用 `javap` 在 `neoforge-21.1.252-merged.jar` 上核对，非猜测）：

- `net.minecraft.world.entity.player.Player#displayClientMessage(Component, boolean)` 存在；
  参数语义 `overlay = true` → **动作栏（action bar）**，`false` → 聊天栏。
- 该方法在 `Player` 上是空实现（`Code: return`），由 `ServerPlayer` override 后转发到
  `sendSystemMessage(Component, boolean)` → 发 actionBar 包。
- **R3 合规**：`displayClientMessage` 属 common 侧类（`Player`），**不引入** `net.minecraft.client.*`；
  静态闸门不受影响。
- 对照主树既有规约：`THKaguyaLib.java:377/404` 的 `sendSystemMessage` 都在 `!world.isClientSide` 内，
  本补丁沿用同一模式，故加 `if(!world.isClientSide)`。

为什么加 `!world.isClientSide`（而不是裸调）：

1. `Item#use` 在**逻辑客户端与逻辑服务端各执行一次**（`ItemTHSpellCard.use` 两侧都会进
   `checkSpellCardDeclaration`），两侧都调会重复；且只有服务端的 `Player` 是 `ServerPlayer`，
   客户端的 `LocalPlayer` 会直接本地显示——服务端发包才是规范路径。
2. 单人（集成服务器）场景下，若不加守卫，客户端那份也会显示，与服务端下发叠加。
3. 与文件内既有发送规约一致（`:377`、`:404`），减少审查面。

### 3.3 默认开启 vs 配置门控 —— 建议（不做架构决定）

**建议：默认开启、暂不引入配置项**，并在补丁里留 `// TODO K3-review`。

理由（按权重）：

1. **不改判定结果**：补丁只在失败分支加一行反馈，不消耗资源、不改返回 `false`、不改任何数值，
   与「玩家可见行为一致」原则的冲突面最小（仅"多一行动作栏文字"）。
2. **1.7.10 的静默是缺失反馈，不是设计**：P13 之所以"保持静默"，唯一理由就是"文案键缺失"
   （`THKaguyaLib.java:202` 原文）。键已存在且 5 语言齐备，该前置条件已消失。
3. **配置项反而制造回归**：关掉后玩家又无从诊断「右键没反应」，等于把本批次解决的问题原样还给玩家；
   且 §4.3 配置对照表、`ModConfig.java`（静态字段 + `CFG_*` + `sync()`）、`USER_MIGRATION` §4 都要同步扩条目。
4. **动作栏而非聊天栏**：`overlay=true` 不写聊天记录、不触发聊天滚动、不打扰其他玩家；
   与 §3.4 里神子切换难度用聊天栏的既有做法区分开，不构成信息噪音。
5. **零运行时成本**：仅在守卫拦下时触发（玩家右键一张未充能卡），无每 tick 开销。

若 K3 要求「严格行为等价 + 可关」，**替代方案 A（推荐备选）**——`Other` 节配置门控：

```java
// ModConfig.java 三处改动（本批次未落盘，仅给草稿）
// 1) 静态字段区（:42-44 附近）
public static boolean showMiraclePowerWarning = true;
// 2) CFG_* 声明区（:71 附近）
private static final ModConfigSpec.BooleanValue CFG_MIRACLE_POWER_WARNING;
// 3) 静态块 Other 节（:105-107）
CFG_MIRACLE_POWER_WARNING = B.define("Show Miracle Power Warning", true);
// 4) sync()（:137 附近）
showMiraclePowerWarning = CFG_MIRACLE_POWER_WARNING.get();
```

调用点相应变为 `if(!world.isClientSide && ModConfig.showMiraclePowerWarning) { ... }`
（`ModConfig` 已在该文件 `:25` 导入，无需新增 import）。
代价：`USER_MIGRATION.md` §4.3 需加 1 行（旧版无此键 → 新键 `Show Miracle Power Warning`，默认 true），
且 RELEASE_NOTES 的配置项数量口径需复核。

**替代方案 B（不推荐）**：每张卡/每玩家只提示一次（需玩家持久数据或 cooldown 状态），
增加状态与网络面，收益不抵成本。

### 3.4 补丁验证（本批次实测，全部通过）

| 验证 | 命令 | 结果 |
|---|---|---|
| 补丁可套用 | `patch -p1 --dry-run < charge-hint.patch`（在只有主树 `THKaguyaLib.java` 的临时目录） | `checking file ...`，**exit 0** |
| 套用结果与暂存文件一致 | `patch -p1` 后 `diff` 与 `staging/charge-hint/thkaguyamod/danmaku/THKaguyaLib.java` | **逐字节相同** |
| 全树编译 | 主树 253 个 `.java` + 暂存覆盖文件，用 `neoforge-21.1.252-merged.jar` + `clientLegacyClasspath.txt`（`compile_staging.sh` 同款 classpath，仅补丁文件去重后直跑 `javac`） | **exit 0，0 错误**（`THKaguyaLib` 相关 0 条） |
| lang 闸门 | `python porting/tools/check_lang.py` | **exit 0**（274 键 × 5 语言，键集一致） |

> 说明：`bash porting/tools/compile_staging.sh charge-hint` 会报 `类重复: thkaguyamod.danmaku.THKaguyaLib`
> —— 该脚本把**主树 + 暂存目录整份**一起编译，而本批次的暂存文件是"主树同名文件的替换版"（不是新增类），
> 与 `charge_guard`/`spellcard_fix` 等历史批次的用法不同。因此改用等效手工流程：
> 把补丁后的文件覆盖到临时拷贝的主树上，用脚本同一 classpath 编译 253 个文件 → 0 错误。
> **合并方式应为"拷贝覆盖"，不是"新增文件"**（见 §3.5）。
> 该脚本的 `staging_blocked.txt` 未覆盖 `ItemCursedDecoyDoll`（18 条"找不到符号"，基线同样存在），
> 与本批次无关。

### 3.5 合并步骤（给主代理）

1. `cp porting/staging/charge-hint/thkaguyamod/danmaku/THKaguyaLib.java porting/neoforge-1.21.1/src/main/java/thkaguyamod/danmaku/THKaguyaLib.java`
   （**覆盖**，不是新增；或 `cd porting/neoforge-1.21.1 && patch -p1 < ../staging/charge-hint/charge-hint.patch`）
2. 无需改 lang、无需改 `ModConfig`（默认开启方案）。
3. 台账：更新 `integration-notes.md` P13 行（见 §4.5）。
4. 回归：`./gradlew runGameTestServer`（现有 `SpellCardDeclarationTest.sanaeCardWithoutChargeFails`
   仍应通过——补丁不改返回值）+ 双模式 smoke（`porting/tools/smoke_test.sh`）。
5. **建议追加 GameTest**（可选，本批次未落盘）：断言 `sanaeCardWithoutChargeFails` 场景下
   mock player 收到动作栏消息。注意 `GameTestHelper.makeMockPlayer` 返回的是 `Player`
   （`Player#displayClientMessage` 是**空实现**，不转发、无记录），**无法**直接断言；
   需要 `helper.makeMockServerPlayerInLevel()`（存在，已用 `javap` 核对）或改为断言
   `logFail` 日志/用 `ServerPlayer` 的 `connection` 侧通道。此项成本不低，建议 K3 决定是否值得。

---

## 4. 文档同步清单（启用后；本节只列建议措辞，**未修改任何文件**）

前提：采用 §3.3 的**默认开启**方案（无配置项）。若改走配置门控，§4.3 需额外加一行配置对照。

### 4.1 `USER_MIGRATION.md` §3.4 玩法机制表（现 146-157 行）— **新增一行**

现表无充能提示相关条目（该行为只在 §6 Q2 与 §5 第 15 条描述），建议在表末（第 157 行 `| 隙间的染色 |` 之后）追加：

```markdown
| 早苗符卡的**充能不足反馈** | 充能为 0 时右键**完全无反应**，没有任何提示 | 右键时在**动作栏**显示一行「奇迹之力不足！」（不写聊天记录） | 旧版缺少反馈属于实现缺口；文案键在移植期新写，本次接通显示 |
```

### 4.2 `USER_MIGRATION.md` §5 第 15 条（现第 251 行）— **改写，不删条**

现文：

```markdown
| 15 | **早苗符卡充能不能宣言时没有提示**（沿用旧版行为） | 对着生物右键没有反应时不知道原因 | 见 §6「早苗的符卡为什么用不了」 |
```

建议改为：

```markdown
| 15 | **早苗符卡充能不足时只有动作栏一行提示**（旧版完全无提示） | 提示会一闪而过，聊天栏里查不到；若动作栏被其他消息占用可能没注意到 | 提示文案「奇迹之力不足！」；充能与使用流程见 §6「早苗的符卡为什么用不了」。**注意这是与旧版的可见差异**（新版更友好） |
```

> **强烈建议保留条号**：`RELEASE_NOTES.md` 的「§5 已知问题 22 条」（第 10、58 行）与
> `USER_MIGRATION.md:327`（§6 Q11 引用「§5 第 22 条」）都依赖现有编号。删掉第 15 条会让
> 16..22 全部上移一位，至少 3 处交叉引用失效。**改写措辞、保留编号**是最小改动。

### 4.3 `USER_MIGRATION.md` 附录 A 第 9 条（现第 363 行）— **改写为已拍板**

现文：

```markdown
9. **早苗符卡"奇迹之力不足"提示**：语言文件里已存在提示文本键，但代码仍按旧版静默失败；确认是否改为显示提示（属行为差异，需在本文 §3.4/§5 同步更新）。
```

建议改为：

```markdown
9. ~~**早苗符卡"奇迹之力不足"提示**~~ **已拍板**：语言文件里的提示键 `thKaguya.msg.sneScCharge`（5 语言齐备）已接线到
   `THKaguyaLib.checkSpellCardDeclaration` 的充能守卫失败分支，以**动作栏**（`displayClientMessage(..., true)`）显示；
   默认开启、无配置项（若后续改为配置门控，请在此补记键名与默认值）。§3.4 与 §5 第 15 条已同步更新。
```

### 4.4 `USER_MIGRATION.md` §6 FAQ Q2（现第 271-283 行）— **改写首段**

现文：

```markdown
**Q2. 早苗的符卡为什么用不了？提示也没有。**
早苗系的 5 张符卡（…）需要**「奇迹之力」充能**才能宣言；充能为 0 时宣言会**静默失败**（旧版同样没有报错提示，新版保持一致）。
```

建议改为：

```markdown
**Q2. 早苗的符卡为什么用不了？**
早苗系的 5 张符卡（…）需要**「奇迹之力」充能**才能宣言；充能为 0 时宣言会失败，
新版会在**动作栏**显示一行「奇迹之力不足！」（旧版完全没有提示，这是新版新增的反馈）。
若没看到这行提示，说明失败原因不是充能，而是目标/等级问题——见 Q1。
```

Q2 后半的充能方法表格、发光说明、50 秒衰减说明**保持不变**。

### 4.5 额外同步项（简报未列，但不同步会造成文档自相矛盾）

| 文件:位置 | 现状 | 建议 |
|---|---|---|
| `porting/integration-notes.md:34`（P13 行） | "提示文案键缺失→保持旧版静默失败，待补键后可启用 displayClientMessage" | **事实错误**：键早已存在（5 语言，`check_lang.py` 的 `GUI_NEW` 表）。改为：`✅ 已落实并合并；提示键 thKaguya.msg.sneScCharge 早已存在于 5 语言（P28 overlay 维护），charge-hint 批已接线到动作栏；默认开启` |
| `porting/RELEASE_NOTES.md` §5（现 47-52 行） | 只列了 I7 修复 | 追加一条：「早苗符卡充能不足提示接线（可选反馈，不改判定）」；§6.2 的「已知问题 22 条」口径**不变**（因 §5 第 15 条是改写而非删除） |
| `porting/neoforge-1.21.1/.../item/ItemOharaibouS.java:40-43` Javadoc | "符卡側の Charge ガードは未確定のため、書き込んだ値が実際に宣言判定へ効くかは符卡側の対応待ち" | 守卫早已落地（P13），该 TODO 已过期；建议改为「CHARGE 守卫见 THKaguyaLib.checkSpellCardDeclaration（早苗 meta 14..18）」 |
| `porting/neoforge-1.21.1/.../test/SpellCardDeclarationTest.java:25-41` 类 Javadoc | 第 2 条把"消耗校验"与 CHARGE 守卫并列描述 | 可在第 3 条补一句"充能不足时新版在动作栏提示"，非必须 |
| `porting/staging/charge_guard/`（旧暂存） | 是 `THKaguyaLib` 的**合并前**快照（无 `logFail`、无 `THKaguyaMod` import） | 仅作历史留档，勿再合并（会把 agent-32 的诊断日志回退掉） |

---

## 5. 风险与遗留

1. **动作栏遮挡**：`overlay=true` 的消息可能被同帧其他动作栏消息顶掉（如物品切换提示）。
   若 K3 认为必须"绝对可见"，备选是 `overlay=false`（聊天栏），但会污染聊天记录，**不推荐**。
2. **提示粒度**：只在"充能 ≤ 0"时提示，不显示剩余百分比。
   若要更友好（如"奇迹之力不足（还剩 30%）"），需要新增带占位符的键 —— **超出本批次范围**，
   且会打破 `check_lang.py` 的 `GUI_NEW` 单表维护方式（需新增键 + 5 语言译文）。
3. **驱魔棒侧的静默（§1.4 ⑥）**：玩家用驱魔棒蓄力但未达任一档时，棒子不会有任何反馈
   （`ItemOharaibouS.java:79-115` 全为 `if/else if`，无 else）。这是另一个"无反应"来源，
   **本批次未处理**；若后续要做，建议同一批文案体系（如"蓄力不足"）。
4. **`CHARGE` 组件被两个语义复用**：`ItemHouraiJeweledBranch`（玉枝的"累计消耗量"，`:129-141`）
   与 `ItemTHSpellCard`（奇迹之力，`:105-110`）共用同一个 `ModDataComponents.CHARGE`。
   两者在不同物品上，当前无冲突，但**若将来给"充能"做统一 HUD/渲染器会串味**。
   已在 `ModDataComponents.java:27` 注明用途，建议 K3 知悉（本批次不改）。
5. **`ItemTHSpellCard.appendHoverText` 仍用旧键体系**：`:126-128` 用 `thKaguya.msg.sneSc1..3`
   （1.7.10 遗留键，已存在于 5 语言），与本批次无关，但属附录 A 第 19 条（符名/文案键统一）范围。
6. **`logFail` 无 `isClientSide` 守卫**：单人集成服务器下每次失败会记 2 条 debug 日志
   （客户端线程 + 服务端线程各一份）。既有行为，本批次未改（debug 级，无实际影响）。

---

## 6. 一页速查

- **守卫**：`THKaguyaLib.java:199`，条件 `spellCardNumber ∈ [14,18] && !creative && CHARGE ≤ 0`，
  与 `:192`（手持物一致）、`:184`（非创造）共同构成前置链。
- **键**：`thKaguya.msg.sneScCharge` 在 5 语言第 273 行**全部存在**，值与 `check_lang.py` 的
  `GUI_NEW` 表一致 → **无缺失、lang 零写入**。
- **补丁**：`charge-hint.patch`，替换 4 行注释为 11 行（含 `if(!world.isClientSide)` 包裹的
  `player.displayClientMessage(Component.translatable("thKaguya.msg.sneScCharge"), true)`），
  无新 import、无新成员；已实测**套用一致 + 全树 javac 0 错误 + check_lang exit 0**。
- **建议**：默认开启（理由见 §3.3），留 `// TODO K3-review`。
- **文档**：改 §3.4（加 1 行）、§5 第 15 条（改写**保留编号**）、附录 A 第 9 条（标为已拍板）、
  FAQ Q2（改写首段）；另需修 `integration-notes.md` P13 行的事实错误。
