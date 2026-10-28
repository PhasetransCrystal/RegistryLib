---
sidebar_position: 1
title: Installation & Setup
description: Set up RegistryLib in your NeoForge project in 3 minutes.
---

# Installation & Setup

This page walks you through dependency setup and local validation so you can start registering content with RegistryLib as quickly as possible.

## Prerequisites

- A working NeoForge mod project (Minecraft 1.21.x)
- Gradle 8+ with a Java 21 toolchain

## Step 1: Add the Maven Repository

RegistryLib is published on the Gtodyssey Maven repository. No authentication is required to download.

Add the following to your `settings.gradle`:

```groovy
dependencyResolutionManagement {
        repositories {
                maven {
                        name = 'Gtodyssey'
                        url = uri('https://maven.gtodyssey.com/releases')
                }
        }
}
```

Or, if you prefer to configure repositories in `build.gradle`:

```groovy
repositories {
        maven {
                url = 'https://maven.gtodyssey.com/releases'
        }
}
```

## Step 2: Add the Dependency

In your `build.gradle`:

```groovy
dependencies {
        implementation 'net.ptcrysb:1.0.0'
}
```

:::note
Replace the version with the target release you are using, and keep your Minecraft, mappings, and NeoForge versions aligned with that release.
:::

## Step 3: Verify the Local Tooling Path

| Command | Purpose |
| --- | --- |
| `./gradlew build` | Validate that the project resolves and builds |
| `./gradlew runClient` | Start the client and verify registrations in game |
| `./gradlew runServer` | Check dedicated-server safety |
| `./gradlew runData` | Confirm datagen output |

Run `./gradlew build` first. If it succeeds, your dependency setup is correct.

## Create `REGISTRYLIB` First

Before you can register an Item, Block, or Fluid, create a registry entry point. A shared static field is a convenient starting point for your registration calls.

```java
public static final String MOD_ID = "examplemod";
public static final ModRegistryCore REGISTRYLIB = ModRegistryCore.create(MOD_ID);
```

:::tip
The test mod uses `ModRegistryCore` instead of the base `RegistryCore` because its custom Builder layer adds `langCn(...)`. If you do not need that custom Builder behavior, the plain equivalent is:

```java
public static final RegistryCore REGISTRYLIB = RegistryCore.create(MOD_ID);
```
:::

## Multiple Cores for One Mod

You can keep using one shared Core throughout your mod. If separate modules need their own Core instances or custom Core subclasses, give them the same `modid`:

```java
RegistryCore materials = RegistryCore.create(MOD_ID);
RegistryCore machines = RegistryCore.create(MOD_ID);

materials.item("copper_plate", Item::new).lang("Copper Plate").register();
machines.item("machine_frame", Item::new).lang("Machine Frame").register();
```

Both items are registered under your modid, and both names appear in your mod's `en_us.json`. Either Core can query both entries with `get(...)` or `getAll(...)`. Cores with different modids keep their registrations and generated data separate.

Create and configure Cores during mod construction, before registry events and datagen start. Each resource ID must still be unique within its registry; sharing a pipeline does not merge duplicate item or block definitions.

Builder defaults and the default creative tab remain local to each Core, so configure them on the instance that creates your builders. Creating another Core returns a new object; `getCoreByModId(MOD_ID)` retrieves the first Core created for that modid.

See [Using Multiple Cores](/concepts/design-decisions#using-multiple-cores-for-one-mod) for configuration boundaries.

## What's Next

Your environment is ready. Head to [Your First Item](/tutorials/first-item) to register your first piece of content with RegistryLib.
