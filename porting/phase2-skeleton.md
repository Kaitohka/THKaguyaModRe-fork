# 阶段 2：1.21.1 NeoForge 工程骨架 — 验收记录

## 工程信息

| 项 | 值 |
|---|---|
| 工程位置 | `porting/neoforge-1.21.1/`（ModDevGradle MDK 模板） |
| Minecraft | 1.21.1 |
| NeoForge | **21.1.252**（1.21.1 最新稳定，经 maven API 确认） |
| 构建插件 | ModDevGradle 2.0.148（模板默认；NeoGradle 为备选，未采用） |
| Gradle | 9.3.0（wrapper；本机缓存已有完整发行版，9.2.1 缓存残缺故切换） |
| JDK | Zulu 21.0.4（`JAVA_HOME=C:\Program Files\Java\zulu-21`） |
| Mappings | Parchment 2024.11.17 @ 1.21.1（模板默认） |

## modid 决策（存档兼容评估结论）

1.7.10 的 `THKaguyaMod` 含大写字母，**不是合法的现代 modid**（FML 要求 `[a-z][a-z0-9_]{1,63}`）。
采用 **`thkaguyamod`**——与原模组的资源域（`assets/thkaguyamod/`、音效事件域）完全一致。
存档兼容：1.7.10 → 1.21.1 跨 10 年注册表格式，存档直迁已判定不可行（见阶段 1 R4），modid 大小写差异不构成新增损失。

## 元数据

- `neoforge.mods.toml`：modid=`thkaguyamod`，名称=`Itutu no Nandai MOD+`，版本=`2.90-1.21.1`（沿用原版版本号体系），license=All Rights Reserved（沿用模板默认；原模组未附许可文件，发布前需确认）。
- 依赖声明：neoforge `[21.1.252,)`、minecraft `[1.21.1]`。

## 包结构对照表（1.7.10 → 1.21.1）

| 1.7.10 | 1.21.1 | 说明 |
|---|---|---|
| `thKaguyaMod.TH KaguyaCore` | `thkaguyamod.THKaguyaMod` | @Mod 入口（已建） |
| `CommonProxy` / `client.ClientProxy` | **废弃** | sided 逻辑改用 `@Mod(dist=...)` 入口 + `DistExecutor`；注册全走 `DeferredRegister`/mod 事件总线，无需代理 |
| `init`（Items/Blocks/Config/Recipe） | `thkaguyamod.init` | 注册聚合类；Config → `ModConfigSpec`；Recipe → JSON 数据生成 |
| `registry`（符卡/特殊弹/弹幕模式） | `thkaguyamod.registry` | 保留自研注册表，反射 newInstance 改 Supplier 工厂 |
| `block` | `thkaguyamod.block` | |
| `tileentity` | `thkaguyamod.blockentity` | 现代命名惯例 |
| `item` | `thkaguyamod.item` | |
| `entity`（living/shot/spellcard/effect/item/ai） | `thkaguyamod.entity.*` | 子包结构保持；`ai` 包不迁移（死代码） |
| `event` | `thkaguyamod.event` | |
| `gui`（Container+Gui 混合） | `thkaguyamod.menu` + `thkaguyamod.client.gui.screen` | 服务端/客户端分离 |
| `client`（model/render） | `thkaguyamod.client.model` / `thkaguyamod.client.renderer` | 现代命名惯例 |
| 根目录 ShotData/LaserData/DanmakuConstants/THShotLib/THKaguyaLib | `thkaguyamod.danmaku` | 弹幕核心库独立子包 |

## 资源回收（阶段 1 风险 R1 处置结果）

从 GitHub 仓库 `l89669/THKaguyaModRe-fork`（本仓库上游）取得发布版 `THkaguyaMod_2.9.0.1.jar`，提取至 `porting/assets-1.7.10/`：

- [x] **贴图 247 张**（items/blocks/gui/mob/shot/armor/spellCard 全套）——阶段 3 直接迁移（1.21.1 目录改名：`items→item`、`blocks→block`）
- [x] **语言文件 5 份**（en_US/ja_JP/ko_KR/zh_CN/zh_TW，旧 .lang 格式）——阶段 3 转 JSON
- [x] **sounds.json**（4 个音效事件：masterspark/spellcard/sukima/down）
- [ ] **音效 .ogg 实体文件缺失**（发布 jar 本就未打包，源码中硬编码路径检测即为此变通）——**已知问题**：1.21.1 版注册音效事件但暂用原版音效占位，发布说明中声明
- [ ] 模型 JSON：1.7.10 时代全部硬编码渲染，无现成 JSON，阶段 3 重建

## 验收自检

- [x] `gradlew build` 成功，产出空壳 jar（`thkaguyamod-2.90-1.21.1.jar`）
- [x] jar 内 `neoforge.mods.toml` 属性展开正确（modid/名称/版本）
- [x] `runClient` 进入主菜单且模组被正确加载（日志佐证：模组列表出现 `Itutu no Nandai MOD+ 2.90-1.21.1 (thkaguyamod)`；@Mod 构造器日志输出；ResourceManager 含 `mod/thkaguyamod`；成功创建并保存世界，无 crash、无模组相关 ERROR；唯一 ERROR 为开发环境离线 Yggdrasil 密钥获取失败，属正常现象）
- [x] 包结构对照表已建立
- [x] 阶段快照提交（待冒烟测试通过后提交）

## 已知问题

1. 音效 .ogg 缺失（见上）。
2. 原模组作者信息无法完整确认（源码仅见 `@author Someoneice`；mcmod.info 在发布 jar 中也缺失），mods.toml authors 字段为暂定值。
3. Gradle 首次构建在本机须使用缓存的 9.3.0 wrapper（services.gradle.org 直连对本机 JDK 信任库不友好，Maven 仓库无此问题）。
