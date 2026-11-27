package it.adryh.doodlepals.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.Power;
import it.adryh.doodlepals.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/** The sketchbook: a canvas, twelve colours, a spinning preview and the powers the picture would have. */
public final class SketchScreen extends Screen {
    private enum Tool { BRUSH, ERASE, FILL, EYES }

    private static final int ACCENT = 0xFFFFC857;
    private static final int TEXT = 0xFFF2F0FA;
    private static final int MUTED = 0xFF9E98BC;
    private static final String[] BUTTONS = {"brush", "erase", "fill", "eyes", "size", "mirror", "undo", "clear", "idea"};

    // the last sketch survives closing the screen
    private static byte[] pixels = new byte[Drawing.CELLS];
    private static final List<Integer> eyes = new ArrayList<>();
    private static String name = "";
    private static int colour = Power.FIRE.code();
    private static Tool tool = Tool.BRUSH;
    private static int brush = 2;
    private static boolean mirror;
    private static int idea;

    private record Snapshot(byte[] pixels, List<Integer> eyes) {}

    private final Deque<Snapshot> history = new ArrayDeque<>();
    private final boolean free;
    private EditBox nameBox;
    private Drawing drawing;
    private boolean painting;
    private boolean erasing;
    private int lastCell = -1;
    private int left;
    private int top;
    private int cell;
    private int panelW;
    private int panelH;
    private int ticks;

    public SketchScreen(boolean free) {
        super(Component.translatable("doodlepals.sketch.title"));
        this.free = free;
        refresh();
    }

    @Override
    protected void init() {
        cell = Math.max(5, Math.min(8, Math.min((height - 48) / Drawing.SIZE, (width - 200) / Drawing.SIZE)));
        panelW = 54 + cell * Drawing.SIZE + 144;
        panelH = Math.min(height, Math.max(240, 48 + cell * Drawing.SIZE));
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        nameBox = new EditBox(font, sideX() + 5, top + 128, 118, 12, Component.translatable("doodlepals.sketch.name"));
        nameBox.setBordered(false);
        nameBox.setMaxLength(24);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("doodlepals.sketch.name").withStyle(style -> style.withColor(MUTED & 0xFFFFFF)));
        nameBox.setResponder(value -> name = value);
        addRenderableWidget(nameBox);
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int canvasX() {
        return left + 54;
    }

    private int canvasY() {
        return top + 24;
    }

    private int sideX() {
        return canvasX() + cell * Drawing.SIZE + 8;
    }

    private int buttonStep() {
        return Math.min(20, (panelH - 30) / BUTTONS.length);
    }

    private void refresh() {
        int[] at = eyes.stream().mapToInt(Integer::intValue).toArray();
        drawing = new Drawing(pixels, at);
        eyes.clear();
        for (int eye : drawing.eyes()) {
            eyes.add(eye);
        }
    }

    private void remember() {
        history.push(new Snapshot(pixels.clone(), new ArrayList<>(eyes)));
        while (history.size() > 40) {
            history.removeLast();
        }
    }

