package it.adryh.wardrobe.compat;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}

    public static KeyMapping create() {
        return new KeyMapping("key.wardrobe.open", GLFW.GLFW_KEY_J, "key.categories.wardrobe");
    }
}
