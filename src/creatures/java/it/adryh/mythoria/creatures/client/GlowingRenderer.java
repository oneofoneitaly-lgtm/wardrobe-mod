package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.Creatures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** A creature with a texture and its glowing parts. */
final class GlowingRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;

    GlowingRenderer(EntityRendererProvider.Context context, M model, String name, float shadow) {
        super(context, model, shadow);
        this.texture = Creatures.id("textures/entity/" + name + ".png");
        addLayer(new GlowLayer<>(this, Creatures.id("textures/entity/" + name + "_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
