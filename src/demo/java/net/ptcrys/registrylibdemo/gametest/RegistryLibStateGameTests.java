package net.ptcrys.registrylibdemo.gametest;

import net.ptcrys.registrylib.state.StateRegistryManager;
import net.ptcrys.registrylib.state.StateScope;
import net.ptcrys.registrylibdemo.RegistryLibDemo;
import net.ptcrys.registrylibdemo.state.SimpleStateExample;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;

public final class RegistryLibStateGameTests {

    private RegistryLibStateGameTests() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegistryLibStateGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = RegistryLibGameTestSupport.registerDefaultEnvironment(event);
        RegistryLibGameTestSupport.register(
                event, environment, "state_attachments", 40, RegistryLibStateGameTests::stateAttachments);
        RegistryLibGameTestSupport.register(
                event, environment, "state_registry_manager", 40, RegistryLibStateGameTests::stateRegistryManager);
    }

    private static void stateRegistryManager(GameTestHelper helper) {
        StateRegistryManager manager = new StateRegistryManager();
        helper.assertTrue(manager.all().isEmpty(), Component.literal("new state manager is empty"));
        helper.assertTrue(
                manager.get(StateScope.WORLD, Identifier.fromNamespaceAndPath(RegistryLibDemo.MOD_ID, "missing_state"))
                        .isEmpty(),
                Component.literal("unknown state ID is absent"));

        manager.register(SimpleStateExample.ESSENCE_EPOCH);
        manager.register(SimpleStateExample.AMBIENT_ESSENCE_WORLD);
        manager.register(SimpleStateExample.AMBIENT_ESSENCE);
        helper.assertTrue(
                manager.get(StateScope.WORLD, SimpleStateExample.ESSENCE_EPOCH.identifier()).orElse(null) == SimpleStateExample.ESSENCE_EPOCH && manager.get(StateScope.CHUNK, SimpleStateExample.AMBIENT_ESSENCE.identifier()).orElse(null) == SimpleStateExample.AMBIENT_ESSENCE,
                Component.literal("state lookup returns entries in their declared scopes"));
        helper.assertTrue(
                manager.get(StateScope.CHUNK, SimpleStateExample.ESSENCE_EPOCH.identifier()).isEmpty(),
                Component.literal("lookup in the wrong scope is absent"));
        helper.assertTrue(
                List.copyOf(manager.all()).equals(List.of(
                        SimpleStateExample.AMBIENT_ESSENCE,
                        SimpleStateExample.AMBIENT_ESSENCE_WORLD,
                        SimpleStateExample.ESSENCE_EPOCH)),
                Component.literal("state entries are sorted by ID, not insertion order"));

        boolean duplicateRejected = false;
        try {
            manager.register(SimpleStateExample.AMBIENT_ESSENCE);
        } catch (IllegalStateException expected) {
            duplicateRejected = true;
        }
        helper.assertTrue(
                duplicateRejected && manager.get(StateScope.CHUNK, SimpleStateExample.AMBIENT_ESSENCE.identifier()).orElse(null) == SimpleStateExample.AMBIENT_ESSENCE && manager.all().size() == 3,
                Component.literal("duplicate state ID is rejected without changing the original entry"));
        helper.succeed();
    }

    private static void stateAttachments(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos pos = level.getChunk(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).getPos();
        ChunkAccess chunk = level.getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
        if (chunk != null) {
            chunk.removeData(SimpleStateExample.AMBIENT_ESSENCE.attachmentType());
        }
        level.removeData(SimpleStateExample.ESSENCE_EPOCH.attachmentType());
        level.removeData(SimpleStateExample.AMBIENT_ESSENCE_WORLD.attachmentType());

        helper.assertValueEqual(
                SimpleStateExample.AMBIENT_ESSENCE.getOrCreate(level, pos),
                0,
                Component.literal("chunk default value"));
        helper.assertValueEqual(
                SimpleStateExample.AMBIENT_ESSENCE_WORLD.getOrCreate(level),
                100,
                Component.literal("world default value"));

        SimpleStateExample.AMBIENT_ESSENCE.set(level, pos, 3);
        helper.assertValueEqual(
                SimpleStateExample.AMBIENT_ESSENCE.getIfLoaded(level, pos).orElse(-1),
                3,
                Component.literal("chunk set value"));
        helper.assertTrue(
                chunk != null && chunk.isUnsaved(), Component.literal("chunk state marks chunk unsaved"));

        SimpleStateExample.AMBIENT_ESSENCE.modify(level, pos, value -> {});
        SimpleStateExample.AMBIENT_ESSENCE.set(
                level, pos, SimpleStateExample.AMBIENT_ESSENCE.getOrCreate(level, pos) + 4);
        helper.assertValueEqual(
                SimpleStateExample.AMBIENT_ESSENCE.getIfLoaded(level, pos).orElse(-1),
                7,
                Component.literal("chunk modify/set value"));

        ChunkPos unloaded = new ChunkPos(pos.x() + 4096, pos.z() + 4096);
        helper.assertFalse(
                SimpleStateExample.AMBIENT_ESSENCE.getIfLoaded(level, unloaded).isPresent(),
                Component.literal("unloaded chunk getIfLoaded boundary"));

        SimpleStateExample.ESSENCE_EPOCH.set(level, 11);
        SimpleStateExample.ESSENCE_EPOCH.modify(level, value -> {});
        SimpleStateExample.ESSENCE_EPOCH.set(
                level, SimpleStateExample.ESSENCE_EPOCH.getOrCreate(level) + 5);
        helper.assertValueEqual(
                SimpleStateExample.ESSENCE_EPOCH.getIfPresent(level).orElse(-1),
                16,
                Component.literal("world modify/set value"));
        helper.assertValueEqual(
                SimpleStateExample.AMBIENT_ESSENCE_WORLD.getIfPresent(level).orElse(-1),
                100,
                Component.literal("world and chunk states with distinct IDs remain independent"));

        SimpleStateExample.AMBIENT_ESSENCE.sync(level, pos);
        SimpleStateExample.ESSENCE_EPOCH.sync(level);
        helper.succeed();
    }
}
