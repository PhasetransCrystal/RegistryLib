package net.ptcrys.registrylib.datagen.provider;

import javax.annotation.Nullable;

public interface RegistryLibLangEntryProvider extends RegistryLibProvider {

    void add(@Nullable String key, @Nullable String value);
}
