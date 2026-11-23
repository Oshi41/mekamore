package com.mekamore.registries;

import com.mekamore.MekaMore;
import com.mekamore.content.gear.*;
import com.mekamore.content.gear.reveal_googles.ModuleRevealingGoggles;
import com.mekamore.content.gear.reveal_googles.ModuleRevealingGogglesConfig;
import mekanism.api.MekanismIMC;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.gear.ModuleData;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public class MekaMoreModules {
    public static DeferredHolder<ModuleData<?>, ModuleData<ModuleLootingUnit>> LOOTING_UNIT;
    public static DeferredHolder<ModuleData<?>, ModuleData<ModuleBrushUnit>> BRUSH_UNIT;
    public static DeferredHolder<ModuleData<?>, ModuleData<ModuleZoomUnit>> ZOOM_UNIT;
    public static DeferredHolder<ModuleData<?>, ModuleData<ModuleLiquidTankUnit>> LIQUID_TANK_UNIT;
    public static DeferredHolder<ModuleData<?>, ModuleData<ModuleRevealingGoggles>> REVEALING_UNIT;

    public static void register(IEventBus modEventBus) {
        MekaMoreItems.MODULE_LOOTING_UNIT = addItemModule("module_looting",
                () -> MekaMoreModules.LOOTING_UNIT,
                x -> x.stacksTo(4).rarity(Rarity.UNCOMMON));
        LOOTING_UNIT = addModule(MekaMoreItems.MODULE_LOOTING_UNIT, ModuleLootingUnit::new);


        MekaMoreItems.MODULE_BRUSH = addItemModule("module_brush",
                () -> MekaMoreModules.BRUSH_UNIT,
                x -> x.stacksTo(1).rarity(Rarity.UNCOMMON));
        BRUSH_UNIT = addModule(MekaMoreItems.MODULE_BRUSH, ModuleBrushUnit::new);

        MekaMoreItems.ZOOM_UNIT = addItemModule("module_zoom",
                () -> MekaMoreModules.ZOOM_UNIT,
                x -> x.stacksTo(4).rarity(Rarity.COMMON));
        ZOOM_UNIT = addModule(MekaMoreItems.ZOOM_UNIT, ModuleZoomUnit::new);


        MekaMoreItems.LIQUID_TANK_UNIT = addItemModule("module_liquid_tank",
                () -> MekaMoreModules.LIQUID_TANK_UNIT,
                x -> x.stacksTo(4).rarity(Rarity.RARE));
        LIQUID_TANK_UNIT = addModule(MekaMoreItems.LIQUID_TANK_UNIT, ModuleLiquidTankUnit::new);


        MekaMoreItems.REVEALING_UNIT = addItemModule("module_revealing_goggles",
                () -> MekaMoreModules.REVEALING_UNIT,
                x -> x.stacksTo(10).rarity(Rarity.EPIC));
        REVEALING_UNIT = MekaMore.MODULES.register(MekaMoreItems.REVEALING_UNIT.getId().getPath(),
                () -> new ModuleData<>(ModuleData.ModuleDataBuilder
                        .customInstanced(ModuleRevealingGoggles::new, MekaMoreItems.REVEALING_UNIT)
                        .maxStackSize(MekaMoreItems.REVEALING_UNIT.asItem().getDefaultMaxStackSize())
                        .addConfig(ModuleRevealingGogglesConfig.createDefault(ModuleRevealingGogglesConfig.FILTERS_KEY),
                                ModuleRevealingGogglesConfig.CODEC,
                                ModuleRevealingGogglesConfig.STREAM_CODEC)
                ));


        modEventBus.addListener(MekaMoreModules::onInterModEnqueue);
    }

    private static void onInterModEnqueue(InterModEnqueueEvent event) {
        MekanismIMC.addMekaToolModules(MekaMoreModules.LOOTING_UNIT, MekaMoreModules.BRUSH_UNIT, MekaMoreModules.LIQUID_TANK_UNIT);
        MekanismIMC.addMekaSuitHelmetModules(MekaMoreModules.ZOOM_UNIT, MekaMoreModules.REVEALING_UNIT);
    }

    //#region registry

    private static DeferredItem<Item> addItemModule(String id,
                                                    Supplier<Holder<ModuleData<?>>> module,
                                                    Function<Item.Properties, Item.Properties> builderFn) {
        return MekaMore.ITEMS.register(id, () -> IModuleHelper.INSTANCE.createModuleItem(module, builderFn.apply(new Item.Properties())));
    }

    private static <T extends ICustomModule<T>> DeferredHolder<ModuleData<?>, ModuleData<T>> addModule(DeferredItem<Item> item,
                                                                                                       Supplier<@NotNull T> customModule) {
        return MekaMore.MODULES.register(item.getId().getPath(),
                () -> new ModuleData<T>(ModuleData.ModuleDataBuilder
                        .customInstanced(customModule, item)
                        .maxStackSize(item.asItem().getDefaultMaxStackSize())));
    }


    //#endregion registry
}