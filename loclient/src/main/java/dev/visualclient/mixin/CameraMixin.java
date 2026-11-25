package dev.visualclient.mixin;

import dev.visualclient.Config;
import dev.visualclient.Freelook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    // Both names listed so it works whichever one this Minecraft version uses.
    @Inject(method = {"setup", "update"}, at = @At("HEAD"), require = 0)
    private void visualclient$before(CallbackInfo ci) {
        Freelook.applyBefore();
    }

    @Inject(method = {"setup", "update"}, at = @At("TAIL"), require = 0)
    private void visualclient$after(CallbackInfo ci) {
        Freelook.restoreAfter();
    }

    @Inject(method = "getMaxZoom", at = @At("RETURN"), cancellable = true, require = 0)
    private void visualclient$distance(CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() instanceof Float dist) {
            cir.setReturnValue(dist * Config.get().thirdPersonDistance);
        }
    }
}
