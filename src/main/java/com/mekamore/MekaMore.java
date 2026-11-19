package com.mekamore;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import mekanism.api.MekanismIMC;
import com.mekamore.config.MoreMekaConfig;
import com.mekamore.registries.MekaMoreCreativeTabs;
import com.mekamore.registries.MekaMoreItems;
import com.mekamore.registries.MekaMoreModules;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MekaMore.MODID)
public class MekaMore {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mekamore";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MekaMore(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MoreMekaConfig.SPEC);
        MekaMoreModules.MODULES.register(modEventBus);
        MekaMoreItems.ITEMS.register(modEventBus);
        MekaMoreCreativeTabs.CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(this::onInterModEnqueue);
    }

    private void onInterModEnqueue(InterModEnqueueEvent event) {
        MekanismIMC.addMekaToolModules(MekaMoreModules.LOOTING_UNIT);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MekaMore.MODID, path);
    }
}
