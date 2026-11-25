package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Color wheel + brightness slider + hex code box + preset swatches. Changes apply live. */
public class ColorPickerScreen extends Screen {
    private static final int R = 46;
    private static final int[] PRESETS = {
            0x4D8DFF, 0xFF4D5E, 0x4DFF88, 0x8F6BFF, 0xFFAA33, 0xFF6BD6, 0x33E0FF, 0xFFFFFF
    };
    private static final String[] PRESET_NAMES = {
            "Blue", "Red", "Green", "Purple", "Orange", "Pink", "Cyan", "White"
    };

    private final Screen parent;
    private final String label;
    private final IntConsumer set;
    private final Runnable onDone;

    private float h, s, v;
    private boolean draggingWheel, updating;
    private EditBox hex;
    private GhostSlider bright;
    private int px, py, wcx, wcy;
    private final int pw = 300, ph = 214;

    public ColorPickerScreen(Screen parent, String label, IntSupplier get, IntConsumer set, Runnable onDone) {
        super(Component.literal(label));
        this.parent = parent;
        this.label = label;
        this.set = set;
        this.onDone = onDone;
        float[] hsv = rgbToHsv(get.getAsInt() & 0xFFFFFF);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
    }

    @Override
    protected void init() {
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        wcx = px + 16 + R;
        wcy = py + 44 + R;

        hex = new EditBox(this.font, px + 170, py + 40, 96, 18, Component.literal("Hex"));
        hex.setMaxLength(7);
        hex.setValue(hexString(currentRgb()));
        hex.setResponder(this::onHex);
        addRenderableWidget(hex);

        bright = new GhostSlider(px + 170, py + 72, 110, 18, 0f, 1f, v, val -> {
            if (!updating) {
                v = val;
                apply(true);
            }
        });
        addRenderableWidget(bright);

        for (int i = 0; i < PRESETS.length; i++) {
            final int rgb = PRESETS[i];
            addRenderableWidget(UI.ghost(px + 170 + (i % 4) * 28, py + 112 + (i / 4) * 28, 24, 24,
                    () -> setFromRgb(rgb, true)));
        }
        addRenderableWidget(UI.ghost(px + pw - 84, py + ph - 30, 70, 20, this::onClose));
    }

    // ------------------------------------------------------------------ color logic

    private int currentRgb() {
        return Mth.hsvToRgb(h, s, v) & 0xFFFFFF;
    }

    private static String hexString(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private void apply(boolean updateHex) {
        int rgb = currentRgb();
        set.accept(rgb);
        if (updateHex && hex != null) {
            updating = true;
            hex.setValue(hexString(rgb));
            updating = false;
        }
    }

    private void setFromRgb(int rgb, boolean updateHex) {
        float[] hsv = rgbToHsv(rgb & 0xFFFFFF);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
        updating = true;
        if (bright != null) bright.setFrac(v);
        updating = false;
        set.accept(rgb & 0xFFFFFF);
        if (updateHex && hex != null) {
            updating = true;
            hex.setValue(hexString(rgb));
            updating = false;
        }
    }

    private void onHex(String text) {
        if (updating) return;
        String t = text.startsWith("#") ? text.substring(1) : text;
        if (t.length() != 6) return;
        try {
            setFromRgb(Integer.parseInt(t, 16), false);
        } catch (NumberFormatException ignored) {
            // still typing
        }
    }

    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float hue;
        if (d == 0f) hue = 0f;
        else if (max == r) hue = ((g - b) / d) % 6f;
        else if (max == g) hue = (b - r) / d + 2f;
        else hue = (r - g) / d + 4f;
        hue /= 6f;
        if (hue < 0f) hue += 1f;
        float sat = max == 0f ? 0f : d / max;
        return new float[]{hue, sat, max};
    }

