package dev.visualclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Tiny 5x7 pixel font used for the big logo (so we never need to scale text). */
public final class PixelFont {
    private static final String[] UP_C = {".####", "#....", "#....", "#....", "#....", "#....", ".####"};
    private static final String[] UP_L = {"#....", "#....", "#....", "#....", "#....", "#....", "#####"};
    private static final String[] UP_O = {".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."};
    private static final String[] UP_I = {"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"};
    private static final String[] UP_E = {"#####", "#....", "#....", "####.", "#....", "#....", "#####"};
    private static final String[] UP_N = {"#...#", "##..#", "##..#", "#.#.#", "#..##", "#..##", "#...#"};
    private static final String[] UP_T = {"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."};
    private static final String[] UP_V = {"#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."};
    private static final String[] UP_S = {".####", "#....", "#....", ".###.", "....#", "....#", "####."};
    private static final String[] UP_U = {"#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."};
    private static final String[] UP_A = {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"};

    private static final String[] LO_O = {".....", ".....", ".###.", "#...#", "#...#", "#...#", ".###."};
    private static final String[] LO_L = {".##..", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."};
    private static final String[] LO_I = {"..#..", ".....", ".##..", "..#..", "..#..", "..#..", ".###."};
    private static final String[] LO_E = {".....", ".....", ".###.", "#...#", "#####", "#....", ".###."};
    private static final String[] LO_N = {".....", ".....", "####.", "#...#", "#...#", "#...#", "#...#"};
    private static final String[] LO_T = {".#...", ".#...", "####.", ".#...", ".#...", ".#..#", "..##."};
    private static final String[] LO_C = {".....", ".....", ".####", "#....", "#....", "#....", ".####"};

    private PixelFont() {}

    private static String[] glyph(char ch) {
        return switch (ch) {
            case 'C' -> UP_C; case 'L' -> UP_L; case 'O' -> UP_O; case 'I' -> UP_I;
            case 'E' -> UP_E; case 'N' -> UP_N; case 'T' -> UP_T; case 'V' -> UP_V;
            case 'S' -> UP_S; case 'U' -> UP_U; case 'A' -> UP_A;
            case 'o' -> LO_O; case 'l' -> LO_L; case 'i' -> LO_I; case 'e' -> LO_E;
            case 'n' -> LO_N; case 't' -> LO_T; case 'c' -> LO_C;
            default -> null;
        };
    }

    public static int width(String text, int cell) {
        return text.length() * 6 * cell - cell;
    }

    /** Draws the text. Rainbow mode drifts per letter, otherwise it uses the accent color. */
    public static void draw(GuiGraphics g, String text, int x, int y, int cell, long time) {
        Config c = Config.get();
        int shadowOff = Math.max(1, cell / 3);
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < text.length(); i++) {
                String[] gl = glyph(text.charAt(i));
                if (gl == null) continue;
                int color;
                if (pass == 0) {
                    color = 0x66000000;
                } else if (c.rainbow) {
                    color = 0xFF000000 | Mth.hsvToRgb(((i * 0.06f) + time / 5000f) % 1f, 0.55f, 1f);
                } else {
                    float shade = 0.12f * (i % 3);
                    color = 0xFF000000 | UI.lerpRgb(c.accentColor, 0xFFFFFF, shade);
                }
                int ox = x + i * 6 * cell + (pass == 0 ? shadowOff : 0);
                int oy = y + (pass == 0 ? shadowOff : 0);
                for (int row = 0; row < 7; row++) {
                    for (int col = 0; col < 5; col++) {
                        if (gl[row].charAt(col) == '#') {
                            g.fill(ox + col * cell, oy + row * cell,
                                    ox + (col + 1) * cell, oy + (row + 1) * cell, color);
                        }
                    }
                }
            }
        }
    }
}
