package it.adryh.aura.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Registry of the pack modules. Mods may be constructed in parallel, so it is synchronised. */
public final class Aura {
    private static final List<Module> MODULES = new ArrayList<>();

    private Aura() {
    }

    /** Loads the saved values and shows the module in Aura Studio. */
    public static synchronized void register(Module module) {
        module.load();
        MODULES.add(module);
        MODULES.sort(Comparator.comparingInt(m -> m.order));
    }

    public static synchronized List<Module> modules() {
        return new ArrayList<>(MODULES);
    }

    public static void saveAll() {
        modules().forEach(Module::save);
    }
}
