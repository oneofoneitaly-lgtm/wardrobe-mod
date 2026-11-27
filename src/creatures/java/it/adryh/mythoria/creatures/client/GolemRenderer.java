package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.entity.CrystalGolem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

final class GolemRenderer extends MobRenderer<CrystalGolem, GolemModel> {
    private static final ResourceLocation TEXTURE = Creatures.id("textures/entity/crystal_golem.png");

    GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(CreaturesClient.GOLEM)), 1.4F);
        addLayer(new GlowLayer<>(this, Creatures.id("textures/entity/crystal_golem_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(CrystalGolem entity) {
        return TEXTURE;
    }
}
