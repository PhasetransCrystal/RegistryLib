package net.ptcrys.registrylib.datagen.provider;

import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.datagen.ProviderType;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class RegistryLibAdvancementProvider extends AdvancementSubProvider
                                            implements Consumer<AdvancementHolder> {

    private final RegistryCore owner;

    public RegistryLibAdvancementProvider(RegistryCore owner, BootstrapContext<Advancement> output) {
        super(output);
        this.owner = owner;
    }

    @Override
    public void generate() {
        owner.genData(ProviderType.ADVANCEMENT, this);
    }

    public BootstrapContext<Advancement> output() {
        return output;
    }

    public MutableComponent title(String category, String name, String title) {
        return owner.addLang(
                "advancements", Identifier.fromNamespaceAndPath(category, name), "title", title);
    }

    public MutableComponent desc(String category, String name, String desc) {
        return owner.addLang(
                "advancements", Identifier.fromNamespaceAndPath(category, name), "description", desc);
    }

    @Override
    public void accept(@Nullable AdvancementHolder holder) {
        withConditions(holder, List.of());
    }

    public void withConditions(@Nullable AdvancementHolder holder, List<ICondition> conditions) {
        Objects.requireNonNull(holder, "Cannot accept a null advancement");
        output.register(
                ResourceKey.create(Registries.ADVANCEMENT, holder.id()),
                holder.value(),
                conditions.toArray(ICondition[]::new));
    }
}