    private void pickFromWheel(double mx, double my) {
        double dx = mx - wcx, dy = my - wcy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double ang = Math.atan2(dy, dx) / (2 * Math.PI);
        if (ang < 0) ang += 1;
        h = (float) ang;
        s = (float) Math.min(1.0, r / R);
        apply(true);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double dx = event.x() - wcx, dy = event.y() - wcy;
            if (dx * dx + dy * dy <= (R + 2) * (R + 2)) {
                draggingWheel = true;
                pickFromWheel(event.x(), event.y());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingWheel) {
            pickFromWheel(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingWheel = false;
        return super.mouseReleased(event);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick); // background + widgets (hex box is real)
        Config c = Config.get();
        int accent = Hud.accent(c);

        g.fill(0, 0, this.width, this.height, 0xAA000000);
        UI.rect(g, px, py, pw, ph, 0xF2101018);
        UI.border(g, px, py, pw, ph, 0xFF2A2A3C);
        g.drawString(this.font, label, px + 12, py + 12, accent, true);

        // Wheel: hue = angle, saturation = distance from the middle
        for (int y = -R; y < R; y += 2) {
            for (int x = -R; x < R; x += 2) {
                double dx = x + 1, dy = y + 1;
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r > R) continue;
                float hue = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
                if (hue < 0) hue += 1f;
                int rgb = Mth.hsvToRgb(hue, (float) (r / R), v);
                g.fill(wcx + x, wcy + y, wcx + x + 2, wcy + y + 2, 0xFF000000 | rgb);
            }
        }
        // Marker
        double ang = h * 2 * Math.PI;
        int mxp = wcx + (int) Math.round(Math.cos(ang) * s * R);
        int myp = wcy + (int) Math.round(Math.sin(ang) * s * R);
        UI.border(g, mxp - 4, myp - 4, 9, 9, 0xFF000000);
        UI.border(g, mxp - 3, myp - 3, 7, 7, 0xFFFFFFFF);

        // Hex label
        g.drawString(this.font, "Color code", px + 170, py + 28, 0xFF9999BB, false);

        // Brightness track (dark -> bright)
        g.drawString(this.font, "Brightness", px + 170, py + 62, 0xFF9999BB, false);
        int tx = px + 174, ty = py + 80, tw = 102;
        int steps = 20;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            int rgb = Mth.hsvToRgb(h, s, t);
            g.fill(tx + i * tw / steps, ty, tx + (i + 1) * tw / steps, ty + 4, 0xFF000000 | rgb);
        }
        int kx = tx + (int) (tw * v);
        g.fill(kx - 2, ty - 3, kx + 2, ty + 7, 0xFFFFFFFF);

        // Presets
        g.drawString(this.font, "Presets", px + 170, py + 100, 0xFF9999BB, false);
        for (int i = 0; i < PRESETS.length; i++) {
            int x = px + 170 + (i % 4) * 28, y = py + 112 + (i / 4) * 28;
            UI.rect(g, x, y, 24, 24, 0xFF000000 | PRESETS[i]);
            boolean hover = UI.in(mx, my, x, y, 24, 24);
            UI.border(g, x, y, 24, 24, hover ? 0xFFFFFFFF : 0xFF2A2A3C);
            if (hover) {
                String n = PRESET_NAMES[i];
                g.drawString(this.font, n, px + 170, py + 170, 0xFFFFFFFF, false);
            }
        }

        // Preview + Done
        int now = currentRgb();
        UI.rect(g, px + 16, py + ph - 38, 60, 22, 0xFF000000 | now);
        UI.border(g, px + 16, py + ph - 38, 60, 22, 0xFF2A2A3C);
        g.drawString(this.font, hexString(now), px + 84, py + ph - 31, 0xFFFFFFFF, false);

        int dx = px + pw - 84, dy = py + ph - 30;
        boolean hv = UI.in(mx, my, dx, dy, 70, 20);
        UI.rect(g, dx, dy, 70, 20, hv ? 0x66FFFFFF : 0x33FFFFFF);
        g.drawString(this.font, "Done", dx + 35 - this.font.width("Done") / 2, dy + 6, hv ? 0xFFFFFFFF : accent, false);
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
        if (onDone != null) onDone.run();
    }
}
