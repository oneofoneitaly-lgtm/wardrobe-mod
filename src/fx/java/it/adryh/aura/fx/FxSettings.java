package it.adryh.aura.fx;

import it.adryh.aura.core.Module;
import it.adryh.aura.core.Setting;

final class FxSettings {
    static final Module MODULE = new Module("fx", "Effetti", "Mirino, contorno blocchi, zoom e particelle", 3);

    static final Setting.Toggle CROSS;
    static final Setting.Choice STYLE;
    static final Setting.Color CROSS_COLOR;
    static final Setting.Slider SIZE;
    static final Setting.Slider THICK;
    static final Setting.Slider GAP;
    static final Setting.Toggle BORDER;
    static final Setting.Toggle DYNAMIC;
    static final Setting.Toggle OUTLINE;
    static final Setting.Color OUTLINE_COLOR;
    static final Setting.Toggle RAINBOW;
    static final Setting.Slider OUTLINE_WIDTH;
    static final Setting.Slider FILL;
    static final Setting.Toggle PULSE;
    static final Setting.Toggle ZOOM;
    static final Setting.Slider ZOOM_LEVEL;
    static final Setting.Toggle ZOOM_SMOOTH;
    static final Setting.Toggle ZOOM_CINEMA;
    static final Setting.Toggle HIT;
    static final Setting.Choice HIT_STYLE;
    static final Setting.Slider HIT_AMOUNT;
    static final Setting.Toggle TRAIL;
    static final Setting.Choice TRAIL_STYLE;
    static final Setting.Slider VIGNETTE;
    static final Setting.Color VIGNETTE_COLOR;
    static final Setting.Toggle DAMAGE;
    static final Setting.Toggle LOW_HEALTH;

    static final String[] PARTICLES = {"Stelle", "Cuori", "Anime", "Magia", "Fiamme", "Ciliegio", "Note"};

    static {
        Module m = MODULE;
        m.header("Mirino");
        CROSS = m.toggle("cross", "Mirino personalizzato", true);
        STYLE = m.choice("style", "Forma", 0, "Croce", "Punto", "Cerchio", "Cerchio e punto", "Freccia", "Quadrato");
        CROSS_COLOR = m.color("crossColor", "Colore", Setting.Color.FOLLOW);
        SIZE = m.slider("size", "Grandezza", 5, 2, 14, 0.5, "");
        THICK = m.slider("thick", "Spessore", 1.0, 0.5, 4, 0.5, "");
        GAP = m.slider("gap", "Spazio centrale", 2, 0, 8, 0.5, "");
        BORDER = m.toggle("border", "Bordo scuro", true);
        DYNAMIC = m.<Setting.Toggle>toggle("dynamic", "Mirino dinamico", true).hint("Si allarga muovendoti e attaccando");
        m.header("Contorno dei blocchi");
        OUTLINE = m.toggle("outline", "Contorno colorato", true);
        OUTLINE_COLOR = m.color("outlineColor", "Colore", Setting.Color.FOLLOW);
        RAINBOW = m.toggle("rainbow", "Arcobaleno", false);
        OUTLINE_WIDTH = m.slider("outlineWidth", "Spessore", 3, 1, 10, 1, "");
        FILL = m.slider("fill", "Riempimento", 12, 0, 60, 2, "%");
        PULSE = m.toggle("pulse", "Pulsazione", true);
        m.header("Zoom (tasto C)");
        ZOOM = m.toggle("zoom", "Zoom", true);
        ZOOM_LEVEL = m.<Setting.Slider>slider("zoomLevel", "Ingrandimento", 4, 2, 10, 0.5, "x").hint("La rotellina lo cambia mentre tieni il tasto");
        ZOOM_SMOOTH = m.toggle("zoomSmooth", "Zoom fluido", true);
        ZOOM_CINEMA = m.toggle("zoomCinema", "Visuale cinematografica", false);
        m.header("Particelle");
        HIT = m.toggle("hit", "Particelle quando colpisci", true);
        HIT_STYLE = m.choice("hitStyle", "Tipo", 0, PARTICLES);
        HIT_AMOUNT = m.slider("hitAmount", "Quantità", 14, 4, 40, 2, "");
        TRAIL = m.toggle("trail", "Scia mentre cammini", false);
        TRAIL_STYLE = m.<Setting.Choice>choice("trailStyle", "Tipo di scia", 5, PARTICLES).onlyIf(() -> TRAIL.on());
        m.header("Vignetta");
        VIGNETTE = m.slider("vignette", "Bordi colorati", 25, 0, 100, 5, "%");
        VIGNETTE_COLOR = m.color("vignetteColor", "Colore", 0x000000);
        DAMAGE = m.toggle("damage", "Lampo quando subisci danni", true);
        LOW_HEALTH = m.toggle("lowHealth", "Battito con poca vita", true);
    }

    private FxSettings() {
    }
}
