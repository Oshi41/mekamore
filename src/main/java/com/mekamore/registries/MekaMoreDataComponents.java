package com.mekamore.registries;

import com.mekamore.MekaMore;
import com.mekamore.fluid.SimpleContainer;
import com.mekamore.network4.RevealFilterList;
import mekanism.api.MekanismAPI;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class MekaMoreDataComponents {
    public static DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> MEKA_TOOL_FLUID;
    public static DeferredHolder<DataComponentType<?>, DataComponentType<RevealFilterList>> REVEAL_FILTERS;

    public static void register(IEventBus modEventBus) {
        MEKA_TOOL_FLUID = MekaMore.DATA_COMPONENTS.registerComponentType(
                "meka_tool_fluid",
                builder -> builder
                        .persistent(SimpleFluidContent.CODEC)
                        .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

        REVEAL_FILTERS = MekaMore.DATA_COMPONENTS.registerComponentType(
                "reveal_filters",
                builder -> builder
                        .persistent(RevealFilterList.CODEC)
                        .networkSynchronized(RevealFilterList.STREAM_CODEC));

        modEventBus.addListener(MekaMoreDataComponents::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        Optional<Item> itemOptional = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "meka_tool"));
        if (itemOptional.isPresent()) {
            if (!event.isItemRegistered(Capabilities.FluidHandler.ITEM, itemOptional.get())) {
                event.registerItem(Capabilities.FluidHandler.ITEM,
                        (stack, ctx) -> new SimpleContainer(stack),
                        itemOptional.get());
            }
        }
    }
}
