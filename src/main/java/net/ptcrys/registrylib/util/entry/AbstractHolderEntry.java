package net.ptcrys.registrylib.util.entry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

/**
 * Abstract base for registry entries backed by a {@link Holder}.
 *
 * <p>
 * {@link Holder} is sealed in current Minecraft versions, so entries expose their delegate through
 * {@link #holder()} instead of implementing it directly.
 *
 * @param <R> the registry element type (e.g. {@code Item}, {@code Block}, {@code Fluid})
 * @param <T> the concrete registered type ({@code T extends R})
 */
public abstract class AbstractHolderEntry<R, T extends R> extends RegistryEntry<R, T> {

    protected AbstractHolderEntry(ResourceKey<R> key) {
        super(key);
    }

    /**
     * Returns the {@link Holder} to which holder operations delegate. Typically {@code
     * value.builtInRegistryHolder} (or equivalent).
     */
    protected abstract Holder<R> delegate();

    public final Holder<R> holder() {
        if (value == null) {
            throw new IllegalStateException(
                    "Registry entry '" + key + "' has not been bound yet. This usually means you're accessing it before registration is complete.");
        }
        return delegate();
    }
}
