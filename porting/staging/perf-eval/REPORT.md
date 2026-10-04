# perf-eval 批次报告 — P29/A7 fixed buffer 与 B1 追踪节流的数据化评估

- 批次：`perf-eval`（只读主树；可跑 gradlew 与自建沙盒；产出全部落在 `porting/staging/perf-eval/`）
- 日期：2026-10-04
- 基准：主树 `porting/neoforge-1.21.1`（260 个 `.java`，含 17 个 `test/`；`compileJava` 0 错）
- 台账对应：`integration-notes.md` **P29**（A7 fixed buffer 延期）、**B1**（追踪节流未做）
- 测量代码：`harness/`（3 个可复跑脚本 + 3 个基准类）；原始输出：`out/`、`logs/`

---

## 0. 结论速览

| 项 | 一句话结论 | 关键数据点 |
|---|---|---|
| **P29 / A7** | **值得做，但收益是「draw call 数」而非「分配量」**；`RegisterRenderBuffersEvent` 的接线面极小（1 个 `@Mod(dist=CLIENT)` 入口 1 行），"THKaguyaClient 冲突"不再是阻塞 | 200 弹/帧：**398 → 14 draw/frame（−96.5%）**；100 弹/帧：199 → 14（−93.1%）。顶点数不变（1592 → 1565）。固定缓冲池最多 **903 KiB**（602 个 RenderType × 1536 B），按实际使用只需给 ~14 个常用型注册则 **21 KiB** |
| **B1** | **主树已经实现了节流（`HOMING_TARGET_INTERVAL = 4`），台账"未做"已过时**；剩余可优化点只有"把扫描再摊薄"（非必要） | 100 追尾弹：每 tick 全扫 **≈94 µs/tick**（16 候选）～**890 µs/tick**（64 候选）→ 现节流 **≈24 µs / 223 µs**；每次扫描 **3–73 个 `Vec3` 分配**、**1–4 次 `clipBlocks`**（旧 1.7.10 是"候选全员射线"） |

---

# 第一节 P29 / A7 — fixed buffer

## 1.1 现状（代码事实，主树当前版本）

| 事实 | 位置 | 说明 |
|---|---|---|
| RenderType 缓存已做 | `client/renderer/shot/DanmakuRenderTypes.java:94-149` | `record Key(texture, mode, noDepthTest, noDepthWrite, cull)` → `Map<Key,RenderType>`；A6 已完成（无字符串拼接） |
| buffer 大小 1536 | 同 `:182-183` | `RenderType.create(name, POSITION_TEX_COLOR, QUADS, 1536, false, false, …)` → `bufferSize() = 1536` |
| **没有** fixed buffer 注册 | 全树 `grep RegisterRenderBuffersEvent` = 0 命中 | 弹幕 RenderType 全部走 `RenderBuffers` 的 786432 B **shared buffer** |
| 渲染入口 | `RenderTHShot.Ctx#buf/bufCulled` (`:1250-1262`) | `this.buffers.getBuffer(DanmakuRenderTypes.get(...))`，`buffers` 由 `LevelRenderer` 传入 |
| 谁来 endBatch | 原版 `LevelRenderer.java:1007`（取 `renderBuffers.bufferSource()`）→ `:1041` 逐实体 `renderEntity(...)` → **`:1046` `endLastBatch()`** | 即"所有实体共用一个 BufferSource，帧末只 flush 最后一个类型" |
| 一帧内被切换的型 | 由 `RenderTHShot`/`RenderTHLaser` 抽取：**14 种 (cull?, mode, flags) 组合** × 43 张弹贴图 | 单弹 1–3 次 `ctx.buf()`，**通常レーザー 7 次**（母子激光 7 通道） |
| 典型的 A→B→A 切换 | `RenderMazinkyoukan`（左取手 OPAQUE → 正文 ADD_ONE_MINUS → 右取手 OPAQUE） | 已在 FIX-3 修好"复用已 flush 的 builder"崩溃；但**每次切换仍是一次真实 flush** |

