package it.adryh.aura.core;

/** The look shared by every mod of the pack. */
public final class Theme {
    public static final Module MODULE = new Module("menu", "Aspetto", "Colori, vetro, menu e schermata del titolo", 0);

    public static final int TEXT = 0xFFF2F3FA;
    public static final int TEXT_DIM = 0xFF9BA0BC;

    private static final Setting.Choice PRESET;
    private static final Setting.Color ACCENT;
    private static final Setting.Color SECOND;
    private static final Setting.Slider OPACITY;
    private static final Setting.Slider RADIUS;
    private static final Setting.Toggle FONT;
    private static final Setting.Slider SPEED;
    public static final Setting.Toggle TITLE;
    public static final Setting.Choice BACKGROUND;
    public static final Setting.Toggle PARTICLES;
    public static final Setting.Toggle PAUSE;
    public static final Setting.Toggle HOVER;
    public static final Setting.Toggle MENUS;

    private static final String[] PRESETS = {"Viola", "Oceano", "Smeraldo", "Tramonto", "Rubino", "Oro", "Ghiaccio", "Personalizzato"};
    private static final int[][] PAIRS = {
            {0x8B5CF6, 0x22D3EE}, {0x3B82F6, 0x2DD4BF}, {0x10B981, 0xA3E635}, {0xF97316, 0xEC4899},
            {0xF43F5E, 0xA855F7}, {0xF59E0B, 0xFDE047}, {0x7DD3FC, 0xE0F2FE}};

    static {
        Module m = MODULE;
        m.header("Colori");
        PRESET = m.choice("preset", "Tema colore", 0, PRESETS);
        ACCENT = m.add(new Setting.Color("accent", "Colore principale", 0x8B5CF6, false))
                .onlyIf(() -> PRESET.index() == PRESETS.length - 1);
        SECOND = m.add(new Setting.Color("second", "Colore secondario", 0x22D3EE, false))
                .onlyIf(() -> PRESET.index() == PRESETS.length - 1);
        m.header("Vetro");
        OPACITY = m.slider("opacity", "Opacita' pannelli", 78, 40, 100, 1, "%");
        RADIUS = m.slider("radius", "Angoli arrotondati", 6, 0, 10, 1, " px");
        FONT = m.toggle("font", "Font moderno", true);
        SPEED = m.slider("speed", "Velocita' animazioni", 1.0, 0.0, 2.0, 0.1, "x");
        m.header("Menu");
        TITLE = m.toggle("title", "Schermata del titolo Aura", true);
        MENUS = m.toggle("menus", "Sfondo Aura nei menu", true);
        BACKGROUND = m.choice("background", "Sfondo", 0, "Aurora", "Onde", "Panorama");
        PARTICLES = m.toggle("particles", "Particelle sullo sfondo", true);
        PAUSE = m.toggle("pause", "Menu di pausa Aura", true);
        HOVER = m.toggle("hover", "Evidenzia i pulsanti", true);
    }

    private Theme() {
    }

    public static int accent() {
        return PRESET.index() < PAIRS.length ? 0xFF000000 | PAIRS[PRESET.index()][0] : ACCENT.get();
    }

    public static int second() {
        return PRESET.index() < PAIRS.length ? 0xFF000000 | PAIRS[PRESET.index()][1] : SECOND.get();
    }

    /** Dark glass fill using the chosen opacity. */
    public static int glass() {
        return Colors.alpha(0x0D0F1C, OPACITY.f() / 100F);
    }

    public static float radius() {
        return RADIUS.f();
    }

    public static boolean modernFont() {
        return FONT.on();
    }

    /** Seconds used to drive animations, scaled by the chosen speed. */
    public static float time() {
        return (System.currentTimeMillis() % 3_600_000L) / 1000F * SPEED.f();
    }

    /** Smoothly moves a value towards a target, independently of the frame rate. */
    public static float approach(float value, float target, float seconds) {
        if (SPEED.get() <= 0.01) {
            return target;
        }
        return value + (target - value) * Math.min(1F, seconds * 14F * SPEED.f());
    }
}
