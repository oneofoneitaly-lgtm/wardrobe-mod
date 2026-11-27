package it.adryh.aura.hud;

import it.adryh.aura.core.AuraButton;
import it.adryh.aura.core.Backdrop;
import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Text;
import it.adryh.aura.core.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Drag the boxes where you like. */
final class HudEditorScreen extends Screen {
    private final Screen parent;
    private Widget dragged;
    private float grabX;
    private float grabY;
    private boolean snapX;
    private boolean snapY;

    HudEditorScreen(Screen parent) {
        super(Component.literal("Riquadri HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = height / 2 + 6;
        addRenderableWidget(new AuraButton(width / 2 - 104, y, 100, 20, Component.literal("Ripristina"),
                () -> HudSettings.MODULE.extra().remove("positions")));
        addRenderableWidget(new AuraButton(width / 2 + 4, y, 100, 20, Component.literal("Fatto"), this::onClose).primary());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            Backdrop.render(g, width, height);
        } else {
            Draw.rect(g, 0, 0, width, height, 0, 0x60000000);
        }
        if (dragged != null && snapX) {
            Draw.rect(g, width / 2F - 0.5F, 0, 1, height, 0, Colors.alpha(Theme.second(), 0.8F));
        }
        if (dragged != null && snapY) {
            Draw.rect(g, 0, height / 2F - 0.5F, width, 1, 0, Colors.alpha(Theme.second(), 0.8F));
        }
        Text.centered(g, "Trascina i riquadri", width / 2F, height / 2F - 22, Theme.TEXT, true, 1.4F);
        Text.centered(g, "Le posizioni vengono salvate automaticamente", width / 2F, height / 2F - 6, Theme.TEXT_DIM);
        for (Widget w : Widgets.ALL) {
            if (!w.enabled.on()) {
                continue;
            }
            w.render(g);
            float x = w.x(width), y = w.y(height), ww = w.width() * Widget.scale(), hh = w.height() * Widget.scale();
            boolean hover = w == dragged || mouseX >= x && mouseX < x + ww && mouseY >= y && mouseY < y + hh;
            Draw.outline(g, x - 2, y - 2, ww + 4, hh + 4, Theme.radius(), hover ? 1.2F : 0.6F,
                    hover ? Theme.second() : 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        for (int i = Widgets.ALL.size() - 1; i >= 0; i--) {
            Widget w = Widgets.ALL.get(i);
            float x = w.x(width), y = w.y(height);
            if (w.enabled.on() && mx >= x && mx < x + w.width() * Widget.scale() && my >= y && my < y + w.height() * Widget.scale()) {
                dragged = w;
                grabX = (float) mx - x;
                grabY = (float) my - y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragged == null) {
            return super.mouseDragged(mx, my, button, dx, dy);
        }
        float w = dragged.width() * Widget.scale(), h = dragged.height() * Widget.scale();
        float x = (float) mx - grabX, y = (float) my - grabY;
        snapX = Math.abs(x + w / 2F - width / 2F) < 5;
        snapY = Math.abs(y + h / 2F - height / 2F) < 5;
        if (snapX) {
            x = width / 2F - w / 2F;
        }
        if (snapY) {
            y = height / 2F - h / 2F;
        }
        dragged.moveTo(x, y, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragged = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        HudSettings.MODULE.save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
