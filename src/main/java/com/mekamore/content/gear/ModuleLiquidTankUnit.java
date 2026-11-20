package com.mekamore.content.gear;

import com.mekamore.MekaMore;
import com.mekamore.registries.MekaMoreConfig;
import com.mekamore.registries.MekaMoreModules;
import com.mekamore.utils.MekaMoreUtils;
import mekanism.api.MekanismAPI;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddAttributeTooltipsEvent;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.ArrayList;

/**
 * Meka-Tool liquid tank module.
 * <p>
 * Capacity: 500 buckets per installed module (maximum four modules).
 * Each use transfers at most one bucket per installed module.
 * AOE radius: 0/1/2/3 for one/two/three/four installed modules.
 * <p>
 * Register the capability listener on the MOD event bus and register
 * MekaMoreDataComponents.DATA_COMPONENTS on that same bus.
 */
@EventBusSubscriber(modid = MekaMore.MODID)
public class ModuleLiquidTankUnit implements ICustomModule<ModuleLiquidTankUnit> {
    private static Item canteenItem;

    @Override
    public boolean canPerformAction(IModule<ModuleLiquidTankUnit> module, IModuleContainer moduleContainer, ItemStack stack, ItemAbility ability) {
        return ICustomModule.super.canPerformAction(module, moduleContainer, stack, ability);
    }

    @Override
    public InteractionResult onItemUse(
            IModule<ModuleLiquidTankUnit> module,
            UseOnContext context
    ) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack toolStack = context.getItemInHand();
        int radius = module.getInstalledCount();
        var energyCost = MekaMoreConfig.mekaToolLiquidTankEnergyPerOperation.getAsLong();

        boolean pickupMode = !player.isShiftKeyDown();
        var hit = Item.getPlayerPOVHitResult(
                level,
                player,
                pickupMode ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE
        );

        if (hit.getType() == HitResult.Type.MISS)
            return InteractionResult.PASS;

        var tank = FluidUtil.getFluidHandler(toolStack).orElse(null);
        if (tank == null)
            return InteractionResult.PASS;

        if (level.isClientSide())
            return InteractionResult.sidedSuccess(true);

        var candidates = MekaMoreUtils.findAOE(
                player,
                hit,
                radius,
                pickupMode
                        ? (search -> search.blockState.get().getBlock() instanceof BucketPickup)
                        : (search -> true)
        ).toList();

        var wasCHanged = false;

        for (MekaMoreUtils.BlockSearchContext candidate : candidates) {

            if (!pickupMode && tank.getFluidInTank(0).isEmpty())
                break;

            if (pickupMode && tank.getTankCapacity(0) <= tank.getFluidInTank(0).getAmount())
                break;

            if (!module.hasEnoughEnergy(toolStack, energyCost))
                break;

            if (pickupMode) {
                var pickupTry = FluidUtil.tryPickUpFluid(toolStack, player, player.level(), candidate.pos, hit.getDirection());
                if (pickupTry.isSuccess()) {
                    player.setItemInHand(player.swingingArm, pickupTry.getResult());

                    wasCHanged = true;
                    module.useEnergy(player, toolStack, energyCost);
                }
            }

            if (!pickupMode && FluidUtil.tryPlaceFluid(player, player.level(), player.swingingArm, candidate.pos, tank, tank.getFluidInTank(0))) {
                wasCHanged = true;
                module.useEnergy(player, toolStack, energyCost);
            }
        }

