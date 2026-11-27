package it.adryh.aura.inventory;

import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;

import java.lang.reflect.Field;

final class Effects {
    private static final int LABEL = 0xE9EBF8;
    private static long openedAt;
    private static boolean pushed;

    private Effects() {
    }

    static void opened() {
        openedAt = System.currentTimeMillis();
    }

    static void before(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Themer.apply();
        int style = InventorySettings.ANIMATION.index();
        float length = 220F;
        float k = (System.currentTimeMillis() - openedAt) / length;
        if (style == 2 || k >= 1F || event.isCanceled()) {
            return;
        }
        float ease = 1F - (float) Math.pow(1F - Math.max(0F, k), 3);
        GuiGraphics g = event.getGuiGraphics();
        g.pose().pushPose();
        if (style == 0) {
            g.pose().translate(0F, (1F - ease) * 28F, 0F);
        } else {
            float scale = 0.85F + 0.15F * ease;
            g.pose().translate(screen.width / 2F, screen.height / 2F, 0F);
            g.pose().scale(scale, scale, 1F);
            g.pose().translate(-screen.width / 2F, -screen.height / 2F, 0F);
        }
        pushed = true;
    }

    static void after(ScreenEvent.Render.Post event) {
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }

    private static int rarityColor(ItemStack stack) {
        Rarity rarity = stack.getRarity();
        ChatFormatting format = rarity.color();
        Integer color = format.getColor();
        if (rarity != Rarity.COMMON && color != null) {
            return 0xFF000000 | color;
        }
        return stack.isEnchanted() ? 0xFFB26BFF : 0;
    }

    /** Runs before the items are drawn, in screen coordinates. */
    static void background(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics g = event.getGuiGraphics();
        int left = screen.getGuiLeft(), top = screen.getGuiTop();
        if (InventorySettings.FRAME.on()) {
            float x = left - 0.5F, y = top - 0.5F, w = screen.getXSize() + 1, h = screen.getYSize() + 1;
            Draw.shadow(g, x, y, w, h, 4F, 10F, Colors.alpha(Theme.accent(), 0.35F));
            Draw.outlineH(g, x, y, w, h, 4F, 1F, Theme.accent(), Theme.second());
        }
        if (InventorySettings.RARITY.on()) {
            for (Slot slot : screen.getMenu().slots) {
                if (!slot.isActive() || !slot.hasItem()) {
                    continue;
                }
                int color = rarityColor(slot.getItem());
                if (color != 0) {
                    float x = left + slot.x, y = top + slot.y;
                    Draw.gradientV(g, x, y, 16, 16, 2F, Colors.alpha(color, 0.08F), Colors.alpha(color, 0.50F));
                    Draw.rect(g, x + 1, y + 15, 14, 1, 0.5F, color);
                }
            }
        }
    }

    /** Runs after the labels, with the origin on the top left of the panel. */
    static void foreground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics g = event.getGuiGraphics();
        if (InventorySettings.DARK.on()) {
            labels(g, screen);
        }
        Slot hovered = screen.getSlotUnderMouse();
        if (InventorySettings.GLOW.on() && hovered != null && hovered.isActive() && hovered.isHighlightable()) {
            float pulse = 0.75F + 0.25F * (float) Math.sin(System.currentTimeMillis() / 150.0);
            Draw.shadow(g, hovered.x - 1, hovered.y - 1, 18, 18, 3F, 6F, Colors.alpha(Theme.accent(), 0.8F * pulse));
            Draw.outlineH(g, hovered.x - 1, hovered.y - 1, 18, 18, 3F, 1F, Theme.accent(), Theme.second());
        }
    }

    /** The vanilla titles are dark grey: draw them again in a light colour, exactly on top. */
    private static void labels(GuiGraphics g, AbstractContainerScreen<?> screen) {
        Font font = Minecraft.getInstance().font;
        if (screen instanceof CreativeModeInventoryScreen) {
            Object tab = read(CreativeModeInventoryScreen.class, null, "selectedTab");
            if (tab instanceof CreativeModeTab selected && selected.showTitle()) {
                g.drawString(font, selected.getDisplayName(), 8, 6, LABEL, false);
            }
            return;
        }
        if (screen instanceof BeaconScreen) {
            return;
        }
        int inventoryX = number(screen, "inventoryLabelX"), inventoryY = number(screen, "inventoryLabelY");
        Object inventoryTitle = read(AbstractContainerScreen.class, screen, "playerInventoryTitle");
        if (screen instanceof MerchantScreen merchant) {
            MerchantMenu menu = merchant.getMenu();
            Component title = screen.getTitle();
            int level = menu.getTraderLevel();
            if (level > 0 && level <= 5 && menu.showProgressBar()) {
                title = Component.translatable("merchant.title", title, Component.translatable("merchant.level." + level));
            }
            g.drawString(font, title, 49 + screen.getXSize() / 2 - font.width(title) / 2, 6, LABEL, false);
            Component trades = Component.translatable("merchant.trades");
            g.drawString(font, trades, 5 - font.width(trades) / 2 + 48, 6, LABEL, false);
        } else {
            g.drawString(font, screen.getTitle(), number(screen, "titleLabelX"), number(screen, "titleLabelY"), LABEL, false);
            if (screen instanceof InventoryScreen) {
                return;
            }
        }
        if (inventoryTitle instanceof Component c) {
            g.drawString(font, c, inventoryX, inventoryY, LABEL, false);
        }
    }

    private static int number(AbstractContainerScreen<?> screen, String name) {
        return read(AbstractContainerScreen.class, screen, name) instanceof Integer i ? i : 0;
    }

    private static Object read(Class<?> owner, Object instance, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    static void tooltip(RenderTooltipEvent.Color event) {
        if (!InventorySettings.TOOLTIP.on()) {
            return;
        }
        int top = Theme.accent(), bottom = Theme.second();
        ItemStack stack = event.getItemStack();
        if (InventorySettings.TOOLTIP_BORDER.index() == 1 && !stack.isEmpty()) {
            int rarity = rarityColor(stack);
            if (rarity != 0) {
                top = rarity;
                bottom = Colors.lerp(rarity, 0xFF000000, 0.45F);
            }
        }
        event.setBackgroundStart(0xF4141730);
        event.setBackgroundEnd(0xF4090A14);
        event.setBorderStart(top);
        event.setBorderEnd(bottom);
    }
}
