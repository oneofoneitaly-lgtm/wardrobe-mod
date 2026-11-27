package it.adryh.mythoria.core.market;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Every Mythoria mod adds its goods here from its constructor. */
public final class MarketCatalog {
    private static final List<MarketEntry> ENTRIES = new CopyOnWriteArrayList<>();

    private MarketCatalog() {}

    public static void add(MarketEntry entry) {
        ENTRIES.add(entry);
    }

    public static List<MarketEntry> all() {
        return ENTRIES.stream().sorted(Comparator.comparing(MarketEntry::category)
                .thenComparingInt(MarketEntry::order)
                .thenComparing(e -> e.id().toString())).toList();
    }

    public static List<MarketEntry> in(MarketCategory category) {
        return all().stream().filter(e -> e.category() == category).toList();
    }

    public static MarketEntry get(ResourceLocation id) {
        return ENTRIES.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }
}
