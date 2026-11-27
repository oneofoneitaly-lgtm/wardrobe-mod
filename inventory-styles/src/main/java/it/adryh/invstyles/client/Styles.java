package it.adryh.invstyles.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import it.adryh.invstyles.paint.Accent;
import it.adryh.invstyles.paint.Category;
import it.adryh.invstyles.paint.Style;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Puts the chosen styles on the game's textures, live: no resource reload needed. */
public final class Styles {
    private static final Logger LOG = LoggerFactory.getLogger("invstyles");
    private static final int VANILLA_TEXT = 0x404040;
    private static final String SPRITES = "textures/gui/sprites/";
    /** textures currently showing a style instead of their own pixels */
    private static final Set<ResourceLocation> CHANGED = new HashSet<>();

    private Styles() {}

    /** Repaints the textures of one group, or of every group when null. */
    static void apply(Category only) {
        Minecraft mc = Minecraft.getInstance();
        Map<ResourceLocation, Resource> files = mc.getResourceManager().listResources("textures/gui", location -> {
            Category category = Category.ofTexture(location.getPath());
            return location.getNamespace().equals("minecraft") && location.getPath().endsWith(".png")
                    && category != null && (only == null || category == only);
        });
        Accent accent = Config.accent();
        int painted = 0;
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            ResourceLocation location = file.getKey();
            Style style = Config.style(Category.ofTexture(location.getPath()));
            if (style == Style.VANILLA && !CHANGED.contains(location)) {
                continue;
            }
            try (InputStream in = file.getValue().open(); NativeImage original = NativeImage.read(in)) {
                if (location.getPath().startsWith(SPRITES)) {
                    sprite(mc, location, original, style, accent);
                } else if (style == Style.VANILLA) {
                    mc.getTextureManager().register(location, new SimpleTexture(location));
                } else {
                    mc.getTextureManager().register(location,
                            new DynamicTexture(Images.paint(original, location.getPath(), style, accent)));
                }
                if (style == Style.VANILLA) {
                    CHANGED.remove(location);
                } else {
                    CHANGED.add(location);
                }
                painted++;
            } catch (IOException | RuntimeException e) {
                LOG.warn("Could not restyle {}", location, e);
            }
        }
        LOG.info("[invstyles] restyled {} textures of {} ({}, {})", painted, only == null ? "all" : only.id(),
                only == null ? "mixed" : Config.style(only).id(), accent.id());
    }

    /** Sprites live together in the gui atlas: paint over their own spot. */
    private static void sprite(Minecraft mc, ResourceLocation location, NativeImage original, Style style, Accent accent) {
        String path = location.getPath();
        ResourceLocation id = location.withPath(path.substring(SPRITES.length(), path.length() - 4));
        TextureAtlasSprite sprite = mc.getGuiSprites().getSprite(id);
        if (!sprite.contents().name().equals(id) || sprite.contents().width() != original.getWidth()
                || sprite.contents().height() != original.getHeight()) {
            return;
        }
        try (NativeImage image = Images.paint(original, path, style, accent)) {
            mc.getTextureManager().getTexture(sprite.atlasLocation()).bind();
            image.upload(0, sprite.getX(), sprite.getY(), false);
        }
    }

    public static void beforeBlit(ResourceLocation texture) {
        if (!CHANGED.isEmpty() && (CHANGED.contains(texture) || texture.getPath().equals("textures/atlas/gui.png"))) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    /** Titles on vanilla inventories take the colour of their style. */
    public static int textColour(int colour) {
        if ((colour & 0xFFFFFF) != VANILLA_TEXT) {
            return colour;
        }
        Category category = categoryOf(Minecraft.getInstance().screen);
        if (category == null) {
            return colour;
        }
        return colour & 0xFF000000 | Config.style(category).text(Config.accent());
    }

    private static Category categoryOf(Screen screen) {
        if (screen == null || !screen.getClass().getName().startsWith("net.minecraft.")) {
            return null;
        }
        if (screen instanceof CreativeModeInventoryScreen) {
            return Category.CREATIVE;
        }
        if (screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen
                || screen instanceof DispenserScreen || screen instanceof HopperScreen) {
            return Category.CHESTS;
        }
        if (screen instanceof CraftingScreen || screen instanceof CrafterScreen || screen instanceof StonecutterScreen
                || screen instanceof LoomScreen || screen instanceof CartographyTableScreen) {
            return Category.CRAFTING;
        }
        if (screen instanceof AbstractFurnaceScreen<?>) {
            return Category.FURNACES;
        }
        if (screen instanceof EnchantmentScreen || screen instanceof BrewingStandScreen || screen instanceof BeaconScreen) {
            return Category.MAGIC;
        }
        if (screen instanceof AnvilScreen || screen instanceof SmithingScreen || screen instanceof GrindstoneScreen) {
            return Category.SMITHING;
        }
        if (screen instanceof MerchantScreen || screen instanceof HorseInventoryScreen) {
            return Category.TRADING;
        }
        return screen instanceof AbstractContainerScreen<?> ? Category.INVENTORY : null;
    }
}
