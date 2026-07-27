package net.ptcrys.registrylibtest.item;

import net.ptcrys.registrylib.util.ImageUtil;
import net.ptcrys.registrylib.util.entry.ItemEntry;
import net.ptcrys.registrylibtest.RegistryLibTest;

import net.minecraft.world.item.Item;

import java.awt.Color;

/**
 * 最简单的物品注册：一个物品 + 语言。
 *
 * <p>
 * 演示 <b>Approach 2</b>：通过 {@code item(parent, name, factory)} 的两参数形式， 返回类型为 {@link
 * net.ptcrys.registrylibtest.builder.ModItemBuilder}， 直接调用 {@code .langCn()}
 * 设置简体中文名称，无需传递 {@code ProviderType} 参数。
 */
public class SimpleItemExample {

    public static final ItemEntry<Item> COPPER_COIN = RegistryLibTest.REGISTRYLIB
            .item("copper_coin")
            .langCn("铜币") // Approach 2: ModItemBuilder.langCn()
            .lang("Copper Coin")
            .texture(
                    () -> ImageUtil.generateIcon(
                            new Color(214, 164, 65), ImageUtil.CIRCLE, new Color(92, 58, 23)))
            .register();
}
