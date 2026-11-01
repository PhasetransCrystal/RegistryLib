package net.ptcrys.registrylibdemo.state;

import net.ptcrys.registrylib.state.ChunkStateEntry;
import net.ptcrys.registrylib.state.WorldStateEntry;
import net.ptcrys.registrylibdemo.RegistryLibDemo;

import com.mojang.serialization.Codec;

public final class SimpleStateExample {

    public static final ChunkStateEntry<Integer> AMBIENT_ESSENCE = RegistryLibDemo.REGISTRYLIB
            .chunkState("ambient_essence", Codec.INT, () -> 0)
            .debug(config -> config.writable(true).writePermission(4))
            .register();

    public static final WorldStateEntry<Integer> ESSENCE_EPOCH = RegistryLibDemo.REGISTRYLIB
            .worldState("essence_epoch", Codec.INT, () -> 0)
            .debug(config -> config.writable(true).writePermission(4))
            .register();

    public static final WorldStateEntry<Integer> AMBIENT_ESSENCE_WORLD = RegistryLibDemo.REGISTRYLIB
            .worldState("ambient_essence_world", Codec.INT, () -> 100)
            .debug()
            .register();

    public static final WorldStateEntry<Integer> PERMISSION_PRIVATE_READ = RegistryLibDemo.REGISTRYLIB
            .worldState("permission_private_read", Codec.INT, () -> 0)
            .debug(config -> config.readPermission(4))
            .register();

    public static final WorldStateEntry<Integer> PERMISSION_PUBLIC_READ = RegistryLibDemo.REGISTRYLIB
            .worldState("permission_public_read", Codec.INT, () -> 0)
            .debug(config -> config.readPermission(0))
            .register();

    public static final WorldStateEntry<Integer> PERMISSION_PRIVATE_WRITE = RegistryLibDemo.REGISTRYLIB
            .worldState("permission_private_write", Codec.INT, () -> 0)
            .debug(config -> config.writable(true).readPermission(4).writePermission(4))
            .register();

    public static final WorldStateEntry<Integer> PERMISSION_WRITE_ONLY = RegistryLibDemo.REGISTRYLIB
            .worldState("permission_write_only", Codec.INT, () -> 0)
            .debug(config -> config.writable(true).readPermission(4).writePermission(0))
            .register();

    private SimpleStateExample() {}
}
