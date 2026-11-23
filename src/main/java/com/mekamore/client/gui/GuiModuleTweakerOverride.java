package com.mekamore.client.gui;

import mekanism.client.gui.GuiModuleTweaker;
import mekanism.client.gui.element.custom.module.GuiModuleScreen;
import mekanism.common.inventory.container.ModuleTweakerContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GuiModuleTweakerOverride extends GuiModuleTweaker {
    public GuiModuleTweakerOverride(ModuleTweakerContainer container, Inventory inv, Component title) {
        super(container, inv, title);
    }


    protected ItemStack getCurrentItem(){
        return renderables
                .stream()
                .filter(x -> x instanceof GuiModuleScreen)
                .map(x -> ((GuiModuleScreen)(x)).getContainerStack())
                .findFirst()
                .orElse(ItemStack.EMPTY);
    }
}
