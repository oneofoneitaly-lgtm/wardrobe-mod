package it.adryh.invstyles.paint;

/** The highlight colour of the Glass, Neon and Pastel styles. */
public enum Accent {
    VIOLET("violet", 0x9D6BFF),
    CYAN("cyan", 0x35DCFF),
    EMERALD("emerald", 0x3EE58F),
    GOLD("gold", 0xFFC23D),
    ROSE("rose", 0xFF5FA8),
    RED("red", 0xFF4B4B);

    private final String id;
    private final int colour;

    Accent(String id, int colour) {
        this.id = id;
        this.colour = colour;
    }

    public String id() {
        return id;
    }

    public int colour() {
        return colour;
    }
}
