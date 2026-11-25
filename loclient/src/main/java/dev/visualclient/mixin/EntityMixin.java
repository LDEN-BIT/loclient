package dev.visualclient.mixin;

import dev.visualclient.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    // While freelook is held, mouse movement turns the camera instead of the player.
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
    private void visualclient$turn(double yaw, double pitch, CallbackInfo ci) {
        if (Freelook.active && (Object) this == Minecraft.getInstance().player) {
            Freelook.turn(yaw, pitch);
            ci.cancel();
        }
    }
}
