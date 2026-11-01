package net.ptcrys.registrylibdemo.blockentity;

import net.ptcrys.registrylib.util.entry.BlockEntityTypeEntry;
import net.ptcrys.registrylibdemo.RegistryLibDemo;
import net.ptcrys.registrylibdemo.block.FullBlockExample;

/** 最简单的 BlockEntity 注册：绑定一个方块。 */
public class SimpleBlockEntityExample {

    public static final BlockEntityTypeEntry<TimerBlockEntity> SIMPLE_TIMER_BE = RegistryLibDemo.REGISTRYLIB
            .blockEntity("simple_timer", TimerBlockEntity::new)
            .validBlock(FullBlockExample.STANDALONE_TIMER)
            .register();
}
