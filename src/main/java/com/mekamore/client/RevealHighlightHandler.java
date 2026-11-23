package com.mekamore.client;

import com.mekamore.MekaMore;
import com.mekamore.registries.MekaMoreModules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.*;

@EventBusSubscriber(modid = MekaMore.MODID, value = Dist.CLIENT)
public final class RevealHighlightHandler {

    // State written from packet, read from render thread — both on the main client thread
    private static List<BlockPos> highlightedBlocks = List.of();
    private static Set<Integer> glowingEntityIds = Set.of();

    // Expire highlights if no packet arrives (module removed / unequipped)
    private static long lastUpdateTick = -1;
    private static final long EXPIRY_TICKS = 40; // 2 seconds

    public static void update(List<BlockPos> blocks, Set<Integer> entityIds) {
        highlightedBlocks = List.copyOf(blocks);
        glowingEntityIds = Set.copyOf(entityIds);
        lastUpdateTick = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : lastUpdateTick;

        // Apply/remove glowing tag on client entities
        applyEntityGlow();
    }

    private static void applyEntityGlow() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // Reset all previously glowing entities first
        level.entitiesForRendering().forEach(e -> {
            if (!glowingEntityIds.contains(e.getId()))
                return;

            if (!e.isCurrentlyGlowing()){
                e.setGlowingTag(true);
            }
        });
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Expire if module was removed
        long now = mc.level.getGameTime();
        if (now - lastUpdateTick > EXPIRY_TICKS) {
            highlightedBlocks = List.of();
            glowingEntityIds = Set.of();
            return;
        }

        // Check the module is still equipped
        ItemStack helmet = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (IModuleHelper.INSTANCE.getIfEnabled(helmet, MekaMoreModules.REVEALING_UNIT) == null) {
            highlightedBlocks = List.of();
            return;
        }

        if (highlightedBlocks.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Camera camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        double cx = camera.getPosition().x;
        double cy = camera.getPosition().y;
        double cz = camera.getPosition().z;

        pose.pushPose();

        for (BlockPos pos : highlightedBlocks) {
            double dx = pos.getX() - cx;
            double dy = pos.getY() - cy;
            double dz = pos.getZ() - cz;

            // Distance fade: full alpha within 20 blocks, fades to 60 at SCAN_RADIUS
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int alpha = (int) Math.max(60, 255 - (dist / 64.0) * 195);

            drawBlockOutline(pose, lines, dx, dy, dz, 0xFF, 0xDD, 0x00, alpha); // golden yellow
        }

        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /**
     * Draws a 1-pixel wireframe cube around a block.
     * dx/dy/dz are the block corner offset from the camera origin.
     */
    private static void drawBlockOutline(
            PoseStack pose, VertexConsumer lines,
            double dx, double dy, double dz,
            int r, int g, int b, int a
    ) {
        Matrix4f m = pose.last().pose();
        float x0 = (float) dx, y0 = (float) dy, z0 = (float) dz;
        float x1 = (float) (dx + 1), y1 = (float) (dy + 1), z1 = (float) (dz + 1);

        // 12 edges of a box
        edge(m, lines, x0, y0, z0, x1, y0, z0, r, g, b, a);
        edge(m, lines, x1, y0, z0, x1, y0, z1, r, g, b, a);
        edge(m, lines, x1, y0, z1, x0, y0, z1, r, g, b, a);
        edge(m, lines, x0, y0, z1, x0, y0, z0, r, g, b, a);

        edge(m, lines, x0, y1, z0, x1, y1, z0, r, g, b, a);
        edge(m, lines, x1, y1, z0, x1, y1, z1, r, g, b, a);
        edge(m, lines, x1, y1, z1, x0, y1, z1, r, g, b, a);
        edge(m, lines, x0, y1, z1, x0, y1, z0, r, g, b, a);

        edge(m, lines, x0, y0, z0, x0, y1, z0, r, g, b, a);
        edge(m, lines, x1, y0, z0, x1, y1, z0, r, g, b, a);
        edge(m, lines, x1, y0, z1, x1, y1, z1, r, g, b, a);
        edge(m, lines, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void edge(Matrix4f m, VertexConsumer v,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             int r, int g, int b, int a) {
        // RenderType.lines() needs pos + color + normal
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0) return;
        nx /= len;
        ny /= len;
        nz /= len;

        v.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz);
        v.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}