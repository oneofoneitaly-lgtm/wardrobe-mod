package it.adryh.invstyles.mixin;

import it.adryh.invstyles.client.Styles;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The two things a texture cannot change: see-through backgrounds and the colour of titles. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    /** Vanilla draws inventory backgrounds without blending, which would make glass opaque. */
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"))
    private void invstyles$blend(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                 float u1, float u2, float v1, float v2, CallbackInfo info) {
        Styles.beforeBlit(texture);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int invstyles$textColour(int colour) {
        return Styles.textColour(colour);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int invstyles$sequenceColour(int colour) {
        return Styles.textColour(colour);
    }
}
