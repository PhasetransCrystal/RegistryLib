package net.ptcrys.registrylib.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record WorldgenFeatureEntry(
                                   ResourceKey<Feature> featureKey, ResourceKey<PlacedFeature> placedKey) {}