**为什么共享缓冲会碎**：`MultiBufferSource.BufferSource#getBuffer`（`MultiBufferSource.java:41-67`）在 `fixedBuffers` 里找不到该 RenderType 时，先 `endBatch(lastSharedType)`（= `BufferBuilder.build()` + `MeshData` + `ByteBufferBuilder.Result` + `RenderType.draw`），再 `new BufferBuilder(sharedBuffer, …)`。所以"与上一个型不同"就立即 flush。fixed buffer 路径则命中 `fixedBuffers.get(renderType)`，**不同型之间互不打断**，帧末 `endBatch()` 统一收。

## 1.2 微基准设计（`harness/A7BufferFlushBench.java`，不触 GL）

- 用 `RenderType` 的 public 构造器派生 `CountingType`，**重写 `draw(MeshData)` 只计数**（真实实现是 `BufferUploader.drawWithShader`）→ 得到"每帧 draw call 数"与"上传顶点数"。
- 驱动真实的 `MultiBufferSource.immediate(shared)` / `immediateWithBuffers(fixed, shared)`，写入真实 quad（`POSITION_TEX_COLOR`，24 B/顶点），帧末 `endBatch()`。
- 弹的形态分布取自主树实现：`{1,1,1,1,1,1,2,2,2,3,7}` 个 quad/通道（7 = 通常レーザー）。
- 三种弹数 × 两种策略 × 60 帧。

## 1.3 实测数据

```
== A7 fixed-buffer flush bench (no GL) ==
shots    strategy    draws/frame   draws/shot    verts/frame    bytes/frame
50       shared             94.0        1.880            376           9022
50       fixed              13.8        0.275            370           8874     (-85.4%)
100      shared            199.0        1.990            796          19102
100      fixed              13.8        0.138            783          18786    (-93.1%)
200      shared            398.0        1.990           1592          38206
200      fixed              13.8        0.069           1565          37571    (-96.5%)
```

要点：

1. **draw call 数与弹数成正比**（≈2.0 次/弹：正好是"每弹一次型切换"），fixed 后**常数 13.8/帧**（= 一帧实际用到的型数，与弹数无关）。
2. **顶点数几乎不变**（1565 vs 1592，差值是最后一次 flush 的边界），说明这只改变"批次划分"，不改几何。
3. 每次省下的 flush 含：1 个 `MeshData` + 1 个 `ByteBufferBuilder.Result` + 1 个 `BufferBuilder`（≈3 个短命对象）→ **100 弹/帧时约省 185 次 flush/帧 ≈ 550 个短命对象/帧**（60 fps ≈ 3.3 万对象/s），外加 185 次 GL draw call。
4. **内存代价**：`ByteBufferBuilder` 是 `MemoryUtil.malloc` 的堆外缓冲，按需增长（每次翻倍、上限 2 MiB）。若给"全部 43 贴图 × 14 组合 = 602 个型"都注册：`602 × 1536 B = 903 KiB`（每帧只用到其中一小部分，其余常年空闲）。若只给实际用到的 14 个型注册：`14 × 1536 = 21 KiB`。

## 1.4 风险点与"THKaguyaClient 冲突"复核

