package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

final class GriffinRenderer extends MobRenderer<GoldenGriffin, GriffinModel> {
    private static final ResourceLocation TEXTURE = Creatures.id("textures/entity/golden_griffin.png");

    GriffinRenderer(EntityRendererProvider.Context context) {
        super(context, new GriffinModel(context.bakeLayer(CreaturesClient.GRIFFIN)), 0.9F);
        addLayer(new GlowLayer<>(this, Creatures.id("textures/entity/golden_griffin_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(GoldenGriffin entity) {
        return TEXTURE;
    }
}
