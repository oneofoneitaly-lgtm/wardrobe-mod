package it.adryh.aura.sky;

import it.adryh.aura.core.Aura;
import it.adryh.aura.core.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("aurasky")
public final class AuraSkyMod {
    private static final long[] TIMES = {0, 23500, 6000, 12600, 18000};

    public AuraSkyMod(FMLJavaModLoadingContext context) {
        Aura.register(SkySettings.MODULE);
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                SkyRenderer.render(e.getPoseStack(), e.getCamera());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(AuraSkyMod::fogColor);
        MinecraftForge.EVENT_BUS.addListener(AuraSkyMod::fog);
        // the server keeps sending the real time, so the chosen one is applied before every frame
        MinecraftForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent.Pre e) -> time());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post e) -> time());
    }

    private static void time() {
        int choice = SkySettings.TIME.index();
        ClientLevel level = Minecraft.getInstance().level;
        if (choice == 0 || level == null) {
            return;
        }
        long now = level.getDayTime();
        long wanted = Math.floorDiv(now, 24000L) * 24000L + TIMES[choice];
        if (now != wanted) {
            level.setDayTime(wanted);
        }
    }

    private static void fogColor(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float r = event.getRed(), g = event.getGreen(), b = event.getBlue();
        if (SkySettings.SKY.on() && SkyRenderer.hasSky(level)) {
            int horizon = SkyRenderer.horizon(level);
            float k = SkySettings.INTENSITY.f() / 100F * 0.85F;
            r += (Colors.red(horizon) - r) * k;
            g += (Colors.green(horizon) - g) * k;
            b += (Colors.blue(horizon) - b) * k;
        }
        if (SkySettings.FOG_TINT.on()) {
            int fog = SkySettings.FOG_COLOR.get();
            float light = Math.max(0.15F, (r + g + b) / 2F);
            r = Colors.red(fog) * Math.min(1F, light);
            g = Colors.green(fog) * Math.min(1F, light);
            b = Colors.blue(fog) * Math.min(1F, light);
        }
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    private static void fog(ViewportEvent.RenderFog event) {
        float factor = SkySettings.FOG_DISTANCE.f() / 100F;
        if (Math.abs(factor - 1F) < 0.01F || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        float far = event.getFarPlaneDistance() * factor;
        // a closer fog also starts earlier, so it looks like mist rather than a wall
        float near = factor < 1F ? far * 0.15F : event.getNearPlaneDistance() * factor;
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }
}
