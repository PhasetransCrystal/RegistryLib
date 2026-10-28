package net.ptcrys.registrylibtest.gametest;

import net.ptcrys.registrylib.Group;
import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.datagen.ProviderType;
import net.ptcrys.registrylib.util.entry.BlockEntry;
import net.ptcrys.registrylib.util.entry.ItemEntry;
import net.ptcrys.registrylibtest.RegistryLibTest;
import net.ptcrys.registrylibtest.blockentity.TimerBlockEntity;
import net.ptcrys.registrylibtest.builder.ModItemBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RegistryLibGroupGameTests {

    private static final List<String> CONFIGURATION_ORDER = new ArrayList<>();
    private static final List<String> TYPE_CALLBACKS = new ArrayList<>();
    private static final ItemEntry<Item> FIRST_ITEM;
    private static final ItemEntry<Item> SECOND_ITEM;
    private static final ItemEntry<Item> CUSTOM_ITEM;
    private static final BlockEntry<Block> BLOCK;

    static {
        RegistryCore core = RegistryCore.create(RegistryLibTest.MOD_ID);
        core.withItemDefaults(builder -> {
            CONFIGURATION_ORDER.add(builder.getName() + ":core");
            builder.properties(p -> p.stacksTo(64));
        });
        Group.Builder configuration = core.group("group_callbacks")
                .itemProperties(p -> p.stacksTo(8))
                .configureItem(builder -> {
                    CONFIGURATION_ORDER.add(builder.getName() + ":first");
                    builder.properties(p -> p.stacksTo(16));
                })
                .configureItem(builder -> CONFIGURATION_ORDER.add(builder.getName() + ":second"))
                .configureBlock(builder -> {
                    TYPE_CALLBACKS.add("block");
                    builder.properties(p -> p.strength(7.0F));
                })
                .configureFluid(builder -> {
                    TYPE_CALLBACKS.add("fluid");
                    builder.noBlock().noBucket();
                })
                .configureBlockEntity(builder -> {
                    TYPE_CALLBACKS.add("block_entity");
                    builder.validBlock(() -> core.get("group_callback_block", Registries.BLOCK).get());
                });
        Group first = configuration.build();
        configuration.configureItem(builder -> builder.properties(p -> p.stacksTo(32)));
        GroupProbe second = configuration.build(GroupProbe::new);
        FIRST_ITEM = first.item("group_callback_first", Item::new).register();
        SECOND_ITEM = second.item("group_callback_second", Item::new)
                .properties(p -> p.stacksTo(4)).register();
        CUSTOM_ITEM = second.customItem("group_callback_custom").langCn("分组扩展物品").register();
        BLOCK = first.block("group_callback_block", Block::new).simpleItem().register();
        first.blockEntity("group_callback_block_entity", TimerBlockEntity::new).register();
        Identifier still = Identifier.fromNamespaceAndPath("registrylib", "block/fluid/liquid_still");
        Identifier flowing = Identifier.fromNamespaceAndPath("registrylib", "block/fluid/liquid_flow");
        first.fluid("group_callback_fluid", still, flowing).register();
        first.fluid("group_callback_custom_fluid", still, flowing, BaseFlowingFluid.Flowing::new).register();
    }

    private RegistryLibGroupGameTests() {}

    public static void register(IEventBus bus) {
        bus.addListener(RegistryLibGroupGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = RegistryLibGameTestSupport.registerEnvironment(event, "group_environment");
        RegistryLibGameTestSupport.register(event, environment, "group_configuration_snapshot", 40,
                RegistryLibGroupGameTests::groupConfigurationSnapshot);
        RegistryLibGameTestSupport.register(event, environment, "group_builder_configuration", 40,
                RegistryLibGroupGameTests::groupBuilderConfiguration);
    }

    private static void groupBuilderConfiguration(GameTestHelper helper) {
        helper.assertTrue(FIRST_ITEM.get().getDefaultMaxStackSize() == 16,
                Component.literal("built Group keeps its callback snapshot and overrides built-in defaults"));
        helper.assertTrue(SECOND_ITEM.get().getDefaultMaxStackSize() == 4,
                Component.literal("entry configuration runs after Group callbacks"));
        helper.assertTrue(CUSTOM_ITEM.get().getDefaultMaxStackSize() == 32,
                Component.literal("custom Builder retains its type and receives Group configuration"));
        helper.assertTrue(CONFIGURATION_ORDER.equals(List.of(
                "group_callback_first:core", "group_callback_first:first", "group_callback_first:second",
                "group_callback_second:core", "group_callback_second:first", "group_callback_second:second",
                "group_callback_custom:first", "group_callback_custom:second", "group_callback_block:core")),
                Component.literal("Core defaults precede Group callbacks, which run once in declaration order"));
        helper.assertTrue(TYPE_CALLBACKS.equals(List.of("block", "block_entity", "fluid", "fluid")),
                Component.literal("type callbacks cover blocks, block entities and both fluid factory overloads"));
        helper.assertTrue(BLOCK.get().defaultBlockState().getDestroySpeed(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO)) == 7.0F,
                Component.literal("block callbacks configure the registered block"));
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath(RegistryLibTest.MOD_ID,
                "group_callback_block_entity")).isValid(BLOCK.get().defaultBlockState()),
                Component.literal("block entity callbacks configure valid blocks"));
        helper.succeed();
    }

    private static void groupConfigurationSnapshot(GameTestHelper helper) {
        DatagenProbe core = new DatagenProbe("group_" + UUID.randomUUID().toString().replace("-", ""));
        core.enabled = true;
        TagKey<Item> firstTag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(core.getModid(), "first"));
        TagKey<Item> secondTag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(core.getModid(), "second"));
        Group.Builder builder = core.group("snapshot").addItemTag(firstTag);
        GroupProbe first = new GroupProbe(builder);
        builder.addItemTag(secondTag);
        helper.assertTrue(first.itemTagCount() == 1,
                Component.literal("built Group retains its tag configuration when the Builder changes"));
        helper.assertTrue(new GroupProbe(builder).itemTagCount() == 2,
                Component.literal("a later Group receives the updated configuration"));
        helper.succeed();
    }

    private static final class GroupProbe extends Group {

        private GroupProbe(Builder builder) {
            super(builder);
        }

        private ModItemBuilder<Item, GroupProbe> customItem(String name) {
            return applyItemDefaults(ModItemBuilder.create(core, this, name, Item::new, false));
        }

        private int itemTagCount() {
            return tagsByType.get(ProviderType.ITEM_TAGS).size();
        }
    }

    private static final class DatagenProbe extends RegistryCore {

        private boolean enabled;

        private DatagenProbe(String modid) {
            super(modid);
        }

        @Override
        public boolean doDatagen() {
            return enabled;
        }
    }
}
