package it.adryh.aura.core;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Animated glass button used by the pack's own screens. */
public class AuraButton extends AbstractButton {
    private final Runnable action;
    private boolean primary;
    private float hover;
    private long last = System.currentTimeMillis();

    public AuraButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    public AuraButton primary() {
        this.primary = true;
        return this;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        hover = Theme.approach(hover, isHoveredOrFocused() && active ? 1F : 0F, (now - last) / 1000F);
        last = now;
        float x = getX(), y = getY() - hover, w = getWidth(), h = getHeight();
        float r = Math.min(Theme.radius(), h / 2F);
        int a = Theme.accent(), b = Theme.second();
        if (hover > 0.02F) {
            Draw.shadow(g, x, y, w, h, r, 9F, Colors.alpha(a, 0.45F * hover * alpha));
        }
        if (primary) {
            Draw.gradientH(g, x, y, w, h, r, Colors.alpha(Colors.lerp(a, 0xFFFFFFFF, 0.12F * hover), 0.92F * alpha),
                    Colors.alpha(Colors.lerp(Colors.lerp(a, b, 0.65F), 0xFFFFFFFF, 0.12F * hover), 0.92F * alpha));
        } else {
            Draw.rect(g, x, y, w, h, r, Colors.fade(Colors.lerp(0x9A121426, 0xD0282C48, hover), alpha));
            Draw.gradientH(g, x, y, w, h, r, Colors.alpha(a, 0.30F * hover * alpha), Colors.alpha(b, 0.08F * hover * alpha));
        }
        Draw.outline(g, x, y, w, h, r, 0.75F, Colors.alpha(0xFFFFFF, (0.14F + 0.45F * hover) * alpha));
        int color = Colors.alpha(active ? 0xFFFFFF : 0x777A90, Math.max(0.05F, alpha));
        String label = Text.fit(getMessage().getString(), w - 8, primary, 1F);
        Text.centered(g, label, x + w / 2F, y + (h - 8F) / 2F, color, primary, 1F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
