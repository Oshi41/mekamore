package com.mekamore.content.gear;

import com.mekamore.MekaMore;
import com.mekamore.registries.KeyBinding;
import com.mekamore.registries.MekaMoreConfig;
import com.mekamore.registries.MekaMoreModules;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.function.Consumer;

/**
 * Helmet module that zooms the camera when the zoom key (Z by default) is held.
 * <ul>
 *     <li>Uses energy while zooming; configure the per-tick cost in MekaMoreConfig.</li>
 *     <li>FOV is multiplied by { the number of installed units
 *         provides finer zoom (each unit shaves the multiplier closer to the minimum).</li>
 *     <li>Key registration is client-only and happens on the mod-event bus.</li>
 * </ul>
 * All server/client split and energy logic is kept here in one file.
 */
@EventBusSubscriber(modid = MekaMore.MODID)
public class ModuleZoomUnit implements ICustomModule<ModuleZoomUnit> {

    // -------------------------------------------------------------------------
    // Zoom parameters
    // -------------------------------------------------------------------------

    /**
     * Minimum FOV multiplier at max installed count (closest zoom).
     */
    private static final float MIN_FOV_MULTIPLIER = 0.15f;
    /**
     * FOV multiplier when only 1 unit is installed (lightest zoom).
     */
    private static final float MAX_FOV_MULTIPLIER = 0.5f;

    // -------------------------------------------------------------------------
    // ICustomModule implementation
    // -------------------------------------------------------------------------

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickClient(IModule<ModuleZoomUnit> module, IModuleContainer moduleContainer, ItemStack stack, Player player) {
        // Nothing to do client-side tick; zoom is applied in the FOV event below.
    }

    @Override
    public void tickServer(IModule<ModuleZoomUnit> module, IModuleContainer moduleContainer, ItemStack stack, Player player) {
        // Drain energy every tick while the module is enabled.
        // Actual zoom activation is client-only (key state), but we always drain
        // a small idle cost to match Mekanism's convention for visor modules.
        // If you only want to drain while zooming, see the note at the bottom of this file.
        module.useEnergy(player, stack, MekaMoreConfig.mekaToolEnergyUsageZoom.getAsLong());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHUDStrings(IModule<ModuleZoomUnit> module, IModuleContainer moduleContainer, ItemStack stack, Player player,
                              Consumer<net.minecraft.network.chat.Component> hudStringAdder) {
        if (module.isEnabled()) {
            hudStringAdder.accept(net.minecraft.network.chat.Component.translatable(
                    "hud.mekamore.module_zoom",
                    Math.round((1.0f - zoomFovMultiplier(module)) * 100)));
        }
    }

    // -------------------------------------------------------------------------
    // FOV event (client-only)
    // -------------------------------------------------------------------------

    /**
     * Fired every frame. Reduces FOV while the zoom key is held and the module
     * is installed + enabled + has energy.
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!(event.getCamera().getEntity() instanceof Player player)) {
            return;
        }
        if (!isZoomKeyDown()) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return;
        }
        IModule<ModuleZoomUnit> module = IModuleHelper.INSTANCE.getIfEnabled(head, MekaMoreModules.ZOOM_UNIT);
        if (module == null) {
            return;
        }
        if (!module.hasEnoughEnergy(head, MekaMoreConfig.mekaToolEnergyUsageZoom)) {
            return;
        }
        float multiplier = zoomFovMultiplier(module);
        event.setFOV(event.getFOV() * multiplier);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the FOV multiplier for the given module instance.
     * With 1 unit installed the view is zoomed to {@link #MAX_FOV_MULTIPLIER} of default.
     * Each additional unit moves linearly toward {@link #MIN_FOV_MULTIPLIER}.
     */
    private static float zoomFovMultiplier(IModule<ModuleZoomUnit> module) {
        int installed = module.getInstalledCount();
        int maxStack = module.getUntypedData().getMaxStackSize();
        float t = maxStack > 1 ? (float) (installed - 1) / (maxStack - 1) : 1f;
        return MAX_FOV_MULTIPLIER + t * (MIN_FOV_MULTIPLIER - MAX_FOV_MULTIPLIER);
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isZoomKeyDown() {
        return KeyBinding.ZOOM.isDown();
    }
}