package it.adryh.wardrobe;

/** Does nothing in the released mod; the CI test build replaces it (src/autotest). */
final class DevHook {
    private DevHook() {}

    static void tick() {}
}
