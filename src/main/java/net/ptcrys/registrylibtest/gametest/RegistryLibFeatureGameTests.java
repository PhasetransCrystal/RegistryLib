package net.ptcrys.registrylibtest.gametest;

import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.datagen.DataProviderInitializer;
import net.ptcrys.registrylib.datagen.GeneratorType;
import net.ptcrys.registrylib.datagen.ProviderType;
import net.ptcrys.registrylib.datagen.provider.RegistryLibGeneralResourceProvider;
import net.ptcrys.registrylib.util.entry.RegistryEntry;
import net.ptcrys.registrylibtest.RegistryLibTest;
import net.ptcrys.registrylibtest.block.SimpleBlockExample;
import net.ptcrys.registrylibtest.crop.SimpleCropExample;
import net.ptcrys.registrylibtest.state.SimpleStateExample;
import net.ptcrys.registrylibtest.worldgen.SimpleWorldgenExample;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class RegistryLibFeatureGameTests {

    private static final List<String> PIPELINE_CALLBACKS = new ArrayList<>();
    private static RegistryCore secondCore;
    private static RegistryEntry<SoundEvent, SoundEvent> secondSound;
    private static RegistryEntry<Item, Item> singleBuilderItem;
    private static boolean duplicateEarlyRejected;
    private static boolean doubleBuilderRejected;
    private static final RegistryEntry<SoundEvent, SoundEvent> FIRST_SOUND = RegistryLibTest.REGISTRYLIB.registry(
            "pipeline_first", Registries.SOUND_EVENT, List.of(value -> PIPELINE_CALLBACKS.add("first")),
            key -> SoundEvent.createVariableRangeEvent(key.identifier()), RegistryEntry::new);

    static {
        try {
            secondCore = RegistryCore.create(RegistryLibTest.MOD_ID);
            RegistryLibTest.REGISTRYLIB.addRawLang("registrylibtest.pipeline.first", "First pipeline contribution");
            secondCore.addRawLang("registrylibtest.pipeline.second", "Second pipeline contribution");
            secondSound = secondCore.registry(
                    "pipeline_second", Registries.SOUND_EVENT, List.of(value -> PIPELINE_CALLBACKS.add("second")),
                    key -> SoundEvent.createVariableRangeEvent(key.identifier()), RegistryEntry::new);
            var itemBuilder = secondCore.item("pipeline_first");
            singleBuilderItem = itemBuilder.register();
            try {
                itemBuilder.register();
            } catch (IllegalStateException expected) {
                doubleBuilderRejected = expected.getMessage().contains("pipeline_first");
            }
            try {
                secondCore.registry(
                        "pipeline_first", Registries.SOUND_EVENT,
                        key -> SoundEvent.createVariableRangeEvent(key.identifier()));
            } catch (IllegalStateException expected) {
                duplicateEarlyRejected = expected.getMessage().contains(RegistryLibTest.MOD_ID + ":pipeline_first");
            }
            RegistryLibTest.REGISTRYLIB.addRegisterCallback(Registries.SOUND_EVENT, () -> PIPELINE_CALLBACKS.add("after_first"));
            secondCore.addRegisterCallback(Registries.SOUND_EVENT, () -> PIPELINE_CALLBACKS.add("after_second"));
        } catch (IllegalStateException duplicateCore) {
            // Keep the baseline runnable so GameTest reports the unsupported scenario.
        }
    }

    private RegistryLibFeatureGameTests() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegistryLibFeatureGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = RegistryLibGameTestSupport.registerEnvironment(event, "feature_environment");
        RegistryLibGameTestSupport.register(
                event, environment, "shared_registration_pipeline", 40,
                RegistryLibFeatureGameTests::sharedRegistrationPipeline);
        RegistryLibGameTestSupport.register(
                event, environment, "registry_entry_binding", 40,
                RegistryLibFeatureGameTests::registryEntryBinding);
        RegistryLibGameTestSupport.register(
                event, environment, "generated_resource_cleanup_paths", 40,
                RegistryLibFeatureGameTests::generatedResourceCleanupPaths);
        RegistryLibGameTestSupport.register(
                event, environment, "generated_resource_write_failures", 40,
                RegistryLibFeatureGameTests::generatedResourceWriteFailures);
        RegistryLibGameTestSupport.register(
                event, environment, "shared_datagen_pipeline", 40,
                RegistryLibFeatureGameTests::sharedDatagenPipeline);
        RegistryLibGameTestSupport.register(
                event,
                environment,
                "state_debug_commands",
                40,
                RegistryLibFeatureGameTests::stateDebugCommands);
        RegistryLibGameTestSupport.register(
                event, environment, "essence_crop", 120, RegistryLibFeatureGameTests::essenceCrop);
        RegistryLibGameTestSupport.register(
                event, environment, "essence_worldgen", 80, RegistryLibFeatureGameTests::essenceWorldgen);
    }

    private static void sharedRegistrationPipeline(GameTestHelper helper) {
        helper.assertTrue(secondCore != null && secondSound != null,
                Component.literal("multiple Core instances can contribute to one modid"));
        RegistryCore first = RegistryLibTest.REGISTRYLIB;
        helper.assertTrue(BuiltInRegistries.SOUND_EVENT.getValue(FIRST_SOUND.identifier()) == FIRST_SOUND.get() && BuiltInRegistries.SOUND_EVENT.getValue(secondSound.identifier()) == secondSound.get(),
                Component.literal("both contributions reach the Minecraft registry"));
        helper.assertTrue(first.get("pipeline_second", Registries.SOUND_EVENT) == secondSound && secondCore.get("pipeline_first", Registries.SOUND_EVENT) == FIRST_SOUND,
                Component.literal("both Core instances see the same entries"));
        helper.assertTrue(PIPELINE_CALLBACKS.equals(List.of("first", "second", "after_first", "after_second")),
                Component.literal("all callbacks execute once in registration order"));
        helper.assertTrue(first.isRegistered(Registries.SOUND_EVENT) && secondCore.isRegistered(Registries.SOUND_EVENT),
                Component.literal("registration completion is shared"));
        boolean lateCallbackRejected = false;
        try {
            first.addRegisterCallback(Registries.SOUND_EVENT, () -> {});
        } catch (IllegalStateException expected) {
            lateCallbackRejected = true;
        }
        helper.assertTrue(lateCallbackRejected,
                Component.literal("callbacks added after registry completion are rejected"));

        helper.assertTrue(duplicateEarlyRejected && secondCore.get("pipeline_first", Registries.SOUND_EVENT) == FIRST_SOUND,
                Component.literal("duplicate registry id is rejected before replacing the original entry"));
        helper.assertTrue(
                singleBuilderItem != null && doubleBuilderRejected && first.get("pipeline_first", Registries.ITEM) == singleBuilderItem && singleBuilderItem.identifier().equals(FIRST_SOUND.identifier()) && BuiltInRegistries.ITEM.getValue(singleBuilderItem.identifier()) == singleBuilderItem.get(),
                Component.literal("different registries share an ID while one Builder cannot register twice"));

        String lateName = "late_" + UUID.randomUUID().toString().replace("-", "");
        boolean lateRegistrationRejected = false;
        try {
            secondCore.registry(
                    lateName, Registries.SOUND_EVENT,
                    key -> SoundEvent.createVariableRangeEvent(key.identifier()));
        } catch (IllegalStateException expected) {
            lateRegistrationRejected = expected.getMessage().contains(Registries.SOUND_EVENT.identifier().toString());
        }
        helper.assertTrue(lateRegistrationRejected && secondCore.get(lateName, Registries.SOUND_EVENT) == null,
                Component.literal("registration after the registry event is rejected before changing the index"));

        RegistryCore lateCore = RegistryCore.create(RegistryLibTest.MOD_ID + "_late_probe");
        boolean newCoreRegistrationRejected = false;
        boolean newCoreCallbackRejected = false;
        try {
            lateCore.registry(
                    "missed_sound", Registries.SOUND_EVENT,
                    key -> SoundEvent.createVariableRangeEvent(key.identifier()));
        } catch (IllegalStateException expected) {
            newCoreRegistrationRejected = true;
        }
        try {
            lateCore.addRegisterCallback(Registries.SOUND_EVENT, () -> {});
        } catch (IllegalStateException expected) {
            newCoreCallbackRejected = true;
        }
        helper.assertTrue(
                lateCore.isRegistered(Registries.SOUND_EVENT) && newCoreRegistrationRejected && newCoreCallbackRejected && lateCore.get("missed_sound", Registries.SOUND_EVENT) == null && lateCore.get("pipeline_first", Registries.SOUND_EVENT) == null,
                Component.literal("a Core created after the event sees the global registration deadline"));
        helper.succeed();
    }

    private static void registryEntryBinding(GameTestHelper helper) {
        RegistryEntry<SoundEvent, SoundEvent> entry = new RegistryEntry<>(FIRST_SOUND.getKey());
        boolean unboundGetRejected = false;
        try {
            entry.get();
        } catch (IllegalStateException expected) {
            unboundGetRejected = expected.getMessage().contains("has not been bound yet");
        }
        helper.assertTrue(
                !entry.isBound() && entry.getOptional().isEmpty() && unboundGetRejected,
                Component.literal("unbound entries do not expose a value"));

        SoundEvent value = FIRST_SOUND.get();
        SoundEvent other = SoundEvent.createVariableRangeEvent(FIRST_SOUND.identifier());
        entry.bound(value);
        helper.assertTrue(
                entry.isBound() && entry.get() == value && entry.getOptional().orElse(null) == value && entry.is(value) && !entry.is(other),
                Component.literal("bound entry access and identity checks use the original value"));
        boolean rebindRejected = false;
        try {
            entry.bound(other);
        } catch (IllegalStateException expected) {
            rebindRejected = true;
        }
        helper.assertTrue(rebindRejected && entry.get() == value,
                Component.literal("a second binding is rejected without replacing the original value"));
        helper.succeed();
    }

    private static void sharedDatagenPipeline(GameTestHelper helper) {
        String modid = "pipeline_" + UUID.randomUUID().toString().replace("-", "");
        DatagenProbe first = new DatagenProbe(modid);
        DatagenProbe second;
        try {
            second = new DatagenProbe(modid);
        } catch (IllegalStateException duplicateCore) {
            helper.assertTrue(false, Component.literal("same-modid Core instances share datagen"));
            return;
        }
        first.enabled = true;
        second.enabled = true;
        GeneratorType<List<String>> type = new GeneratorType<>() {};
        first.addDataGenerator(type, output -> output.add("first"));
        second.addDataGenerator(type, output -> output.add("second"));
        DatagenProbe other = new DatagenProbe(modid + "_other");
        other.enabled = true;
        other.addDataGenerator(type, output -> output.add("other namespace"));
        List<String> output = new ArrayList<>();
        first.genData(type, output);
        helper.assertTrue(output.containsAll(List.of("first", "second")) && output.size() == 2,
                Component.literal("one generator consumes callbacks from both Core instances"));
        helper.assertTrue(first.getDataGenInitializer() == second.getDataGenInitializer(),
                Component.literal("datapack bootstraps use the same initializer"));
        helper.assertTrue(first.getDataGenInitializer() != other.getDataGenInitializer(),
                Component.literal("different namespaces retain separate pipelines"));
        DataProviderInitializerProbe initializer = new DataProviderInitializerProbe();
        boolean duplicateDependencyRejected = false;
        try {
            initializer.addDependency(ProviderType.ENCHANTMENT_TAGS, ProviderType.BLOCK_TAGS);
        } catch (IllegalStateException expected) {
            duplicateDependencyRejected = true;
        }
        var enchantmentTags = initializer.sortedProviders().stream()
                .filter(sorted -> sorted.type() == ProviderType.ENCHANTMENT_TAGS)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(duplicateDependencyRejected && enchantmentTags.parent() == ProviderType.DATAPACK_REGISTRIES,
                Component.literal("rejected prerequisite leaves the original dependency unchanged"));

        DatagenProbe stateCore = new DatagenProbe("state_conflict_" + UUID.randomUUID().toString().replace("-", ""));
        boolean lateStateRejected = false;
        try {
            stateCore.worldState("shared_id", Codec.INT, () -> 0).register();
        } catch (IllegalStateException expected) {
            lateStateRejected = true;
        }
        helper.assertTrue(
                lateStateRejected && stateCore.get("world_state/shared_id", NeoForgeRegistries.Keys.ATTACHMENT_TYPES) == null,
                Component.literal("state registration after the register event is rejected without leaving attachments"));
        helper.succeed();
    }

    private static void generatedResourceCleanupPaths(GameTestHelper helper) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("registrylib-cleanup-");
            String modid = "cleanup_test_" + UUID.randomUUID().toString().replace("-", "");
            RegistryCore core = RegistryCore.create(modid);
            PackOutput output = new PackOutput(tempDir);
            RegistryLibGeneralResourceProvider provider = new RegistryLibGeneralResourceProvider(core, output);
            Method cleanup = RegistryLibGeneralResourceProvider.class
                    .getDeclaredMethod("cleanGeneratedNamespace", String.class);
            cleanup.setAccessible(true);

            Path namespace = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(modid);
            Path langFile = namespace.resolve("lang/keep.json");
            Files.createDirectories(langFile.getParent());
            Files.writeString(langFile, "keep");
            boolean parentTraversalRejected = rejectsCleanup(cleanup, provider, "textures/item/../../lang");
            boolean namespaceTraversalRejected = rejectsCleanup(cleanup, provider, "textures/item/../..");
            helper.assertTrue(parentTraversalRejected && namespaceTraversalRejected && Files.exists(langFile),
                    Component.literal("parent traversal is rejected without deleting non-texture output"));

            Path targetFile = namespace.resolve("textures/item/generated/old.png");
            Path neighboringFile = namespace.resolve("textures/item/keep.png");
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, "old");
            Files.writeString(neighboringFile, "keep");
            cleanup.invoke(provider, "textures/item/generated");
            helper.assertTrue(!Files.exists(targetFile) && Files.exists(neighboringFile),
                    Component.literal("valid cleanup removes only its requested texture subtree"));
            helper.succeed();
        } catch (Exception e) {
            helper.assertTrue(false, Component.literal("resource cleanup test failed: " + e));
        } finally {
            if (tempDir != null) {
                try (var files = Files.walk(tempDir)) {
                    files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
        }
    }

    private static void generatedResourceWriteFailures(GameTestHelper helper) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("registrylib-write-failure-");
            String modid = "write_test_" + UUID.randomUUID().toString().replace("-", "");
            PackOutput output = new PackOutput(tempDir);
            RegistryLibGeneralResourceProvider provider = new RegistryLibGeneralResourceProvider(
                    RegistryCore.create(modid), output);
            provider.add("generated", "resources", (target, stream) -> target.resolve("failed.json"));
            provider.add("generated", "resources", (target, stream) -> target.resolve("succeeded.json"));

            Path successfulOutput = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                    .resolve(modid)
                    .resolve("generated/resources/succeeded.json");
            CachedOutput cache = (path, bytes, hash) -> {
                if (path.getFileName().toString().equals("failed.json")) {
                    throw new IOException("simulated write failure");
                }
                Files.createDirectories(path.getParent());
                Files.write(path, bytes);
            };
            Throwable failure = null;
            try {
                provider.run(cache).join();
            } catch (CompletionException expected) {
                failure = expected.getCause();
            }
            helper.assertTrue(
                    failure instanceof RuntimeException && failure.getMessage().contains("1 resource output") && failure.getSuppressed().length == 1 && failure.getSuppressed()[0].getMessage().contains("failed.json") && Files.exists(successfulOutput),
                    Component.literal("write failure is reported after the independent resource is generated"));
            helper.succeed();
        } catch (Exception e) {
            helper.assertTrue(false, Component.literal("resource write failure test failed: " + e));
        } finally {
            if (tempDir != null) {
                try (var files = Files.walk(tempDir)) {
                    files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
        }
    }

    private static boolean rejectsCleanup(Method cleanup, RegistryLibGeneralResourceProvider provider, String path)
                                                                                                                    throws Exception {
        try {
            cleanup.invoke(provider, path);
            return false;
        } catch (InvocationTargetException e) {
            return e.getCause() instanceof IllegalArgumentException;
        }
    }

    private static final class DataProviderInitializerProbe extends DataProviderInitializer {

        private List<Sorted> sortedProviders() {
            return getSortedProviders();
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

    private static void stateDebugCommands(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos pos = level.getChunk(helper.absolutePos(BlockPos.ZERO)).getPos();
        CommandDispatcher<CommandSourceStack> dispatcher = level.getServer().getCommands().getDispatcher();
        CommandSourceStack admin = source(helper, 4);

        assertCommandSucceeds(helper, dispatcher, admin, "registrylib state list");
        assertCommandSucceeds(
                helper, dispatcher, admin, "registrylib state debug registrylibtest:essence_epoch");
        assertCommandSucceeds(
                helper,
                dispatcher,
                admin,
                "registrylib state debug registrylibtest:ambient_essence " + pos.x() + " " + pos.z());

        assertCommandSucceeds(
                helper, dispatcher, admin, "registrylib state set registrylibtest:essence_epoch 21");
        helper.assertValueEqual(
                SimpleStateExample.ESSENCE_EPOCH.getIfPresent(level).orElse(-1),
                21,
                Component.literal("world state command set"));

        assertCommandSucceeds(
                helper,
                dispatcher,
                admin,
                "registrylib state set registrylibtest:ambient_essence " + pos.x() + " " + pos.z() + " 9");
        helper.assertValueEqual(
                SimpleStateExample.AMBIENT_ESSENCE.getIfLoaded(level, pos).orElse(-1),
                9,
                Component.literal("chunk state command set"));

        assertCommandSucceeds(
                helper, dispatcher, admin, "registrylib state get registrylibtest:essence_epoch");
        assertCommandSucceeds(
                helper,
                dispatcher,
                admin,
                "registrylib state get registrylibtest:ambient_essence " + pos.x() + " " + pos.z());

        assertCommandSucceeds(helper, dispatcher, source(helper, 1), "registrylib state list");
        assertCommandFails(
                dispatcher, admin, "registrylib state set registrylibtest:ambient_essence 12");
        assertCommandFails(
                dispatcher, admin, "registrylib state set registrylibtest:essence_epoch nope");
        assertCommandFails(
                dispatcher, admin, "registrylib state set registrylibtest:essence_epoch \"text\"");

        String permissionModId = RegistryLibTest.MOD_ID;

        List<String> levelTwoMessages = new ArrayList<>();
        CommandSourceStack levelTwo = recordingSource(helper, 2, levelTwoMessages);
        assertCommandSucceeds(helper, dispatcher, levelTwo, "registrylib state list");
        helper.assertTrue(
                levelTwoMessages.stream().anyMatch(message -> message.contains(permissionModId + ":permission_public_read")) && levelTwoMessages.stream().noneMatch(message -> message.contains(permissionModId + ":permission_private_read")),
                Component.literal("state list only includes entries readable by the command source"));
        assertDoesNotSuggest(
                helper,
                dispatcher,
                levelTwo,
                "registrylib state get " + permissionModId + ":",
                permissionModId + ":permission_private_read");
        assertSuggests(
                helper,
                dispatcher,
                levelTwo,
                "registrylib state set " + permissionModId + ":",
                permissionModId + ":permission_write_only");
        assertDoesNotSuggest(
                helper,
                dispatcher,
                levelTwo,
                "registrylib state set " + permissionModId + ":",
                permissionModId + ":permission_private_write");

        List<String> adminMessages = new ArrayList<>();
        CommandSourceStack adminWithOutput = recordingSource(helper, 4, adminMessages);
        assertCommandSucceeds(helper, dispatcher, adminWithOutput, "registrylib state list");
        helper.assertTrue(
                adminMessages.stream().anyMatch(message -> message.contains(permissionModId + ":permission_private_read")),
                Component.literal("admin state list includes entries with admin read permission"));
        assertSuggests(
                helper,
                dispatcher,
                adminWithOutput,
                "registrylib state get " + permissionModId + ":",
                permissionModId + ":permission_private_read");

        assertSuggests(
                helper,
                dispatcher,
                admin,
                "registrylib state get registrylibtest:a",
                "registrylibtest:ambient_essence");
        assertSuggests(
                helper,
                dispatcher,
                admin,
                "registrylib state set registrylibtest:e",
                "registrylibtest:essence_epoch");
        helper.succeed();
    }

    private static void essenceCrop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos cropPos = new BlockPos(2, 2, 2);
        BlockPos absoluteCropPos = helper.absolutePos(cropPos);
        var crop = SimpleCropExample.ESSENCE_CARROT.get();

        helper.setBlock(cropPos.below(), Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(cropPos, crop.getStateForAge(0));
        helper.assertValueEqual(crop.getMaxAge(), 7, Component.literal("crop max age"));
        helper.assertTrue(
                helper.getBlockState(cropPos).canSurvive(level, absoluteCropPos),
                Component.literal("crop survives on farmland"));

        BlockPos wrongCropPos = new BlockPos(4, 2, 2);
        helper.setBlock(wrongCropPos.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(wrongCropPos, crop.getStateForAge(0));
        helper.assertFalse(
                helper.getBlockState(wrongCropPos).canSurvive(level, helper.absolutePos(wrongCropPos)),
                Component.literal("crop rejects wrong soil"));

        ChunkPos chunkPos = level.getChunk(absoluteCropPos).getPos();
        SimpleStateExample.AMBIENT_ESSENCE.set(level, chunkPos, 7);
        int ageBeforeTicks = crop.getAge(helper.getBlockState(cropPos));
        for (int i = 0; i < 80 && crop.getAge(helper.getBlockState(cropPos)) == ageBeforeTicks; i++) {
            helper.randomTick(cropPos);
        }
        helper.assertTrue(
                crop.getAge(helper.getBlockState(cropPos)) > ageBeforeTicks,
                Component.literal("crop growth reads high ambient essence"));

        crop.performBonemeal(
                level, RandomSource.create(9L), absoluteCropPos, helper.getBlockState(cropPos));
        helper.assertTrue(
                crop.getAge(helper.getBlockState(cropPos)) <= 7,
                Component.literal("bonemeal does not exceed max age"));

        List<net.minecraft.world.item.ItemStack> drops = Block.getDrops(crop.getStateForAge(7), level, absoluteCropPos, null);
        var seedItem = BuiltInRegistries.ITEM.getValue(
                Identifier.fromNamespaceAndPath(RegistryLibTest.MOD_ID, "essence_carrot_seeds"));
        helper.assertTrue(
                drops.stream().anyMatch(stack -> stack.is(Items.CARROT)),
                Component.literal("mature crop loot includes produce"));
        helper.assertTrue(
                seedItem != null && drops.stream().anyMatch(stack -> stack.is(seedItem)),
                Component.literal("mature crop loot includes seeds"));

        SimpleStateExample.ESSENCE_EPOCH.set(level, 30);
        helper.setBlock(cropPos, crop.getStateForAge(7));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        InteractionResult result = helper
                .getBlockState(cropPos)
                .useWithoutItem(
                        level,
                        player,
                        new net.minecraft.world.phys.BlockHitResult(
                                Vec3.atCenterOf(absoluteCropPos),
                                net.minecraft.core.Direction.UP,
                                absoluteCropPos,
                                true));
        helper.assertTrue(
                result.consumesAction(),
                Component.literal("mature crop right-click harvest consumes action"));
        helper.assertValueEqual(
                crop.getAge(helper.getBlockState(cropPos)),
                0,
                Component.literal("harvest replants age zero"));
        helper.assertValueEqual(
                SimpleStateExample.ESSENCE_EPOCH.getIfPresent(level).orElse(-1),
                31,
                Component.literal("harvest callback fires once"));
        helper.succeed();
    }

    private static void essenceWorldgen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var configured = level
                .registryAccess()
                .lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .get(SimpleWorldgenExample.ESSENCE_NODE_PATCH.configuredKey());
        var placed = level
                .registryAccess()
                .lookupOrThrow(Registries.PLACED_FEATURE)
                .get(SimpleWorldgenExample.ESSENCE_NODE_PATCH.placedKey());
        var modifier = level
                .registryAccess()
                .lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS)
                .get(
                        Identifier.fromNamespaceAndPath(
                                RegistryLibTest.MOD_ID, "essence_node_patch_add_feature"));

        helper.assertTrue(
                configured.isPresent(), Component.literal("configured feature is registered"));
        helper.assertTrue(placed.isPresent(), Component.literal("placed feature is registered"));
        helper.assertTrue(modifier.isPresent(), Component.literal("biome modifier is registered"));
        helper.assertTrue(
                modifier.orElseThrow().value() instanceof BiomeModifiers.AddFeaturesBiomeModifier addFeatures && addFeatures.step() == GenerationStep.Decoration.VEGETAL_DECORATION,
                Component.literal("biome modifier step"));
        helper.assertTrue(
                placed.orElseThrow().value().placement().stream()
                        .anyMatch(modifierEntry -> modifierEntry.type() == PlacementModifierType.HEIGHTMAP),
                Component.literal("placed feature uses heightmap placement"));
        helper.assertTrue(
                placed.orElseThrow().value().placement().stream()
                        .anyMatch(modifierEntry -> modifierEntry.type() == PlacementModifierType.BIOME_FILTER),
                Component.literal("placed feature uses biome filter"));

        BlockPos origin = new BlockPos(2, 2, 2);
        prepareWorldgenSurface(helper, origin, Blocks.GRASS_BLOCK.defaultBlockState());
        boolean placedResult = tryPlace(placed.orElseThrow(), level, helper.absolutePos(origin.above()));
        int generated = countBlocks(
                helper,
                origin.offset(-1, -1, -1),
                origin.offset(18, 8, 18),
                SimpleBlockExample.DECORATIVE_STONE.get().defaultBlockState());
        helper.assertTrue(
                placedResult && generated > 0, Component.literal("placed feature generates on grass"));

        BlockPos negativeOrigin = new BlockPos(34, 2, 2);
        prepareWorldgenSurface(helper, negativeOrigin, Blocks.STONE.defaultBlockState());
        boolean negativeResult = tryPlace(placed.orElseThrow(), level, helper.absolutePos(negativeOrigin.above()));
        int negativeGenerated = countBlocks(
                helper,
                negativeOrigin.offset(-1, -1, -1),
                negativeOrigin.offset(18, 8, 18),
                SimpleBlockExample.DECORATIVE_STONE.get().defaultBlockState());
        helper.assertFalse(
                negativeResult || negativeGenerated > 0,
                Component.literal("placed feature rejects wrong base"));
        helper.succeed();
    }

    private static boolean tryPlace(
                                    Holder.Reference<net.minecraft.world.level.levelgen.placement.PlacedFeature> placed,
                                    ServerLevel level,
                                    BlockPos origin) {
        net.minecraft.world.level.levelgen.placement.PlacedFeature feature = new net.minecraft.world.level.levelgen.placement.PlacedFeature(
                placed.value().feature(),
                placed.value().placement().stream()
                        .filter(modifier -> modifier.type() == PlacementModifierType.BLOCK_PREDICATE_FILTER)
                        .toList());
        for (long seed = 0; seed < 64; seed++) {
            if (feature.place(
                    level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin)) {
                return true;
            }
        }
        return false;
    }

    private static CommandSourceStack source(GameTestHelper helper, int permission) {
        return commandSourceStack(helper, permission).withSuppressedOutput();
    }

    private static CommandSourceStack recordingSource(
                                                      GameTestHelper helper, int permission, List<String> messages) {
        CommandSourceStack stack = commandSourceStack(helper, permission);
        return stack.withSource(new CommandSource() {

            @Override
            public void sendSystemMessage(Component message) {
                messages.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        });
    }

    private static CommandSourceStack commandSourceStack(GameTestHelper helper, int permission) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        return helper
                .getLevel()
                .getServer()
                .createCommandSourceStack()
                .withLevel(helper.getLevel())
                .withPosition(Vec3.atCenterOf(pos))
                .withPermission(permissionSet(permission));
    }

    private static PermissionSet permissionSet(int level) {
        return permission -> level >= requiredLevel(permission);
    }

    private static int requiredLevel(Permission permission) {
        if (permission == Permissions.COMMANDS_OWNER) return 4;
        if (permission == Permissions.COMMANDS_ADMIN) return 3;
        if (permission == Permissions.COMMANDS_GAMEMASTER) return 2;
        if (permission == Permissions.COMMANDS_MODERATOR) return 1;
        return 0;
    }

    private static void assertCommandSucceeds(
                                              GameTestHelper helper,
                                              CommandDispatcher<CommandSourceStack> dispatcher,
                                              CommandSourceStack source,
                                              String command) {
        try {
            helper.assertTrue(
                    dispatcher.execute(command, source) > 0,
                    Component.literal("command succeeds: " + command));
        } catch (CommandSyntaxException exception) {
            helper.fail(
                    Component.literal(
                            "command failed unexpectedly: " + command + " -> " + exception.getMessage()));
        }
    }

    private static void assertCommandFails(
                                           CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String command) {
        try {
            dispatcher.execute(command, source);
            throw new AssertionError("command succeeded unexpectedly: " + command);
        } catch (CommandSyntaxException expected) {
            // Expected command failure path.
        }
    }

    private static void assertSuggests(
                                       GameTestHelper helper,
                                       CommandDispatcher<CommandSourceStack> dispatcher,
                                       CommandSourceStack source,
                                       String command,
                                       String expected) {
        try {
            ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, source);
            List<String> suggestions = dispatcher.getCompletionSuggestions(parsed).get().getList().stream()
                    .map(suggestion -> suggestion.getText())
                    .toList();
            helper.assertTrue(
                    suggestions.contains(expected), Component.literal("suggestions contain " + expected));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            helper.fail(Component.literal("suggestion interrupted: " + command));
        } catch (ExecutionException exception) {
            helper.fail(
                    Component.literal("suggestion failed: " + command + " -> " + exception.getMessage()));
        }
    }

    private static void assertDoesNotSuggest(
                                             GameTestHelper helper,
                                             CommandDispatcher<CommandSourceStack> dispatcher,
                                             CommandSourceStack source,
                                             String command,
                                             String unexpected) {
        try {
            ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, source);
            List<String> suggestions = dispatcher.getCompletionSuggestions(parsed).get().getList().stream()
                    .map(suggestion -> suggestion.getText())
                    .toList();
            helper.assertTrue(
                    !suggestions.contains(unexpected), Component.literal("suggestions hide " + unexpected));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            helper.fail(Component.literal("suggestion interrupted: " + command));
        } catch (ExecutionException exception) {
            helper.fail(
                    Component.literal(
                            "suggestion failed: " + command + " -> " + exception.getMessage()));
        }
    }

    private static void prepareWorldgenSurface(
                                               GameTestHelper helper, BlockPos origin, BlockState base) {
        for (int x = -1; x <= 18; x++) {
            for (int z = -1; z <= 18; z++) {
                BlockPos floor = origin.offset(x, 0, z);
                for (int y = -8; y < 0; y++) {
                    helper
                            .getLevel()
                            .setBlock(helper.absolutePos(floor.above(y)), Blocks.STONE.defaultBlockState(), 3);
                }
                helper.getLevel().setBlock(helper.absolutePos(floor), base, 3);
                for (int y = 1; y <= 8; y++) {
                    helper
                            .getLevel()
                            .setBlock(helper.absolutePos(floor.above(y)), Blocks.AIR.defaultBlockState(), 3);
                }
                helper
                        .getLevel()
                        .setBlock(helper.absolutePos(floor.above(9)), Blocks.BARRIER.defaultBlockState(), 3);
            }
        }
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(
                helper.getLevel().getChunk(helper.absolutePos(origin)),
                EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(
                helper.getLevel().getChunk(helper.absolutePos(origin.offset(18, 0, 18))),
                EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
    }

    private static int countBlocks(
                                   GameTestHelper helper, BlockPos from, BlockPos to, BlockState state) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (helper.getBlockState(pos).is(state.getBlock())) {
                count++;
            }
        }
        return count;
    }
}
