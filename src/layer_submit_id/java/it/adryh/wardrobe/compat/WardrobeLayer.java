package it.adryh.wardrobe.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import it.adryh.wardrobe.Outfit;
import it.adryh.wardrobe.OutfitMesh;
import it.adryh.wardrobe.Part;
import it.adryh.wardrobe.Parts;
import it.adryh.wardrobe.Quad;
import it.adryh.wardrobe.Wardrobe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public final class WardrobeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public WardrobeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.id != mc.player.getId() || state.isInvisible) return;
        for (Outfit outfit : Wardrobe.worn()) {
            for (int i = 0; i < outfit.batches.size(); i++) {
                for (Map.Entry<Part, List<Quad>> entry : outfit.batches.get(i).entrySet()) {
                    ModelPart part = Parts.of(getParentModel(), entry.getKey());
                    if (!part.visible) continue;
                    List<Quad> quads = entry.getValue();
                    pose.pushPose();
                    part.translateAndRotate(pose);
                    collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull((Identifier) outfit.texture(i)),
                            (last, buffer) -> OutfitMesh.emit(last, buffer, quads, light, OverlayTexture.NO_OVERLAY));
                    pose.popPose();
                }
            }
        }
    }
}
