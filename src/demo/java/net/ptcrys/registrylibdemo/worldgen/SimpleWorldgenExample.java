package net.ptcrys.registrylibdemo.worldgen;

import net.ptcrys.registrylib.worldgen.WorldgenFeatureEntry;
import net.ptcrys.registrylibdemo.RegistryLibDemo;
import net.ptcrys.registrylibdemo.block.SimpleBlockExample;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;

import java.util.List;

public final class SimpleWorldgenExample {

    public static final WorldgenFeatureEntry ESSENCE_NODE_PATCH = RegistryLibDemo.REGISTRYLIB
            .worldgenFeature(
                    "essence_node_patch",
                    () -> new SimpleBlockFeature(
                            new SimpleStateProvider(SimpleBlockExample.DECORATIVE_STONE.get().defaultBlockState())))
            .placement(CountPlacement.of(1))
            .placement(InSquarePlacement.spread())
            .placement(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
            .placement(
                    BlockPredicateFilter.forPredicate(
                            BlockPredicate.allOf(
                                    BlockPredicate.replaceable(),
                                    BlockPredicate.noFluid(),
                                    BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), List.of(Blocks.GRASS_BLOCK)))))
            .placement(BiomeFilter.biome())
            .addToBiomes(
                    BiomeTags.IS_OVERWORLD,
                    net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION)
            .register();

    private SimpleWorldgenExample() {}
}
