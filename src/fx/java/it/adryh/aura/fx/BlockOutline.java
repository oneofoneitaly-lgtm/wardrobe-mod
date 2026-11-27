package it.adryh.aura.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.adryh.aura.core.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;

final class BlockOutline {
    private BlockOutline() {
    }

    static boolean render(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir() || !mc.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        VoxelShape shape = state.getShape(mc.level, pos, CollisionContext.of(event.getCamera().getEntity()));
        if (shape.isEmpty()) {
            return false;
        }
        float time = (System.currentTimeMillis() % 3_600_000L) / 1000F;
        int color = FxSettings.RAINBOW.on() ? Colors.hsv(time * 0.18F % 1F, 0.75F, 1F) : FxSettings.OUTLINE_COLOR.get();
        float pulse = FxSettings.PULSE.on() ? 0.78F + 0.22F * (float) Math.sin(time * 3.5F) : 1F;
        float r = Colors.red(color), g = Colors.green(color), b = Colors.blue(color);

        Vec3 camera = event.getCamera().getPosition();
        double ox = pos.getX() - camera.x, oy = pos.getY() - camera.y, oz = pos.getZ() - camera.z;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource source = event.getMultiBufferSource();
        VertexConsumer buffer = source.getBuffer(RenderType.debugFilledBox());

        float fill = FxSettings.FILL.f() / 100F * pulse;
        if (fill > 0.005F) {
            for (AABB box : shape.toAabbs()) {
                box(pose, buffer, box.minX + ox, box.minY + oy, box.minZ + oz, box.maxX + ox, box.maxY + oy, box.maxZ + oz,
                        0.003, r, g, b, fill);
            }
        }
        double half = FxSettings.OUTLINE_WIDTH.get() * 0.0025;
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> box(pose, buffer,
                Math.min(x1, x2) + ox, Math.min(y1, y2) + oy, Math.min(z1, z2) + oz,
                Math.max(x1, x2) + ox, Math.max(y1, y2) + oy, Math.max(z1, z2) + oz,
                half, r, g, b, pulse));
        if (source instanceof MultiBufferSource.BufferSource batch) {
            batch.endBatch(RenderType.debugFilledBox());
        }
        return true;
    }

    private static void box(PoseStack pose, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2,
                            double grow, float r, float g, float b, float a) {
        LevelRenderer.addChainedFilledBoxVertices(pose, buffer,
                (float) (x1 - grow), (float) (y1 - grow), (float) (z1 - grow),
                (float) (x2 + grow), (float) (y2 + grow), (float) (z2 + grow), r, g, b, a);
    }
}
