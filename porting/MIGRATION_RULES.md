# 迁移与现代化规则（项目级规范，全批次适用）

> 生效范围：THKaguyaMod 1.7.10 → 1.21.1 NeoForge 移植全程。
> 优先级：**现代化实现优先于逐行等价**；玩家可见行为保持一致。与旧简报中"忠实移植"表述冲突时，以本规则为准（行为不变，实现允许现代化）。

## R1 技术基线
Java 21 + MC 1.21.1 + NeoForge 21.1.252 + Mojmap/Parchment；构建脚本与元数据与目标版本一致。✔ 已落实（阶段 2）。

## R2 API 现代化
DeferredRegister / 统一事件总线 / NeoForge ModConfigSpec / Payload 网络包 / DataComponent（物品数据）与 Attachment/Capability（实体与方块数据，按语义选）。禁用 cpw.mods.fml 与已移除 API。✔ 已落实于注册/配置/组件；网络包在 GUI-2 批落地。

## R3 客户端/服务端隔离
公共代码禁止引用 Minecraft/Screen/Renderer 等 client-only 类（1.7.10 的 CommonProxy 就有此 bug，移植即修复）。客户端代码集中在 `thkaguyamod.client.*` 与 `@Mod(dist=CLIENT)` 入口；单端逻辑用 `level().isClientSide` 守卫。
**审查闸门**：合并任何批次前 grep 检查 `import net.minecraft.client` 只允许出现在 client 包。

## R4 兼容与行为边界
玩家可见行为默认一致；失效引擎特性用 1.21.1 等价机制重建，并在代码注释/迁移文档说明差异与迁移策略。旧存档不兼容已在阶段 1 声明，发布说明必须包含。

## R5 性能
- 已修复：特殊弹每 tick 反射 newInstance → 实例注册表；HenyoriLaser 每段 new dummy 实体 → 纯计算；THShotLib `new Random()` → ThreadLocalRandom。
- 待办：RenderTHShot FORM_KNIFE 每帧 new ModelSilverKnife（台账 P2）；homing() 每 tick 24 格全实体扫描+逐候选射线（阶段 5 评估节流）。
- 规则：高频路径禁不必要分配；网络按变化发送；渲染线程不做世界逻辑；逻辑线程不碰 GL。

## R6 稳定性
- 时停/隙间等跨实体改写必须判空与生命周期清理（实体移除时清引用）。
- 数据包 reload/服务端关闭无泄漏；异常不吞没，关键路径有日志。

## R7 代码质量
- 死代码不移植（清单见阶段 1 文档 §3-R6）；上帝类拆分以小步重构为准，不追求一步到位。
- 双命名体系已统一为注册名；静态可变全局状态（ModConfig 静态字段）保留但收敛于 sync() 单点写入。
- 测试：阶段 5 引入 GameTest（注册完整性、弹幕生成/命中、配方产出、数据组件编解码往返）。

## R8 验收标准（每阶段验收必查）
1. 客户端 **与专用服务端** 均能启动进入世界（smoke_test.sh 双模式）。
2. 无 common 引用 client-only 类（grep 闸门）。
3. 高频场景无明显 tick 卡顿/内存上涨/网络风暴（阶段 5 实测）。
4. 单人/局域网/专用服务端行为一致。
5. 配置可读取；旧存档不兼容的说明文档化。

## 对子代理简报的统一补充条款（即时生效）
1. 「忠实移植」指**行为等价**，不阻碍现代化替换（如 Optional 替代 null 惯例、enhanced-for、try-with-resources）。
2. 反射 hack、已移除 API、客户端类引用公共代码，一律替换为现代机制并注释说明。
3. 发现旧版 bug（自递归、off-by-one、服务端引用客户端类）：**修复并注释**，不得照搬。
4. 每 tick 分配热点、无缓存反射、字符串查表等低效逻辑：顺手修复并在报告中列出。
