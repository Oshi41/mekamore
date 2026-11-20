package com.mekamore.registries;

import com.mekamore.client.MekaToolDecorator;
import mekanism.api.MekanismAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;


public class Client {
    @OnlyIn(Dist.CLIENT)
    public static void register(RegisterItemDecorationsEvent event) {
        event.register(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "meka_tool")), new MekaToolDecorator());
    }
}
