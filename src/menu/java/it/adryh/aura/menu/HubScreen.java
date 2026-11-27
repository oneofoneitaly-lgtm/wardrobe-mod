package it.adryh.aura.menu;

import it.adryh.aura.core.Aura;
import it.adryh.aura.core.AuraButton;
import it.adryh.aura.core.Backdrop;
import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Glass;
import it.adryh.aura.core.Module;
import it.adryh.aura.core.Setting;
import it.adryh.aura.core.Text;
import it.adryh.aura.core.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aura Studio: every option of the pack, changed live. */
public final class HubScreen extends Screen {
    private static final int[] SWATCHES = {0x8B5CF6, 0x6366F1, 0x3B82F6, 0x22D3EE, 0x10B981, 0xA3E635,
            0xFDE047, 0xF97316, 0xF43F5E, 0xEC4899, 0xFFFFFF, 0x111827};
    private static int selected;

    private final Screen parent;
    private final Map<Setting, Float> anim = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<AuraButton> buttons = new ArrayList<>();
    private Setting.Color openColor;
    private Setting dragged;
    private int dragBar;
    private float scroll;
    private float scrollTarget;
    private float contentHeight;
    private float selector = -1;
    private float appear;
    private long last = System.currentTimeMillis();
    private boolean preview;
    private int px, py, pw, ph, side;

    private record Row(Setting setting, float y, float height) {
    }

    public HubScreen(Screen parent) {
        super(Component.literal("Aura Studio"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(470, width - 16);
        ph = Math.min(290, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        side = pw < 380 ? 96 : 128;
        buttons.clear();
        int y = py + ph - 26;
        buttons.add(addRenderableWidget(new AuraButton(px + pw - 78, y, 70, 18, Component.literal("Fatto"), this::onClose).primary()));
        buttons.add(addRenderableWidget(new AuraButton(px + pw - 154, y, 70, 18, Component.literal("Ripristina"), () -> {
            module().reset();
            openColor = null;
        })));
    }

    private Module module() {
        List<Module> modules = Aura.modules();
        selected = Math.min(selected, modules.size() - 1);
        return modules.get(selected);
    }

    private float listTop() {
        return py + 40;
    }

    private float listBottom() {
        return py + ph - 32;
    }

    private float listLeft() {
        return px + side + 8;
    }

    private float listRight() {
        return px + pw - 8;
    }

    private void layout() {
        rows.clear();
        float y = 0;
        for (Setting s : module().settings()) {
            if (!s.visible()) {
                continue;
            }
            float h;
            if (s instanceof Setting.Header) {
                h = 18;
            } else {
                h = s.hint().isEmpty() ? 22 : 28;
                if (s == openColor) {
                    h += 58;
                }
            }
            rows.add(new Row(s, y, h));
            y += h + 3;
        }
        contentHeight = y;
    }

    private float maxScroll() {
        return Math.max(0, contentHeight - (listBottom() - listTop()));
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - last) / 1000F);
        last = now;
        for (AuraButton b : buttons) {
            b.visible = !preview;
        }
        if (minecraft.level == null) {
            Backdrop.render(g, width, height);
        }
        if (preview) {
            Text.centered(g, "Anteprima - rilascia TAB per tornare", width / 2F, 8, 0xFFFFFFFF);
            return;
        }
        if (minecraft.level != null) {
            Draw.rect(g, 0, 0, width, height, 0, 0x55000000);
        }
        appear = Theme.approach(appear, 1F, dt);
        layout();
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget));
        scroll = Theme.approach(scroll, scrollTarget, dt);

        Glass.panel(g, px, py, pw, ph);
        Draw.rect(g, px + 4, py + 4, side - 4, ph - 8, Theme.radius(), 0x40000000);
        sidebar(g, mouseX, mouseY, dt);

        Module m = module();
        Text.draw(g, m.title, listLeft() + 2, py + 10, Theme.TEXT, true, 1.4F);
        Text.draw(g, Text.fit(m.subtitle, listRight() - listLeft() - 4, false, 0.85F), listLeft() + 2, py + 25, Theme.TEXT_DIM, false, 0.85F);

        g.enableScissor((int) listLeft() - 2, (int) listTop(), (int) listRight() + 2, (int) listBottom());
        for (Row row : rows) {
            float y = listTop() + row.y - scroll;
            if (y + row.height < listTop() || y > listBottom()) {
                continue;
            }
            drawRow(g, row, y, mouseX, mouseY, dt);
        }
        g.disableScissor();

