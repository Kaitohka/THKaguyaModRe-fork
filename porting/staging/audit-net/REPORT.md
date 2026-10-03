# audit-net — 第三轮严重性普查：network / menu / client Screen 数据流

**范围**：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/`
- `network/`（2 个双向 Payload）
- `menu/`（4 Menu + 4 Slot）
- `client/gui/screen/`（4 Screen）

**只报**：P0（崩溃 / 数据损坏 / 物品复制 dupe / 核心功能失效）与 P1（疑似，需复现）。
**纪律**：只读，不改任何既有文件；禁 gradlew / git；不杀进程。
**对照基线**：1.7.10 原作 `src/main/java/thKaguyaMod/`（gui/container 对应类）。

## 逐文件结论（每读一个文件立即追加一行）

| 文件 | 严重性 | 结论 |
|---|---|---|
