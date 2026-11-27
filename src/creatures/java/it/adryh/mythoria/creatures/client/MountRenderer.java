package it.adryh.mythoria.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Griffin and drake share a skeleton, so they share the renderer too. */
final class MountRenderer<T extends GoldenGriffin> extends MobRenderer<T, GriffinModel<T>> {
    private final ResourceLocation texture;
    private final float size;

    MountRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String name, float size) {
        super(context, new GriffinModel<>(context.bakeLayer(layer)), 0.9F * size);
        this.texture = Creatures.id("textures/entity/" + name + ".png");
        this.size = size;
        addLayer(new GlowLayer<>(this, Creatures.id("textures/entity/" + name + "_glow.png")));
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partial) {
        pose.scale(size, size, size);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
