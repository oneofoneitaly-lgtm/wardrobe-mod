package it.adryh.aura.inventory;

import com.mojang.blaze3d.platform.NativeImage;
import it.adryh.aura.core.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns the container textures of the active resource packs into dark ones while the game runs,
 * so it also follows custom packs and can be switched off at any time.
 */
final class Themer {
    private static final Logger LOG = LoggerFactory.getLogger("AuraInventory");
    private static final ResourceLocation ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/gui.png");
    private static final String[] FILES = {
            "inventory", "generic_54", "crafting_table", "furnace", "blast_furnace", "smoker", "dispenser", "hopper",
            "shulker_box", "anvil", "grindstone", "smithing", "stonecutter", "loom", "cartography_table", "brewing_stand",
            "enchanting_table", "beacon", "horse", "villager", "creative_inventory/tab_items",
            "creative_inventory/tab_item_search", "creative_inventory/tab_inventory"};
    /** Sprites whose bright pixels carry information and must stay as they are. */
    private static final String[] KEEP = {"progress", "burn", "lit", "fuel", "bubbles", "arrow", "bar", "level"};

    /** brightness of the original pixel, then red, green, blue, alpha of the dark one */
    private static final int[][][] PALETTES = {
            {{0, 0, 0, 0}, {55, 3, 4, 9}, {85, 30, 33, 52}, {139, 9, 10, 19}, {198, 23, 26, 42}, {222, 42, 47, 72}, {255, 78, 85, 122}},
            {{0, 0, 0, 0}, {55, 6, 6, 7}, {85, 40, 40, 43}, {139, 16, 16, 18}, {198, 36, 36, 40}, {222, 56, 56, 62}, {255, 98, 98, 108}},
            {{0, 0, 0, 0}, {55, 10, 3, 18}, {85, 48, 28, 74}, {139, 20, 11, 34}, {198, 39, 25, 62}, {222, 64, 42, 98}, {255, 112, 78, 160}},
            {{0, 0, 0, 0}, {55, 2, 9, 15}, {85, 18, 48, 68}, {139, 6, 20, 31}, {198, 14, 38, 56}, {222, 26, 62, 88}, {255, 52, 108, 142}}};

    private static final List<ResourceLocation> replaced = new ArrayList<>();
    private static final List<ResourceLocation> sprites = new ArrayList<>();
    private static String applied = "";

    private Themer() {
    }

    /** Called after resource packs are reloaded: the atlas is vanilla again. */
    static void invalidate() {
        applied = "stale";
        sprites.clear();
    }

    private static String signature() {
        if (!InventorySettings.DARK.on()) {
            return "";
        }
        int palette = InventorySettings.PALETTE.index();
        return "dark" + palette + (palette >= PALETTES.length ? Integer.toHexString(Theme.accent()) : "");
    }

    /** Brings the textures in line with the settings. Must run on the render thread. */
    static void apply() {
        String wanted = signature();
        if (wanted.equals(applied)) {
            return;
        }
        restore();
        applied = wanted;
        if (wanted.isEmpty()) {
            return;
        }
        int[][] palette = palette();
        Minecraft mc = Minecraft.getInstance();
        TextureManager textures = mc.getTextureManager();
        ResourceManager resources = mc.getResourceManager();
        List<ResourceLocation> files = new ArrayList<>();
        for (String name : FILES) {
            files.add(ResourceLocation.withDefaultNamespace("textures/gui/container/" + name + ".png"));
        }
        files.add(ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png"));
        for (ResourceLocation file : files) {
            NativeImage image = read(resources, file);
            if (image != null) {
                recolor(image, palette);
                textures.register(file, new DynamicTexture(image));
                replaced.add(file);
            }
        }
        for (String folder : new String[]{"textures/gui/sprites/container", "textures/gui/sprites/recipe_book"}) {
            for (ResourceLocation file : resources.listResources(folder, id -> id.getPath().endsWith(".png")).keySet()) {
                if (file.getNamespace().equals("minecraft") && !keep(file.getPath())) {
                    paintSprite(file, palette);
                }
            }
        }
        LOG.info("Dark inventory theme applied: {} textures, {} sprites", replaced.size(), sprites.size());
    }

    private static boolean keep(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        for (String word : KEEP) {
            if (name.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static void restore() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation file : replaced) {
            mc.getTextureManager().release(file);
        }
        replaced.clear();
        for (ResourceLocation file : sprites) {
            paintSprite(file, null);
        }
        sprites.clear();
    }

    /** Uploads a sprite into the GUI atlas, recoloured with the palette or original when it is null. */
    private static void paintSprite(ResourceLocation file, int[][] palette) {
        Minecraft mc = Minecraft.getInstance();
        String path = file.getPath();
        ResourceLocation id = file.withPath(path.substring("textures/gui/sprites/".length(), path.length() - 4));
        TextureAtlasSprite sprite = mc.getGuiSprites().getSprite(id);
        if (!sprite.contents().name().equals(id)) {
            return;
        }
        try (NativeImage image = read(mc.getResourceManager(), file)) {
            // animated sprites keep their own frames
            if (image == null || image.getWidth() != sprite.contents().width() || image.getHeight() != sprite.contents().height()) {
                return;
            }
            if (palette != null) {
                recolor(image, palette);
                sprites.add(file);
            }
            mc.getTextureManager().getTexture(ATLAS).bind();
            image.upload(0, sprite.getX(), sprite.getY(), false);
        }
    }

    private static NativeImage read(ResourceManager resources, ResourceLocation file) {
        Optional<Resource> resource = resources.getResource(file);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream in = resource.get().open()) {
            return NativeImage.read(in);
        } catch (IOException e) {
            LOG.warn("Could not read {}", file, e);
            return null;
        }
    }

    private static int[][] palette() {
        int index = InventorySettings.PALETTE.index();
        if (index < PALETTES.length) {
            return PALETTES[index];
        }
        int accent = Theme.accent();
        int[][] base = PALETTES[0];
        int[][] mixed = new int[base.length][];
        for (int i = 0; i < base.length; i++) {
            float k = i < 2 ? 0.06F : i >= 5 ? 0.40F : 0.16F;
            mixed[i] = new int[]{base[i][0],
                    Math.round(base[i][1] + (((accent >> 16) & 255) - base[i][1]) * k),
                    Math.round(base[i][2] + (((accent >> 8) & 255) - base[i][2]) * k),
                    Math.round(base[i][3] + ((accent & 255) - base[i][3]) * k)};
        }
        return mixed;
    }

    /** Remaps the greys of the interface; coloured pixels such as icons are left untouched. */
    private static void recolor(NativeImage image, int[][] palette) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // NativeImage stores pixels as ABGR
                int pixel = image.getPixelRGBA(x, y);
                int a = pixel >>> 24, b = (pixel >> 16) & 255, g = (pixel >> 8) & 255, r = pixel & 255;
                if (a == 0 || Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 28) {
                    continue;
                }
                int light = (r + g + b) / 3;
                int i = 1;
                while (i < palette.length - 1 && light > palette[i][0]) {
                    i++;
                }
                int[] low = palette[i - 1], high = palette[i];
                float k = Math.max(0F, Math.min(1F, (light - low[0]) / (float) (high[0] - low[0])));
                int nr = Math.round(low[1] + (high[1] - low[1]) * k);
                int ng = Math.round(low[2] + (high[2] - low[2]) * k);
                int nb = Math.round(low[3] + (high[3] - low[3]) * k);
                int na = a * 242 / 255;
                image.setPixelRGBA(x, y, (na << 24) | (nb << 16) | (ng << 8) | nr);
            }
        }
    }
}
