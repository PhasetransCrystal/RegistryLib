package net.phasetranscrystal.registrylibtest;

import net.phasetranscrystal.registrylib.RegistryCore;
import net.phasetranscrystal.registrylib.datagen.ProviderType;
import net.phasetranscrystal.registrylib.datagen.provider.RegistryLibLangProvider;

import net.minecraft.data.PackOutput;

/**
 * Simplified-Chinese lang provider for the test mod.
 *
 * <p>
 * Extends {@link RegistryLibLangProvider} with locale {@code zh_cn} and ties itself to {@link
 * ModRegistryCore#LANG_ZH_CN} so that every builder callback registered against that {@code
 * ProviderType} ends up written into {@code zh_cn.json}.
 */
public class ZhCnLangProvider extends RegistryLibLangProvider {

    public ZhCnLangProvider(RegistryCore owner, PackOutput packOutput) {
        super(owner, packOutput, "zh_cn");
    }

    @Override
    protected ProviderType<? extends RegistryLibLangProvider> getProviderType() {
        return ModRegistryCore.LANG_ZH_CN;
    }
}