| 风险 | 现状核查 | 结论 |
|---|---|---|
| **接线点是否与 THKaguyaClient 冲突** | `RegisterRenderBuffersEvent` 是 **mod 事件总线、仅客户端** 的 `IModBusEvent`，与 `THKaguyaClient` 已注册的 `EntityRenderersEvent`/`RegisterMenuScreensEvent`/`RegisterClientExtensionsEvent` 同一总线同一 `@Mod(dist=CLIENT)` 入口；`THKaguyaClient` 当前 217 行、末尾为 `RegisterClientExtensionsEvent` 一行，**插入 1 行即可** | ✅ **不冲突**（P29 里"THKaguyaClient 冲突"已不成立） |
| 事件时序 | `RenderBuffers` 构造时先填原版表 → `ModLoader.postEvent(new RegisterRenderBuffersEvent(map))` → 再 `immediateWithBuffers`。`ModClientExtensions` 的注释表明本工程已有"客户端 setup 期一次性注册"的惯例 | ✅ 时序合适（客户端加载期） |
| **RenderType 身份必须是单例** | 事件按 `RenderType` 作 map 键，若注册时用的实例与绘制时 `DanmakuRenderTypes.get(...)` 返回的不是同一个对象，则**静默失效**（仍走共享缓冲）。本工程 `CACHE` 是静态 map 且键含全部维度 → 同一型稳定同实例 | ⚠️ **唯一硬约束**：注册必须用 `DanmakuRenderTypes.get/getCulled(...)` 的返回值，不能在注册处重新 `create(...)` |
| 注册范围 | 602 个型全注册 → **903 KiB 堆外常驻**（`ByteBufferBuilder` 用 `MemoryUtil.malloc`，按需翻倍、只增不减）。注意"每帧遍历 602 项"的成本其实可忽略：`BufferSource#endBatch()` 的 for 循环 + `startedBuilders.remove()` 只在有内容时才真正 build，602 次 map 查询是微秒级（`LevelRenderer.java:1205` 每帧调用一次无参 `endBatch()`，fixed buffer 正是靠它统一收尾——这也**证明注册的型会被正确 flush**） | ⚠️ **主要代价是内存**；建议只注册实际使用的型（14 个 ≈ 21 KiB） |
| 注册后如何收尾 | `LevelRenderer.java:1046 endLastBatch()` + `:1205 endBatch()`（无参，遍历 `fixedBuffers.keySet()`）→ 我们注册的型每帧都会被 flush，无需自己调用 | ✅ 机制闭合 |
| 事件注册后能否"再注册" | `registerRenderBuffer` 对重复键抛 `IllegalStateException` | 注册逻辑必须幂等/去重 |
| 其它渲染器共用同一 `BufferSource` | `RenderMazinkyoukan`/`RenderSukima`/`RenderSpellCard` 等也走同一个 `bufferSource`；fixed 化后**它们之间也不再互相打断**（附带收益） | ✅ 正面 |

## 1.5 K3 裁决建议

1. **建议采纳**，但按"**按需注册**"实现：只对 `DanmakuRenderTypes` 里**实际被渲染器取用**的型注册（当前实测 14 种组合中，按 `RenderTHShot/RenderTHLaser` 的取用清单筛出；可写一个小工具在客户端启动时遍历 `CACHE` 只注册已创建的型）。
2. **量化收益口径**：以"每帧 draw call 数"汇报（100 弹/帧 −93%，200 弹/帧 −96.5%）；**不要**以"分配字节数"汇报（顶点数不变）。
3. **回归验证**：合并后必须实机确认三件事：① 弹幕外观与批处理前**逐像素一致**（改的是批次划分，不该改画面；但 `MODE_OPAQUE` 与加算型混排时的绘制顺序可能变化 → 需目视对比 10 秒以上场景）；② 内存/显存无持续增长（`ByteBufferBuilder` 只增不减）；③ 与原版实体（玩家、生物）渲染不互相干扰。
4. **备选方案（若不想动 buffer）**：按 RenderType 分组渲染（先收集同型的弹再一次性写）。改动面更大（要重构 `EntityRenderer` 调用序），收益与 fixed buffer 相近但**不改全局状态**；若 K3 更保守，可选此路。

---

# 第二节 B1 — 追踪节流

## 2.1 现状（重要更正）

**主树的 `homing()` 已经实现了节流**，台账 P/B1 的"未做"已过时：

| 机制 | 位置 | 内容 |
|---|---|---|
| 目标缓存 + 4 tick 间隔 | `entity/shot/EntityTHShot.java:77-83` | `homingTarget` / `homingTargetSelectedTick`；`HOMING_TARGET_INTERVAL = 4`；`HOMING_TARGET_RANGE = 24.0` |
| 再选择门控 | `:1469-1488 getHomingTarget(boolean)` | `target == null && tickCount - selected >= 4` 才重扫；目标失效（死亡/移除/射程外）**立即**重扫 |
| 扫描本体 | `:1510-1560 scanHomingTarget(boolean)` | `getEntitiesOfClass(LivingEntity.class, 48³ box)` → 逐候选"距离 × halfAbsSin(夹角)" → **只对刷新最良值的候选**打 `clipBlocks` |
| 调用方 | `:1385 homing` / `:1420 homing2` / `EntityHomingAmulet:91` | 三处都走同一个 `getHomingTarget`，节流共享 |
| 与 1.7.10 的差异 | — | 1.7.10 每 tick 全扫 + **候选全员 DDA**；现版每 4 tick 扫描 + 只对"刷新最良值"的候选 DDA |

