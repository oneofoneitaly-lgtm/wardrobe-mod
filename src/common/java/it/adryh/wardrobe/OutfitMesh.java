package it.adryh.wardrobe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.List;

/** Writes the faces of one body part into a vertex buffer. */
public final class OutfitMesh {
    private OutfitMesh() {}

    /** {@code pose} must already be moved onto the body part. */
    public static void emit(PoseStack.Pose pose, VertexConsumer buffer, List<Quad> quads, int light, int overlay) {
        for (Quad q : quads) {
            Part part = q.part;
            for (int i = 0; i < 4; i++) {
                // Blockbench space -> model part space (X and Y are mirrored)
                float x = (-q.pos[i * 3] - part.pivotX) / 16F;
                float y = (24F - q.pos[i * 3 + 1] - part.pivotY) / 16F;
                float z = q.pos[i * 3 + 2] / 16F;
                buffer.addVertex(pose, x, y, z)
                        .setColor(255, 255, 255, 255)
                        .setUv(q.uv[i * 2], q.uv[i * 2 + 1])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, -q.nx, -q.ny, q.nz);
            }
        }
    }
}
