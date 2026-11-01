package net.ptcrys.registrylib.datagen.provider;

import net.ptcrys.registrylib.RegistryCore;
import net.ptcrys.registrylib.datagen.ProviderType;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.neoforged.fml.LogicalSide;

import java.util.concurrent.CompletableFuture;

public interface RegistryLibTagsProvider<T> extends RegistryLibLookupFillerProvider {

    CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter();

    ResourceKey<? extends Registry<T>> registry();

    TagBuilder rawBuilder(TagKey<T> key);

    interface Intrinsic<T> extends RegistryLibTagsProvider<T> {

        TagAppender<T> tag(TagKey<T> key);
    }

    class IntrinsicImpl<T> extends TagsProvider<T>
                       implements RegistryLibTagsProvider.Intrinsic<T> {

        private final RegistryCore owner;
        private final ProviderType<? extends IntrinsicImpl<T>> type;
        private final String name;
        private CompletableFuture<HolderLookup.Provider> contentsProvider;

        public IntrinsicImpl(
                             RegistryCore owner,
                             ProviderType<? extends IntrinsicImpl<T>> type,
                             String name,
                             PackOutput packOutput,
                             ResourceKey<? extends Registry<T>> registryIn,
                             CompletableFuture<HolderLookup.Provider> registriesLookup) {
            super(packOutput, registryIn, registriesLookup, owner.getModid());

            this.owner = owner;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return "Tags (%s)".formatted(name);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            owner.genData(type, this);
        }

        @Override
        public LogicalSide getSide() {
            return LogicalSide.SERVER;
        }

        @Override
        public TagBuilder rawBuilder(TagKey<T> key) {
            return super.getOrCreateRawBuilder(key);
        }

        @Override
        public TagAppender<T> tag(TagKey<T> key) {
            return super.tag(key);
        }

        @Override
        protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
            if (contentsProvider == null) {
                contentsProvider = super.createContentsProvider();
            }
            return contentsProvider;
        }

        @Override
        public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
            return createContentsProvider();
        }

        @Override
        public ResourceKey<? extends Registry<T>> registry() {
            return registryKey;
        }
    }
}
