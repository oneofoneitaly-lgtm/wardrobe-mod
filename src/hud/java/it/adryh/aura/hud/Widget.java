package it.adryh.aura.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.adryh.aura.core.Setting;
import net.minecraft.client.gui.GuiGraphics;

/** A movable box. Its position is stored as a fraction of the free screen space. */
abstract class Widget {
    static final float MARGIN = 6F;

    final String id;
    final String name;
    final Setting.Toggle enabled;
    private final float defaultX;
    private final float defaultY;

    Widget(String id, String name, Setting.Toggle enabled, float defaultX, float defaultY) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    /** True when there is something to show during the game. */
    boolean hasContent() {
        return true;
    }

    abstract float width();

    abstract float height();

    abstract void draw(GuiGraphics g);

    static float scale() {
        return HudSettings.SCALE.f() / 100F;
    }

    private float[] fraction() {
        JsonObject positions = HudSettings.MODULE.extra().getAsJsonObject("positions");
        if (positions != null && positions.has(id)) {
            JsonArray a = positions.getAsJsonArray(id);
            return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()};
        }
        return new float[]{defaultX, defaultY};
    }

    float x(int screenWidth) {
        float free = Math.max(0, screenWidth - width() * scale() - MARGIN * 2);
        return MARGIN + fraction()[0] * free;
    }

    float y(int screenHeight) {
        float free = Math.max(0, screenHeight - height() * scale() - MARGIN * 2);
        return MARGIN + fraction()[1] * free;
    }

    void moveTo(float x, float y, int screenWidth, int screenHeight) {
        float freeX = Math.max(1, screenWidth - width() * scale() - MARGIN * 2);
        float freeY = Math.max(1, screenHeight - height() * scale() - MARGIN * 2);
        JsonObject extra = HudSettings.MODULE.extra();
        JsonObject positions = extra.getAsJsonObject("positions");
        if (positions == null) {
            positions = new JsonObject();
            extra.add("positions", positions);
        }
        JsonArray a = new JsonArray();
        a.add(Math.max(0F, Math.min(1F, (x - MARGIN) / freeX)));
        a.add(Math.max(0F, Math.min(1F, (y - MARGIN) / freeY)));
        positions.add(id, a);
    }

    void render(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(x(g.guiWidth()), y(g.guiHeight()), 0F);
        g.pose().scale(scale(), scale(), 1F);
        draw(g);
        g.pose().popPose();
    }
}
