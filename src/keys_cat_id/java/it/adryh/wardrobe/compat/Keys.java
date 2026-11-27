package it.adryh.wardrobe.compat;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}

    public static KeyMapping create() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("wardrobe", "main"));
        return new KeyMapping("key.wardrobe.open", GLFW.GLFW_KEY_J, category);
    }
}
