package it.adryh.invstyles.paint;

/** The groups of inventories that can each have their own style. */
public enum Category {
    INVENTORY("inventory", "textures/gui/container/inventory.png", 176, 166),
    CREATIVE("creative", "textures/gui/container/creative_inventory/tab_items.png", 195, 136),
    CHESTS("chests", "textures/gui/container/generic_54.png", 176, 222),
    CRAFTING("crafting", "textures/gui/container/crafting_table.png", 176, 166),
    FURNACES("furnaces", "textures/gui/container/furnace.png", 176, 166),
    MAGIC("magic", "textures/gui/container/brewing_stand.png", 176, 166),
    SMITHING("smithing", "textures/gui/container/anvil.png", 176, 166),
    TRADING("trading", "textures/gui/container/villager.png", 276, 166),
    HOTBAR("hotbar", "textures/gui/sprites/hud/hotbar.png", 182, 22);

    private final String id;
    private final String sample;
    private final int width;
    private final int height;

    Category(String id, String sample, int width, int height) {
        this.id = id;
        this.sample = sample;
        this.width = width;
        this.height = height;
    }

    public String id() {
        return id;
    }

    /** A vanilla texture that shows the look of this group, and the size of its visible part. */
    public String sample() {
        return sample;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Which group a vanilla gui texture belongs to; null for textures this mod leaves alone. */
    public static Category ofTexture(String path) {
        if (path.startsWith("textures/gui/sprites/hud/")) {
            String file = path.substring("textures/gui/sprites/hud/".length());
            return file.startsWith("hotbar.") || file.startsWith("hotbar_selection.") || file.startsWith("hotbar_offhand_")
                    ? HOTBAR : null;
        }
        if (path.startsWith("textures/gui/recipe_book") || path.startsWith("textures/gui/sprites/recipe_book/")) {
            return INVENTORY;
        }
        if (!path.startsWith("textures/gui/container/") && !path.startsWith("textures/gui/sprites/container/")) {
            return null;
        }
        if (path.contains("gamemode_switcher") || path.contains("stats_icons")) {
            return null;
        }
        if (path.contains("creative_inventory")) {
            return CREATIVE;
        }
        if (has(path, "generic_54", "shulker", "dispenser", "hopper")) {
            return CHESTS;
        }
        if (has(path, "crafting", "crafter", "stonecutter", "loom", "cartography")) {
            return CRAFTING;
        }
        if (has(path, "furnace", "smoker")) {
            return FURNACES;
        }
        if (has(path, "enchant", "brewing", "beacon")) {
            return MAGIC;
        }
        if (has(path, "anvil", "smithing", "grindstone")) {
            return SMITHING;
        }
        if (has(path, "villager", "horse")) {
            return TRADING;
        }
        return INVENTORY;
    }

    private static boolean has(String path, String... words) {
        for (String word : words) {
            if (path.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
