package com.mekamore.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MoreMekaConfig {
    private static final long MAX_ENERGY_PER_LEVEL = Long.MAX_VALUE / 4;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final Gear gear = new Gear();
    public static final ModConfigSpec SPEC = BUILDER.build();

    private MoreMekaConfig() {
    }

    public static class Gear {
        public final ModConfigSpec.LongValue mekaToolEnergyUsageLooting;

        private Gear() {
            BUILDER.push("gear");
            mekaToolEnergyUsageLooting = BUILDER
                  .comment("Energy used per installed Looting Unit level when the Meka-Tool hits an entity.")
                  .defineInRange("mekaToolEnergyUsageLooting", 10000L, 0L, MAX_ENERGY_PER_LEVEL);
            BUILDER.pop();
        }
    }
}
