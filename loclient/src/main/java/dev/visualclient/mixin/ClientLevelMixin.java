package dev.visualclient.mixin;

import dev.visualclient.Config;
import dev.visualclient.Tint;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void visualclient$sky(CallbackInfoReturnable<Object> cir) {
        Config c = Config.get();
        if (c.skyTint && cir.getReturnValue() instanceof Integer color) {
            cir.setReturnValue(Tint.blend(color, c.skyColor, c.skyStrength));
        }
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void visualclient$cloud(CallbackInfoReturnable<Object> cir) {
        Config c = Config.get();
        if (c.cloudTint && cir.getReturnValue() instanceof Integer color) {
            cir.setReturnValue(Tint.blend(color, c.cloudColor, 0.6f));
        }
    }

    // Star field boost: makes stars brighter (and visible earlier / longer).
    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true, require = 0)
    private void visualclient$stars(CallbackInfoReturnable<Object> cir) {
        float boost = Config.get().starBoost;
        if (boost > 0f && cir.getReturnValue() instanceof Float f) {
            cir.setReturnValue(Math.max(f, boost));
        }
    }
}
