package it.adryh.aura.inventory;

import it.adryh.aura.core.Module;
import it.adryh.aura.core.Setting;

final class InventorySettings {
    static final Module MODULE = new Module("inventory", "Inventario", "Inventari scuri, animazioni e tooltip", 4);

    static final Setting.Toggle DARK;
    static final Setting.Choice PALETTE;
    static final Setting.Toggle FRAME;
    static final Setting.Choice ANIMATION;
    static final Setting.Toggle GLOW;
    static final Setting.Toggle RARITY;
    static final Setting.Toggle TOOLTIP;
    static final Setting.Choice TOOLTIP_BORDER;

    static {
        Module m = MODULE;
        m.header("Aspetto");
        DARK = m.toggle("dark", "Inventari scuri", true);
        PALETTE = m.<Setting.Choice>choice("palette", "Tonalità", 0, "Notte", "Grafite", "Viola", "Oceano", "Tema")
                .onlyIf(() -> DARK.on());
        FRAME = m.toggle("frame", "Cornice colorata", true);
        ANIMATION = m.choice("animation", "Animazione di apertura", 0, "Scorrimento", "Zoom", "Nessuna");
        m.header("Oggetti");
        GLOW = m.toggle("glow", "Bagliore sulla casella", true);
        RARITY = m.<Setting.Toggle>toggle("rarity", "Evidenzia la rarità", true).hint("Oggetti rari e incantati colorati");
        m.header("Descrizioni");
        TOOLTIP = m.toggle("tooltip", "Tooltip in vetro", true);
        TOOLTIP_BORDER = m.<Setting.Choice>choice("tooltipBorder", "Bordo", 1, "Colore tema", "Rarità")
                .onlyIf(() -> TOOLTIP.on());
    }

    private InventorySettings() {
    }
}
