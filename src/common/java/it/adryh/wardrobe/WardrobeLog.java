package it.adryh.wardrobe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WardrobeLog {
    private static final Logger LOG = LoggerFactory.getLogger("Wardrobe");

    private WardrobeLog() {}

    public static void info(String message) {
        LOG.info(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
    }
}
