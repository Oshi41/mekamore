package com.mekamore.registries;

import com.mekamore.MekaMore;
import com.mekamore.content.gear.ModuleLootingUnit;
import mekanism.api.MekanismAPI;
import mekanism.api.gear.ModuleData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MekaMoreModules {
    public static final DeferredRegister<ModuleData<?>> MODULES = DeferredRegister.create(MekanismAPI.MODULE_REGISTRY_NAME, MekaMore.MODID);

    public static final DeferredHolder<ModuleData<?>, ModuleData<ModuleLootingUnit>> LOOTING_UNIT = MODULES.register("looting_unit", () -> new ModuleData<>(ModuleData.ModuleDataBuilder
          .customInstanced(ModuleLootingUnit::new, MekaMoreItems.MODULE_LOOTING_UNIT)
          .maxStackSize(4)));

    private MekaMoreModules() {
    }
}