    // ------------------------------------------------------------------ drawing it
    private static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int edge) {
        g.fill(x + 2, y, x + w - 2, y + h, fill);
        g.fill(x, y + 2, x + 2, y + h - 2, fill);
        g.fill(x + w - 2, y + 2, x + w, y + h - 2, fill);
        g.fill(x + 1, y + 1, x + 2, y + 2, fill);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, fill);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, fill);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, fill);
        if (edge != 0) {
            g.fill(x + 2, y, x + w - 2, y + 1, edge);
            g.fill(x + 2, y + h - 1, x + w - 2, y + h, edge);
            g.fill(x, y + 2, x + 1, y + h - 2, edge);
            g.fill(x + w - 1, y + 2, x + w, y + h - 2, edge);
            g.fill(x + 1, y + 1, x + 2, y + 2, edge);
            g.fill(x + w - 2, y + 1, x + w - 1, y + 2, edge);
            g.fill(x + 1, y + h - 2, x + 2, y + h - 1, edge);
            g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, edge);
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0x90080612);
        panel(g, left, top, panelW, panelH, 0xE8181428, 0xFF4B4172);
        g.drawString(font, title, left + 9, top + 8, ACCENT, true);
        Component cost = Component.translatable(free ? "doodlepals.sketch.free" : "doodlepals.sketch.cost");
        g.drawString(font, cost, left + panelW - 9 - font.width(cost), top + 8, MUTED, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        Component tooltip = null;

        // tools
        for (int i = 0; i < BUTTONS.length; i++) {
            int x = left + 6;
            int y = canvasY() + i * buttonStep();
            boolean active = i < 4 && tool.ordinal() == i || i == 5 && mirror;
            boolean hover = inside(mouseX, mouseY, x, y, 42, buttonStep() - 2);
            panel(g, x, y, 42, buttonStep() - 2, active ? 0xFF6D52C8 : hover ? 0xFF3A3260 : 0xFF262042, active ? ACCENT : 0);
            Component label = i == 4 ? Component.translatable("doodlepals.tool.size", brush)
                    : Component.translatable("doodlepals.tool." + BUTTONS[i]);
            g.drawCenteredString(font, label, x + 21, y + (buttonStep() - 10) / 2, TEXT);
            if (hover) {
                tooltip = Component.translatable("doodlepals.tool." + BUTTONS[i] + ".info");
            }
        }

        // canvas
        int cx = canvasX();
        int cy = canvasY();
        int side = cell * Drawing.SIZE;
        g.fill(cx - 1, cy - 1, cx + side + 1, cy + side + 1, 0xFF4B4172);
        for (int y = 0; y < Drawing.SIZE; y++) {
            for (int x = 0; x < Drawing.SIZE; x++) {
                Power power = Power.of(pixels[y * Drawing.SIZE + x]);
                int fill = power != null ? 0xFF000000 | power.colour() : (x + y) % 2 == 0 ? 0xFFE9E4D8 : 0xFFDED8CA;
                g.fill(cx + x * cell, cy + y * cell, cx + (x + 1) * cell, cy + (y + 1) * cell, fill);
            }
        }
        if (mirror) {
            g.fill(cx + side / 2, cy, cx + side / 2 + 1, cy + side, 0x806D52C8);
        }
        for (int eye : eyes) {
            int x = cx + eye % Drawing.SIZE * cell;
            int y = cy + eye / Drawing.SIZE * cell;
            g.fill(x, y, x + 2 * cell, y + 2 * cell, 0xFF20202A);
            g.fill(x + 1, y + 1, x + 2 * cell - 1, y + 2 * cell - 1, 0xFFFFFFFF);
            g.fill(x + cell / 2 + 1, y + cell / 2 + 1, x + cell * 3 / 2 + 1, y + cell * 3 / 2 + 1, 0xFF15151C);
        }
        if (inside(mouseX, mouseY, cx, cy, side, side)) {
            int size = tool == Tool.EYES ? 2 : tool == Tool.FILL ? 1 : brush;
            int x = Math.min((mouseX - cx) / cell, Drawing.SIZE - size);
            int y = Math.min((mouseY - cy) / cell, Drawing.SIZE - size);
            g.renderOutline(cx + x * cell, cy + y * cell, size * cell, size * cell, 0xFF3A2F6E);
        }

        // palette
        int swatch = side / Power.values().length;
        for (Power power : Power.values()) {
            int x = cx + power.ordinal() * swatch;
            int y = cy + side + 4;
            boolean active = colour == power.code();
            panel(g, x, y - (active ? 1 : 0), swatch - 1, 14 + (active ? 2 : 0), 0xFF000000 | power.colour(), active ? 0xFFFFFFFF : 0xFF000000);
            if (inside(mouseX, mouseY, x, y, swatch, 14)) {
                tooltip = Component.translatable(power.nameKey()).append(": ").append(Component.translatable(power.infoKey()));
            }
        }

        tooltip = side(g, mouseX, mouseY, partial, tooltip);
        if (tooltip != null) {
            List<FormattedCharSequence> lines = font.split(tooltip, 180);
            g.renderTooltip(font, lines, mouseX, mouseY);
        }
    }

    private Component side(GuiGraphics g, int mouseX, int mouseY, float partial, Component tooltip) {
        int x = sideX();
        int boxH = 96;
        panel(g, x, top + 24, 128, boxH, 0xFF231C3E, 0xFF4B4172);
        if (drawing.painted() > 0) {
            preview(g, x + 64, top + 24 + boxH - 10, (boxH - 20) / (float) Math.max(12, drawing.height()), partial);
        } else {
            g.drawCenteredString(font, Component.translatable("doodlepals.sketch.empty"), x + 64, top + 24 + boxH / 2 - 4, MUTED);
        }
        panel(g, x, top + 124, 128, 16, 0xFF100C20, 0xFF4B4172);
        nameBox.render(g, mouseX, mouseY, partial);

        int y = top + 146;
        if (drawing.isAlive()) {
            Component body = Component.translatable("doodlepals.sketch.body", (int) drawing.health(), (int) drawing.attack());
            g.drawString(font, body, x + 2, y, TEXT, false);
            y += 12;
            for (Drawing.Level level : drawing.powers()) {
                Power power = level.power();
                g.fill(x + 2, y, x + 9, y + 7, 0xFF000000 | power.colour());
                g.renderOutline(x + 1, y - 1, 9, 9, 0xFF6A6290);
                g.drawString(font, Component.translatable(power.nameKey()).append(" " + "I".repeat(level.level())), x + 14, y, TEXT, false);
                if (inside(mouseX, mouseY, x, y - 2, 128, 11)) {
                    tooltip = Component.translatable(power.infoKey());
                }
                y += 11;
            }
            if (drawing.powers().isEmpty()) {
                for (FormattedCharSequence line : font.split(Component.translatable("doodlepals.sketch.nopower"), 124)) {
                    g.drawString(font, line, x + 2, y, MUTED, false);
                    y += 10;
                }
            }
        } else {
            for (FormattedCharSequence line : font.split(Component.translatable("doodlepals.sketch.hint"), 124)) {
                g.drawString(font, line, x + 2, y, MUTED, false);
                y += 10;
            }
        }

        int by = top + panelH - 26;
        boolean ready = drawing.isAlive();
        boolean hover = inside(mouseX, mouseY, x, by, 128, 20);
        panel(g, x, by, 128, 20, ready ? hover ? 0xFF3FCB78 : 0xFF2FA862 : 0xFF3A3550, ready ? 0xFFBFF5D2 : 0xFF55507A);
        g.drawCenteredString(font, Component.translatable("doodlepals.sketch.create"), x + 64, by + 6, ready ? 0xFFFFFFFF : MUTED);
        return tooltip;
    }

    private void preview(GuiGraphics g, int x, int y, float scale, float partial) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 150.0F);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(8.0F));
        // swings from side to side, so the face stays in sight
        float turn = (float) Math.sin((ticks + partial) * 0.06F) * 40.0F;
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        Lighting.setupForEntityInInventory();
        DoodleMesh.of(drawing).render(pose, g.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.0F, 0.0F, ticks % 70 < 4);
        g.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    // ------------------------------------------------------------------ using it
    private int cellAt(double mx, double my) {
        int side = cell * Drawing.SIZE;
        if (!inside(mx, my, canvasX(), canvasY(), side, side)) {
            return -1;
        }
        return ((int) my - canvasY()) / cell * Drawing.SIZE + ((int) mx - canvasX()) / cell;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int at = cellAt(mx, my);
        if (at >= 0) {
            setFocused(null);
            nameBox.setFocused(false);
            remember();
            erasing = button == 1 || tool == Tool.ERASE;
            if (tool == Tool.EYES) {
                placeEye(at, button == 1);
            } else if (tool == Tool.FILL) {
                fill(at, erasing ? 0 : colour);
            } else {
                painting = true;
                lastCell = at;
                stroke(at);
            }
            refresh();
            return true;
        }
        for (int i = 0; i < BUTTONS.length; i++) {
            if (inside(mx, my, left + 6, canvasY() + i * buttonStep(), 42, buttonStep() - 2)) {
                press(i);
                click();
                return true;
            }
        }
        int swatch = cell * Drawing.SIZE / Power.values().length;
        for (Power power : Power.values()) {
            if (inside(mx, my, canvasX() + power.ordinal() * swatch, canvasY() + cell * Drawing.SIZE + 4, swatch, 14)) {
                colour = power.code();
                if (tool == Tool.ERASE || tool == Tool.EYES) {
                    tool = Tool.BRUSH;
                }
                click();
                return true;
            }
        }
        if (inside(mx, my, sideX(), top + panelH - 26, 128, 20) && drawing.isAlive()) {
            Net.create(drawing, name);
            click();
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void press(int index) {
        switch (index) {
            case 0, 1, 2, 3 -> tool = Tool.values()[index];
            case 4 -> brush = brush % 3 + 1;
            case 5 -> mirror = !mirror;
            case 6 -> undo();
            case 7 -> {
                remember();
                Arrays.fill(pixels, (byte) 0);
                eyes.clear();
                nameBox.setValue("");
                refresh();
            }
            default -> {
                remember();
                Drawing template = Templates.get(idea);
                idea = (idea + 1) % Templates.count();
                pixels = template.pixels();
                eyes.clear();
                for (int eye : template.eyes()) {
                    eyes.add(eye);
                }
                nameBox.setValue("");   // a new drawing wants a new name
                refresh();
            }
        }
    }

    private void undo() {
        Snapshot last = history.poll();
        if (last != null) {
            pixels = last.pixels();
            eyes.clear();
            eyes.addAll(last.eyes());
            refresh();
        }
    }

    /** A brush stamp, and its twin on the other side when mirroring. */
    private void stroke(int at) {
        int x0 = at % Drawing.SIZE;
        int y0 = at / Drawing.SIZE;
        for (int dy = 0; dy < brush; dy++) {
            for (int dx = 0; dx < brush; dx++) {
                int x = x0 + dx;
                int y = y0 + dy;
                if (x < Drawing.SIZE && y < Drawing.SIZE) {
                    byte value = (byte) (erasing ? 0 : colour);
                    pixels[y * Drawing.SIZE + x] = value;
                    if (mirror) {
                        pixels[y * Drawing.SIZE + Drawing.SIZE - 1 - x] = value;
                    }
                }
            }
        }
    }

    private void fill(int start, int value) {
        byte old = pixels[start];
        if (old == (byte) value) {
            return;
        }
        Deque<Integer> open = new ArrayDeque<>();
        open.add(start);
        if (mirror) {
            open.add(start - start % Drawing.SIZE + Drawing.SIZE - 1 - start % Drawing.SIZE);
        }
        while (!open.isEmpty()) {
            int at = open.poll();
            if (pixels[at] != old) {
                continue;
            }
            pixels[at] = (byte) value;
            int x = at % Drawing.SIZE;
            if (x > 0) {
                open.add(at - 1);
            }
            if (x < Drawing.SIZE - 1) {
                open.add(at + 1);
            }
            if (at >= Drawing.SIZE) {
                open.add(at - Drawing.SIZE);
            }
            if (at < Drawing.CELLS - Drawing.SIZE) {
                open.add(at + Drawing.SIZE);
            }
        }
    }

    private void placeEye(int at, boolean remove) {
        int x = Math.min(at % Drawing.SIZE, Drawing.SIZE - 2);
        int y = Math.min(at / Drawing.SIZE, Drawing.SIZE - 2);
        // a click on an eye takes it away
        boolean removed = eyes.removeIf(eye -> Math.abs(eye % Drawing.SIZE - x) < 2 && Math.abs(eye / Drawing.SIZE - y) < 2);
        if (!removed && !remove) {
            addEye(y * Drawing.SIZE + x);
            if (mirror) {
                addEye(y * Drawing.SIZE + Drawing.SIZE - 2 - x);
            }
        }
    }

    private void addEye(int at) {
        if (eyes.size() < Drawing.MAX_EYES && Drawing.fitsEye(pixels, at) && !eyes.contains(at)) {
            eyes.add(at);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        int at = cellAt(mx, my);
        if (painting && at >= 0) {
            // join the points so that quick moves leave no gaps
            int x0 = lastCell % Drawing.SIZE;
            int y0 = lastCell / Drawing.SIZE;
            int x1 = at % Drawing.SIZE;
            int y1 = at / Drawing.SIZE;
            int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
            for (int i = 1; i <= steps; i++) {
                stroke(Math.round(y0 + (y1 - y0) * i / (float) steps) * Drawing.SIZE + Math.round(x0 + (x1 - x0) * i / (float) steps));
            }
            lastCell = at;
            refresh();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        painting = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_Z && hasControlDown() && !nameBox.isFocused()) {
            undo();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
