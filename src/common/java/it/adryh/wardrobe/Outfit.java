package it.adryh.wardrobe;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A wearable 3D model. */
public final class Outfit {
    public final String id;
    public final String name;
    public final Slot slot;
    public final List<Quad> quads;
    /** PNG files, indexed by {@link Quad#texture}. */
    public final List<byte[]> textures;

    /** quads grouped by texture, then by body part (what the renderer needs). */
    public final List<Map<Part, List<Quad>>> batches = new ArrayList<>();

    private Object[] handles;
    private Object thumbnail;

    public Outfit(String id, String name, Slot slot, List<Quad> quads, List<byte[]> textures) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.quads = quads;
        this.textures = textures;
        for (int i = 0; i < textures.size(); i++) {
            batches.add(new EnumMap<>(Part.class));
        }
        for (Quad q : quads) {
            batches.get(q.texture).computeIfAbsent(q.part, p -> new ArrayList<>()).add(q);
        }
    }

    /** GPU texture for batch {@code index}; must be called on the render thread. */
    public Object texture(int index) {
        if (handles == null) handles = new Object[textures.size()];
        if (handles[index] == null) handles[index] = it.adryh.wardrobe.compat.Gfx.createTexture(textures.get(index));
        return handles[index];
    }

    public Object thumbnail() {
        if (thumbnail == null) thumbnail = it.adryh.wardrobe.compat.Gfx.createTexture(Thumbnails.render(this));
        return thumbnail;
    }

    public void release() {
        if (handles != null) {
            for (Object h : handles) {
                if (h != null) it.adryh.wardrobe.compat.Gfx.releaseTexture(h);
            }
            handles = null;
        }
        if (thumbnail != null) {
            it.adryh.wardrobe.compat.Gfx.releaseTexture(thumbnail);
            thumbnail = null;
        }
    }
}