        if (maxScroll() > 0) {
            float track = listBottom() - listTop();
            float len = Math.max(20, track * track / contentHeight);
            Draw.rect(g, listRight() + 2, listTop() + (track - len) * scroll / maxScroll(), 2, len, 1, Colors.alpha(Theme.accent(), 0.8F));
        }
        if (minecraft.level != null) {
            Text.draw(g, "Tieni TAB per vedere il gioco", listLeft() + 2, py + ph - 20, Theme.TEXT_DIM, false, 0.8F);
        }
    }

    private void sidebar(GuiGraphics g, int mouseX, int mouseY, float dt) {
        Text.draw(g, "AURA", px + 14, py + 12, Theme.TEXT, true, 1.7F);
        Draw.gradientH(g, px + 14, py + 29, 34, 2, 1, Theme.accent(), Theme.second());
        List<Module> modules = Aura.modules();
        float step = Math.min(34, (ph - 62) / (float) Math.max(1, modules.size()));
        float top = py + 40;
        float target = top + selected * step;
        selector = selector < 0 ? target : Theme.approach(selector, target, dt);
        Draw.gradientH(g, px + 8, selector, side - 12, step - 4, Theme.radius(),
                Colors.alpha(Theme.accent(), 0.55F), Colors.alpha(Theme.second(), 0.12F));
        Draw.rect(g, px + 8, selector + 6, 2, step - 16, 1, Theme.second());
        for (int i = 0; i < modules.size(); i++) {
            float y = top + i * step;
            boolean hover = inside(mouseX, mouseY, px + 8, y, side - 12, step - 4);
            if (hover && i != selected) {
                Draw.rect(g, px + 8, y, side - 12, step - 4, Theme.radius(), 0x18FFFFFF);
            }
            int color = i == selected ? 0xFFFFFFFF : hover ? Theme.TEXT : Theme.TEXT_DIM;
            float size = Math.min(16, step - 10);
            Draw.texture(g, modules.get(i).icon, px + 16, y + (step - 4 - size) / 2F, size, size, 64, color);
            Text.draw(g, Text.fit(modules.get(i).title, side - 48, true, 1F), px + 39, y + (step - 12) / 2F, color, true, 1F);
        }
        Text.draw(g, "by @Adryh01", px + 14, py + ph - 18, Theme.TEXT_DIM, false, 0.85F);
    }

    private void drawRow(GuiGraphics g, Row row, float y, int mouseX, int mouseY, float dt) {
        Setting s = row.setting;
        float x = listLeft(), w = listRight() - x;
        if (s instanceof Setting.Header) {
            Text.draw(g, s.label.toUpperCase(java.util.Locale.ROOT), x + 2, y + 7, Theme.second(), true, 0.8F);
            float tw = Text.width(s.label.toUpperCase(java.util.Locale.ROOT), true, 0.8F);
            Draw.gradientH(g, x + tw + 10, y + 10.5F, w - tw - 12, 0.75F, 0, Colors.alpha(Theme.second(), 0.45F), Colors.alpha(Theme.second(), 0F));
            return;
        }
        boolean hover = inside(mouseX, mouseY, x, y, w, row.height) && mouseY >= listTop() && mouseY < listBottom();
        Draw.rect(g, x, y, w, row.height, Math.min(Theme.radius(), 8), hover ? 0x22FFFFFF : 0x12FFFFFF);
        float base = s.hint().isEmpty() ? 22 : 28;
        float cy = y + base / 2F;
        float labelWidth = w - controlWidth(s) - 20;
        if (s.hint().isEmpty()) {
            Text.draw(g, Text.fit(s.label, labelWidth, false, 1F), x + 8, cy - 4.5F, Theme.TEXT);
        } else {
            Text.draw(g, Text.fit(s.label, labelWidth, false, 1F), x + 8, cy - 9.5F, Theme.TEXT);
            Text.draw(g, Text.fit(s.hint(), labelWidth, false, 0.75F), x + 8, cy + 3F, Theme.TEXT_DIM, false, 0.75F);
        }
        float right = listRight() - 8;
        if (s instanceof Setting.Toggle t) {
            float value = anim.getOrDefault(s, t.on() ? 1F : 0F);
            value = Theme.approach(value, t.on() ? 1F : 0F, dt);
            anim.put(s, value);
            Draw.rect(g, right - 28, cy - 7, 28, 14, 7, 0xB0262A42);
            Draw.gradientH(g, right - 28, cy - 7, 28, 14, 7, Colors.alpha(Theme.accent(), value), Colors.alpha(Theme.second(), value));
            Draw.circle(g, right - 21 + 14 * value, cy, 5, Colors.lerp(0xFFB8BCD4, 0xFFFFFFFF, value));
        } else if (s instanceof Setting.Slider slider) {
            float sw = sliderWidth();
            float sx = right - sw - 5;
            float k = (float) ((slider.get() - slider.min) / (slider.max - slider.min));
            Draw.rect(g, sx, cy - 2, sw, 4, 2, 0xB0262A42);
            Draw.gradientH(g, sx, cy - 2, Math.max(4, sw * k), 4, 2, Theme.accent(), Theme.second());
            Draw.shadow(g, sx + sw * k - 4.5F, cy - 4.5F, 9, 9, 4.5F, 5, Colors.alpha(Theme.accent(), dragged == s ? 0.8F : 0.35F));
            Draw.circle(g, sx + sw * k, cy, 4.5F, 0xFFFFFFFF);
            String text = slider.text();
            Text.draw(g, text, sx - 10 - Text.width(text), cy - 4.5F, Theme.TEXT_DIM);
        } else if (s instanceof Setting.Choice choice) {
            float cw = controlWidth(s);
            Draw.rect(g, right - cw, cy - 8, cw, 16, 8, 0xB0262A42);
            Draw.outline(g, right - cw, cy - 8, cw, 16, 8, 0.6F, Colors.alpha(Theme.accent(), 0.6F));
            Text.centered(g, "<", right - cw + 9, cy - 4.5F, Theme.second(), true, 1F);
            Text.centered(g, ">", right - 9, cy - 4.5F, Theme.second(), true, 1F);
            Text.centered(g, Text.fit(choice.text(), cw - 30, false, 1F), right - cw / 2F, cy - 4.5F, Theme.TEXT);
        } else if (s instanceof Setting.Color color) {
            Draw.rect(g, right - 36, cy - 7, 36, 14, 7, color.get());
            Draw.outline(g, right - 36, cy - 7, 36, 14, 7, 0.75F, 0xB0FFFFFF);
            if (color.following()) {
                Text.draw(g, "tema", right - 42 - Text.width("tema"), cy - 4.5F, Theme.TEXT_DIM);
            }
            if (s == openColor) {
                picker(g, color, y + base);
            }
        } else if (s instanceof Setting.Action action) {
            float cw = controlWidth(s);
            boolean over = inside(mouseX, mouseY, right - cw, cy - 8, cw, 16);
            Draw.gradientH(g, right - cw, cy - 8, cw, 16, 8, Colors.alpha(Theme.accent(), over ? 1F : 0.75F),
                    Colors.alpha(Theme.second(), over ? 0.9F : 0.55F));
            Text.centered(g, action.button, right - cw / 2F, cy - 4.5F, 0xFFFFFFFF, true, 1F);
        }
    }

    private float sliderWidth() {
        return Math.max(60, Math.min(110, (listRight() - listLeft()) * 0.34F));
    }

    private float controlWidth(Setting s) {
        if (s instanceof Setting.Toggle) {
            return 28;
        }
        if (s instanceof Setting.Slider) {
            return sliderWidth() + 48;
        }
        if (s instanceof Setting.Choice) {
            return Math.max(70, Math.min(130, (listRight() - listLeft()) * 0.42F));
        }
        if (s instanceof Setting.Action a) {
            return Text.width(a.button, true, 1F) + 24;
        }
        return 68;
    }

    private float barLeft() {
        return listLeft() + 8;
    }

    private float barWidth() {
        return listRight() - listLeft() - 16;
    }

    private int swatchCount(Setting.Color color) {
        return SWATCHES.length + (color.canFollow ? 1 : 0);
    }

    private void picker(GuiGraphics g, Setting.Color color, float top) {
        float x = barLeft(), w = barWidth();
        int count = swatchCount(color);
        float step = Math.min(20, w / count);
        for (int i = 0; i < count; i++) {
            boolean follow = i >= SWATCHES.length;
            float cx = x + i * step + step / 2F;
            if (follow) {
                Draw.gradientH(g, cx - 6, top + 1, 12, 12, 6, Theme.accent(), Theme.second());
                Text.centered(g, "T", cx, top + 3, 0xFFFFFFFF, true, 1F);
            } else {
                Draw.circle(g, cx, top + 7, 6, 0xFF000000 | SWATCHES[i]);
            }
            boolean current = follow ? color.following() : !color.following() && (color.get() & 0xFFFFFF) == SWATCHES[i];
            if (current) {
                Draw.outline(g, cx - 7.5F, top - 0.5F, 15, 15, 7.5F, 1F, 0xFFFFFFFF);
            }
        }
        float[] hsv = Colors.toHsv(color.get());
        for (int bar = 0; bar < 3; bar++) {
            float by = top + 19 + bar * 13;
            int parts = bar == 0 ? 6 : 1;
            for (int i = 0; i < parts; i++) {
                int from, to;
                if (bar == 0) {
                    from = Colors.hsv(i / 6F, 0.85F, 1F);
                    to = Colors.hsv((i + 1) / 6F, 0.85F, 1F);
                } else if (bar == 1) {
                    from = Colors.hsv(hsv[0], 0F, Math.max(0.35F, hsv[2]));
                    to = Colors.hsv(hsv[0], 1F, Math.max(0.35F, hsv[2]));
                } else {
                    from = 0xFF000000;
                    to = Colors.hsv(hsv[0], hsv[1], 1F);
                }
                Draw.gradientH(g, x + i * w / parts, by, w / parts, 7, parts == 1 ? 3.5F : 0, from, to);
            }
            float knob = x + w * hsv[bar];
            Draw.circle(g, knob, by + 3.5F, 5, 0xFFFFFFFF);
            Draw.circle(g, knob, by + 3.5F, 3.2F, color.get());
        }
    }

    // ------------------------------------------------------------------ input

    private static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (preview) {
            return true;
        }
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        List<Module> modules = Aura.modules();
        float step = Math.min(34, (ph - 62) / (float) Math.max(1, modules.size()));
        for (int i = 0; i < modules.size(); i++) {
            if (inside(mx, my, px + 8, py + 40 + i * step, side - 12, step - 4)) {
                if (selected != i) {
                    selected = i;
                    scroll = scrollTarget = 0;
                    openColor = null;
                    click();
                }
                return true;
            }
        }
        if (my < listTop() || my >= listBottom() || mx < listLeft() || mx > listRight()) {
            return false;
        }
        layout();
        for (Row row : rows) {
            float y = listTop() + row.y - scroll;
            if (my < y || my >= y + row.height || row.setting instanceof Setting.Header) {
                continue;
            }
            Setting s = row.setting;
            float base = s.hint().isEmpty() ? 22 : 28;
            float right = listRight() - 8;
            if (s instanceof Setting.Toggle t) {
                t.set(!t.on());
                click();
            } else if (s instanceof Setting.Slider) {
                if (mx >= right - sliderWidth() - 12) {
                    dragged = s;
                    drag(mx);
                }
            } else if (s instanceof Setting.Choice choice) {
                float cw = controlWidth(s);
                boolean back = button == 1 || mx >= right - cw && mx < right - cw + 20;
                choice.cycle(back ? -1 : 1);
                click();
            } else if (s instanceof Setting.Color color) {
                if (my < y + base) {
                    openColor = openColor == color ? null : color;
                    click();
                } else {
                    float top = y + base;
                    if (my < top + 15) {
                        int count = swatchCount(color);
                        int i = (int) ((mx - barLeft()) / Math.min(20, barWidth() / count));
                        if (i >= 0 && i < count) {
                            color.set(i >= SWATCHES.length ? Setting.Color.FOLLOW : SWATCHES[i]);
                            click();
                        }
                    } else {
                        dragged = s;
                        dragBar = Math.max(0, Math.min(2, (int) ((my - top - 16) / 13)));
                        drag(mx);
                    }
                }
            } else if (s instanceof Setting.Action action) {
                if (mx >= right - controlWidth(s)) {
                    click();
                    module().save();
                    action.run.run();
                }
            }
            return true;
        }
        return false;
    }

    private void drag(double mx) {
        if (dragged instanceof Setting.Slider slider) {
            float sw = sliderWidth();
            double k = Math.max(0, Math.min(1, (mx - (listRight() - 8 - sw - 5)) / sw));
            slider.set(slider.min + k * (slider.max - slider.min));
        } else if (dragged instanceof Setting.Color color) {
            float k = (float) Math.max(0, Math.min(1, (mx - barLeft()) / barWidth()));
            float[] hsv = Colors.toHsv(color.get());
            hsv[dragBar] = dragBar == 0 ? Math.min(0.999F, k) : k;
            color.set(Colors.hsv(hsv[0], hsv[1], hsv[2]) & 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragged != null) {
            drag(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragged = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scrollTarget = (float) Math.max(0, Math.min(maxScroll(), scrollTarget - sy * 26));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_TAB && minecraft.level != null) {
            preview = true;
            return true;
        }
        if (AuraMenuMod.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_TAB) {
            preview = false;
            return true;
        }
        return super.keyReleased(key, scan, modifiers);
    }

    @Override
    public void onClose() {
        Screen back = parent;
        if (back instanceof AuraTitleScreen && !Theme.TITLE.on()) {
            back = new TitleScreen();
        }
        minecraft.setScreen(back);
    }

    @Override
    public void removed() {
        Aura.saveAll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
