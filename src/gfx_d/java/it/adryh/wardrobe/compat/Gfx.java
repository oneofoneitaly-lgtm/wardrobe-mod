package it.adryh.wardrobe.compat;

import com.mojang.blaze3d.platform.NativeImage;
import it.adryh.wardrobe.WardrobeLog;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Path;

/** Textures and GUI calls that changed between Minecraft versions. */
public final class Gfx {
    private static int counter;

    private Gfx() {}

    /** Uploads a PNG and returns its handle. Render thread only. */
    public static Object createTexture(byte[] png) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("wardrobe", "dynamic/" + counter++);
        NativeImage image;
        try {
            image = NativeImage.read(png);
        } catch (IOException e) {
            WardrobeLog.warn("Broken texture: " + e);
            image = new NativeImage(1, 1, true);
        }
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "wardrobe", image));
        return id;
    }

    public static void releaseTexture(Object handle) {
        Minecraft.getInstance().getTextureManager().release((ResourceLocation) handle);
    }

    /** Draws a whole texture stretched to width x height. */
    public static void blit(GuiGraphics g, Object handle, int x, int y, int width, int height) {
        g.blit(RenderPipelines.GUI_TEXTURED, (ResourceLocation) handle, x, y, 0F, 0F, width, height, width, height);
    }

    public static void openFolder(Path folder) {
        Util.getPlatform().openPath(folder);
    }
}
