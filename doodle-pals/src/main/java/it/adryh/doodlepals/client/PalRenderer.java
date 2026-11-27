package it.adryh.doodlepals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.entity.DoodlePal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the pal and gives it life: hopping, breathing, blinking, lunging. */
public final class PalRenderer extends EntityRenderer<DoodlePal> {
    public PalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DoodlePal pal, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Drawing drawing = pal.drawing();
        if (drawing.painted() > 0 && !pal.isInvisible()) {
            float age = pal.tickCount + partial;
            float stride = Math.min(1.0F, pal.walkAnimation.speed(partial) * 1.6F);
            float step = pal.walkAnimation.position(partial) * 1.1F;
            float hop = Math.abs(Mth.sin(step)) * stride;
            float breath = Mth.sin(age * 0.09F) * 0.025F;
            boolean sitting = pal.isInSittingPose();
            float tall = 1.0F + breath + (hop - 0.4F) * 0.12F * stride;
            if (sitting) {
                tall *= 0.86F;
            }

            pose.pushPose();
            pose.translate(0.0F, hop * 0.14F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, pal.yBodyRotO, pal.yBodyRot)));
            if (pal.deathTime > 0) {
                float fallen = Math.min(1.0F, (pal.deathTime + partial) / 14.0F);
                pose.mulPose(Axis.XP.rotationDegrees(-90.0F * fallen));
            }
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(step) * 6.0F * stride));
            if (pal.lunge > 0) {
                float t = Mth.sin((8 - pal.lunge + partial) / 8.0F * Mth.PI);
                pose.translate(0.0F, 0.0F, t * 0.35F);
                pose.mulPose(Axis.XP.rotationDegrees(t * 22.0F));
            }
            pose.scale(1.0F / 16.0F, tall / 16.0F, 1.0F / 16.0F);
            boolean blink = (pal.tickCount + pal.getId() * 17) % 90 < 4;
            DoodleMesh.of(drawing).render(pose, buffers, light, LivingEntityRenderer.getOverlayCoords(pal, 0.0F),
                    step, stride, blink);
            pose.popPose();
        }
        super.render(pal, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DoodlePal pal) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
