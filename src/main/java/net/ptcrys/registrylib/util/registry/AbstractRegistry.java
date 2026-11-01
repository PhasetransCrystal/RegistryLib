package net.ptcrys.registrylib.util.registry;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractRegistry<E> implements Iterable<E> {

    @Getter
    private boolean frozen = false;

    protected AbstractRegistry() {}

    public abstract int size();

    public abstract void forEach(Consumer<? super E> action);

    public abstract @NonNull Iterator<E> iterator();

    public abstract Spliterator<E> spliterator();

    public Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    protected final void checkModifiable() {
        if (this.frozen) throw new IllegalStateException("Cannot modify registry: it is frozen!");
    }

    public void freeze() {
        if (frozen) throw new IllegalStateException("Registry is already frozen!");
        this.frozen = true;
    }
}
