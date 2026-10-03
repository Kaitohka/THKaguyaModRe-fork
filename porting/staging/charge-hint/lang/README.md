# charge-hint / lang 片段说明（所有权：`porting/tools/check_lang.py`）

本目录 5 个 `*.fragment.json` **不是需要合并的补丁**：`thKaguya.msg.sneScCharge` 键在
5 个语言文件中**已经存在**（每个文件第 273 行，值见下），本批次**没有缺失语言**，
因此对 lang 目录**零写入**。

这些片段只做两件事：

1. 记录「最终值」的权威快照（= `check_lang.py` 的 `GUI_NEW` 表，第 134-136 行）；
2. 万一 `build_phase3_assets.py` 重跑把该键冲掉（台账 P28 的已知冲突），
   用 `python porting/tools/check_lang.py --apply` 可确定性恢复——该键的 overlay 表就是
   单一事实来源，片段文件仅作为人工核对用。

## 键现状核对（本批次实测）

| 语言 | 文件行 | 值 | 核对结果 |
|---|---|---|---|
| en_us | `lang/en_us.json:273` | `Not enough Miracle Power!` | 与 `GUI_NEW` 表一致 |
| ja_jp | `lang/ja_jp.json:273` | `奇跡の力が足りない！` | 一致 |
| ko_kr | `lang/ko_kr.json:273` | `기적의 힘이 부족합니다!` | 一致 |
| zh_cn | `lang/zh_cn.json:273` | `奇迹之力不足！` | 一致 |
| zh_tw | `lang/zh_tw.json:273` | `奇蹟之力不足！` | 一致 |

`python porting/tools/check_lang.py` 当前 **exit 0**：5 语言各 274 键、键集完全相同、
注册表与 Java 字面量全部覆盖。启用补丁后 `literal_java_keys()` 会把这句
`Component.translatable(...)` 纳为「必须存在的字面键」——该键已在 5 文件中，
故**不会**新增失败，无需任何 lang 变更。

## 不要做的事

- 不要用脚本批量 merge 这些片段（会重写整个 JSON、打乱 `sort_keys=True` 缩进与既有键序）；
- 不要手改 `assets/thkaguyamod/lang/*.json` 的键集合——生成器重跑会覆盖（P28 裁决）。