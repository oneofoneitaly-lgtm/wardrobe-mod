package it.adryh.wardrobe.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.adryh.wardrobe.Outfit;
import it.adryh.wardrobe.OutfitMesh;
import it.adryh.wardrobe.Part;
import it.adryh.wardrobe.Parts;
import it.adryh.wardrobe.Quad;
import it.adryh.wardrobe.Wardrobe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public final class WardrobeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public WardrobeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
        if (player != Minecraft.getInstance().player || player.isInvisible()) return;
        for (Outfit outfit : Wardrobe.worn()) {
            for (int i = 0; i < outfit.batches.size(); i++) {
                if (outfit.batches.get(i).isEmpty()) continue;
                VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull((ResourceLocation) outfit.texture(i)));
                for (Map.Entry<Part, List<Quad>> entry : outfit.batches.get(i).entrySet()) {
                    ModelPart part = Parts.of(getParentModel(), entry.getKey());
                    if (!part.visible) continue;
                    pose.pushPose();
                    part.translateAndRotate(pose);
                    OutfitMesh.emit(pose.last(), buffer, entry.getValue(), light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }
    }
}
