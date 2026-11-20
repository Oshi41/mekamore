package com.mekamore.registries;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class MekaMoreConfig {

    //#region Register

    public static void register(ModContainer container) {
        var builder = new ModConfigSpec.Builder();

        builder.translation("modular_units").push("modular_units");

        mekaToolEnergyUsageLooting = builder
                .comment("How much energy is consumed after entity was slain by this Meka-Tool")
                .translation("mekamore.configuration.mekaToolEnergyUsageLooting")
                .defineInRange("mekaToolEnergyUsageLooting", 10000L, 0, Integer.MAX_VALUE);
        mekaToolEnergyUsageBrush = builder
                .comment("How much energy is consumed for every brushed block")
                .translation("mekamore.configuration.mekaToolEnergyUsageBrush")
                .defineInRange("mekaToolEnergyUsageBrush", 25000L, 0, Integer.MAX_VALUE);
        mekaToolEnergyUsageZoom = builder
                .comment("How much energy is consumed per tick while zooming")
                .translation("mekamore.configuration.module_zoom")
                .defineInRange("mekaToolEnergyUsageZoom", 500L, 0, Integer.MAX_VALUE);
        mekaToolLiquidTankEnergyPerOperation = builder
                .comment("How much energy will be consumed per bucket")
                .defineInRange("mekaToolLiquidTankEnergyPerOperation", 10000L, 0, Integer.MAX_VALUE);

        builder.pop();

        var spec = builder.build();
        container.registerConfig(ModConfig.Type.COMMON, spec);
    }

    //#endregion Register


    public static ModConfigSpec.LongValue mekaToolEnergyUsageLooting;
    public static ModConfigSpec.LongValue mekaToolEnergyUsageBrush;
    public static ModConfigSpec.LongValue mekaToolEnergyUsageZoom;
    public static ModConfigSpec.LongValue mekaToolLiquidTankEnergyPerOperation;
}