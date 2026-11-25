package dev.visualclient.mixin;

import dev.visualclient.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // require = 0: if this method changes in a future version, zoom simply stops
    // working instead of crashing the game on launch.
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void visualclient$zoom(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * Zoom.factor());
    }
}
