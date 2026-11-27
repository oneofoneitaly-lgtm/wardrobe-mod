package it.adryh.aura.sky;

import it.adryh.aura.core.Module;
import it.adryh.aura.core.Setting;
import it.adryh.aura.core.Theme;

final class SkySettings {
    static final Module MODULE = new Module("sky", "Cielo", "Colori del cielo, aurora, stelle e nebbia", 2);

    static final Setting.Toggle SKY;
    static final Setting.Choice PRESET;
    static final Setting.Slider INTENSITY;
    private static final Setting.Color ZENITH;
    private static final Setting.Color HORIZON;
    static final Setting.Toggle AURORA;
    static final Setting.Color AURORA_LOW;
    static final Setting.Color AURORA_HIGH;
    static final Setting.Slider AURORA_POWER;
    static final Setting.Toggle STARS;
    static final Setting.Slider STAR_COUNT;
    static final Setting.Toggle SHOOTING;
    static final Setting.Toggle NIGHT_ONLY;
    static final Setting.Toggle FOG_TINT;
    static final Setting.Color FOG_COLOR;
    static final Setting.Slider FOG_DISTANCE;
    static final Setting.Choice TIME;

    private static final String[] PRESETS = {"Segui il tema", "Tramonto", "Boreale", "Notte profonda", "Ciliegio", "Smeraldo", "Personalizzato"};
    /** zenith, horizon */
    private static final int[][] COLORS = {
            {0xF97316, 0xFBB6CE}, {0x0B3B5A, 0x34D399}, {0x070B24, 0x3730A3}, {0xF9A8D4, 0xFFE4E6}, {0x047857, 0xA7F3D0}};

    static {
        Module m = MODULE;
        m.header("Colori del cielo");
        SKY = m.toggle("sky", "Cielo Aura", true);
        PRESET = m.choice("preset", "Stile", 0, PRESETS);
        INTENSITY = m.slider("intensity", "Intensita'", 55, 0, 100, 5, "%");
        ZENITH = m.add(new Setting.Color("zenith", "Colore in alto", 0x312E81, false))
                .onlyIf(() -> PRESET.index() == PRESETS.length - 1);
        HORIZON = m.add(new Setting.Color("horizon", "Colore all'orizzonte", 0xF472B6, false))
                .onlyIf(() -> PRESET.index() == PRESETS.length - 1);
        TIME = m.choice("time", "Orario (solo visivo)", 0, "Reale", "Alba", "Giorno", "Tramonto", "Notte");
        m.header("Aurora e stelle");
        AURORA = m.toggle("aurora", "Aurora boreale", true);
        AURORA_LOW = m.color("auroraLow", "Colore aurora in basso", 0x34D399);
        AURORA_HIGH = m.color("auroraHigh", "Colore aurora in alto", 0x8B5CF6);
        AURORA_POWER = m.slider("auroraPower", "Luminosita' aurora", 70, 10, 100, 5, "%");
        STARS = m.toggle("stars", "Stelle scintillanti", true);
        STAR_COUNT = m.slider("starCount", "Numero di stelle", 600, 100, 1500, 50, "");
        SHOOTING = m.toggle("shooting", "Stelle cadenti", true);
        NIGHT_ONLY = m.<Setting.Toggle>toggle("nightOnly", "Solo di notte", true).hint("Aurora e stelle spariscono di giorno");
        m.header("Nebbia");
        FOG_DISTANCE = m.slider("fogDistance", "Distanza della nebbia", 100, 20, 200, 5, "%");
        FOG_TINT = m.toggle("fogTint", "Colora la nebbia", false);
        FOG_COLOR = m.<Setting.Color>color("fogColor", "Colore nebbia", 0xA78BFA).onlyIf(() -> FOG_TINT.on());
    }

    private SkySettings() {
    }

    static int zenith() {
        int i = PRESET.index();
        if (i == 0) {
            return it.adryh.aura.core.Colors.lerp(Theme.accent(), 0xFF0B1030, 0.25F);
        }
        return i == PRESETS.length - 1 ? ZENITH.get() : 0xFF000000 | COLORS[i - 1][0];
    }

    static int horizon() {
        int i = PRESET.index();
        if (i == 0) {
            return it.adryh.aura.core.Colors.lerp(Theme.second(), 0xFFFFFFFF, 0.2F);
        }
        return i == PRESETS.length - 1 ? HORIZON.get() : 0xFF000000 | COLORS[i - 1][1];
    }
}
