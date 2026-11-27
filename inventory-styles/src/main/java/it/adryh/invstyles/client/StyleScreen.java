package it.adryh.invstyles.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import it.adryh.invstyles.InventoryStyles;
import it.adryh.invstyles.paint.Accent;
import it.adryh.invstyles.paint.Category;
import it.adryh.invstyles.paint.Style;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/** Pick a style for each group of inventories, with a live preview. */
public final class StyleScreen extends Screen {
    private static final int W = 400;
    private static final int H = 228;
    private static final int LIST_W = 112;
    private static final int RIGHT_X = 128;
    private static final int RIGHT_W = 264;
    private static final int PREVIEW_H = 124;
    private static final int CARD_W = 51;
    private static final int CARD_H = 50;

    private final Supplier<Screen> back;
    /** null = all inventories */
    private Category selected;
    private int left;
    private int top;
    private boolean previewsReady;
    private int textureWidth = 256;
    private int textureHeight = 256;

    StyleScreen(Supplier<Screen> back) {
        super(Component.translatable("invstyles.title"));
        this.back = back;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (!previewsReady) {
            paintPreviews();
        }
    }

    private Category shown() {
        return selected == null ? Category.INVENTORY : selected;
    }

    private static ResourceLocation preview(Style style) {
        return ResourceLocation.fromNamespaceAndPath(InventoryStyles.ID, "preview/" + style.id());
    }

    /** One picture of the selected group in every style. */
    private void paintPreviews() {
        String path = shown().sample();
        var resource = minecraft.getResourceManager().getResource(ResourceLocation.withDefaultNamespace(path));
        if (resource.isEmpty()) {
            return;
        }
        try (InputStream in = resource.get().open(); NativeImage original = NativeImage.read(in)) {
            textureWidth = original.getWidth();
            textureHeight = original.getHeight();
            for (Style style : Style.values()) {
                minecraft.getTextureManager().register(preview(style),
                        new DynamicTexture(Images.paint(original, path, style, Config.accent())));
            }
            previewsReady = true;
        } catch (IOException e) {
            previewsReady = false;
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(graphics);
        graphics.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF000000 | Config.accent().colour());
        graphics.fillGradient(left, top, left + W, top + H, 0xF5191826, 0xF50E0D17);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        int accent = 0xFF000000 | Config.accent().colour();
        graphics.drawString(font, title, left + 8, top + 8, 0xFFFFFFFF, false);
        String author = "by @Adryh01";
        graphics.drawString(font, author, left + W - 8 - font.width(author), top + 8, accent, false);

        // the groups
        for (int i = 0; i <= Category.values().length; i++) {
            Category category = i == 0 ? null : Category.values()[i - 1];
            int y = rowY(i);
            boolean active = category == selected;
            boolean hover = inside(mouseX, mouseY, left + 8, y, LIST_W, 18);
            graphics.fill(left + 8, y, left + 8 + LIST_W, y + 18, active ? 0x553F3A66 : hover ? 0x33FFFFFF : 0x18FFFFFF);
            if (active) {
                graphics.fill(left + 8, y, left + 10, y + 18, accent);
            }
            graphics.drawString(font, name(category), left + 15, y + 5, active ? 0xFFFFFFFF : 0xFFC9C6DD, false);
            Style style = common(category);
            int dot = style == null ? 0xFF6A6785 : dot(style);
            graphics.fill(left + LIST_W - 1, y + 6, left + LIST_W + 5, y + 12, dot);
        }

        // the big preview, over something like a landscape so that glass shows
        int px = left + RIGHT_X;
        int py = top + 22;
        graphics.fillGradient(px, py, px + RIGHT_W, py + PREVIEW_H * 2 / 3, 0xFF6FA8E8, 0xFFB9D8F5);
        graphics.fill(px, py + PREVIEW_H * 2 / 3, px + RIGHT_W, py + PREVIEW_H, 0xFF5C8A3A);
        for (int i = 0; i < RIGHT_W; i += 12) {
            graphics.fill(px + i, py + PREVIEW_H * 2 / 3 - (i * 7 % 17), px + i + 12, py + PREVIEW_H * 2 / 3, 0xFF4C7A30);
        }
        Style current = common(shown());
        if (previewsReady && current != null) {
            picture(graphics, current, px, py, RIGHT_W, PREVIEW_H, true);
        }

        // one card per style
        for (Style style : Style.values()) {
            int x = cardX(style);
            int y = top + 150;
            boolean active = common(selected) == style;
            boolean hover = inside(mouseX, mouseY, x, y, CARD_W, CARD_H);
            graphics.fill(x, y, x + CARD_W, y + CARD_H, active ? accent : hover ? 0xFF6A6785 : 0xFF34324A);
            graphics.fill(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, 0xFF1D1C2C);
            if (previewsReady) {
                picture(graphics, style, x + 3, y + 3, CARD_W - 6, CARD_H - 18, false);
            }
            Component label = Component.translatable("invstyles.style." + style.id());
            float scale = Math.min(1.0F, (CARD_W - 4) / (float) font.width(label));
            graphics.pose().pushPose();
            graphics.pose().translate(x + CARD_W / 2.0F, y + CARD_H - 11, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, label, -font.width(label) / 2, 0, active ? 0xFFFFFFFF : 0xFFC9C6DD, false);
            graphics.pose().popPose();
        }

        // the accent colour
        graphics.drawString(font, Component.translatable("invstyles.colour"), px, top + 209, 0xFFC9C6DD, false);
        for (Accent colour : Accent.values()) {
            int x = swatchX(colour);
            graphics.fill(x - 1, top + 205, x + 15, top + 221, colour == Config.accent() ? 0xFFFFFFFF : 0xFF000000);
            graphics.fill(x, top + 206, x + 14, top + 220, 0xFF000000 | colour.colour());
        }
        boolean hover = inside(mouseX, mouseY, doneX(), top + 205, 52, 16);
        graphics.fill(doneX(), top + 205, doneX() + 52, top + 221, hover ? 0xFFFFFFFF : accent);
        graphics.fill(doneX() + 1, top + 206, doneX() + 51, top + 220, 0xFF2B2942);
        graphics.drawCenteredString(font, Component.translatable("gui.done"), doneX() + 26, top + 209, 0xFFFFFFFF);
    }