        return wasCHanged ? InteractionResult.SUCCESS : InteractionResult.PASS;


//
//        if (player == null || player.isSpectator()) {
//            return InteractionResult.PASS;
//        }
//
//        // All world modifications must happen on the logical server.
//        if (level.isClientSide()) {
//            return InteractionResult.SUCCESS;
//        }
//
//        // Get the item fluid handler.
//        IFluidHandlerItem tank = FluidUtil.getFluidHandler(toolStack).orElse(null);
//        if (tank == null || toolStack.getCount() != 1) {
//            return InteractionResult.PASS;
//        }
//
//        int radius = Math.max(0, module.getInstalledCount() - 1);
//
//        long energyCost = MekaMoreConfig
//                .mekaToolLiquidTankEnergyPerOperation.getAsLong();
//
//        if (energyCost < 0) {
//            return InteractionResult.PASS;
//        }
//
//        boolean pickupMode = !player.isShiftKeyDown();
//
//        // ============================================================
//        // PICKUP: World -> Meka-Tool
//        // ============================================================
//
//        if (pickupMode) {
//            FluidStack stored = tank.getFluidInTank(0);
//            int capacity = tank.getTankCapacity(0);
//
//            if (capacity <= stored.getAmount()) {
//                return InteractionResult.PASS;
//            }
//
//            var hit = Item.getPlayerPOVHitResult(
//                    level,
//                    player,
//                    ClipContext.Fluid.SOURCE_ONLY
//            );
//
//            if (hit.getType() != HitResult.Type.BLOCK) {
//                return InteractionResult.PASS;
//            }
//
//            var candidates = MekaMoreUtils.findAOE(
//                    player,
//                    hit,
//                    radius,
//                    search -> search.blockState.get().getBlock()
//                            instanceof BucketPickup
//            ).toList();
//
//            boolean changed = false;
//
//            for (var candidate : candidates) {
//                if (!player.mayBuild()
//                        || !level.mayInteract(player, candidate.pos)) {
//                    continue;
//                }
//
//                if (tank.getTankCapacity(0)
//                        <= tank.getFluidInTank(0).getAmount()) {
//                    break;
//                }
//
//                if (!module.hasEnoughEnergy(toolStack, energyCost)) {
//                    break;
//                }
//
//                BlockState state = level.getBlockState(candidate.pos);
//
//                if (!(state.getBlock() instanceof BucketPickup pickup)) {
//                    continue;
//                }
//
//                FluidState fluidState = state.getFluidState();
//
//                if (fluidState.isEmpty() || !fluidState.isSource()) {
//                    continue;
//                }
//
//                FluidStack expected = new FluidStack(
//                        fluidState.getType(),
//                        FluidType.BUCKET_VOLUME
//                );
//
//                // Simulate before removing the source block.
//                if (tank.fill(expected, IFluidHandler.FluidAction.SIMULATE)
//                        < FluidType.BUCKET_VOLUME) {
//                    continue;
//                }
//
//                // Use vanilla BucketPickup behavior.
//                ItemStack filledBucket = pickup.pickupBlock(
//                        player,
//                        level,
//                        candidate.pos,
//                        state
//                );
//
//                if (filledBucket.isEmpty()) {
//                    continue;
//                }
//
//                FluidStack pickedFluid = FluidUtil
//                        .getFluidContained(filledBucket)
//                        .orElse(FluidStack.EMPTY);
//
//                if (pickedFluid.isEmpty()) {
//                    continue;
//                }
//
//                int accepted = tank.fill(
//                        pickedFluid,
//                        IFluidHandler.FluidAction.EXECUTE
//                );
//
//                if (accepted < pickedFluid.getAmount()) {
//                    // Never destroy fluid if a third-party handler behaves
//                    // differently between simulation and execution.
//                    ItemStack remainder = FluidUtil.getFilledBucket(
//                            pickedFluid.copyWithAmount(
//                                    pickedFluid.getAmount() - accepted
//                            )
//                    );
//
//                    if (accepted == 0) {
//
//                    } else {
//
//                    }
//                }
//
//                if (accepted <= 0) {
//                    continue;
//                }
//
//                module.useEnergy(player, toolStack, energyCost, true);
//
//                player.awardStat(Stats.ITEM_USED.get(toolStack.getItem()));
//
//                level.gameEvent(
//                        player,
//                        GameEvent.FLUID_PICKUP,
//                        candidate.pos
//                );
//
//                pickup.getPickupSound(state).ifPresent(sound ->
//                        level.playSound(
//                                null,
//                                candidate.pos,
//                                sound,
//                                SoundSource.BLOCKS,
//                                1.0F,
//                                1.0F
//                        )
//                );
//
//                changed = true;
//            }
//
//            return changed
//                    ? InteractionResult.SUCCESS
//                    : InteractionResult.PASS;
//        }
//
//        // ============================================================
//        // PLACEMENT: Meka-Tool -> World
//        // ============================================================
//
//        FluidStack stored = tank.getFluidInTank(0);
//
//        if (stored.getAmount() < FluidType.BUCKET_VOLUME) {
//            return InteractionResult.PASS;
//        }
//
//        var hit = Item.getPlayerPOVHitResult(
//                level,
//                player,
//                ClipContext.Fluid.NONE
//        );
//
//        if (hit.getType() != HitResult.Type.BLOCK) {
//            return InteractionResult.PASS;
//        }
//
//        // BucketItem places fluid in the adjacent block.
//        BlockPos placementOrigin = hit.getBlockPos().relative(hit.getDirection());
//
//        final FluidStack fluidToPlace = stored.copy();
//        var candidates = MekaMoreUtils.findAOE(
//                player,
//                hit,
//                radius,
//                search -> {
//                    BlockState state = search.blockState.get();
//
//                    return state.canBeReplaced(fluidToPlace.getFluid())
//                            || state.getBlock() instanceof LiquidBlockContainer;
//                }
//        ).toList();
//
//        boolean changed = false;
//
//        for (var candidate : candidates) {
//            stored = tank.getFluidInTank(0);
//
//            if (stored.getAmount() < FluidType.BUCKET_VOLUME) {
//                break;
//            }
//
//            if (!module.hasEnoughEnergy(toolStack, energyCost)) {
//                break;
//            }
//
//            BlockState state = level.getBlockState(candidate.pos);
//
//            boolean canPlace = state.canBeReplaced(stored.getFluid());
//
//            if (state.getBlock() instanceof LiquidBlockContainer container) {
//                canPlace = container.canPlaceLiquid(
//                        player,
//                        level,
//                        candidate.pos,
//                        state,
//                        stored.getFluid()
//                ) || canPlace;
//            }
//
//            if (!canPlace) {
//                continue;
//            }
//
//            FluidStack toPlace = stored.copyWithAmount(
//                    FluidType.BUCKET_VOLUME
//            );
//
//            // NeoForge implementation modeled after BucketItem.emptyContents.
//            // It handles LiquidBlockContainer, replaceable blocks,
//            // vaporization, sounds and draining the handler.
//            boolean placed = FluidUtil.tryPlaceFluid(
//                    player,
//                    level,
//                    context.getHand(),
//                    candidate.pos,
//                    tank,
//                    toPlace
//            );
//
//            if (!placed) {
//                continue;
//            }
//
//            module.useEnergy(player, toolStack, energyCost, true);
//
//            changed = true;
//        }
//
//        return changed
//                ? InteractionResult.SUCCESS
//                : InteractionResult.PASS;
    }

    @SubscribeEvent
    public static void onAddTooltip(AddAttributeTooltipsEvent event) {
        var key = event.getStack().getItemHolder().getKey();
        if (key != null
                && key.location().getNamespace().equals(MekanismAPI.MEKANISM_MODID)
                && key.location().getPath().equals("meka_tool")
        ) {
            var module = IModuleHelper.INSTANCE.getIfEnabled(event.getStack(), MekaMoreModules.LIQUID_TANK_UNIT);
            if (module != null) {
                var tank = FluidUtil.getFluidContained(event.getStack());
                if (tank.isPresent() && tank.get().getAmount() > 0) {

                    if (canteenItem == null) {
                        canteenItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "canteen"));
                    }

                    var array = new ArrayList<Component>() {
                        @Override
                        public boolean add(Component component) {
                            event.addTooltipLines(component);
                            return super.add(component);
                        }
                    };

                    canteenItem.appendHoverText(event.getStack(), event.getContext(), array, event.getContext().flag());
                }
            }
        }
    }
}
