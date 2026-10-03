# VARIANT-B 的 `ModItems.java` 改动：**无**

本变体**不触碰** `porting/neoforge-1.21.1/src/main/java/thkaguyamod/init/ModItems.java`。
第 114 行保持主树原样：

```java
    public static final DeferredItem<ItemKaigo> ROD_OF_REMORSE = ITEMS.registerItem("rod_of_remorse", props -> new ItemKaigo(ModTiers.KAIGO, props), new Item.Properties().durability(2));
```

## 保留 durability(2) 的后果（合并前请确认 K3 已知悉）

1. 物品仍是 `isDamageableItem() == true`（`MAX_DAMAGE` + `DAMAGE` 都在），
   一旦**将来**有代码调用 `hurtAndBreak`，第 2 次就会把棒子消耗掉（1.7.10 是不可损的）。
   当前全树无此类调用（`SwordItem.postHurtEnemy` 已被 `ItemKaigo` 覆写为空），所以是**潜在**而非现实风险。
2. 物品栏**不会**出现耐久条（`isDamaged()` 要求 damage > 0，组件化后无处写 damage）；
   只有 tooltip / F3+H 会显示 `0 / 2`。
3. 叠加数为 1（`durability()` 隐式 `stacksTo(1)`），与 A 案相同。
4. 挖掘速度与 A 案相同（`SwordItem` 固定装 `DataComponents.TOOL`，与 `MAX_DAMAGE` 无关）。

## 为什么仍要提供本变体

- 主树改动面最小（只加组件 + 替换物品类 + 3 个资源 + 1 行客户端接线），
  万一 `stacksTo(1)`/无耐久属性触发某种未预期的原版判定，B 案是**零风险回退位**。
- 与 A 案共用**完全相同**的 `ModDataComponents.java` / `ItemKaigo.java` / `RodOfRemorseClientProperties.java`
  / 两个模型 JSON / 贴图 → 两案之间切换只需改这 1 行，**不需要重做任何其它文件**。
