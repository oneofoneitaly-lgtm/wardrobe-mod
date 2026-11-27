package it.adryh.aura.menu;

import it.adryh.aura.core.AuraButton;
import it.adryh.aura.core.Backdrop;
import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Text;
import it.adryh.aura.core.Theme;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.fml.ModList;

/** Replacement title screen with an animated background and glass buttons. */
public final class AuraTitleScreen extends Screen {
    private static boolean played;
    private long opened;
    private int logoY;
    private float logoScale;

    public AuraTitleScreen() {
        super(Component.literal("Aura"));
    }

    @Override
    protected void init() {
        if (opened == 0) {
            opened = System.currentTimeMillis();
        }
        boolean small = height < 300;
        int bw = Math.min(220, width - 40);
        int bh = small ? 18 : 22;
        int gap = small ? 3 : 5;
        int x = (width - bw) / 2;
        int half = (bw - gap) / 2;
        logoScale = Math.min(small ? 3F : 4.6F, width / 90F);
        logoY = Math.max(small ? 14 : 26, height / 8);
        int y = Math.max(logoY + (int) (logoScale * 9) + 22, (int) (height * 0.40F));
        y = Math.min(y, height - 5 * (bh + gap) - 22);

        button(x, y, bw, bh, Component.translatable("menu.singleplayer"),
                () -> minecraft.setScreen(new SelectWorldScreen(this))).primary();
        y += bh + gap;
        button(x, y, bw, bh, Component.translatable("menu.multiplayer"), () -> minecraft.setScreen(
                minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
        y += bh + gap;
        button(x, y, half, bh, Component.translatable("menu.online"),
                () -> minecraft.setScreen(new com.mojang.realmsclient.RealmsMainScreen(this)));
        button(x + bw - half, y, half, bh, Component.translatable("fml.menu.mods"),
                () -> minecraft.setScreen(new ModListScreen(this)));
        y += bh + gap;
        button(x, y, bw, bh, Component.literal("Aura Studio  -  personalizza"),
                () -> minecraft.setScreen(new HubScreen(this)));
        y += bh + gap;
        button(x, y, half, bh, Component.translatable("menu.options"),
                () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        button(x + bw - half, y, half, bh, Component.translatable("menu.quit"), minecraft::stop);

        button(width - 168, 8, 70, 18, Component.literal("Lingua"), () -> minecraft.setScreen(
                new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())));
        button(width - 94, 8, 86, 18, Component.literal("Accessibilità"), () -> minecraft.setScreen(
                new AccessibilityOptionsScreen(this, minecraft.options)));
    }

    private AuraButton button(int x, int y, int w, int h, Component label, Runnable action) {
        return addRenderableWidget(new AuraButton(x, y, w, h, label, action));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (Theme.BACKGROUND.index() == 2) {
            renderPanorama(g, partialTick);
            Draw.gradientV(g, 0, 0, width, height, 0, 0x30000000, 0xA0000000);
        } else {
            Backdrop.render(g, width, height);
        }
        logo(g);

        String version = "Minecraft " + SharedConstants.getCurrentVersion().getName() + "  |  Forge  |  "
                + ModList.get().size() + " mod";
        Text.draw(g, version, 8, height - 14, Theme.TEXT_DIM);
        String copyright = "Copyright Mojang AB. Do not distribute!";
        Text.draw(g, copyright, width - Text.width(copyright) - 8, height - 14, Theme.TEXT_DIM);
        Text.draw(g, "AURA", 10, 10, Theme.TEXT, true, 1.2F);
        Text.draw(g, "by @Adryh01", 10, 22, Theme.TEXT_DIM, false, 0.85F);
    }

    private void logo(GuiGraphics g) {
        float intro = played ? 1F : Math.min(1F, (System.currentTimeMillis() - opened) / 900F);
        if (intro >= 1F) {
            played = true;
        }
        float ease = 1F - (1F - intro) * (1F - intro);
        String title = "MINECRAFT";
        float spacing = logoScale * 0.7F;
        float total = -spacing;
        for (int i = 0; i < title.length(); i++) {
            total += Text.width(title.substring(i, i + 1), true, logoScale) + spacing;
        }
        float x = (width - total) / 2F;
        float y = logoY - (1F - ease) * 14F;
        Draw.glow(g, width / 2F, y + logoScale * 4F, total * 1.3F, Colors.alpha(Theme.accent(), 0.28F * ease));
        float t = Theme.time();
        for (int i = 0; i < title.length(); i++) {
            String letter = title.substring(i, i + 1);
            float wave = 0.5F + 0.5F * (float) Math.sin(t * 1.3F - i * 0.55F);
            int color = Colors.lerp(0xFFFFFFFF, Colors.lerp(Theme.accent(), Theme.second(), i / 8F), 0.15F + 0.5F * wave);
            Text.draw(g, letter, x + logoScale * 0.35F, y + logoScale * 0.45F, Colors.alpha(0x000000, 0.45F * ease), true, logoScale);
            Text.draw(g, letter, x, y, Colors.alpha(color, ease), true, logoScale);
            x += Text.width(letter, true, logoScale) + spacing;
        }
        float lineY = y + logoScale * 9F + 4F;
        String sub = "A U R A    E D I T I O N";
        float sw = Text.width(sub, true, 0.9F);
        Text.centered(g, sub, width / 2F, lineY, Colors.alpha(0xD5D8EE, ease), true, 0.9F);
        float lw = 46F * ease;
        Draw.gradientH(g, width / 2F - sw / 2F - lw - 8, lineY + 3, lw, 1.2F, 0.6F, Colors.alpha(Theme.accent(), 0F), Theme.accent());
        Draw.gradientH(g, width / 2F + sw / 2F + 8, lineY + 3, lw, 1.2F, 0.6F, Theme.second(), Colors.alpha(Theme.second(), 0F));
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (AuraMenuMod.OPEN.matches(key, scan)) {
            minecraft.setScreen(new HubScreen(this));
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
