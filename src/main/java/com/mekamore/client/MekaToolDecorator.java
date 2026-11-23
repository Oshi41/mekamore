package com.mekamore.client;

import com.mekamore.registries.MekaMoreModules;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

@OnlyIn(Dist.CLIENT)
public class MekaToolDecorator implements IItemDecorator {

    private static final int BAR_WIDTH = 13;
    private static final int BAR_HEIGHT = 2;
    private static final int BAR_X_OFFSET = 2;   // pixels from item left edge
    private static final int BAR_Y_OFFSET = 11;  // pixels from item top edge (13 = bottom area)


    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (stack.isEmpty()) {
            return false;
        }

        // Only render when the module is installed
        var module = IModuleHelper.INSTANCE.getIfEnabled(stack, MekaMoreModules.LIQUID_TANK_UNIT);
        if (module == null) {
            return false;
        }

        var handler = FluidUtil.getFluidHandler(stack).orElse(null);
        if (handler == null)
            return false;

        var fluid = handler.getFluidInTank(0);

        var total = handler.getTankCapacity(0);
        var current = fluid.getAmount();

        // Always draw the background so the bar slot is visible even when empty
        int bx = xOffset + BAR_X_OFFSET;
        int by = yOffset + BAR_Y_OFFSET;

        // Black background
        guiGraphics.fill(bx, by, bx + BAR_WIDTH, by + BAR_HEIGHT, 0xFF000000);

        if (current > 0 && total > 0) {
            int filled = Math.round((float) current / total * BAR_WIDTH);
            filled = Math.max(1, Math.min(filled, BAR_WIDTH)); // clamp [1, 13]

            // Use the fluid's natural color (same helper Mekanism uses for its bars)
            int color = getRGBColor(fluid) | 0xFF000000;

            guiGraphics.fill(bx, by, bx + filled, by + BAR_HEIGHT, color);
        }

        return true;
    }

    public static int getRGBColor(FluidStack stack){
        if (stack.isEmpty()) {
            return 0xFFFFFFFF;
        }
        //TODO: Technically doesn't support things where the color is part of the texture such as lava
        // for chemicals it is supported via allowing people to override getColorRepresentation in their
        // chemicals
        if (stack.getFluid().isSame(Fluids.LAVA)) {//Special case lava
            return 0xFFDB6B19;
        } else if (FMLEnvironment.dist.isClient()) {
            //Note: We can only return an accurate result on the client side. This method should never be called from the server
            // but in case it is make sure we only run on the client side
            return IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
        }
        return 0xFFFFFFFF;
    }
}

