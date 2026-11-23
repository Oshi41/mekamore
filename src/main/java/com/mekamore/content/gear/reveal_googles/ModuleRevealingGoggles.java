package com.mekamore.content.gear.reveal_googles;

import com.mekamore.network.packages.ScanResult;
import com.mekamore.registries.MekaMoreConfig;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.config.ModuleConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Reveal Goggles: scans around the wearer for the blocks / entities listed in the module's
 * {@link ModuleRevealingGogglesConfig} (edited through {@code GuiRevealGogglesOptions}).
 *
 * <p>Registration must include the config:
 * <pre>{@code
 * .addConfig(ModuleRevealingGogglesConfig.createDefault(ModuleRevealingGogglesConfig.FILTERS_KEY),
 *            ModuleRevealingGogglesConfig.CODEC, ModuleRevealingGogglesConfig.STREAM_CODEC)
 * }</pre>
 */
public final class ModuleRevealingGoggles implements ICustomModule<ModuleRevealingGoggles> {

    @Override
    public void tickServer(IModule<ModuleRevealingGoggles> module, IModuleContainer moduleContainer, ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // Stateless cooldown: no per-player map to leak (this instance is shared by every wearer)
        if (player.tickCount % MekaMoreConfig.revealGooglesScanDelay.getAsInt() != 0) {
            return;
        }

        ModuleConfig<List<ResourceKey<?>>> config = module.getConfig(ModuleRevealingGogglesConfig.FILTERS_KEY);
        if (config == null || config.get().isEmpty()) {
            return;
        }

        if (!module.hasEnoughEnergy(stack, getEnergy(module)))
            return;

        // Split the configured keys by kind
        Set<ResourceLocation> entityTypes = new HashSet<>();
        Set<Block> blocks = new HashSet<>();
        for (ResourceKey<?> key : config.get()) {
            if (key.isFor(Registries.ENTITY_TYPE)) {
                entityTypes.add(key.location());
            } else if (key.isFor(Registries.BLOCK)) {
                BuiltInRegistries.BLOCK.getOptional(key.location()).ifPresent(blocks::add);
            }
        }

        ScanResult result = new ScanResult(
                scanEntities(module, level, player, entityTypes),
                scanBlocks(module, level, player.blockPosition(), blocks)
        );

        module.useEnergy(player, stack, getEnergy(module), true);

        if (!result.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, result);
        }
    }

    private static long getEnergy(IModule<ModuleRevealingGoggles> module) {
        var volume = Math.pow(getRadius(module), 3);
        var energy = volume * MekaMoreConfig.revealGooglesEnergyPerBlock.get();
        return (long) Math.ceil(energy);
    }

    private static int getRadius(IModule<ModuleRevealingGoggles> module) {
        var part = (double) module.getInstalledCount() / module.getData().getMaxStackSize();
        var radius = MekaMoreConfig.revealGooglesMaxScanRadius.get() * part;
        return (int) Math.floor(radius);
    }

    // ── Entities ──────────────────────────────────────────────────────

    private static Set<Integer> scanEntities(IModule<ModuleRevealingGoggles> module, ServerLevel level, Player player, Set<ResourceLocation> entityTypes) {
        Set<Integer> ids = new HashSet<>();
        if (!entityTypes.isEmpty()) {
            AABB area = new AABB(player.blockPosition()).inflate(getRadius(module));
            for (Entity entity : level.getEntitiesOfClass(Entity.class, area,
                    e -> e != player && !e.isRemoved() && entityTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())))) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    // ── Blocks ────────────────────────────────────────────────────────

    /**
     * Walks only chunks that are already loaded (level.getBlockState() on an unloaded chunk can load or even generate it),
     * skips whole 16³ sections whose palette can't contain a target, and keeps the nearest
     */
    private static List<BlockPos> scanBlocks(IModule<ModuleRevealingGoggles> module, ServerLevel level, BlockPos origin, Set<Block> targets) {
        if (targets.isEmpty()) {
            return List.of();
        }
        Predicate<BlockState> matches = state -> targets.contains(state.getBlock());
        Comparator<BlockPos> farthestFirst = Comparator.comparingDouble((BlockPos p) -> p.distSqr(origin)).reversed();
        Map<Block, PriorityQueue<BlockPos>> nearest = new HashMap<>();

        var radius = getRadius(module);

        int minX = origin.getX() - radius, maxX = origin.getX() + radius;
        int minZ = origin.getZ() - radius, maxZ = origin.getZ() + radius;
        int minY = Math.max(origin.getY() - radius, level.getMinBuildHeight());
        int maxY = Math.min(origin.getY() + radius, level.getMaxBuildHeight() - 1);

        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                int baseX = chunkX << 4, baseZ = chunkZ << 4;
                int fromX = Math.max(minX - baseX, 0), toX = Math.min(maxX - baseX, 15);
                int fromZ = Math.max(minZ - baseZ, 0), toZ = Math.min(maxZ - baseZ, 15);

                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section.hasOnlyAir() || !section.maybeHas(matches)) {
                        continue;
                    }
                    int baseY = (chunk.getMinSection() + i) << 4;
                    int fromY = Math.max(minY - baseY, 0), toY = Math.min(maxY - baseY, 15);
                    for (int y = fromY; y <= toY; y++) {
                        for (int z = fromZ; z <= toZ; z++) {
                            for (int x = fromX; x <= toX; x++) {
                                Block block = section.getBlockState(x, y, z).getBlock();
                                if (!targets.contains(block)) {
                                    continue;
                                }
                                PriorityQueue<BlockPos> queue = nearest.computeIfAbsent(block, b -> new PriorityQueue<>(farthestFirst));
                                BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
                                if (queue.size() < MekaMoreConfig.revealGooglesMaxResult.get()) {
                                    queue.add(pos);
                                } else if (pos.distSqr(origin) < queue.peek().distSqr(origin)) {
                                    queue.poll();
                                    queue.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        List<BlockPos> found = new ArrayList<>();
        nearest.values().forEach(found::addAll);
        found.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
        return found;
    }

}