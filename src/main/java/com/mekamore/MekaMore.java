package com.mekamore;

import mekanism.api.MekanismAPI;
import mekanism.api.gear.ModuleData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import com.mekamore.registries.MekaMoreConfig;
import com.mekamore.registries.CreativeTabs;
import com.mekamore.registries.MekaMoreItems;
import com.mekamore.registries.MekaMoreModules;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MekaMore.MODID)
public class MekaMore {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mekamore";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();


    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MekaMore.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekaMore.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MekaMore.MODID);
    public static final DeferredRegister<ModuleData<?>> MODULES = DeferredRegister.create(MekanismAPI.MODULE_REGISTRY_NAME, MekaMore.MODID);


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MekaMore(IEventBus modEventBus, ModContainer modContainer) {
        MekaMoreConfig.register(modContainer);
        MekaMoreItems.register();
        MekaMoreModules.register(modEventBus);
        CreativeTabs.register();

        TABS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        MODULES.register(modEventBus);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MekaMore.MODID, path);
    }
}
