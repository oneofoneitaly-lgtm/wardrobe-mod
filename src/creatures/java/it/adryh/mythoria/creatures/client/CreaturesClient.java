package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.client.gen.GolemLayer;
import it.adryh.mythoria.creatures.client.gen.GriffinLayer;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class CreaturesClient {
    static final ModelLayerLocation GRIFFIN = new ModelLayerLocation(Creatures.id("golden_griffin"), "main");
    static final ModelLayerLocation GOLEM = new ModelLayerLocation(Creatures.id("crystal_golem"), "main");

    private CreaturesClient() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            event.registerLayerDefinition(GRIFFIN, GriffinLayer::create);
            event.registerLayerDefinition(GOLEM, GolemLayer::create);
        });
        context.getModEventBus().addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(Creatures.GRIFFIN.get(), GriffinRenderer::new);
            event.registerEntityRenderer(Creatures.GOLEM.get(), GolemRenderer::new);
        });
        GoldenGriffin.jumpHeld = () -> Minecraft.getInstance().screen == null && Minecraft.getInstance().options.keyJump.isDown();
    }
}
