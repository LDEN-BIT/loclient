package dev.visualclient.mixin;

import dev.visualclient.Config;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    // Drops a share of particles according to the "Particles" slider.
    @Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 0)
    private void visualclient$thin(CallbackInfo ci) {
        float keep = Config.get().particles;
        if (keep < 1f && ThreadLocalRandom.current().nextFloat() > keep) {
            ci.cancel();
        }
    }
}
