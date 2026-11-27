package it.adryh.invstyles.client;

import it.adryh.invstyles.paint.Accent;
import it.adryh.invstyles.paint.Category;
import it.adryh.invstyles.paint.Style;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/** The player's choices, saved in config/invstyles.properties. */
final class Config {
    private static final Logger LOG = LoggerFactory.getLogger("invstyles");
    private static final Map<Category, Style> STYLES = new EnumMap<>(Category.class);
    private static Accent accent = Accent.VIOLET;

    private Config() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("invstyles.properties");
    }

    static Style style(Category category) {
        return STYLES.getOrDefault(category, Style.GLASS);
    }

    static void style(Category category, Style style) {
        STYLES.put(category, style);
    }

    static Accent accent() {
        return accent;
    }

    static void accent(Accent value) {
        accent = value;
    }

    static void load() {
        Path file = file();
        if (!Files.exists(file)) {
            return;
        }
        Properties saved = new Properties();
        try (Reader in = Files.newBufferedReader(file)) {
            saved.load(in);
        } catch (IOException e) {
            LOG.warn("Could not read {}", file, e);
            return;
        }
        for (Category category : Category.values()) {
            for (Style style : Style.values()) {
                if (style.id().equals(saved.getProperty(category.id()))) {
                    STYLES.put(category, style);
                }
            }
        }
        for (Accent value : Accent.values()) {
            if (value.id().equals(saved.getProperty("colour"))) {
                accent = value;
            }
        }
    }

    static void save() {
        Properties saved = new Properties();
        for (Category category : Category.values()) {
            saved.setProperty(category.id(), style(category).id());
        }
        saved.setProperty("colour", accent.id());
        try (Writer out = Files.newBufferedWriter(file())) {
            saved.store(out, "Inventory Styles by @Adryh01. Styles: vanilla, glass, fantasy, neon, pastel");
        } catch (IOException e) {
            LOG.warn("Could not save {}", file(), e);
        }
    }
}
