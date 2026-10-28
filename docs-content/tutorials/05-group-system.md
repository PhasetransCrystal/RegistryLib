---
sidebar_position: 5
title: Group System
description: Share defaults across multiple entries with the Group system.
---

# Group System

## What You Will Learn

In this tutorial you will learn how to use `Group` to share default settings —lang prefixes, creative tabs, block/item properties, and tags —across a family of related entries. By the end you will be able to:

- Create a `Group` with shared defaults.
- Register multiple blocks and items through that group.
- Override group defaults on individual entries when needed.
- Apply tags to every entry in a group with a single call.

:::note
If you are only registering one isolated entry, using `RegistryCore` directly is usually simpler. Group adds value through **reuse**, not by replacing every entry point.
:::

## Step 1 —Decide Whether You Need a Group

`Group` is a layer of shared defaults wrapped around `RegistryCore`. The question it solves is not "can this be registered?" but "should this content family keep repeating the same configuration?"

Ask yourself:

- Do several entries share the same lang prefix (e.g. "Timer", "Machine")?
- Should they all appear in the same creative tab?
- Do they need identical base block or item properties?

If you answered **yes** to at least two, a Group will save you repetition. If not, register entries directly through `RegistryCore`.

## Step 2 —Create the Group

Call `.group(name)` on your `RegistryCore` instance, configure the shared defaults, and finish with `.build()`:

```java
public static final Group TIMER_GROUP = REGISTRYLIB.group("timers")
        .langPrefix("Timer")
        .blockProperties(p -> p.strength(5.0F, 6.0F))
        .build();
```

Every block registered through `TIMER_GROUP` will now inherit the `"Timer"` lang prefix and `strength(5.0F, 6.0F)`.

## Step 3 —Register Entries Through the Group

Use the group instead of `RegistryCore` as the starting point:

```java
public static final BlockEntry<TimerBlock> TIMER_TIER_1 = TIMER_GROUP
        .block("tier_1", p -> new TimerBlock(p, 1))
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .simpleItem()
        .register();
```

`tier_1` automatically inherits the lang prefix and the block property modifier from the group.

## Step 4 —Understand What Gets Applied Automatically

Here is the full list of defaults that a Group can apply:

| Default | Applies to |
| --- | --- |
| `langPrefix` | Blocks, regular Items, and Fluids created through that Group |
| `tab` | BlockItems, regular Items, and fluid buckets |
| `initialBlockProperties` | All Blocks registered through that Group |
| `blockProperties` | All Blocks registered through that Group |
| `initialItemProperties` | All regular Items registered through that Group |
| `itemProperties` | All regular Items registered through that Group |
| `addBlockTag(...)` | All Blocks registered through that Group |
| `addItemTag(...)` | All Items (and BlockItems) registered through that Group |
| `addFluidTag(...)` | All Fluids registered through that Group |

### Override Order

Configuration is applied when a Builder is created: Core defaults, built-in Group defaults, Group configuration callbacks, then your per-entry chain. Later property assignments can override earlier values, while additive operations such as tags accumulate:

```java
public static final BlockEntry<TimerBlock> TIMER_TIER_3 = TIMER_GROUP
        .block("tier_3", p -> new TimerBlock(p, 3))
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)  // overrides group's initial properties
        .properties(p -> p.strength(50.0F, 1200.0F))       // overrides group's strength
        .simpleItem()
        .register();
```

## Step 5 —Set Initial Properties Across the Group

`Group.Builder` exposes `initialBlockProperties(...)` and `initialItemProperties(...)` for setting the base property template:

```java
public static final Group MACHINE_GROUP = REGISTRYLIB.group("machines")
        .langPrefix("Machine")
        .initialBlockProperties(Blocks.IRON_BLOCK)
        .blockProperties(p -> p.strength(5.0F, 6.0F))
        .build();
```

`initialBlockProperties` accepts either a `Block` directly or a `Supplier<? extends Block>`. It is applied **before** `blockProperties` modifiers, so the modifier always has a consistent base.

## Step 6 —Apply Tags Across the Group

Use `addBlockTag(...)`, `addItemTag(...)`, and `addFluidTag(...)` to tag every entry of the corresponding type. Multiple tags can be passed in a single call, and calls are cumulative:

