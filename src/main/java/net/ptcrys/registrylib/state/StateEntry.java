package net.ptcrys.registrylib.state;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.attachment.AttachmentType;

import com.mojang.serialization.Codec;

public interface StateEntry<T> {

    Identifier identifier();

    StateScope scope();

    Codec<T> codec();

    AttachmentType<T> attachmentType();

    StateDebugConfig debugConfig();
}
