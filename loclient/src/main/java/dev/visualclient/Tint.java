package dev.visualclient;

public final class Tint {
    private Tint() {}

    /** Blends an ARGB/RGB color toward a tint color (RGB) by the given strength (0..1). */
    public static int blend(int argb, int tintRgb, float strength) {
        int r1 = (argb >> 16) & 255, g1 = (argb >> 8) & 255, b1 = argb & 255;
        int r2 = (tintRgb >> 16) & 255, g2 = (tintRgb >> 8) & 255, b2 = tintRgb & 255;
        int r = (int) (r1 + (r2 - r1) * strength);
        int g = (int) (g1 + (g2 - g1) * strength);
        int b = (int) (b1 + (b2 - b1) * strength);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
