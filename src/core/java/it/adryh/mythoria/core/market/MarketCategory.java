package it.adryh.mythoria.core.market;

public enum MarketCategory {
    CREATURES, WEAPONS, ARMOR, FEAST;

    public String key() {
        return "mythoria.category." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
