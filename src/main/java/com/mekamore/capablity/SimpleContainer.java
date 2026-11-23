package com.mekamore.capablity;

import com.mekamore.registries.MekaMoreDataComponents;
import com.mekamore.registries.MekaMoreModules;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Objects;

public class SimpleContainer implements IFluidHandlerItem {

    private final ItemStack stack;

    public SimpleContainer(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack);

        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Container ItemStack cannot be empty");
        }
    }

    @Override
    public ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        var module = IModuleHelper.INSTANCE.getIfEnabled(
                stack,
                MekaMoreModules.LIQUID_TANK_UNIT
        );

        if (module == null) {
            return 0;
        }

        return (int) (Math.pow(10, module.getInstalledCount()) * FluidType.BUCKET_VOLUME);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluid) {
        if (fluid == null || fluid.isEmpty())
            return false;

        var current = this.getFluidInTank(tank);
        if (current.isEmpty()) return true;

        if (!FluidStack.isSameFluidSameComponents(current, fluid))
            return false;

        return current.getAmount() < getTankCapacity(tank);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }

        FluidStack current = getFluid();
        int capacity = getTankCapacity(0);
        int space = capacity - current.getAmount();

        if (space <= 0) {
            return 0;
        }

        int accepted = Math.min(space, resource.getAmount());

        if (action.execute()) {
            FluidStack updated = current.isEmpty()
                    ? resource.copyWithAmount(accepted)
                    : current.copyWithAmount(current.getAmount() + accepted);

            setFluid(updated);
        }

        return accepted;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || stack.getCount() != 1) {
            return FluidStack.EMPTY;
        }

        FluidStack current = getFluid();

        if (current.isEmpty()) {
            return FluidStack.EMPTY;
        }

        int drained = Math.min(maxDrain, current.getAmount());

        FluidStack result = current.copyWithAmount(drained);

        if (action.execute()) {
            int remaining = current.getAmount() - drained;

            if (remaining <= 0) {
                setFluid(FluidStack.EMPTY);
            } else {
                setFluid(current.copyWithAmount(remaining));
            }
        }

        return result;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || stack.getCount() != 1) {
            return FluidStack.EMPTY;
        }

        FluidStack current = getFluid();

        if (current.isEmpty()
                || !FluidStack.isSameFluidSameComponents(current, resource)) {
            return FluidStack.EMPTY;
        }

        return drain(resource.getAmount(), action);
    }

    private FluidStack getFluid() {
        return stack.getOrDefault(MekaMoreDataComponents.MEKA_TOOL_FLUID.get(), SimpleFluidContent.EMPTY).copy();
    }

    private void setFluid(FluidStack fluid) {
        stack.set(MekaMoreDataComponents.MEKA_TOOL_FLUID.get(),
                fluid.isEmpty()
                        ? SimpleFluidContent.EMPTY
                        : SimpleFluidContent.copyOf(fluid)
        );
    }
}
