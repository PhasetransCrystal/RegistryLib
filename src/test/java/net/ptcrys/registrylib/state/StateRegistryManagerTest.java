package net.ptcrys.registrylib.state;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link StateRegistryManager}.
 *
 * <p>
 * {@link StateRegistryManager} depends on {@code net.minecraft.resources.Identifier} and {@link
 * StateEntry} (which itself references MC/NeoForge types). When the Minecraft runtime is not on the
 * classpath these classes cannot be loaded, so all tests use {@link Assumptions#assumeTrue} to skip
 * gracefully in a plain unit-test environment.
 */
class StateRegistryManagerTest {

    private static boolean mcAvailable;

    @BeforeAll
    static void checkMinecraftRuntime() {
        try {
            Class.forName("net.minecraft.resources.Identifier");
            mcAvailable = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            mcAvailable = false;
        }
    }

    private StateRegistryManager manager;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(
                mcAvailable, "Skipped: StateRegistryManager requires Minecraft runtime classes");
        manager = new StateRegistryManager();
    }

    @Test
    void allReturnsEmptyCollectionWhenNothingRegistered() {
        Assumptions.assumeTrue(
                mcAvailable, "Skipped: StateRegistryManager requires Minecraft runtime classes");
        Collection<?> result = manager.all();
        assertTrue(result.isEmpty(), "all() on a fresh manager should return an empty collection");
    }
}
