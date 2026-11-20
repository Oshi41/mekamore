package com.mekamore.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.Lazy;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class MekaMoreUtils {
    public static class BlockSearchContext {
        public final BlockPos pos;
        public final Lazy<BlockEntity> entity;
        public final Lazy<BlockState> blockState;

        public BlockSearchContext(BlockPos pos, Level level) {
            this.pos = pos.west(0);
            this.entity = Lazy.of(() -> level.getBlockEntity(pos));
            this.blockState = Lazy.of(() -> level.getBlockState(pos));
        }
    }

    public static Stream<BlockSearchContext> findAOE(LivingEntity entity,
                                                     BlockHitResult hit,
                                                     int radius,
                                                     Predicate<BlockSearchContext> pred) {
        Vec3i adjustment = switch (hit.getDirection()) {
            case EAST, WEST -> new Vec3i(0, radius, radius);
            case UP, DOWN -> new Vec3i(radius, 0, radius);
            case SOUTH, NORTH -> new Vec3i(radius, radius, 0);
        };

        var level = entity.level();

        BlockPos first = hit.getBlockPos().subtract(adjustment);
        BlockPos second = hit.getBlockPos().offset(adjustment);
        AABB box = new AABB(first.getX(), first.getY(), first.getZ(), second.getX(), second.getY(), second.getZ());

        return BlockPos.betweenClosedStream(box)
                .map(x -> new BlockSearchContext(x, level))
                .filter(pred);
    }
}
