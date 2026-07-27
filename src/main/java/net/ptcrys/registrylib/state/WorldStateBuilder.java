package net.ptcrys.registrylib.state;

import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.annotations.StandardAPI;
import net.ptcrys.registrylib.util.entry.AttachmentTypeEntry;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import com.mojang.serialization.Codec;

import java.util.function.Function;
import java.util.function.Supplier;

public final class WorldStateBuilder<T, P>
                                    extends AbstractStateBuilder<T, P, WorldStateBuilder<T, P>> {

    public WorldStateBuilder(
                             RegistryCore core,
                             P parent,
                             String name,
                             Codec<T> codec,
                             Function<IAttachmentHolder, T> defaultValueFactory) {
        super(core, parent, name, codec, defaultValueFactory);
    }

    public static <T, P> WorldStateBuilder<T, P> create(
                                                        RegistryCore core, P parent, String name, Codec<T> codec, Supplier<T> defaultValueFactory) {
        return new WorldStateBuilder<>(core, parent, name, codec, _holder -> defaultValueFactory.get());
    }

    @Override
    protected String attachmentPrefix() {
        return "world_state/";
    }

    @Override
    protected AbstractStateEntry<T> createEntry(
                                                Identifier identifier,
                                                Codec<T> codec,
                                                AttachmentTypeEntry<T> attachment,
                                                StateDebugConfig debugConfig,
                                                boolean syncOnModify) {
        return new WorldStateEntry<>(identifier, codec, attachment, debugConfig, syncOnModify);
    }

    @Override
    @StandardAPI
    public WorldStateEntry<T> register() {
        return (WorldStateEntry<T>) super.register();
    }
}
