package com.mekamore.content.gear;

import com.mekamore.MekaMore;
import com.mekamore.registries.MekaMoreConfig;
import com.mekamore.registries.MekaMoreModules;
import mekanism.api.gear.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = MekaMore.MODID)
public class ModuleLootingUnit implements ICustomModule<ModuleLootingUnit> {
    private static final ResourceLocation LOOTING_UNIT = MekaMore.rl("looting_unit");

    @Override
    public void adjustAttributes(IModule<ModuleLootingUnit> module, ItemAttributeModifierEvent event) {
        var baseEnergy = MekaMoreConfig.mekaToolEnergyUsageLooting.get();
        var totalEnergy = module.getContainerEnergy(event.getItemStack());
        if (baseEnergy > 0 && totalEnergy < baseEnergy) return;

        var lootingLevel = baseEnergy == 0 ? module.getData().getMaxStackSize()
              : Math.min(module.getData().getMaxStackSize(), (int) Math.log10((double) totalEnergy / baseEnergy) + 1);
        if (lootingLevel < 1) return;


        var modifier = new AttributeModifier(LOOTING_UNIT, lootingLevel, AttributeModifier.Operation.ADD_VALUE);
        event.addModifier(Attributes.LUCK, modifier, EquipmentSlotGroup.MAINHAND);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        var attacker = event.getSource().getEntity();
        var weaponItem = event.getSource().getWeaponItem();
        if (weaponItem == null || !(attacker instanceof LivingEntity) || attacker.level().isClientSide()) return;

        var module = IModuleHelper.INSTANCE.getIfEnabled(weaponItem, MekaMoreModules.LOOTING_UNIT);
        if (module == null) return;

        var consume = Math.pow(10, module.getInstalledCount()) * MekaMoreConfig.mekaToolEnergyUsageLooting.get();
        module.useEnergy((LivingEntity) attacker, weaponItem, (long)consume, true);
    }
}