package it.adryh.aura.menu;

import it.adryh.aura.core.AuraButton;
import it.adryh.aura.core.Backdrop;
import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Glass;
import it.adryh.aura.core.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

/** Restyles the vanilla menus. */
final class MenuHooks {
    private MenuHooks() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(MenuHooks::opening);
        MinecraftForge.EVENT_BUS.addListener(MenuHooks::init);
        MinecraftForge.EVENT_BUS.addListener(MenuHooks::background);
        MinecraftForge.EVENT_BUS.addListener(MenuHooks::overlay);
    }

    private static void opening(ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        if (next != null && next.getClass() == TitleScreen.class && Theme.TITLE.on()) {
            event.setNewScreen(new AuraTitleScreen());
        }
    }

    private static void init(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        boolean pause = screen instanceof PauseScreen;
        if (pause || screen instanceof TitleScreen) {
            // an empty pause screen (F3 + Esc) has no buttons
            if (pause && event.getListenersList().stream().noneMatch(l -> l instanceof AbstractButton)) {
                return;
            }
            event.addListener(new AuraButton(screen.width - 108, 8, 100, 20, Component.literal("Aura Studio"),
                    () -> Minecraft.getInstance().setScreen(new HubScreen(screen))).primary());
        }
    }

    private static void background(ScreenEvent.BackgroundRendered event) {
        Screen screen = event.getScreen();
        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (Theme.MENUS.on() && Theme.BACKGROUND.index() != 2) {
                Backdrop.render(g, screen.width, screen.height);
                Draw.rect(g, 0, 0, screen.width, screen.height, 0, 0x40000000);
            }
        } else if (screen instanceof PauseScreen && Theme.PAUSE.on()) {
            pausePanel(g, screen);
        }
    }

    private static void pausePanel(GuiGraphics g, Screen screen) {
        int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = 0, bottom = 0;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractButton w && !(child instanceof AuraButton) && w.visible) {
                left = Math.min(left, w.getX());
                top = Math.min(top, w.getY());
                right = Math.max(right, w.getX() + w.getWidth());
                bottom = Math.max(bottom, w.getY() + w.getHeight());
            }
        }
        if (right == 0) {
            return;
        }
        Draw.gradientV(g, 0, 0, screen.width, screen.height, 0, Colors.alpha(Theme.accent(), 0.16F), 0x70000000);
        int titleTop = Math.min(top - 14, 30);
        Glass.panel(g, left - 14, titleTop, right - left + 28, bottom - titleTop + 14);
        Draw.gradientH(g, screen.width / 2F - 30, 53, 60, 2, 1, Theme.accent(), Theme.second());
    }

    /** Accent frame around the hovered vanilla button or slider. */
    private static void overlay(ScreenEvent.Render.Post event) {
        if (Theme.HOVER.on()) {
            highlight(event.getGuiGraphics(), event.getScreen(), 0);
        }
    }

    private static void highlight(GuiGraphics g, ContainerEventHandler parent, int depth) {
        for (GuiEventListener child : parent.children()) {
            if ((child instanceof AbstractButton || child instanceof AbstractSliderButton) && !(child instanceof AuraButton)) {
                AbstractWidget w = (AbstractWidget) child;
                // icon-only and tab widgets keep their own look
                if (w.visible && w.active && w.isHovered() && w.getHeight() <= 24) {
                    float x = w.getX(), y = w.getY();
                    Draw.shadow(g, x, y, w.getWidth(), w.getHeight(), 4, 7F, Colors.alpha(Theme.accent(), 0.40F));
                    Draw.outlineH(g, x, y, w.getWidth(), w.getHeight(), 4, 1F, Theme.accent(), Theme.second());
                }
            } else if (child instanceof ContainerEventHandler nested && depth < 4) {
                highlight(g, nested, depth + 1);
            }
        }
    }
}
