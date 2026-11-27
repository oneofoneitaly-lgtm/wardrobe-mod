package it.adryh.wardrobe;

/** Player model parts with their pivot in Minecraft model space (pixels). */
public enum Part {
    HEAD(0F, 0F),
    BODY(0F, 0F),
    RIGHT_ARM(-5F, 2F),
    LEFT_ARM(5F, 2F),
    RIGHT_LEG(-1.9F, 12F),
    LEFT_LEG(1.9F, 12F);

    public final float pivotX;
    public final float pivotY;

    Part(float pivotX, float pivotY) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }
}
