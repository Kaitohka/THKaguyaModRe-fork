# VARIANT-A 的 `ModItems.java` 改动（唯一手改点）

目标文件：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/init/ModItems.java`
目标行：**114**（基线 336 行；若主树在合并前又改动，按下面的「查找串」定位而非行号）

## 查找串（1 行，原样）

```java
    public static final DeferredItem<ItemKaigo> ROD_OF_REMORSE = ITEMS.registerItem("rod_of_remorse", props -> new ItemKaigo(ModTiers.KAIGO, props), new Item.Properties().durability(2));
```

## 替换为

```java
    public static final DeferredItem<ItemKaigo> ROD_OF_REMORSE = ITEMS.registerItem("rod_of_remorse", props -> new ItemKaigo(ModTiers.KAIGO, props), new Item.Properties().stacksTo(1));
```

## 依据

- 组件化后 `durability(2)` 不再是语义载体（见 `../VARIANTS.md` §0 事实 1~3）。
- 去掉后 `ItemStack#isDamageableItem()` 为 false → 该物品**永不可损**，与 1.7.10 `setMaxDamage(0)` 等价。
- `stacksTo(1)` 保留「一次只带一把」的既有观感（`durability()` 原本隐式就是 1）。
- `SwordItem` 构造器仍会装入 `DataComponents.TOOL`，挖掘速度与 B 案完全相同。

## 不改的地方

- `ModTiers.KAIGO` **不动**（其 `efficiency 0.0F` 自 1.21.1 起就不参与 `SwordItem` 的速度计算——
  `SwordItem` 用 `createToolProperties()` 固定 1.0/1.5，tier 的 speed 只在 `DiggerItem` 路径生效）。
- `recipe/rod_of_remorse.json` **不动**（产出新栈 = 未使用态）。
- `EntitySukima` **不动**（只做 `is(ROD_OF_REMORSE)` 判定）。
