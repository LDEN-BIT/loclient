package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fullbright: your own player is shown to the renderer as having Night Vision (client-side only,
 * the server never knows). Nothing is sent and no effect is really applied.
 */
public final class Fullbright {
    private static MobEffectInstance fake;

    private Fullbright() {}

    public static boolean fakes(LivingEntity entity, Holder<MobEffect> effect) {
        return Config.get().fullbright
                && effect == MobEffects.NIGHT_VISION
                && entity == Minecraft.getInstance().player;
    }

    public static MobEffectInstance instance() {
        if (fake == null) {
            fake = new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false, false);
        }
        return fake;
    }
}
