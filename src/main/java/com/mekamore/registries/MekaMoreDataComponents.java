package com.mekamore.registries;

import com.mekamore.MekaMore;
import com.mekamore.fluid.SimpleContainer;
import mekanism.api.MekanismAPI;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Optional;

public class MekaMoreDataComponents {
    public static DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> MEKA_TOOL_FLUID;

    public static void register(IEventBus modEventBus) {
        MEKA_TOOL_FLUID = MekaMore.DATA_COMPONENTS.registerComponentType(
                "meka_tool_fluid",
                builder -> builder
                        .persistent(SimpleFluidContent.CODEC)
                        .networkSynchronized(SimpleFluidContent.STREAM_CODEC));
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
