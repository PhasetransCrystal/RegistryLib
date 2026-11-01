package net.ptcrys.registrylib.datagen.loot;

import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.datagen.ProviderType;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.function.TriFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RegistryLibLootTableProvider {

    public interface LootType<T extends RegistryLibLootTables> {

        LootType<RegistryLibBlockLootTables> BLOCK = register("block", LootContextParamSets.BLOCK, RegistryLibBlockLootTables::new);
        LootType<RegistryLibEntityLootTables> ENTITY = register("entity", LootContextParamSets.ENTITY, RegistryLibEntityLootTables::new);

        T getLootCreator(LootTableSubProvider.Context context, RegistryCore parent, Consumer<T> callback);

        ContextKeySet getLootSet();

        static <T extends RegistryLibLootTables> LootType<T> register(
                                                                      String name,
                                                                      ContextKeySet set,
                                                                      TriFunction<LootTableSubProvider.Context, RegistryCore, Consumer<T>, T> factory) {
            LootType<T> type = new LootType<T>() {

                @Override
                public T getLootCreator(
                                        LootTableSubProvider.Context context, RegistryCore parent, Consumer<T> callback) {
                    return factory.apply(context, parent, callback);
                }

                @Override
                public ContextKeySet getLootSet() {
                    return set;
                }
            };
            LOOT_TYPES.put(name, type);
            return type;
        }
    }

    private static final Map<String, LootType<?>> LOOT_TYPES = new HashMap<>();

    @SuppressWarnings("rawtypes")
    private final Multimap<LootType<?>, Consumer<? super RegistryLibLootTables>> specialLootActions = HashMultimap.create();

    public RegistryLibLootTableProvider(RegistryCore parent) {
        parent.genData(ProviderType.LOOT, this);
    }

    public List<LootTableProvider.SubProviderEntry> subProviders(RegistryCore parent) {
        return LOOT_TYPES.values().stream()
                .map(type -> new LootTableProvider.SubProviderEntry(
                        context -> type.getLootCreator(
                                context,
                                parent,
                                cons -> specialLootActions.get(type).forEach(c -> c.accept(cons))),
                        type.getLootSet()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends RegistryLibLootTables> void addLootAction(
                                                                LootType<T> type, Consumer<T> action) {
        this.specialLootActions.put(type, (Consumer<RegistryLibLootTables>) action);
    }
}
