package it.adryh.mythoria.core.market;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * One thing sold by the market.
 *
 * @param id       unique id, also used for the translation keys
 * @param order    position inside its category
 * @param items    items handed to the buyer
 * @param count    how many of each item
 * @param entity   creature shown in the 3d preview, or null
 * @param size     size of the 3d preview (pixels per block)
 * @param lift     moves the previewed model down (in blocks) so that it is centred
 */
public record MarketEntry(ResourceLocation id, MarketCategory category, int order, int price,
                          List<ResourceLocation> items, int count, ResourceLocation entity, float size, float lift) {

    public List<ItemStack> stacks() {
        List<ItemStack> list = new ArrayList<>();
        for (ResourceLocation key : items) {
            Item item = ForgeRegistries.ITEMS.getValue(key);
            if (item != null) {
                list.add(new ItemStack(item, count));
            }
        }
        return list;
    }

    public Component name() {
        Component base = Component.translatable("market." + id.getNamespace() + "." + id.getPath());
        return count > 1 ? base.copy().append(" x" + count) : base;
    }

    public Component description() {
        return Component.translatable("market." + id.getNamespace() + "." + id.getPath() + ".desc");
    }
}
