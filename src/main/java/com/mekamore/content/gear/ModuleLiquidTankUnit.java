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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddAttributeTooltipsEvent;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.Optional;

@EventBusSubscriber(modid = MekaMore.MODID)
public final class ModuleLiquidTankUnit implements ICustomModule<ModuleLiquidTankUnit> {

    private static Item canteenItem;

    @Override
    public boolean canPerformAction(IModule<ModuleLiquidTankUnit> module, IModuleContainer moduleContainer, ItemStack stack, ItemAbility ability) {
        return ICustomModule.super.canPerformAction(module, moduleContainer, stack, ability);
    }

    @Override
    public InteractionResult onItemUse(IModule<ModuleLiquidTankUnit> module, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator()) return InteractionResult.PASS;

        Level level = context.getLevel();
        ItemStack toolStack = context.getItemInHand();
        // radius 0 = single block, radius 3 = 7x7
        int radius = module.getInstalledCount() - 1;
        long energyCost = MekaMoreConfig.mekaToolLiquidTankEnergyPerOperation.getAsLong();
        boolean pickupMode = !player.isShiftKeyDown();

        // One raycast, here, used for everything below
        BlockHitResult hit = Item.getPlayerPOVHitResult(
                level, player,
                pickupMode ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE
        );
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;

        // Acquire our handler once — it wraps toolStack by reference via SimpleContainer,
        // so mutations are immediately visible across loop iterations
        IFluidHandlerItem tank = FluidUtil.getFluidHandler(toolStack).orElse(null);
        if (tank == null) return InteractionResult.PASS;

        // Let the client predict success; all real work happens on the server
        if (level.isClientSide()) return InteractionResult.sidedSuccess(true);

        boolean changed = pickupMode
                ? doPickup(module, player, level, toolStack, tank, hit, radius, energyCost)
                : doPlace(module, player, level, toolStack, tank, hit, radius, energyCost);

        return changed ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    // ── PICKUP: world fluid source → tank ────────────────────────────────────

    private static boolean doPickup(
            IModule<ModuleLiquidTankUnit> module,
            Player player, Level level, ItemStack toolStack,
            IFluidHandlerItem tank,
            BlockHitResult hit, int radius, long energyCost
    ) {
        var candidates = MekaMoreUtils.findAOE(
                player, hit, radius,
                ctx -> {
                    BlockState state = ctx.blockState.get();
                    if (!(state.getBlock() instanceof BucketPickup)) return false;
                    FluidState fs = state.getFluidState();
                    // Only source blocks — flowing blocks give less than a full bucket
                    return !fs.isEmpty() && fs.isSource();
                }
        ).toList();

        boolean changed = false;

        for (var candidate : candidates) {
            // Re-check capacity each iteration; it was updated by previous fills
            if (tank.getTankCapacity(0) <= tank.getFluidInTank(0).getAmount()) break;
            if (!module.hasEnoughEnergy(toolStack, energyCost)) break;

            BlockState state = level.getBlockState(candidate.pos);
            if (!(state.getBlock() instanceof BucketPickup pickup)) continue;

            FluidState fluidState = state.getFluidState();
            if (fluidState.isEmpty() || !fluidState.isSource()) continue;

            FluidStack expected = new FluidStack(fluidState.getType(), FluidType.BUCKET_VOLUME);

            // Simulate fill BEFORE touching the world — bail if the tank won't take a full bucket
            if (tank.fill(expected, IFluidHandler.FluidAction.SIMULATE) < FluidType.BUCKET_VOLUME) continue;

            // Remove the source block from the world (vanilla BucketPickup)
            ItemStack filledBucket = pickup.pickupBlock(player, level, candidate.pos, state);
            if (filledBucket.isEmpty()) continue;

            // Extract the actual fluid from the filled bucket
            FluidStack pickedFluid = FluidUtil.getFluidContained(filledBucket).orElse(FluidStack.EMPTY);
            if (pickedFluid.isEmpty()) continue;

            // Fill our tank directly — no ItemStack juggling
            int accepted = tank.fill(pickedFluid, IFluidHandler.FluidAction.EXECUTE);
            if (accepted <= 0) {
                // Shouldn't happen after a successful simulate, but don't destroy fluid
                // Put the bucket back in the player's inventory as a safety net
                if (!player.addItem(filledBucket)) {
                    player.drop(filledBucket, false);
                }
                continue;
            }

            module.useEnergy(player, toolStack, energyCost, true);
            player.awardStat(Stats.ITEM_USED.get(toolStack.getItem()));
            level.gameEvent(player, GameEvent.FLUID_PICKUP, candidate.pos);
            pickup.getPickupSound(state).ifPresent(sound ->
                    level.playSound(null, candidate.pos, sound, SoundSource.BLOCKS, 1f, 1f));

            changed = true;
        }

        return changed;
    }

