package net.phasetranscrystal.registrylibtest.blockentity;

import net.phasetranscrystal.registrylib.util.entry.BlockEntityTypeEntry;
import net.phasetranscrystal.registrylibtest.RegistryLibTest;
import net.phasetranscrystal.registrylibtest.block.FullBlockExample;

/** 最简单的 BlockEntity 注册：绑定一个方块。 */
public class SimpleBlockEntityExample {

    public static final BlockEntityTypeEntry<TimerBlockEntity> SIMPLE_TIMER_BE = RegistryLibTest.REGISTRYLIB
            .blockEntity("simple_timer", TimerBlockEntity::new)
            .validBlock(FullBlockExample.STANDALONE_TIMER)
            .register();
}