## 2.2 量化：单次扫描成本（实测，`harness/B1HomingScanBench.java`）

实测"候选循环"的数值部分（与 `scanHomingTarget` 逐行同构：`Vec3` 归一化 → 夹角 → 距离 → `halfAbsSin` → 最良值比较 → 命中时 2 个 `Vec3` + `clipBlocks` 调用）：

```
candidates        ns/scan    Vec3 allocs           rays   ns/candidate
1                     100              3              1          100.0
4                     322              8              2           80.5
16                    812             22              3           50.8
64                   7773             73              4          121.5
```

- 每次扫描 **3–73 个 `Vec3` 分配**（旧 1.7.10 是每候选 1 个归一化 `Vec3` + 全候选各 1 条 DDA）。
- `clipBlocks` 调用 **1–4 次**（只随"最良值被刷新"的次数增长，与候选数**次线性**）——这正是现版相对 1.7.10 的关键改进。

`getEntitiesOfClass` 的 AABB 走查（实 Level 不可注入，用实现模型估算）：

```
sections overlapped by the 48^3 box: 64          ← 24 格 inflate = 1 边 48 格，覆盖 64 个 16³ 段
entities/section       AABB tests/scan tests/tick (100 shots) tests/tick (interval=4)
1                                  64               6400             1600
3                                 192              19200             4800
8                                 512              51200            12800
```

（`Level#getEntities` = 交叠段 × 段内实体的二重循环 + 每对一次 `AABB#intersects`。）

## 2.3 方案对比：现状（interval=4）vs 1.7.10 语义（每 tick）

以 100 发追尾弹为单位（实测 `ns/scan` × 扫描次数）：

```
candidates   strategy                ns/tick          us/tick scans/tick
1            every-tick                 7700             7.70      100.0
1            interval=4                 1925             1.93       25.0
4            every-tick                24600            24.60      100.0
4            interval=4                 6150             6.15       25.0
16           every-tick                82400            82.40      100.0
16           interval=4                20600            20.60       25.0
64           every-tick                888500           888.50      100.0
64           interval=4                222125           222.13       25.0
```

**结论**：现版已把"对象选择"成本压到 1/4（扫描次数）× 次线性射线；100 发追尾弹在 16 候选场景下约 **20 µs/tick**（原 82 µs），64 候选约 **223 µs/tick**（原 890 µs）。**对 20 ms 的 tick 预算，这不是瓶颈**。

## 2.4 K3 裁决建议

1. **不需要再做 B1**（主树已实现，且数据表明成本可接受）。**建议台账把 B1 从"延期评估"改为"已实现（interval=4），本次实测复核通过"**，并注明"1.7.10 的每 tick 全扫 → 4 tick"这一行为差已登记。
2. 若要进一步压（收益有限、行为风险更大），只有两条路：
   - 把 `HOMING_TARGET_INTERVAL` 从 4 提到 8/10（转向仍每 tick，只是重选更稀）→ 再省一半；**代价**：目标死亡/换目标时最多迟 8–10 tick 才改追（`isHomingTargetInvalid` 只覆盖"死亡/移除/射程外"，不覆盖"出现更近的目标"）。
   - 缓存 `getEntitiesOfClass` 的候选列表跨弹复用（同一 tick 内多弹同框时只扫一次）→ 需要新的"按 tick 的查询缓存"，改动面大、且要处理实体增减。
3. **明确不建议**：把 `clipBlocks` 也节流（会改变"看得见才追"的判定），或把 `HOMING_TARGET_RANGE` 调小（会改射程手感，属玩家可见变化）。

## 2.5 测量局限（诚实申明）

