package com.mekamore.registries;

import com.mekamore.MekaMore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CreativeTabs {
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> MEKAMORE;

    public static void register() {
        MEKAMORE = MekaMore.TABS.register("mekamore", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mekamore"))
                .icon(() -> new ItemStack(MekaMoreItems.MODULE_LOOTING_UNIT.get()))
                .displayItems((parameters, output) -> output.acceptAll(MekaMore.ITEMS
                        .getEntries()
                        .stream()
                        .map(x -> new ItemStack(x.getDelegate().value()))
                        .toList()))
                .build());

    }
}