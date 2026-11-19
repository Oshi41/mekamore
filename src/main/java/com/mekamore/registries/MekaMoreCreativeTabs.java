package com.mekamore.registries;

import com.mekamore.MekaMore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MekaMoreCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MekaMore.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MEKAMORE = CREATIVE_TABS.register("mekamore", () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.mekamore"))
          .icon(() -> new ItemStack(MekaMoreItems.MODULE_LOOTING_UNIT.get()))
          .displayItems((parameters, output) -> output.accept(MekaMoreItems.MODULE_LOOTING_UNIT.get()))
          .build());

    private MekaMoreCreativeTabs() {
    }
}