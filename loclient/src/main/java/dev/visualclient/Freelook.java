package dev.visualclient;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Freelook: look around with the camera while your character keeps facing
 * the same way. Purely a client-side camera change.
 */
public final class Freelook {
    public static boolean active = false;
    public static float yaw, pitch;

    private static CameraType previous;
    private static float sYaw, sPitch, sYawO, sPitchO;
    private static boolean swapped;

    private Freelook() {}

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;
        yaw = p.getYRot();
        pitch = p.getXRot();
        active = true;
        previous = mc.options.getCameraType();
        if (previous.isFirstPerson() && Config.get().freelookThirdPerson) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        active = false;
        if (previous != null) {
            mc.options.setCameraType(previous);
            previous = null;
        }
    }

    public static void turn(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }

    /** Called right before the camera reads the player's rotation. */
    public static void applyBefore() {
        swapped = false;
        if (!active) return;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        sYaw = p.getYRot();
        sPitch = p.getXRot();
        sYawO = p.yRotO;
        sPitchO = p.xRotO;
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        swapped = true;
    }

    /** Puts the player's real rotation back after the camera is set up. */
    public static void restoreAfter() {
        if (!swapped) return;
        swapped = false;
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        p.setYRot(sYaw);
        p.setXRot(sPitch);
        p.yRotO = sYawO;
        p.xRotO = sPitchO;
    }
}
