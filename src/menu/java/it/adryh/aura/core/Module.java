package it.adryh.aura.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One mod of the pack as shown in Aura Studio, with its saved settings. */
public final class Module {
    private static final Logger LOG = LoggerFactory.getLogger("Aura");

    public final String id;
    public final String title;
    public final String subtitle;
    public final int order;
    public final ResourceLocation icon;
    private final List<Setting> settings = new ArrayList<>();
    /** Free data a mod wants to keep, such as widget positions. */
    private JsonObject extra = new JsonObject();
    private Runnable extraReset = () -> {
    };

    public Module(String id, String title, String subtitle, int order) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.order = order;
        this.icon = ResourceLocation.fromNamespaceAndPath("auramenu", "textures/gui/icon_" + id + ".png");
    }

    public <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public Setting.Header header(String label) {
        return add(new Setting.Header(label));
    }

    public Setting.Toggle toggle(String key, String label, boolean initial) {
        return add(new Setting.Toggle(key, label, initial));
    }

    public Setting.Slider slider(String key, String label, double initial, double min, double max, double step, String suffix) {
        return add(new Setting.Slider(key, label, initial, min, max, step, suffix));
    }

    public Setting.Choice choice(String key, String label, int initial, String... options) {
        return add(new Setting.Choice(key, label, initial, options));
    }

    public Setting.Color color(String key, String label, int initial) {
        return add(new Setting.Color(key, label, initial));
    }

    public Setting.Action action(String label, String button, Runnable run) {
        return add(new Setting.Action(label, button, run));
    }

    public List<Setting> settings() {
        return settings;
    }

    public JsonObject extra() {
        return extra;
    }

    public void onReset(Runnable run) {
        this.extraReset = run;
    }

    public void reset() {
        settings.forEach(Setting::reset);
        extra = new JsonObject();
        extraReset.run();
        save();
    }

    private Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("aura").resolve(id + ".json");
    }

    public void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Setting s : settings) {
                JsonElement value = root.get(s.key);
                if (!s.key.isEmpty() && value != null) {
                    try {
                        s.load(value);
                    } catch (RuntimeException e) {
                        LOG.warn("Ignoring bad value for {}.{}", id, s.key);
                    }
                }
            }
            if (root.has("extra")) {
                extra = root.getAsJsonObject("extra");
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read {}", file, e);
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Setting s : settings) {
            JsonElement value = s.save();
            if (value != null) {
                root.add(s.key, value);
            }
        }
        root.add("extra", extra);
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (IOException e) {
            LOG.warn("Could not save settings of {}", id, e);
        }
    }
}
