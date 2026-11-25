package dev.visualclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Small drawing helpers. Menus draw their own visuals over invisible vanilla buttons. */
public final class UI {
    private UI() {}

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Filled rectangle with clipped corners (looks rounded). */
    public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private static int cornerInset(int row, int h, int r) {
        int k = Math.min(row, h - 1 - row);
        if (k >= r) return 0;
        double dy = r - (k + 0.5);
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Filled rounded rectangle; r = h / 2 gives a fully round (pill) shape. */
    public static void roundRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int in = cornerInset(row, h, rr);
            g.fill(x + in, y + row, x + w - in, y + row + 1, color);
        }
    }

    /** 1px outline following the same rounded shape. */
    public static void roundRing(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        int rr = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int o = cornerInset(row, h, rr);
            int ir = row - 1;
            if (ir < 0 || ir >= h - 2) {
                g.fill(x + o, y + row, x + w - o, y + row + 1, color);
            } else {
                int in = 1 + cornerInset(ir, h - 2, Math.max(0, rr - 1));
                if (in <= o) in = o + 1;
                g.fill(x + o, y + row, x + in, y + row + 1, color);
                g.fill(x + w - in, y + row, x + w - o, y + row + 1, color);
            }
        }
    }

    public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static int lerpRgb(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** A fully transparent button: it handles clicks, we draw the looks. */
    public static Button ghost(int x, int y, int w, int h, Runnable onPress) {
        Button b = Button.builder(Component.empty(), btn -> onPress.run())
                .bounds(x, y, w, h).build();
        b.setAlpha(0f);
        return b;
    }
}
