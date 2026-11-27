package it.adryh.wardrobe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads Blockbench ".bbmodel" files (cubes and meshes, embedded textures).
 * Bones/groups named head, body, right_arm, left_arm, right_leg, left_leg follow that
 * part of the player; anything else is placed automatically.
 */
public final class BbModelLoader {
    private static final int MAX_QUADS = 20000;
    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};

    private final Slot slot;
    private final Map<String, JsonObject> elements = new HashMap<>();
    private final Map<String, JsonObject> groups = new HashMap<>();
    private final List<float[]> textureSizes = new ArrayList<>();
    private final List<byte[]> textures = new ArrayList<>();
    private final List<Quad> quads = new ArrayList<>();
    private final float[] offset = new float[3];
    private float scale = 1F;
    private Part forcedPart;

    private BbModelLoader(Slot slot) {
        this.slot = slot;
    }

    private static final class Bone {
        final float[] origin;
        final float[] rotation;
        final Bone parent;

        Bone(float[] origin, float[] rotation, Bone parent) {
            this.origin = origin;
            this.rotation = rotation;
            this.parent = parent;
        }
    }

    /**
     * @param json     the .bbmodel text
     * @param settings optional adjustments: name, slot, part, offset [x,y,z], scale
     * @param folder   used to find textures that are not embedded, may be null
     * @param fallback slot to use when the file does not say, may be null
     */
    public static Outfit load(String id, String name, Slot fallback, String json, JsonObject settings, Path folder) throws IOException {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject own = root.has("wardrobe") ? root.getAsJsonObject("wardrobe") : new JsonObject();
        if (own.has("name")) name = own.get("name").getAsString();
        Slot slot = fallback != null ? fallback : Slot.byName(string(own, "slot"));
        if (settings != null) {
            if (settings.has("name")) name = settings.get("name").getAsString();
            Slot s = Slot.byName(string(settings, "slot"));
            if (s != null) slot = s;
        }
        BbModelLoader loader = new BbModelLoader(slot);
        if (settings != null) loader.readSettings(settings);
        loader.read(root, folder);
        if (loader.quads.isEmpty()) throw new IOException("the model has no visible textured faces");
        if (slot == null) slot = guessSlot(loader.quads);
        return new Outfit(id, name, slot, loader.quads, loader.textures);
    }

    private void readSettings(JsonObject s) {
        if (s.has("scale")) scale = s.get("scale").getAsFloat();
        if (s.has("offset")) {
            float[] o = vec(s, "offset");
            System.arraycopy(o, 0, offset, 0, 3);
        }
        forcedPart = partFromName(string(s, "part"));
    }

    private void read(JsonObject root, Path folder) throws IOException {
        float resW = 16F, resH = 16F;
        if (root.has("resolution")) {
            JsonObject r = root.getAsJsonObject("resolution");
            resW = r.get("width").getAsFloat();
            resH = r.get("height").getAsFloat();
        }
        if (!root.has("textures") || !root.has("elements")) throw new IOException("missing textures or elements");
        for (JsonElement e : root.getAsJsonArray("textures")) {
            JsonObject t = e.getAsJsonObject();
            textures.add(textureBytes(t, folder));
            float w = t.has("uv_width") ? t.get("uv_width").getAsFloat() : resW;
            float h = t.has("uv_height") ? t.get("uv_height").getAsFloat() : resH;
            textureSizes.add(new float[]{w <= 0 ? resW : w, h <= 0 ? resH : h});
        }
        if (textures.isEmpty()) throw new IOException("the model has no texture");
        for (JsonElement e : root.getAsJsonArray("elements")) {
            JsonObject o = e.getAsJsonObject();
            elements.put(o.get("uuid").getAsString(), o);
        }
        if (root.has("groups")) { // Blockbench 5
            for (JsonElement e : root.getAsJsonArray("groups")) {
                JsonObject o = e.getAsJsonObject();
                groups.put(o.get("uuid").getAsString(), o);
            }
        }
        boolean itemModel = false;
        if (root.has("meta")) {
            String format = string(root.getAsJsonObject("meta"), "model_format");
            itemModel = "java_block".equals(format);
        }
        if (itemModel) centreItemModel();

        Map<String, Boolean> used = new HashMap<>();
        if (root.has("outliner")) walk(root.getAsJsonArray("outliner"), null, null, used, 0);
        for (Map.Entry<String, JsonObject> e : elements.entrySet()) {
            if (!used.containsKey(e.getKey())) element(e.getValue(), null, null);
        }
    }

    /** Java item models are centred on (8, 8, 8): move them onto the player. */
    private void centreItemModel() {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (JsonObject el : elements.values()) {
            if (!el.has("from") || !el.has("to")) continue;
            minY = Math.min(minY, vec(el, "from")[1]);
            maxY = Math.max(maxY, vec(el, "to")[1]);
        }
        if (minY > maxY) return;
        Slot s = slot == null ? Slot.HAT : slot;
        float targetY = switch (s) {
            case HAT -> 32F - minY * scale;
            case FACE -> 28F - (minY + maxY) / 2F * scale;
            case NECK -> 23F - (minY + maxY) / 2F * scale;
            case TOP, BACK -> 18F - (minY + maxY) / 2F * scale;
            case PANTS -> 6F - (minY + maxY) / 2F * scale;
            case SHOES -> -minY * scale;
        };
        offset[0] -= 8F * scale;
        offset[1] += targetY;
        offset[2] -= 8F * scale;
        if (s == Slot.BACK) offset[2] += 5F;
    }

    private void walk(JsonArray nodes, Bone bone, Part part, Map<String, Boolean> used, int depth) throws IOException {
        if (depth > 32) throw new IOException("groups nested too deeply");
        for (JsonElement node : nodes) {
            if (node.isJsonPrimitive()) {
                JsonObject el = elements.get(node.getAsString());
                if (el != null) {
                    used.put(node.getAsString(), true);
                    element(el, bone, part);
                }
                continue;
            }
            JsonObject ref = node.getAsJsonObject();
            JsonObject group = ref;
            if (ref.has("uuid") && groups.containsKey(ref.get("uuid").getAsString())) {
                group = groups.get(ref.get("uuid").getAsString());
            }
            Part named = partFromName(string(group, "name"));
            Bone child = new Bone(vec(group, "origin"), vec(group, "rotation"), bone);
            if (ref.has("children")) {
                walk(ref.getAsJsonArray("children"), child, named != null ? named : part, used, depth + 1);
            }
        }
    }

    private void element(JsonObject el, Bone bone, Part part) throws IOException {
        if (bool(el, "visibility", true) == false || bool(el, "export", true) == false) return;
        String type = el.has("type") ? el.get("type").getAsString() : "cube";
        if (part == null) part = partFromName(string(el, "name"));
        if ("mesh".equals(type)) mesh(el, bone, part);
        else if ("cube".equals(type)) cube(el, bone, part);
        if (quads.size() > MAX_QUADS) throw new IOException("the model is too detailed (more than " + MAX_QUADS + " faces)");
    }

    private void cube(JsonObject el, Bone bone, Part part) {
        if (!el.has("faces")) return;
        float inflate = el.has("inflate") ? el.get("inflate").getAsFloat() : 0F;
        float[] from = vec(el, "from"), to = vec(el, "to");
        float[] origin = vec(el, "origin"), rotation = vec(el, "rotation");
        float x1 = from[0] - inflate, y1 = from[1] - inflate, z1 = from[2] - inflate;
        float x2 = to[0] + inflate, y2 = to[1] + inflate, z2 = to[2] + inflate;
        JsonObject faces = el.getAsJsonObject("faces");

        // the part is chosen once for the whole cube
        float[] centre = transform(new float[]{(x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2}, rotation, origin, bone);
        Part target = resolvePart(part, centre);

        for (String name : FACES) {
            if (!faces.has(name)) continue;
            JsonObject face = faces.getAsJsonObject(name);
            int tex = textureIndex(face);
            if (tex < 0 || !face.has("uv")) continue;
            float[][] c = switch (name) {
                case "north" -> new float[][]{{x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}, {x2, y1, z1}};
                case "south" -> new float[][]{{x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}, {x1, y1, z2}};
                case "east" -> new float[][]{{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}, {x2, y1, z2}};
                case "west" -> new float[][]{{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}, {x1, y1, z1}};
                case "up" -> new float[][]{{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
                default -> new float[][]{{x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}, {x1, y1, z1}};
            };
            float[] n = switch (name) {
                case "north" -> new float[]{0, 0, -1};
                case "south" -> new float[]{0, 0, 1};
                case "east" -> new float[]{1, 0, 0};
                case "west" -> new float[]{-1, 0, 0};
                case "up" -> new float[]{0, 1, 0};
                default -> new float[]{0, -1, 0};
            };
            JsonArray r = face.getAsJsonArray("uv");
            float u1 = r.get(0).getAsFloat(), v1 = r.get(1).getAsFloat(), u2 = r.get(2).getAsFloat(), v2 = r.get(3).getAsFloat();
            float[][] corners = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
            int turns = face.has("rotation") ? Math.floorMod(face.get("rotation").getAsInt() / 90, 4) : 0;
            float[] pos = new float[12];
            float[] uv = new float[8];
            for (int i = 0; i < 4; i++) {
                float[] p = place(transform(c[i], rotation, origin, bone));
                System.arraycopy(p, 0, pos, i * 3, 3);
                float[] t = corners[Math.floorMod(i - turns, 4)];
                uv[i * 2] = t[0] / textureSizes.get(tex)[0];
                uv[i * 2 + 1] = t[1] / textureSizes.get(tex)[1];
            }
            float[] rn = direction(n, rotation, bone);
            quads.add(new Quad(target, tex, pos, uv, rn[0], rn[1], rn[2]));
        }
    }

    private void mesh(JsonObject el, Bone bone, Part part) {
        if (!el.has("vertices") || !el.has("faces")) return;
        float[] origin = vec(el, "origin"), rotation = vec(el, "rotation");
        JsonObject vertices = el.getAsJsonObject("vertices");
        Map<String, float[]> points = new HashMap<>();
        float[] sum = new float[3];
        for (Map.Entry<String, JsonElement> v : vertices.entrySet()) {
            JsonArray a = v.getValue().getAsJsonArray();
            float[] local = {a.get(0).getAsFloat() + origin[0], a.get(1).getAsFloat() + origin[1], a.get(2).getAsFloat() + origin[2]};
            float[] p = transform(local, rotation, origin, bone);
            points.put(v.getKey(), p);
            for (int i = 0; i < 3; i++) sum[i] += p[i];
        }
        if (points.isEmpty()) return;
        for (int i = 0; i < 3; i++) sum[i] /= points.size();
        Part target = resolvePart(part, sum);

        for (Map.Entry<String, JsonElement> f : el.getAsJsonObject("faces").entrySet()) {
            JsonObject face = f.getValue().getAsJsonObject();
            int tex = textureIndex(face);
            if (tex < 0 || !face.has("vertices") || !face.has("uv")) continue;
            JsonArray keys = face.getAsJsonArray("vertices");
            if (keys.size() < 3 || keys.size() > 4) continue;
            String[] k = new String[4];
            for (int i = 0; i < 4; i++) k[i] = keys.get(Math.min(i, keys.size() - 1)).getAsString();
            if (keys.size() == 4) k = unCross(k, points);
            if (k == null) continue;
            float[] pos = new float[12];
            float[] uv = new float[8];
            boolean ok = true;
            for (int i = 0; i < 4; i++) {
                float[] p = points.get(k[i]);
                JsonArray t = face.getAsJsonObject("uv").getAsJsonArray(k[i]);
                if (p == null || t == null) {
                    ok = false;
                    break;
                }
                System.arraycopy(place(p), 0, pos, i * 3, 3);
                uv[i * 2] = t.get(0).getAsFloat() / textureSizes.get(tex)[0];
                uv[i * 2 + 1] = t.get(1).getAsFloat() / textureSizes.get(tex)[1];
            }
            if (!ok) continue;
            float ax = pos[3] - pos[0], ay = pos[4] - pos[1], az = pos[5] - pos[2];
            float bx = pos[6] - pos[0], by = pos[7] - pos[1], bz = pos[8] - pos[2];
            quads.add(new Quad(target, tex, pos, uv, ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx));
        }
    }

    /** Blockbench does not store quad vertices in order: the shortest loop is the right one. */
    private static String[] unCross(String[] k, Map<String, float[]> points) {
        int[][] orders = {{0, 1, 2, 3}, {0, 2, 1, 3}, {0, 1, 3, 2}};
        String[] best = null;
        double bestLength = Double.MAX_VALUE;
        for (int[] o : orders) {
            double length = 0;
            for (int i = 0; i < 4; i++) {
                float[] a = points.get(k[o[i]]), b = points.get(k[o[(i + 1) % 4]]);
                if (a == null || b == null) return null;
                length += Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
            }
            if (length < bestLength - 1.0E-4) {
                bestLength = length;
                best = new String[]{k[o[0]], k[o[1]], k[o[2]], k[o[3]]};
            }
        }
        return best;
    }

    private int textureIndex(JsonObject face) {
        if (!face.has("texture") || face.get("texture").isJsonNull()) return -1;
        JsonElement t = face.get("texture");
        if (t.getAsJsonPrimitive().isBoolean()) return -1;
        int i;
        try {
            i = t.getAsInt();
        } catch (NumberFormatException e) {
            return -1;
        }
        return i >= 0 && i < textures.size() ? i : -1;
    }

    private Part resolvePart(Part named, float[] centre) {
        if (forcedPart != null) return forcedPart;
        if (named != null) return named;
        float[] c = place(centre);
        boolean right = c[0] >= 0;
        if (slot != null) {
            switch (slot) {
                case HAT, FACE:
                    return Part.HEAD;
                case NECK, BACK:
                    return Part.BODY;
                case TOP:
                    if (Math.abs(c[0]) > 4.2F) return right ? Part.RIGHT_ARM : Part.LEFT_ARM;
                    return Part.BODY;
                case SHOES:
                    return right ? Part.RIGHT_LEG : Part.LEFT_LEG;
                case PANTS:
                    if (c[1] >= 12F) return Part.BODY;
                    return right ? Part.RIGHT_LEG : Part.LEFT_LEG;
            }
        }
        if (c[1] >= 24F) return Part.HEAD;
        if (c[1] >= 12F) {
            if (Math.abs(c[0]) > 4.2F) return right ? Part.RIGHT_ARM : Part.LEFT_ARM;
            return Part.BODY;
        }
        return right ? Part.RIGHT_LEG : Part.LEFT_LEG;
    }

    static Part partFromName(String raw) {
        if (raw == null) return null;
        String n = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        boolean right = n.contains("right") || n.contains("destr");
        boolean left = n.contains("left") || n.contains("sinistr");
        if (right || left) {
            if (n.contains("arm") && !n.contains("armor") || n.contains("bracc")) return right ? Part.RIGHT_ARM : Part.LEFT_ARM;
            if (n.contains("leg") || n.contains("boot") || n.contains("shoe") || n.contains("gamba")) return right ? Part.RIGHT_LEG : Part.LEFT_LEG;
            return null;
        }
        if (n.startsWith("head") || n.equals("testa") || n.equals("helmet")) return Part.HEAD;
        if (n.startsWith("body") || n.equals("torso") || n.equals("corpo") || n.equals("chestplate")) return Part.BODY;
        return null;
    }

    private static Slot guessSlot(List<Quad> quads) {
        boolean head = false, upper = false, legs = false;
        float legTop = 0;
        for (Quad q : quads) {
            if (q.part == Part.HEAD) head = true;
            else if (q.part == Part.RIGHT_LEG || q.part == Part.LEFT_LEG) {
                legs = true;
                for (int i = 1; i < 12; i += 3) legTop = Math.max(legTop, q.pos[i]);
            } else upper = true;
        }
        if (upper) return Slot.TOP;
        if (legs) return legTop <= 8F ? Slot.SHOES : Slot.PANTS;
        return head ? Slot.HAT : Slot.BACK;
    }

    private float[] place(float[] p) {
        return new float[]{p[0] * scale + offset[0], p[1] * scale + offset[1], p[2] * scale + offset[2]};
    }

    private static float[] transform(float[] p, float[] rotation, float[] origin, Bone bone) {
        float[] r = rotateAround(p, rotation, origin);
        for (Bone b = bone; b != null; b = b.parent) r = rotateAround(r, b.rotation, b.origin);
        return r;
    }

    private static float[] direction(float[] n, float[] rotation, Bone bone) {
        float[] zero = new float[3];
        float[] r = rotateAround(n, rotation, zero);
        for (Bone b = bone; b != null; b = b.parent) r = rotateAround(r, b.rotation, zero);
        return r;
    }

    /** Blockbench rotation order: v' = Rz * Ry * Rx * (v - origin) + origin. */
    private static float[] rotateAround(float[] p, float[] rot, float[] o) {
        if (rot[0] == 0 && rot[1] == 0 && rot[2] == 0) return p;
        double x = p[0] - o[0], y = p[1] - o[1], z = p[2] - o[2];
        double rx = Math.toRadians(rot[0]), ry = Math.toRadians(rot[1]), rz = Math.toRadians(rot[2]);
        double t = y * Math.cos(rx) - z * Math.sin(rx);
        z = y * Math.sin(rx) + z * Math.cos(rx);
        y = t;
        t = x * Math.cos(ry) + z * Math.sin(ry);
        z = -x * Math.sin(ry) + z * Math.cos(ry);
        x = t;
        t = x * Math.cos(rz) - y * Math.sin(rz);
        y = x * Math.sin(rz) + y * Math.cos(rz);
        x = t;
        return new float[]{(float) x + o[0], (float) y + o[1], (float) z + o[2]};
    }

    private static byte[] textureBytes(JsonObject t, Path folder) throws IOException {
        String source = string(t, "source");
        if (source != null && source.startsWith("data:")) {
            return Base64.getMimeDecoder().decode(source.substring(source.indexOf(',') + 1));
        }
        String name = string(t, "name");
        if (folder != null && name != null) {
            // only a plain file name next to the model is accepted
            Path file = folder.resolve(Path.of(name).getFileName().toString());
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                file = file.resolveSibling(file.getFileName() + ".png");
            }
            if (Files.isRegularFile(file)) return Files.readAllBytes(file);
        }
        throw new IOException("texture not embedded: " + name);
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? def : e.getAsBoolean();
    }

    private static float[] vec(JsonObject o, String key) {
        float[] v = new float[3];
        JsonElement e = o.get(key);
        if (e != null && e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (int i = 0; i < 3 && i < a.size(); i++) v[i] = a.get(i).getAsFloat();
        }
        return v;
    }
}
