# soak-v2 变更说明（对应台账 P39）

交付物：`porting/staging/soak-v2/soak_test.sh`（合并时覆盖 `porting/tools/soak_test.sh`）。

## 改了什么

| # | 项 | 旧版 | soak-v2 |
|---|---|---|---|
| 1 | 闸门统计口径 | 只数 `[.*ERROR]` 行 | 额外统计 `Failed to summon` / `Unable to summon` / `IndexOutOfBounds` / `Exception`（排除认证噪声与栈帧行），全部纳入 FAIL 判定 |
| 2 | 逐条存活回执 | 无（只有一条 `say [soak] roster summoned`） | 每次 `summon` 后追加 `execute as @e[...] run say [soak] alive: <type>` 与 `execute unless entity @e[...] run say [soak] dead: <type>`；断言回执总数 = 36 且 `dead` = 0 |
| 3 | 名单维护 | 35 行硬编码 `summon` | `ROSTER` 数组单一来源（36 条），`EXPECTED_SUMMONS` / `EXPECTED_RECEIPTS` 由数组长度导出，不会再与台账口径脱节 |
| 4 | 杀进程语义 | `SmokeRunVmArgs` 精确匹配 | **完全不变**（两条 powershell 语句逐字保留） |
| 5 | 路径定位 | `cd "$(dirname "$0")/.."` | 向上查找仓库根（含 `porting/tools/smoke_test.sh` 的目录），从 `staging/soak-v2/` 直接运行也可用 |

## 为什么能抓住 I11（实证）

用主树当前 `run-smoke/logs/latest.log`（含 I11 的 18 次 `IndexOutOfBoundsException`）回放：

```
summon_failures=0 exceptions=36 index_out_of_bounds=18
receipts=0/36 (alive=0 dead=0) roster_summons=1/36
=> soak-v2 判定 FAIL（旧闸门判定 PASS）
```

I11 的异常由 `EntityType.loadStaticEntity` 记在 **WARN** 级（`Exception loading entity: ` + 栈），
且 `EntityTHFairy.setPattern` 的 `IndexOutOfBoundsException` 让 `summon` 命令本身**不报错**，
所以旧闸门的 `[ERROR]` 计数为 0、`roster summoned` 也照常打印。soak-v2 的三个新口径（异常头行数 /
回执条数 / `dead` 条数）各自都能独立判死。

## 未运行（按简报要求）

只做了 `bash -n` 语法自检与**数据包生成段的离线渲染**（把 `[0]` 段单独抽出执行）：

- `bash -n` → OK；
- 生成结果：`boot.mcfunction` 36 条 `summon` / 36 条 `alive` 回执 / 36 条 `dead` 回执，
  `pack.mcmeta` 与 `load.json` 正常；
- 未启动服务端、未执行 30 分钟浸泡。

## 合并前需 K3 确认

1. **名单从 35 加到 36**：台账（`integration-notes.md` §回归闸门、`RELEASE_NOTES.md`）写的是
   "36 实体 soak 数据包"，但旧脚本实际只有 35 条 `summon`（多出的第 36 条为 `thkaguyamod:cirno -14 90 14`，
   位置已查重、不与其它条目冲突）。若 K3 认为原口径是 35，删掉该行即可，计数会自动跟随。
2. `Exception` 计数器用的是"非栈帧、非认证噪声"口径（本机日志实测：39 行 → 36 行）。若合并后
   出现新的良性异常源，请往 `BENIGN_EXC` 追加，而不是放宽 FAIL 判定。
