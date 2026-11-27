package it.adryh.aura.hud;

import it.adryh.aura.core.Module;
import it.adryh.aura.core.Setting;
import net.minecraft.client.Minecraft;

final class HudSettings {
    static final Module MODULE = new Module("hud", "HUD", "Barra di gioco e riquadri informativi", 1);

    static final Setting.Toggle BAR;
    static final Setting.Toggle NUMBERS;
    static final Setting.Color BAR_COLOR;
    static final Setting.Slider SCALE;
    static final Setting.Slider OPACITY;
    static final Setting.Color LABELS;
    static final Setting.Toggle FPS;
    static final Setting.Toggle COORDS;
    static final Setting.Toggle COMPASS;
    static final Setting.Toggle CLOCK;
    static final Setting.Toggle ARMOR;
    static final Setting.Toggle POTIONS;
    static final Setting.Toggle KEYS;
    static final Setting.Toggle PING;
    static final Setting.Toggle SPEED;

    static {
        Module m = MODULE;
        m.header("Barra di gioco");
        BAR = m.toggle("bar", "Barra moderna", true);
        NUMBERS = m.toggle("numbers", "Numeri su vita e fame", true);
        BAR_COLOR = m.color("barColor", "Colore della selezione", Setting.Color.FOLLOW);
        m.header("Riquadri");
        m.action("Posizione dei riquadri", "Sposta", () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new HudEditorScreen(mc.screen));
        });
        SCALE = m.slider("scale", "Dimensione", 100, 60, 150, 5, "%");
        OPACITY = m.slider("opacity", "Opacità sfondo", 70, 0, 100, 5, "%");
        LABELS = m.color("labels", "Colore etichette", Setting.Color.FOLLOW);
        m.header("Cosa mostrare");
        FPS = m.toggle("fps", "FPS", true);
        COORDS = m.toggle("coords", "Coordinate e bioma", true);
        COMPASS = m.toggle("compass", "Bussola", true);
        CLOCK = m.toggle("clock", "Orologio", true);
        ARMOR = m.toggle("armor", "Armatura e oggetto in mano", true);
        POTIONS = m.toggle("potions", "Effetti delle pozioni", true);
        KEYS = m.toggle("keys", "Tasti premuti e CPS", true);
        PING = m.toggle("ping", "Ping", false);
        SPEED = m.toggle("speed", "Velocità", false);
    }

    private HudSettings() {
    }
}
