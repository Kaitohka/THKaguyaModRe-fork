# 阶段 3：核心注册系统迁移 — 验收记录

## 交付物

| 项 | 数量/内容 |
|---|---|
| 物品注册（`ModItems`，生成器产出） | **126 个** = 54 个原物品 + metadata 变体拆分（8 珍珠、4 银刀、2 护符、19 弹、3 激光、2 P 点、35 符卡） |
| 方块注册（`ModBlocks`） | 10 个：弹幕工作台 ×2（meta 拆分）、小神灵 ×8（颜色 meta 拆分）+ 全部 BlockItem |
| BlockEntity | `divine_spirit`（1.7.10 为空壳，保持最小实现，自毁逻辑留注释说明） |
| 创造模式物品栏（`ModCreativeTabs`） | 2 个：主标签（图标=蓬莱玉枝，含全部物品+方块）、符卡标签（图标=梦想封印，35 张） |
| 工具材质（`ModTiers`） | 5 个自定义（icicle/laevateinn/hakurouken/roukanken/hisou）+ vanilla WOOD/IRON 别名 |
| 防具材质（`ModArmorMaterials`） | 4 个（hinezumi_robe/marisa/kappa/suwako，NeoForge 签名：耐久移入物品属性） |
| 贴图 | 126 物品 + 10 方块 + 3 防具层 + 符卡等级星标 5 档，全部改为小写路径并归位到 1.21.1 目录规范 |
| 语言文件 | 5 份 .lang → JSON（en_us/ja_jp/ko_kr/zh_cn/zh_tw，各 138 键） |
| 配方 | **110 条 JSON**（102 常规 + 8 条原 config 门控的 `_bonus`），0 跳过，id 交叉校验通过 |
| 生成器 | `porting/tools/build_phase3_assets.py`（单一事实源，可重复执行） |

## 关键决策

- **注册名**：1.7.10 的 `stringHumpToUnderline` 有 bug（`SilverKnife`→`silverknife`、`YinYangOrb`→`yin_yangorb`，仅重复大写字母才加下划线）。因 modid 已变、存档不可迁移，采用**干净 snake_case**（`silver_knife_blue`、`yin_yang_orb`…）；新旧对照见 `porting/items_phase3.json`。
- **metadata 变体** → 独立物品/方块（1.21.1 惯例）。弹幕参数 NBT（Number/Speed/Color/Gravity/Special/Infinity/Charge 等）→ 阶段 4 转 `DataComponentType`。
- **符卡** → 35 个独立物品（`spell_card_<name>`），卡面贴图 + 等级星标 layer1 已在模型 JSON 体现；反射注册表待阶段 4 改工厂模式。
- **配方目录**：1.21.1 用 `data/<modid>/recipe/`（单数）。`recipelessItemsRecipe` 配置门控的 8 条转为常规 `_bonus` 配方（原作默认关闭；纳入以便这些物品可获取，文档化偏差）。
- 修复 1 个迁移冲突：`gap` 误设 stacksTo(1) 与配方产出 ×2 冲突 → 恢复默认 64（1.7.10 原样）。

## 阶段 1 登记的"移植即修复"落实

- 双命名体系（unlocalizedName vs 注册名）→ 统一 ✔
- 硬编码贴图大小写路径 → 全小写 ✔
- `EnumHelper` → SimpleTier/ArmorMaterial 注册表 ✔
- 创造栏数字 ID/硬编码标签 → `CreativeModeTab` 注册 + lang 键 ✔

## 验收自检

- [x] `gradlew build` 成功
- [x] `runClient` 进入主菜单，无 crash
- [x] 配方解析错误 **0**（修复 gap 后复验）
- [x] 模型缺失警告 **0**（126 物品模型 + 10 方块模型全部命中贴图）
- [ ] 创造栏逐一人工核对（需用户在游戏内确认：两个标签页、图标、中文名、贴图显示）
- [ ] 配方游戏内可用性抽查（合成表可见/可合成）

## 遗留（转入阶段 4/5）

1. 物品行为全部未迁移（右键/蓄力/tick 等，阶段 4 主体）。
2. 防具自定义模型（魔理沙帽/诹访子帽 ModelBiped）→ 阶段 4 客户端部分；当前用 vanilla 头盔模型 + layer 贴图占位。
3. 小神灵方块渲染：现为 cube_all 占位模型；原作 TESR 发光效果 → 阶段 4。
4. 弹幕工作台 GUI 打开逻辑 → 阶段 4（当前方块右键无反应属预期）。
5. 发射器行为、ChestGenHooks 战利品注入、村民交易 → 阶段 4。
6. 音效 4 个事件待注册（.ogg 缺失，用原版音效占位）→ 阶段 4。
7. `roukanSenpuuzin`/`ItemHoutou` 等 1.7.10 未注册/半成品内容按"不移植清单"跳过。
