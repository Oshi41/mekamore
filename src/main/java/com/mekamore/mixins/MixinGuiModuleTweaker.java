package com.mekamore.mixins;

import com.mekamore.client.gui.window.GuiRevealGogglesOptionsScreen;
import com.mekamore.content.gear.reveal_googles.ModuleRevealingGogglesConfig;
import com.mekamore.registries.MekaMoreModules;
import mekanism.api.gear.IModule;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiModuleTweaker;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.custom.module.GuiModuleScreen;
import mekanism.common.content.gear.Module;
import mekanism.common.inventory.container.ModuleTweakerContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Adds a "Reveal filters" button to the Module Tweaker. It does NOT touch Mekanism's own Options button,
 * which opens the helmet HUD settings for every MekaSuit helmet regardless of the selected module.
 *
 * <p>The mixin extends GuiMekanism (the superclass of the target) so that protected members such as
 * {@code addRenderableWidget}, {@code menu}, {@code imageWidth} and {@code addWindow} are reachable without @Shadow.
 */
@Mixin(value = GuiModuleTweaker.class, remap = false)
public abstract class MixinGuiModuleTweaker extends GuiMekanism<ModuleTweakerContainer> {

    @Shadow private int selected;
    @Shadow private GuiModuleScreen moduleScreen;

    @Unique private MekanismButton mekamore$filtersButton;

    protected MixinGuiModuleTweaker(ModuleTweakerContainer container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Unique
    private static boolean mekamore$isRevealModule(IModule<?> module) {
        // TODO: adapt to how you registered the module
        return module != null && module.getUntypedData() == MekaMoreModules.REVEALING_UNIT.get();
    }

    /** Same row as Mekanism's Options button (y=137), in the column under the module config panel (x=150). Adjust if it overlaps. */
    @Inject(method = "addGuiElements", at = @At("TAIL"))
    private void mekamore$addFiltersButton(CallbackInfo ci) {
        mekamore$filtersButton = addRenderableWidget(new MekanismButton(this, 150, 137, 116, 16,
                Component.translatable("gui.mekamore.reveal_goggles.open"),
                (element, mouseX, mouseY) -> {
                    mekamore$openFilters();
                    return true;
                }));
        mekamore$filtersButton.active = false;
    }

    /** A module was picked in the list (or the selection changed). */
    @Inject(method = "onModuleSelected", at = @At("TAIL"))
    private void mekamore$onModuleSelected(Module<?> module, CallbackInfo ci) {
        if (mekamore$filtersButton != null) {
            mekamore$filtersButton.active = mekamore$isRevealModule(module);
        }
    }

    /** Selecting another item slot clears the module selection, so disable the button until a module is picked again. */
    @Inject(method = "select", at = @At("RETURN"))
    private void mekamore$onSlotSelected(int index, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (mekamore$filtersButton != null) {
            mekamore$filtersButton.active = false;
        }
    }

    @Unique
    private void mekamore$openFilters() {
        if (moduleScreen == null || selected == -1) {
            return;
        }
        IModule<?> module = moduleScreen.getCurrentModule();
        if (!mekamore$isRevealModule(module)) {
            return;
        }
        int slotIndex = menu.slots.get(selected).getSlotIndex();
        ModuleRevealingGogglesConfig config = (ModuleRevealingGogglesConfig) module.<List<ResourceKey<?>>>getConfigOrThrow(
                ModuleRevealingGogglesConfig.FILTERS_KEY);
        addWindow(new GuiRevealGogglesOptionsScreen(this,
                (imageWidth - GuiRevealGogglesOptionsScreen.W) / 2, (imageHeight - GuiRevealGogglesOptionsScreen.H) / 2,
                module, slotIndex, config));
    }
}