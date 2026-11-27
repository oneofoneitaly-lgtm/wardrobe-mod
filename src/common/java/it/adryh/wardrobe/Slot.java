package it.adryh.wardrobe;

/** Where a piece of clothing is worn. One piece per slot. */
public enum Slot {
    HAT("hat", "cappelli", Tab.HATS),
    TOP("top", "maglie", Tab.TOPS),
    PANTS("pants", "pantaloni", Tab.PANTS),
    SHOES("shoes", "scarpe", Tab.SHOES),
    FACE("face", "viso", Tab.EXTRA),
    NECK("neck", "collo", Tab.EXTRA),
    BACK("back", "schiena", Tab.EXTRA);

    public enum Tab {
        HATS("Cappelli"), TOPS("Maglie"), PANTS("Pantaloni"), SHOES("Scarpe"), EXTRA("Accessori");

        public final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    public final String id;
    public final String folder;
    public final Tab tab;

    Slot(String id, String folder, Tab tab) {
        this.id = id;
        this.folder = folder;
        this.tab = tab;
    }

    public static Slot byName(String name) {
        if (name == null) return null;
        for (Slot s : values()) {
            if (s.id.equalsIgnoreCase(name) || s.folder.equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
