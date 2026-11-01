package net.ptcrys.registrylib.datagen;

import net.ptcrys.registrylib.datagen.provider.RegistryLibLookupFillerProvider;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.resources.ResourceKey;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

public class DataProviderInitializer {

    private final RegistrySetBuilder datapackEntryProvider = new RegistrySetBuilder();

    private final Map<ProviderType<?>, ProviderType<? extends RegistryLibLookupFillerProvider>> providerDependencies = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    private final Map<ResourceKey<? extends Registry<?>>, List<SingleRegistryBootstrap>> pendingBootstraps = new ConcurrentHashMap<>();
    private final List<MultiRegistryBootstrap> pendingMultiBootstraps = Collections.synchronizedList(new ArrayList<>());

    private final Set<ResourceKey<?>> reservedKeys = ConcurrentHashMap.newKeySet();

    public DataProviderInitializer() {
        addDependency(ProviderType.ITEM_TAGS, ProviderType.BLOCK_TAGS);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void applyTo(net.neoforged.neoforge.data.event.DatapackRegistryGatherer gatherer) {
        for (var entry : pendingBootstraps.entrySet()) {
            for (SingleRegistryBootstrap bootstrap : entry.getValue()) {
                gatherer.add((ResourceKey) entry.getKey(), bootstrap);
            }
        }
        pendingBootstraps.clear();
        for (MultiRegistryBootstrap bootstrap : pendingMultiBootstraps) {
            gatherer.add(bootstrap);
        }
        pendingMultiBootstraps.clear();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RegistrySetBuilder getDatapackRegistryProviders() {
        for (var entry : pendingBootstraps.entrySet()) {
            ResourceKey registryKey = entry.getKey();
            List<SingleRegistryBootstrap> bootstraps = entry.getValue();
            datapackEntryProvider.add(
                    registryKey,
                    ctx -> {
                        for (var bootstrap : bootstraps) {
                            bootstrap.run(ctx);
                        }
                    });
        }
        pendingBootstraps.clear();
        for (MultiRegistryBootstrap bootstrap : pendingMultiBootstraps) {
            datapackEntryProvider.add(bootstrap);
        }
        pendingMultiBootstraps.clear();
        return datapackEntryProvider;
    }

    protected List<Sorted> getSortedProviders() {
        List<Sorted> ans = new ArrayList<>();
        Set<ProviderType<?>> added = new ReferenceOpenHashSet<>();
        List<Map.Entry<String, ProviderType<?>>> remain = new ArrayList<>(RegistryLibDataProvider.TYPES.entrySet());
        while (!remain.isEmpty()) {
            if (!remain.removeIf(
                    e -> {
                        ProviderType<?> type = e.getValue();
                        var parent = providerDependencies.get(type);
                        if (parent == null || added.contains(parent)) {
                            ans.add(new Sorted(e.getKey(), type, parent));
                            added.add(type);
                            return true;
                        }
                        return false;
                    }))
                throw new IllegalStateException("Looping dependency detected: " + remain);
        }
        return ans;
    }

    public void reserve(ResourceKey<?> key) {
        if (!reservedKeys.add(key)) {
            throw new IllegalStateException("Duplicate datapack entry: " + key.identifier());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> void add(
                        ResourceKey<Registry<T>> registry, SingleRegistryBootstrap<T> provider) {
        pendingBootstraps
                .computeIfAbsent(
                        (ResourceKey) registry, k -> Collections.synchronizedList(new ArrayList<>()))
                .add((SingleRegistryBootstrap) provider);
    }

    public void add(MultiRegistryBootstrap provider) {
        pendingMultiBootstraps.add(provider);
    }

    public void addDependency(
                              ProviderType<?> dependent, ProviderType<? extends RegistryLibLookupFillerProvider> parent) {
        if (providerDependencies.putIfAbsent(dependent, parent) != null) {
            throw new IllegalStateException("Providers can have only 1 prerequisite");
        }
    }

    public record Sorted(
                         String id,
                         ProviderType<?> type,
                         @Nullable ProviderType<? extends RegistryLibLookupFillerProvider> parent) {}
}