    private void picture(GuiGraphics graphics, Style style, int x, int y, int w, int h, boolean detailed) {
        Category category = shown();
        float scale = Math.min(w / (float) category.width(), (h - (detailed ? 4 : 0)) / (float) category.height());
        if (category == Category.HOTBAR && detailed) {
            scale = 1.4F;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + (w - category.width() * scale) / 2, y + (h - category.height() * scale) / 2, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(preview(style), 0, 0, 0.0F, 0.0F, category.width(), category.height(), textureWidth, textureHeight);
        RenderSystem.disableBlend();
        if (detailed) {
            if (category != Category.HOTBAR) {
                // the player's inventory has its title above the little crafting grid
                boolean own = category == Category.INVENTORY;
                graphics.drawString(font, own ? Component.translatable("container.crafting") : name(category),
                        own ? 97 : 8, own ? 8 : 6, 0xFF000000 | style.text(Config.accent()), false);
            }
            items(graphics, category);
        }
        graphics.pose().popPose();
    }

    /** A few items in the slots, to judge how readable they are. */
    private static void items(GuiGraphics graphics, Category category) {
        ItemStack[] stacks = {new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.GOLDEN_APPLE, 12),
                new ItemStack(Items.TORCH, 64), new ItemStack(Items.ENDER_PEARL, 8), new ItemStack(Items.OAK_LOG, 32)};
        int x;
        int y;
        int step = 18;
        switch (category) {
            case INVENTORY -> { x = 8; y = 142; }
            case CREATIVE -> { x = 9; y = 112; }
            case CHESTS -> { x = 8; y = 18; }
            case HOTBAR -> { x = 3; y = 3; step = 20; }
            case TRADING -> { x = 108; y = 142; }
            default -> { x = 8; y = 142; }
        }
        for (int i = 0; i < stacks.length; i++) {
            graphics.renderItem(stacks[i], x + i * step, y);
        }
    }

    private Component name(Category category) {
        return Component.translatable("invstyles.group." + (category == null ? "all" : category.id()));
    }

    /** The style shared by the group (or by all groups), null if they differ. */
    private static Style common(Category category) {
        if (category != null) {
            return Config.style(category);
        }
        Style first = Config.style(Category.INVENTORY);
        for (Category each : Category.values()) {
            if (Config.style(each) != first) {
                return null;
            }
        }
        return first;
    }

    private static int dot(Style style) {
        return switch (style) {
            case VANILLA -> 0xFFC6C6C6;
            case GLASS -> 0xFF4C4868;
            case FANTASY -> 0xFFD9A83C;
            case NEON -> 0xFF35DCFF;
            case PASTEL -> 0xFFFFB3D9;
        };
    }

    private int rowY(int row) {
        return top + 22 + row * 20;
    }

    private int cardX(Style style) {
        return left + RIGHT_X + style.ordinal() * (CARD_W + 2);
    }

    private int swatchX(Accent colour) {
        return left + RIGHT_X + 40 + colour.ordinal() * 20;
    }

    private int doneX() {
        return left + RIGHT_X + RIGHT_W - 52;
    }

    private static boolean inside(double x, double y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) {
            return super.mouseClicked(x, y, button);
        }
        for (int i = 0; i <= Category.values().length; i++) {
            if (inside(x, y, left + 8, rowY(i), LIST_W, 18)) {
                selected = i == 0 ? null : Category.values()[i - 1];
                paintPreviews();
                return click();
            }
        }
        for (Style style : Style.values()) {
            if (inside(x, y, cardX(style), top + 150, CARD_W, CARD_H)) {
                ClientSetup.choose(selected, style);
                return click();
            }
        }
        for (Accent colour : Accent.values()) {
            if (inside(x, y, swatchX(colour), top + 206, 14, 14)) {
                ClientSetup.choose(colour);
                paintPreviews();
                return click();
            }
        }
        if (inside(x, y, doneX(), top + 205, 52, 16)) {
            onClose();
            return click();
        }
        return super.mouseClicked(x, y, button);
    }

    private boolean click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(back == null ? null : back.get());
    }

    @Override
    public void removed() {
        for (Style style : Style.values()) {
            minecraft.getTextureManager().release(preview(style));
        }
        previewsReady = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
