package dev.visualclient;

/** Smooth zoom: multiplies the field of view while the zoom key is held. */
public final class Zoom {
    public static boolean active = false;
    private static float current = 1.0f;

    private Zoom() {}

    public static float factor() {
        float target = active ? Config.get().zoom : 1.0f;
        current += (target - current) * 0.25f;
        if (Math.abs(target - current) < 0.001f) current = target;
        return current;
    }

    /** Current factor without advancing the animation. */
    public static float current() {
        return current;
    }
}
