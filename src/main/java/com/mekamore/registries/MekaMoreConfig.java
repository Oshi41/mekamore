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
        revealGooglesMaxFilters = builder
                .comment("Max amount of filters you can set with module Google of Revealing")
                .defineInRange("revealGooglesMaxFilters", 3, 1, 10);
        revealGooglesMaxScanRadius = builder
                .comment("How many blocks away from player can be scanned at once at max upgrades")
                .defineInRange("revealGooglesMaxScanRadius", 32, 1, 256);
        revealGooglesEnergyPerBlock = builder
                .comment("How many energy will be consumed by helmet per each scanned block")
                .defineInRange("revealGooglesEnergyPerBlock", 50, 0, Integer.MAX_VALUE);
        revealGooglesScanDelay = builder
                .comment("How many time in ticks will pass between 2 scans for same user")
                .defineInRange("revealGooglesScanDelay", 50, 10, Integer.MAX_VALUE);
        revealGooglesMaxResult = builder
                .comment("How many search results (max amount) of block / entities will return from serveer")
                .defineInRange("revealGooglesMaxResult", 48, 1, 1024);

        builder.pop();

        var spec = builder.build();
        container.registerConfig(ModConfig.Type.COMMON, spec);
    }

    //#endregion Register


    public static ModConfigSpec.LongValue mekaToolEnergyUsageLooting;
    public static ModConfigSpec.LongValue mekaToolEnergyUsageBrush;
    public static ModConfigSpec.LongValue mekaToolEnergyUsageZoom;
    public static ModConfigSpec.LongValue mekaToolLiquidTankEnergyPerOperation;
    public static ModConfigSpec.IntValue revealGooglesMaxFilters;
    public static ModConfigSpec.IntValue revealGooglesMaxScanRadius;
    public static ModConfigSpec.IntValue revealGooglesEnergyPerBlock;
    public static ModConfigSpec.IntValue revealGooglesScanDelay;
    public static ModConfigSpec.IntValue revealGooglesMaxResult;
}