package it.adryh.wardrobe;

import it.adryh.wardrobe.compat.Gfx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/** The wardrobe: wooden cabinet with the clothes on hangers and a mirror. */
public final class WardrobeScreen extends Screen {
    private static final int COLUMNS = 4;
    private static final int ROWS = 2;

    private static final int WOOD_DARK = 0xFF2E1B0E;
    private static final int WOOD = 0xFF6B4226;
    private static final int WOOD_LIGHT = 0xFF8C5A34;
    private static final int INSIDE = 0xFF1D130C;
    private static final int GOLD = 0xFFE8B93C;
    private static final int TEXT = 0xFFF3E7D3;

    private static Slot.Tab tab = Slot.Tab.HATS;
    private static int page;

    private List<Outfit> items = List.of();
    private int left, top, panelW, panelH;
    private int gridX, gridY, cardW, cardH;
    private int mirrorX, mirrorW, mirrorBottom;

    public WardrobeScreen() {
        super(Component.literal("Armadio"));
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 8, 420);
        panelH = Math.min(height - 8, 234);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        mirrorW = Math.max(80, panelW / 4);
        mirrorX = left + panelW - mirrorW - 10;
        gridX = left + 10;
        gridY = top + 46;
        int gridW = mirrorX - gridX - 8;
        int gridH = panelH - 46 - 34;
        cardW = gridW / COLUMNS;
        cardH = gridH / ROWS;
        mirrorBottom = gridY + gridH;

        Slot.Tab[] tabs = Slot.Tab.values();
        int tabW = (panelW - 20) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            Slot.Tab t = tabs[i];
            Button button = Button.builder(Component.literal(t.title), b -> {
                tab = t;
                page = 0;
                rebuildWidgets();
            }).bounds(left + 10 + i * tabW, top + 20, tabW - 2, 20).build();
            button.active = t != tab;
            addRenderableWidget(button);
        }

        items = Wardrobe.byTab(tab);
        int perPage = COLUMNS * ROWS;
        int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < perPage && page * perPage + i < items.size(); i++) {
            Outfit outfit = items.get(page * perPage + i);
            addRenderableWidget(Button.builder(Component.empty(), b -> Wardrobe.toggle(outfit))
                    .bounds(cardX(i), cardY(i), cardW - 3, cardH - 3).build());
        }

        int y = top + panelH - 27;
        Button previous = Button.builder(Component.literal("<"), b -> {
            page--;
            rebuildWidgets();
        }).bounds(gridX, y, 20, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(gridX + 54, y, 20, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);

        int space = left + panelW - 10 - (gridX + 80);
        int w = space / 4;
        addButton("Togli tutto", gridX + 80, y, w, Wardrobe::undressAll);
        addButton("Cartella", gridX + 80 + w, y, w, () -> {
            try {
                Files.createDirectories(Wardrobe.modelsFolder());
            } catch (IOException e) {
                WardrobeLog.warn(e.toString());
            }
            Gfx.openFolder(Wardrobe.modelsFolder());
        });
        addButton("Ricarica", gridX + 80 + w * 2, y, w, () -> {
            Wardrobe.reload();
            rebuildWidgets();
        });
        addButton("Chiudi", gridX + 80 + w * 3, y, w, this::onClose);
    }

    private void addButton(String label, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w - 2, 20).build());
    }

    private int cardX(int i) {
        return gridX + (i % COLUMNS) * cardW;
    }

    private int cardY(int i) {
        return gridY + (i / COLUMNS) * cardH;
    }

    /** Drawn behind the buttons: the cabinet itself. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x90000000);
        // open doors
        int door = Math.min(26, left - 2);
        if (door > 4) {
            door(g, left - door, door, false);
            door(g, left + panelW, door, true);
        }
        g.fill(left - 2, top - 2, left + panelW + 2, top + panelH + 2, WOOD_DARK);
        g.fill(left, top, left + panelW, top + panelH, WOOD);
        g.fill(left + 3, top + 3, left + panelW - 3, top + 17, WOOD_LIGHT);
        g.fill(gridX - 4, gridY - 4, mirrorX - 6, mirrorBottom + 2, INSIDE);
        // mirror with a golden frame
        g.fill(mirrorX - 2, gridY - 4, mirrorX + mirrorW + 2, mirrorBottom + 2, GOLD);
        g.fill(mirrorX, gridY - 2, mirrorX + mirrorW, mirrorBottom, 0xFF9FB9C9);
        g.fill(mirrorX + 4, gridY + 2, mirrorX + mirrorW - 4, mirrorBottom - 4, 0xFF7F9DB0);
    }

    private void door(GuiGraphics g, int x, int w, boolean right) {
        g.fill(x, top + 4, x + w, top + panelH - 4, WOOD_DARK);
        g.fill(x + 2, top + 8, x + w - 2, top + panelH / 2 - 4, WOOD);
        g.fill(x + 2, top + panelH / 2 + 4, x + w - 2, top + panelH - 8, WOOD);
        int knob = right ? x + 3 : x + w - 6;
        g.fill(knob, top + panelH / 2 - 3, knob + 3, top + panelH / 2 + 3, GOLD);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "ARMADIO  -  by @Adryh01", left + panelW / 2, top + 6, TEXT);

        int perPage = COLUMNS * ROWS;
        for (int i = 0; i < perPage && page * perPage + i < items.size(); i++) {
            Outfit outfit = items.get(page * perPage + i);
            card(g, outfit, cardX(i), cardY(i), cardW - 3, cardH - 3, mouseX, mouseY);
        }
        if (items.isEmpty()) {
            g.drawCenteredString(font, "Nessun vestito", (gridX + mirrorX) / 2, gridY + 40, TEXT);
        }
        int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
        g.drawCenteredString(font, (page + 1) + "/" + pages, gridX + 37, top + panelH - 21, TEXT);

        if (minecraft != null && minecraft.player != null) {
            int h = mirrorBottom - gridY;
            int scale = Math.max(20, Math.min((int) (h / 2.6F), (int) (mirrorW / 1.4F)));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, mirrorX, gridY - 2, mirrorX + mirrorW, mirrorBottom,
                    scale, 0.0625F, mouseX, mouseY, minecraft.player);
        }
        if (!Wardrobe.errors().isEmpty()) {
            String message = "Modelli non caricati: " + String.join(", ", Wardrobe.errors());
            g.drawString(font, font.plainSubstrByWidth(message, width - 8), 4, height - 10, 0xFFFF6B6B, true);
        }
    }

    private void card(GuiGraphics g, Outfit outfit, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean worn = Wardrobe.isWorn(outfit);
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, worn ? GOLD : hover ? TEXT : WOOD_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, worn ? 0xFF5A4318 : hover ? 0xFF4A3322 : 0xFF33231A);
        // rail and hanger
        g.fill(x + 1, y + 4, x + w - 1, y + 5, 0xFFB9BDC4);
        g.fill(x + w / 2, y + 4, x + w / 2 + 1, y + 8, 0xFFB9BDC4);
        g.fill(x + w / 2 - 8, y + 8, x + w / 2 + 9, y + 9, 0xFFB9BDC4);
        int size = Math.max(8, Math.min(w - 6, h - 22));
        Gfx.blit(g, outfit.thumbnail(), x + (w - size) / 2, y + 10, size, size);
        String name = font.plainSubstrByWidth(outfit.name, w - 4);
        g.drawCenteredString(font, name, x + w / 2, y + h - 10, worn ? GOLD : TEXT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
