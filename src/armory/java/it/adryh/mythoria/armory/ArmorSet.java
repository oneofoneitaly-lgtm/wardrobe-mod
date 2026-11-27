package it.adryh.mythoria.armory;

/** The armor sets of Mythoria. */
public enum ArmorSet {
    DRAGON, ELVEN, FROST;

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
