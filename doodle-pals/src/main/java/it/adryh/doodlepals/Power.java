package it.adryh.doodlepals;

/**
 * The colours of the palette. Each colour is also a power: the more of the drawing
 * it covers, the stronger the power.
 */
public enum Power {
    FIRE("fire", 0xE8433C),
    BRAVE("brave", 0xF7902E),
    SPARK("spark", 0xFFD93B),
    NATURE("nature", 0x52C04A),
    FROST("frost", 0x69E3EE),
    TIDE("tide", 0x3F70E4),
    MAGIC("magic", 0x9C50DC),
    HEART("heart", 0xF78DC4),
    AIR("air", 0xF4F5F7),
    SHADOW("shadow", 0x2E2D3A),
    ROCK("rock", 0x8E5C36),
    IRON("iron", 0x9AA1AC);

    private final String id;
    private final int colour;

    Power(String id, int colour) {
        this.id = id;
        this.colour = colour;
    }

    public String id() {
        return id;
    }

    /** 0xRRGGBB */
    public int colour() {
        return colour;
    }

    public String nameKey() {
        return "doodlepals.power." + id;
    }

    public String infoKey() {
        return "doodlepals.power." + id + ".info";
    }

    /** The palette code stored in a drawing; 0 is an empty pixel. */
    public int code() {
        return ordinal() + 1;
    }

    public static Power of(int code) {
        return code >= 1 && code <= values().length ? values()[code - 1] : null;
    }
}
