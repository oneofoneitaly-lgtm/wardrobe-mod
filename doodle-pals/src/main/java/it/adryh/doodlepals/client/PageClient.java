package it.adryh.doodlepals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.adryh.doodlepals.Drawing;
import it.adryh.doodlepals.item.PageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** The page item shows the very drawing it holds. */
public final class PageClient implements IClientItemExtensions {
    private static BlockEntityWithoutLevelRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            Minecraft mc = Minecraft.getInstance();
            renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                @Override
                public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                                         int light, int overlay) {
                    Drawing drawing = PageItem.drawing(stack);
                    if (drawing.painted() == 0) {
                        return;
                    }
                    float scale = 0.95F / Math.max(8, Math.max(drawing.width(), drawing.height()));
                    pose.pushPose();
                    pose.translate(0.5F, 0.5F - drawing.height() * scale / 2.0F, 0.5F);
                    pose.scale(scale, scale, scale);
                    DoodleMesh.of(drawing).render(pose, buffers, light, overlay, 0.0F, 0.0F, false);
                    pose.popPose();
                }
            };
        }
        return renderer;
    }
}
