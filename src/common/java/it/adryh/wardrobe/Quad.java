package it.adryh.wardrobe;

/** One textured face. Positions are Blockbench pixels (Y up, feet at 0). */
public final class Quad {
    public final Part part;
    public final int texture;
    /** 4 vertices, xyz. */
    public final float[] pos;
    /** 4 vertices, uv in the 0..1 range. */
    public final float[] uv;
    /** Outward normal in Blockbench space. */
    public final float nx, ny, nz;

    public Quad(Part part, int texture, float[] pos, float[] uv, float nx, float ny, float nz) {
        this.part = part;
        this.texture = texture;
        this.pos = pos;
        this.uv = uv;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) {
            this.nx = 0F;
            this.ny = 1F;
            this.nz = 0F;
        } else {
            this.nx = nx / len;
            this.ny = ny / len;
            this.nz = nz / len;
        }
    }
}
