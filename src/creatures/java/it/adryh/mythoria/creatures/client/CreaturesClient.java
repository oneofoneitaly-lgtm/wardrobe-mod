package it.adryh.mythoria.creatures.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.adryh.mythoria.creatures.Creatures;
import it.adryh.mythoria.creatures.client.gen.DrakeLayer;
import it.adryh.mythoria.creatures.client.gen.FoxLayer;
import it.adryh.mythoria.creatures.client.gen.GolemLayer;
import it.adryh.mythoria.creatures.client.gen.GriffinLayer;
import it.adryh.mythoria.creatures.client.gen.LichLayer;
import it.adryh.mythoria.creatures.entity.EmberDrake;
import it.adryh.mythoria.creatures.entity.GoldenGriffin;
import it.adryh.mythoria.creatures.net.CreatureNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class CreaturesClient {
    static final ModelLayerLocation GRIFFIN = new ModelLayerLocation(Creatures.id("golden_griffin"), "main");
    static final ModelLayerLocation GOLEM = new ModelLayerLocation(Creatures.id("crystal_golem"), "main");
    static final ModelLayerLocation DRAKE = new ModelLayerLocation(Creatures.id("ember_drake"), "main");
    static final ModelLayerLocation FOX = new ModelLayerLocation(Creatures.id("spirit_fox"), "main");
    static final ModelLayerLocation LICH = new ModelLayerLocation(Creatures.id("shadow_lich"), "main");

    private static final KeyMapping ABILITY = new KeyMapping("key.mythoria.mount_ability", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.mythoria");

    private CreaturesClient() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            event.registerLayerDefinition(GRIFFIN, GriffinLayer::create);
            event.registerLayerDefinition(GOLEM, GolemLayer::create);
            event.registerLayerDefinition(DRAKE, DrakeLayer::create);
            event.registerLayerDefinition(FOX, FoxLayer::create);
            event.registerLayerDefinition(LICH, LichLayer::create);
        });
        context.getModEventBus().addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(Creatures.GRIFFIN.get(), c -> new MountRenderer<GoldenGriffin>(c, GRIFFIN, "golden_griffin", 1.0F));
            event.registerEntityRenderer(Creatures.DRAKE.get(), c -> new MountRenderer<EmberDrake>(c, DRAKE, "ember_drake", 1.2F));
            event.registerEntityRenderer(Creatures.GOLEM.get(), GolemRenderer::new);
            event.registerEntityRenderer(Creatures.FOX.get(),
                    c -> new GlowingRenderer<>(c, new FoxModel(c.bakeLayer(FOX)), "spirit_fox", 0.35F));
            event.registerEntityRenderer(Creatures.LICH.get(),
                    c -> new GlowingRenderer<>(c, new LichModel(c.bakeLayer(LICH)), "shadow_lich", 0.7F));
        });
        context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> event.register(ABILITY));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> ability());
        GoldenGriffin.jumpHeld = () -> Minecraft.getInstance().screen == null && Minecraft.getInstance().options.keyJump.isDown();
    }

    private static void ability() {
        Minecraft mc = Minecraft.getInstance();
        boolean pressed = false;
        while (ABILITY.consumeClick()) {
            pressed = true;
        }
        if (pressed && mc.screen == null && mc.player != null && mc.player.getVehicle() instanceof EmberDrake) {
            CreatureNet.useAbility();
        }
    }
}
