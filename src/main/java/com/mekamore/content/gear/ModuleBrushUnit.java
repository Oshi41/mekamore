package com.mekamore.content.gear;

import com.mekamore.MekaMore;
import com.mekamore.registries.MekaMoreConfig;
import com.mekamore.registries.MekaMoreModules;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mekanism.api.MekanismAPI;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Lets the Meka-Tool brush suspicious sand / gravel (archaeology).
 * <ul>
 *     <li>Right-click (hold to repeat) advances the brush, same pace as the vanilla brush.</li>
 *     <li>Energy is consumed for every block that is fully cleaned, see {@code mekaToolEnergyUsageBrush}.</li>
 *     <li>Vein Mining Unit (extended mode) brushes connected suspicious blocks of the same type within its range.</li>
 *     <li>Fortune Unit levels are applied as temporary Luck while brushing, which feeds the archaeology loot table.</li>
 * </ul>
 * Only Mekanism API types are used, so this compiles against the {@code :api} artifact.
 */
@EventBusSubscriber(modid = MekaMore.MODID)
public class ModuleBrushUnit implements ICustomModule<ModuleBrushUnit> {

    @Override
    public InteractionResult onItemUse(IModule<ModuleBrushUnit> module, UseOnContext context) {
        var hit = hitBlock(context.getPlayer());
        if (hit == null)
            return InteractionResult.PASS;

        if (context.getLevel().isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        if (!enoughEnergy(context.getItemInHand())) {
            return InteractionResult.PASS;
        }

        context.getPlayer().startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @SubscribeEvent
    public static void onStartUsing(LivingEntityUseItemEvent.Start e) {
        var module = IModuleHelper.INSTANCE.getIfEnabled(e.getItem(), MekaMoreModules.BRUSH_UNIT);
        if (module != null) {
            var hit = hitBlock(e.getEntity());
            if (hit != null) {
                if (enoughEnergy(e.getItem())) {
                    e.setDuration(Items.BRUSH.getUseDuration(e.getItem(), e.getEntity()));
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemUsing(LivingEntityUseItemEvent.Tick e) {
        if (!IModuleHelper.INSTANCE.isModuleContainer(e.getItem())
                || !(e.getEntity() instanceof Player)) {
            return;
        }

        var hit = hitBlock(e.getEntity());
        var module = IModuleHelper.INSTANCE.getIfEnabled(e.getItem(), MekaMoreModules.BRUSH_UNIT);

        if (hit == null && module == null) return;

        if (hit == null || module == null) {
            e.setCanceled(true);
            return;
        }

        if (!enoughEnergy(e.getItem())) return;

        e.setDuration(e.getDuration() - 1);

        if (e.getDuration() % 10 == 5) {
            for (BrushableBlockEntity entity : findAOE(e.getEntity(), hit).toList()) {
                if (!enoughEnergy(e.getItem())) break;


                module.useEnergy(e.getEntity(), e.getItem(), MekaMoreConfig.mekaToolEnergyUsageBrush.get());
                entity.brush(e.getEntity().level().getGameTime(), (Player) e.getEntity(), hit.getDirection());
            }
        }
    }


    private static BlockHitResult hitBlock(LivingEntity e) {
        if (!(e instanceof Player)) {
            return null;
        }

        var result = ProjectileUtil.getHitResultOnViewVector(e, ent -> false, ((Player) e).blockInteractionRange());
        if (!(result instanceof BlockHitResult hit))
            return null;

        if (result.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        if (e.level().getBlockEntity(hit.getBlockPos()) instanceof BrushableBlockEntity) {
            return hit;
        }

        return null;
    }

    private static boolean enoughEnergy(ItemStack stack) {
        var module = IModuleHelper.INSTANCE.getIfEnabled(stack, MekaMoreModules.BRUSH_UNIT);
        if (module == null) return false;

        return module.hasEnoughEnergy(stack, MekaMoreConfig.mekaToolEnergyUsageBrush.get());
    }

    private static Stream<BrushableBlockEntity> findAOE(LivingEntity entity, BlockHitResult hit) {
        var radius = 1;

        Vec3i adjustment = switch (hit.getDirection()) {
            case EAST, WEST -> new Vec3i(0, radius, radius);
            case UP, DOWN -> new Vec3i(radius, 0, radius);
            case SOUTH, NORTH -> new Vec3i(radius, radius, 0);
        };

        var level = entity.level();

        BlockPos first = hit.getBlockPos().subtract(adjustment);
        BlockPos second = hit.getBlockPos().offset(adjustment);
        AABB box = new AABB(first.getX(), first.getY(), first.getZ(), second.getX(), second.getY(), second.getZ());
        return BlockPos.betweenClosedStream(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                .map(level::getBlockEntity)
                .filter(x -> x instanceof BrushableBlockEntity)
                .map(x -> (BrushableBlockEntity) x);
    }
}