    // ── PLACEMENT: tank → world ───────────────────────────────────────────────

    private static boolean doPlace(
            IModule<ModuleLiquidTankUnit> module,
            Player player, Level level, ItemStack toolStack,
            IFluidHandlerItem tank,
            BlockHitResult hit, int radius, long energyCost
    ) {
        FluidStack stored = tank.getFluidInTank(0);
        if (stored.isEmpty() || stored.getAmount() < FluidType.BUCKET_VOLUME) return false;

        // Placement goes into the face-adjacent block, not the block hit itself
        BlockHitResult placementHit = hit.withPosition(
                hit.getBlockPos().relative(hit.getDirection())
        );

        // Snapshot the fluid type — we don't want to mix fluids mid-AOE
        final FluidStack fluidType = stored.copyWithAmount(FluidType.BUCKET_VOLUME);

        var candidates = MekaMoreUtils.findAOE(
                player, placementHit, radius,
                ctx -> {
                    BlockState state = ctx.blockState.get();
                    if (state.getBlock() instanceof LiquidBlockContainer container) {
                        return container.canPlaceLiquid(player, level, ctx.pos, state, fluidType.getFluid());
                    }
                    return state.canBeReplaced(fluidType.getFluid());
                }
        ).toList();

        boolean changed = false;

        for (var candidate : candidates) {
            // Re-read tank each iteration — previous placements drained it
            stored = tank.getFluidInTank(0);
            if (stored.isEmpty() || stored.getAmount() < FluidType.BUCKET_VOLUME) break;
            if (!module.hasEnoughEnergy(toolStack, energyCost)) break;

            BlockState state = candidate.blockState.get();

            // Drain one bucket from our tank first (simulate)
            FluidStack toDrain = stored.copyWithAmount(FluidType.BUCKET_VOLUME);
            FluidStack simDrain = tank.drain(toDrain, IFluidHandler.FluidAction.SIMULATE);
            if (simDrain.getAmount() < FluidType.BUCKET_VOLUME) continue;

            // Try to place fluid into the world block
            boolean placed = tryPlaceIntoBlock(player, level, candidate.pos, state, toDrain);
            if (!placed) continue;

            // World accepted it — now actually drain from the tank
            tank.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);

            module.useEnergy(player, toolStack, energyCost, true);
            changed = true;
        }

        return changed;
    }

    /**
     * Places one bucket of fluid into a world block.
     * Handles both LiquidBlockContainer (e.g. waterlogged slabs) and
     * plain replaceable blocks (air, grass, etc.).
     * Fires sounds and game events like vanilla bucket placement does.
     */
    private static boolean tryPlaceIntoBlock(
            Player player, Level level,
            BlockPos pos, BlockState state,
            FluidStack fluid
    ) {
        if (state.getBlock() instanceof LiquidBlockContainer container) {
            if (!container.canPlaceLiquid(player, level, pos, state, fluid.getFluid()))
                return false;

            container.placeLiquid(level, pos, state, fluid.getFluid().defaultFluidState());
            playPlaceSound(level, pos, fluid);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            return true;
        }

        if (state.canBeReplaced(fluid.getFluid())) {
            // Replace the block with the fluid's own block (same as vanilla empty-bucket)
            Block fluidBlock = fluid.getFluid().defaultFluidState().createLegacyBlock().getBlock();
            level.setBlockAndUpdate(pos, fluidBlock.defaultBlockState());
            playPlaceSound(level, pos, fluid);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            return true;
        }

        return false;
    }

    private static void playPlaceSound(Level level, BlockPos pos, FluidStack fluid) {
        Optional<SoundEvent> sound = Optional.ofNullable(
                fluid.getFluid().getFluidType().getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY)
        );
        sound.ifPresent(s -> level.playSound(null, pos, s, SoundSource.BLOCKS, 1f, 1f));
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onAddTooltip(AddAttributeTooltipsEvent event) {
        var key = event.getStack().getItemHolder().getKey();
        if (key == null
                || !key.location().getNamespace().equals(MekanismAPI.MEKANISM_MODID)
                || !key.location().getPath().equals("meka_tool")) {
            return;
        }

        var module = IModuleHelper.INSTANCE.getIfEnabled(event.getStack(), MekaMoreModules.LIQUID_TANK_UNIT);
        if (module == null) return;

        var contained = FluidUtil.getFluidContained(event.getStack());
        if (contained.isEmpty() || contained.get().getAmount() <= 0) return;

        if (canteenItem == null) {
            canteenItem = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "canteen"));
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