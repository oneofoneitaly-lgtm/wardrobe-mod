package it.adryh.wardrobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** The catalogue of clothes and what the player is wearing. */
public final class Wardrobe {
    private static final Map<String, Outfit> OUTFITS = new LinkedHashMap<>();
    private static final Map<Slot, String> WORN = new EnumMap<>(Slot.class);
    private static final List<String> ERRORS = new ArrayList<>();
    private static boolean loaded;

    private Wardrobe() {}

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve("wardrobe");
    }

    /** Folder where the player drops their own .bbmodel files. */
    public static Path modelsFolder() {
        return folder().resolve("modelli");
    }

    private static void ensureLoaded() {
        if (!loaded) reload();
    }

    public static void reload() {
        loaded = true;
        OUTFITS.values().forEach(Outfit::release);
        OUTFITS.clear();
        ERRORS.clear();
        loadBuiltIn();
        loadUserModels();
        loadWorn();
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = Wardrobe.class.getResourceAsStream("/assets/wardrobe/outfits/" + path)) {
            if (in == null) throw new IOException("missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void loadBuiltIn() {
        try {
            JsonArray index = JsonParser.parseString(resource("index.json")).getAsJsonArray();
            for (JsonElement e : index) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                try {
                    OUTFITS.put(id, BbModelLoader.load(id, o.get("name").getAsString(),
                            Slot.byName(o.get("slot").getAsString()), resource(id + ".bbmodel"), null, null));
                } catch (IOException | RuntimeException ex) {
                    WardrobeLog.warn("Cannot load " + id + ": " + ex);
                }
            }
        } catch (IOException | RuntimeException ex) {
            WardrobeLog.warn("Cannot load the built-in clothes: " + ex);
        }
    }

    private static void loadUserModels() {
        Path root = modelsFolder();
        try {
            for (Slot s : Slot.values()) Files.createDirectories(root.resolve(s.folder));
            Path help = root.resolve("LEGGIMI.txt");
            if (!Files.exists(help)) Files.writeString(help, HELP);
        } catch (IOException ex) {
            WardrobeLog.warn("Cannot create " + root + ": " + ex);
            return;
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root, 4)) {
            files = walk.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel"))
                    .sorted().toList();
        } catch (IOException ex) {
            WardrobeLog.warn("Cannot read " + root + ": " + ex);
            return;
        }
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String base = fileName.substring(0, fileName.length() - ".bbmodel".length());
            String id = "user/" + root.relativize(file).toString().replace('\\', '/');
            Slot slot = null;
            for (Path p = file.getParent(); p != null && !p.equals(root) && slot == null; p = p.getParent()) {
                slot = Slot.byName(p.getFileName().toString());
            }
            try {
                Path extra = file.resolveSibling(base + ".json");
                JsonObject settings = Files.isRegularFile(extra)
                        ? JsonParser.parseString(Files.readString(extra)).getAsJsonObject() : null;
                OUTFITS.put(id, BbModelLoader.load(id, base.replace('_', ' '), slot,
                        Files.readString(file), settings, file.getParent()));
            } catch (IOException | RuntimeException ex) {
                ERRORS.add(fileName);
                WardrobeLog.warn("Cannot load " + file + ": " + ex);
            }
        }
    }

    private static void loadWorn() {
        WORN.clear();
        Path file = folder().resolve("indossati.json");
        if (!Files.isRegularFile(file)) return;
        try {
            JsonObject o = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                Slot slot = Slot.byName(e.getKey());
                if (slot != null) WORN.put(slot, e.getValue().getAsString());
            }
        } catch (IOException | RuntimeException ex) {
            WardrobeLog.warn("Cannot read " + file + ": " + ex);
        }
    }

    private static void saveWorn() {
        JsonObject o = new JsonObject();
        WORN.forEach((slot, id) -> o.addProperty(slot.id, id));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(folder());
            Files.writeString(folder().resolve("indossati.json"), gson.toJson(o));
        } catch (IOException ex) {
            WardrobeLog.warn("Cannot save the outfit: " + ex);
        }
    }

    public static List<Outfit> byTab(Slot.Tab tab) {
        ensureLoaded();
        return OUTFITS.values().stream().filter(o -> o.slot.tab == tab).toList();
    }

    public static List<Outfit> worn() {
        ensureLoaded();
        List<Outfit> list = new ArrayList<>(WORN.size());
        for (String id : WORN.values()) {
            Outfit o = OUTFITS.get(id);
            if (o != null) list.add(o);
        }
        return list;
    }

    public static boolean isWorn(Outfit outfit) {
        return outfit.id.equals(WORN.get(outfit.slot));
    }

    public static void toggle(Outfit outfit) {
        if (isWorn(outfit)) WORN.remove(outfit.slot);
        else WORN.put(outfit.slot, outfit.id);
        saveWorn();
    }

    public static void undressAll() {
        WORN.clear();
        saveWorn();
    }

    public static List<String> errors() {
        return ERRORS;
    }

    private static final String HELP = """
            WARDROBE by @Adryh01 - come aggiungere i tuoi vestiti
            =====================================================

            1. Copia i file Blockbench (.bbmodel) nella cartella giusta:
               cappelli, maglie, pantaloni, scarpe, viso, collo, schiena
            2. Apri l'armadio (/wardrobe) e premi "Ricarica".

            La texture deve essere salvata dentro il .bbmodel (Blockbench lo fa da solo)
            oppure il PNG deve stare nella stessa cartella.

            Perche' il vestito segua i movimenti, chiama i gruppi cosi':
               head, body, right_arm, left_arm, right_leg, left_leg
            Misure del giocatore in Blockbench: piedi a y=0, testa da y=24 a y=32,
            il davanti guarda a nord.

            Se un modello e' fuori posto, crea accanto un file con lo stesso nome e
            finale .json, per esempio corona.json:
               { "name": "La mia corona", "offset": [0, 1.5, 0], "scale": 1.0, "part": "head" }
            """;
}
