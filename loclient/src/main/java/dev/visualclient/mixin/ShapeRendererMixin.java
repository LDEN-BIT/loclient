package dev.visualclient.mixin;

import dev.visualclient.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Block selection outline color. The outline is drawn through ShapeRenderer.renderShape,
 * whose first int argument is the ARGB color. If this Minecraft version differs, it simply does nothing.
 */
@Mixin(targets = "net.minecraft.client.renderer.ShapeRenderer")
public class ShapeRendererMixin {
    @ModifyVariable(method = "renderShape", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static int visualclient$outline(int color) {
        Config c = Config.get();
        return c.outlineEnabled ? (0xFF000000 | c.outlineColor) : color;
    }
}