```java
public static final Group ORE_GROUP = REGISTRYLIB.group("ores")
        .langPrefix("Magic")
        .tab(MY_TAB)
        .addBlockTag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .addItemTag(ItemTags.DURABILITY_ENCHANTABLE)
        .build();
```

Tags are injected at the same time as other defaults —before any per-entry chain. A per-entry `.addTag(...)` call can always add more tags on top.

:::tip
For more detail on per-entry tags and recipe datagen, see the [Recipes and Tags](./recipes-tags) tutorial.
:::

## Step 7 —Reuse Builder Configuration

Use configuration callbacks when the fixed Group options do not cover a repeated Builder operation:

```java
Group machines = REGISTRYLIB.group("machines")
        .configureBlock(block -> block
                .properties(p -> p.strength(5.0F))
                .defaultLoot())
        .configureItem(item -> item.properties(p -> p.stacksTo(16)))
        .build();

machines.item("machine_part", Item::new)
        .properties(p -> p.stacksTo(4))
        .register();
```

The item above has a stack size of 4. Other Items created through `machines` have the Group's stack size of 16.

| Callback | Applied to |
| --- | --- |
| `configureBlock(...)` | Each Builder returned by `group.block(...)` |
| `configureItem(...)` | Each Builder returned by `group.item(...)` |
| `configureFluid(...)` | Each Builder returned by either `group.fluid(...)` overload |
| `configureBlockEntity(...)` | Each Builder returned by `group.blockEntity(...)` |

Calling a configuration method several times appends callbacks in declaration order. Each callback runs once when its Builder is created, after the built-in Group defaults. Configure the supplied Builder; call `.register()` on the completed entry chain as usual.

Callbacks apply to these direct Group entry points. For example, `configureItem` does not automatically configure a BlockItem created by `.simpleItem()` or a fluid bucket. Configure those through `configureBlock(block -> block.item(...))` or `configureFluid(fluid -> fluid.bucket(...))` when needed. The built-in Group `tab` option still handles those generated item forms.

Each `build()` captures the current tags and callback lists. You may reuse a Group Builder to create a second Group with more defaults; changing that Builder does not change the first Group's captured configuration.

## Step 8 —Add Project-Specific Group Methods

When your project needs a named registration operation, extend `Group` and construct it with `build(factory)`:

```java
public class MachineGroup extends Group {
    public MachineGroup(Group.Builder options) {
        super(options);
    }

    public ItemBuilder<Item, Group> part(String name) {
        return applyItemDefaults(core.item(this, name, Item::new, false));
    }
}

MachineGroup machines = REGISTRYLIB.group("machines")
        .configureItem(item -> item.properties(p -> p.stacksTo(16)))
        .build(MachineGroup::new);

machines.part("copper_gear").register();
```

Use the protected `applyBlockDefaults`, `applyItemDefaults`, `applyFluidDefaults`, and `applyBlockEntityDefaults` methods when your custom entry point creates a Builder directly. They apply the matching Group defaults and callbacks and return the same Builder subtype, so a custom Builder can keep its additional methods. Builders returned by the existing Group entry points already have these defaults applied; do not apply them a second time.

You may also override these methods to extend the default rules. Call the matching `super` method when you want to retain the built-in behavior. `build(factory)` returns your concrete Group type without a cast; the existing no-argument `build()` continues to return `Group`.

## Common Patterns

- **Content families** —Use Group for a shared lang prefix and tab, then override only the few exceptional values on individual entries.
- **Machine tiers** —Use Group for hardness and drop requirements, then add special [tooltips](./tooltip-system) or stronger properties on higher-tier entries.
- **Large ore batches** —Use Group for mining requirements and creative tab, then configure each ore's drop logic separately.

## Boundaries and Pitfalls

:::warning
- Group is **not** a namespace replacement. It only owns shared defaults and does not change when registration is submitted.
- A Group `tab` does not affect bare BlockEntity entries that do not have an Item form.
- If one entry is clearly an exception, do not force it into the same Group just for visual consistency.
:::

## Next Steps

- [Registering Blocks](/how-to/register-blocks) —Detailed block registration guide.
- [Tooltip System](./tooltip-system) —Add rich tooltip content to your grouped items.
- [API Reference](/reference/api-overview) —Full API surface for `Group` and `Group.Builder`.