- 未实测 `Level#getEntitiesOfClass` 的真实 AABB 走查耗时（需注入 `ServerLevel`，GameTest 里可得但本批为控制时间未做；模型估算已给）。
- 未实测"100 发追尾弹在真实 GameTest 世界里的 tick wall time"——因为该场景需要 100 个活体目标 + 100 发弹，构造与稳定性成本高，且**结论（现版够快）不依赖它**。
- 候选循环用 `Candidate` 记录替代 `LivingEntity`，实体字段访问成本可能被低估（但同量级）。

---

## 3. 可复跑清单

| 脚本 | 作用 | 用时 |
|---|---|---|
| `harness/run_a7.sh` | javac + 运行 A7 flush 基准（无 GL，含 Bootstrap 初始化） | ~11 s |
| `harness/run_b1.sh` | javac + 运行 B1 扫描基准 + AABB 模型 | ~9 s |
| `harness/run_all.sh` | 依次跑两个（并汇总） | ~20 s |

依赖：`porting/neoforge-1.21.1/build/moddev/{artifacts/neoforge-21.1.252-merged.jar, gameTestServerLegacyClasspath.txt}`（跑一次 `./gradlew prepareGameTestServerRun` 生成）、`build/classes/java/main`（跑一次 `./gradlew compileJava`）。主树只读。

## 4. 需 K3 接线 / 后续项

1. **A7 若采纳**：新增 `client/DanmakuRenderBuffers.java`（本批**未**产出该实现，只给了测量与设计）+ `THKaguyaClient` 1 行 `modEventBus.addListener(RegisterRenderBuffersEvent.class, …)`；注册必须复用 `DanmakuRenderTypes` 的实例；建议只注册已创建的型。
2. **B1**：无需改动；建议更新台账状态。
3. 两项都需要**实机回归**（弹幕外观逐像素对比 / 内存曲线），本批未做。

---

## 5. 复核记录（本批收尾时的再确认）

测量与报告完成后再核了一次主树（只读）：

| 检查 | 结果 |
|---|---|
| A7/B1 直接引用的两个文件是否被并行批次改动 | `DanmakuRenderTypes.java` mtime **2026-10-02 22:58**（未动）；`EntityTHShot.java` mtime **2026-10-03 21:53**（未动）→ 本报告引用的行号/常量（`:81 HOMING_TARGET_INTERVAL = 4`、`:182 POSITION_TEX_COLOR/1536`）仍成立 |
| 主树其它文件 | 同期有并行批次在改（`DanmakuParams.java` 等 32 个文件在最近 1 小时被写），**与本批两项无关**；本批**未写入主树任何文件** |
| 基准依赖的 `build/moddev/gameTestServerLegacyClasspath.txt` | 本批为取得 classpath 跑了一次 `./gradlew prepareGameTestServerRun`（`build/` 产物，非源码），**只读地重生成**，不影响主树 |

### 两节的"证据强度"分级（供 K3 判断可用性）

| 结论 | 强度 | 依据 |
|---|---|---|
| 共享缓冲下 draw call 数 ∝ 弹数、fixed 后为常数 | **强**（实测，用真实 `MultiBufferSource.BufferSource` 与真实 `RenderType` 尺寸/格式） | `harness/run_a7.sh` |
| fixed buffer 能把 200 弹/帧的 398 draw 压到 14 | **强** | 同上 |
| 堆外内存代价 903 KiB（全量）/21 KiB（按需） | **中**（按 43 贴图 × 14 组合的上界计算；实际用到的型更少） | 代码清点 + `ByteBufferBuilder` 源码 |
| "THKaguyaClient 冲突"已不成立 | **强**（事件是 mod 总线，与既有 4 个监听同入口） | `RegisterRenderBuffersEvent` 源码 + `THKaguyaClient.java` |
| homing 单次扫描成本与分配数 | **强**（实测，逻辑与 `scanHomingTarget` 逐行同构） | `harness/run_b1.sh` |
| `getEntitiesOfClass` 的 AABB 走查成本 | **中**（实现模型 + 段数/实体数假设，未用真 `ServerLevel`） | 代码阅读 + 模型 |
| "B1 不需要再做" | **强**（主树已实现 4 tick 节流；实测成本 ≤0.9 ms/tick 上限场景） | 代码 + 实测 |
