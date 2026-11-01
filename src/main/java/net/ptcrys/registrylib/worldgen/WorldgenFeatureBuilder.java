package net.ptcrys.registrylib.worldgen;

import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.annotations.StandardAPI;
import net.ptcrys.registrylib.datagen.DataProviderInitializer;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WorldgenFeatureBuilder<P> {

    private final RegistryCore core;
    private final P parent;
    private final String name;
    private final Supplier<? extends Feature> feature;
    private final List<PlacementModifier> placements = new ArrayList<>();
    private TagKey<Biome> biomeTag;
    private GenerationStep.Decoration decoration = GenerationStep.Decoration.VEGETAL_DECORATION;
    private boolean registered;

    public WorldgenFeatureBuilder(
                                  RegistryCore core, P parent, String name, Feature feature) {
        this(core, parent, name, () -> feature);
    }

    public WorldgenFeatureBuilder(
                                  RegistryCore core,
                                  P parent,
                                  String name,
                                  Supplier<? extends Feature> feature) {
        this.core = core;
        this.parent = parent;
        this.name = name;
        this.feature = feature;
    }

    public static <P> WorldgenFeatureBuilder<P> create(
                                                       RegistryCore core, P parent, String name, Feature feature) {
        return new WorldgenFeatureBuilder<>(core, parent, name, feature);
    }

    public static <P> WorldgenFeatureBuilder<P> create(
                                                       RegistryCore core,
                                                       P parent,
                                                       String name,
                                                       Supplier<? extends Feature> feature) {
        return new WorldgenFeatureBuilder<>(core, parent, name, feature);
    }

    @StandardAPI
    public WorldgenFeatureBuilder<P> placement(PlacementModifier modifier) {
        placements.add(modifier);
        return this;
    }

    @StandardAPI
    public WorldgenFeatureBuilder<P> placements(List<PlacementModifier> modifiers) {
        placements.addAll(modifiers);
        return this;
    }

    @StandardAPI
    public WorldgenFeatureBuilder<P> addToBiomes(
                                                 TagKey<Biome> biomeTag, GenerationStep.Decoration decoration) {
        if (this.biomeTag != null) {
            throw new IllegalStateException("Biome placement already configured for: " + name);
        }
        this.biomeTag = biomeTag;
        this.decoration = decoration;
        return this;
    }

    @StandardAPI
    public WorldgenFeatureEntry register() {
        if (registered) {
            throw new IllegalStateException("Builder already registered: " + name);
        }
        registered = true;
        ResourceKey<Feature> featureKey = ResourceKey.create(
                Registries.FEATURE, Identifier.fromNamespaceAndPath(core.getModid(), name));
        ResourceKey<PlacedFeature> placedKey = ResourceKey.create(
                Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(core.getModid(), name));
        TagKey<Biome> capturedBiomeTag = biomeTag;
        GenerationStep.Decoration capturedDecoration = decoration;
        List<PlacementModifier> capturedPlacements = List.copyOf(placements);
        ResourceKey<net.neoforged.neoforge.common.world.BiomeModifier> modifierKey = capturedBiomeTag == null ? null : ResourceKey.create(
                NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                Identifier.fromNamespaceAndPath(core.getModid(), name + "_add_feature"));

        DataProviderInitializer initializer = core.getDataGenInitializer();
        initializer.reserve(featureKey);
        initializer.reserve(placedKey);
        if (modifierKey != null) {
            initializer.reserve(modifierKey);
        }

        initializer.add(Registries.FEATURE, ctx -> ctx.register(featureKey, feature.get()));
        initializer
                .add(
                        Registries.PLACED_FEATURE,
                        ctx -> ctx.register(
                                placedKey,
                                new PlacedFeature(
                                        ctx.lookup(Registries.FEATURE).getOrThrow(featureKey),
                                        capturedPlacements)));
        if (modifierKey != null) {
            initializer
                    .add(
                            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                            ctx -> ctx.register(
                                    modifierKey,
                                    new BiomeModifiers.AddFeaturesBiomeModifier(
                                            ctx.lookup(Registries.BIOME).getOrThrow(capturedBiomeTag),
                                            HolderSet.direct(
                                                    ctx.lookup(Registries.PLACED_FEATURE).getOrThrow(placedKey)),
                                            capturedDecoration)));
        }
        return new WorldgenFeatureEntry(featureKey, placedKey);
    }

    @StandardAPI
    public P build() {
        register();
        return parent;
    }
}
