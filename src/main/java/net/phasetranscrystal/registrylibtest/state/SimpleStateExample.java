package net.phasetranscrystal.registrylibtest.state;

import net.phasetranscrystal.registrylib.state.ChunkStateEntry;
import net.phasetranscrystal.registrylib.state.WorldStateEntry;
import net.phasetranscrystal.registrylibtest.RegistryLibTest;

import com.mojang.serialization.Codec;

public final class SimpleStateExample {

    public static final ChunkStateEntry<Integer> AMBIENT_ESSENCE = RegistryLibTest.REGISTRYLIB
            .chunkState("ambient_essence", Codec.INT, () -> 0)
            .debug(config -> config.writable(true).writePermission(4))
            .register();

    public static final WorldStateEntry<Integer> ESSENCE_EPOCH = RegistryLibTest.REGISTRYLIB
            .worldState("essence_epoch", Codec.INT, () -> 0)
            .debug(config -> config.writable(true).writePermission(4))
            .register();

    public static final WorldStateEntry<Integer> AMBIENT_ESSENCE_WORLD = RegistryLibTest.REGISTRYLIB
            .worldState("ambient_essence_world", Codec.INT, () -> 100)
            .debug()
            .register();

    private SimpleStateExample() {}
}
