package dev.visualclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** The Lo Client main menu. Opens the real Minecraft screens, with smooth animations. */
public class VisualTitleScreen extends Screen {
    private record Btn(String label, int x, int y, int w, int h) {}

    private final List<Btn> btns = new ArrayList<>();
    private float[] hover = new float[0];
    private long openTime = 0L;
    private long lastFrame = 0L;

    public VisualTitleScreen() {
        super(Component.literal("Lo Client"));
    }

    @Override
    protected void init() {
        btns.clear();
        int w = 220, h = 24, gap = 8;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 10;

        add("Singleplayer", x, y, w, h, () -> this.minecraft.setScreen(new SelectWorldScreen(this)));
        add("Multiplayer", x, y + (h + gap), w, h, () -> this.minecraft.setScreen(new JoinMultiplayerScreen(this)));
        add("Options", x, y + (h + gap) * 2, w, h,
                () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)));
        add("Lo Client Settings", x, y + (h + gap) * 3, w, h,
                () -> this.minecraft.setScreen(new SettingsScreen(this)));
        add("Quit Game", x, y + (h + gap) * 4, w, h, () -> this.minecraft.stop());

        if (hover.length != btns.size()) hover = new float[btns.size()];
        if (openTime == 0L) openTime = System.currentTimeMillis();
    }

    private void add(String label, int x, int y, int w, int h, Runnable action) {
        btns.add(new Btn(label, x, y, w, h));
        addRenderableWidget(UI.ghost(x, y, w, h, action));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static float hash(float s) {
        float v = (float) Math.sin(s) * 43758.5453f;
        return v - (float) Math.floor(v);
    }

    private static float easeOut(float p) {
        float q = 1f - Mth.clamp(p, 0f, 1f);
        return 1f - q * q * q;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // vanilla background + invisible buttons

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        long t = now;
        Config c = Config.get();
        int accent = Hud.accent(c);

        // Mouse position in -1..1, used for gentle parallax.
        float mxN = (mouseX - this.width / 2f) / (this.width / 2f);
        float myN = (mouseY - this.height / 2f) / (this.height / 2f);

        // ---- Background ----
        g.fillGradient(0, 0, this.width, this.height, 0xFF0A0A18, 0xFF1B0B2E);

        // Slow aurora bands
        for (int b = 0; b < 3; b++) {
            float phase = t / (2800f + b * 900f) + b * 2f;
            int y = (int) (this.height * (0.25f + 0.18f * b) + Math.sin(phase) * 18f - myN * 6f * (b + 1));
            int h = 46 + b * 14;
            int rgb = c.rainbow ? (Mth.hsvToRgb(((t / 22000f) + b * 0.18f) % 1f, 0.55f, 1f) & 0xFFFFFF)
                    : UI.lerpRgb(c.accentColor, 0xFFFFFF, b * 0.12f);
            g.fillGradient(0, y, this.width, y + h / 2, rgb, (0x1C << 24) | rgb);
            g.fillGradient(0, y + h / 2, this.width, y + h, (0x1C << 24) | rgb, rgb);
        }

        // Drifting stars in 3 depth layers
        for (int i = 0; i < 90; i++) {
            int layer = i % 3;
            float fx = hash(i * 12.9898f);
            float fy = hash(i * 78.233f + 3.1f);
            float speed = 0.004f + layer * 0.004f;
            float par = 4f + layer * 7f;
            int x = (int) ((((fx * this.width + t * speed - mxN * par) % this.width) + this.width) % this.width);
            int y = (int) (fy * this.height - myN * par);
            int size = 1 + layer;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 0.002 + i);
            int alpha = (int) (40 + 140 * tw);
            int rgb = c.rainbow ? Mth.hsvToRgb(((i * 0.037f) + t / 20000f) % 1f, 0.4f, 1f)
                    : UI.lerpRgb(c.accentColor, 0xFFFFFF, 0.55f);
            g.fill(x, y, x + size, y + size, (alpha << 24) | (rgb & 0xFFFFFF));
        }

        // ---- Logo (floats gently). Uses your chosen Lo Client font; pixel logo for the default font ----
        String logo = "Lo Client";
        boolean customFont = LoFonts.isCustom(c);
        int bob = (int) Math.round(Math.sin(t / 900.0) * 3.0);
        float introLogo = easeOut((t - openTime) / 700f);
        int logoH;
        int logoY;
        if (customFont) {
            float scale = Mth.clamp((this.width - 40) / (float) Math.max(1, this.font.width(logo)), 2f, 5f);
            int textW = (int) (this.font.width(logo) * scale);
            logoH = (int) (this.font.lineHeight * scale);
            logoY = Math.max(10, this.height / 2 - 10 - logoH - 40) + bob;
            float lx = this.width / 2f - textW / 2f + (1f - introLogo) * -30f + mxN * -4f;
            if (introLogo > 0.05f) {
                g.pose().pushMatrix();
                g.pose().translate(lx, logoY);
                g.pose().scale(scale, scale);
                int cx = 0;
                for (int i = 0; i < logo.length(); i++) {
                    String ch = String.valueOf(logo.charAt(i));
                    int rgb = c.rainbow
                            ? Mth.hsvToRgb(((i * 0.06f) + t / 5000f) % 1f, 0.55f, 1f)
                            : UI.lerpRgb(c.accentColor, 0xFFFFFF, 0.12f * (i % 3));
                    g.drawString(this.font, ch, cx, 0, 0xFF000000 | (rgb & 0xFFFFFF), true);
                    cx += this.font.width(ch);
                }
                g.pose().popMatrix();
            }
        } else {
            int cell = Math.max(2, Math.min(7, (this.width - 40) / (logo.length() * 6)));
            int logoW = PixelFont.width(logo, cell);
            logoH = 7 * cell;
            logoY = Math.max(10, this.height / 2 - 10 - logoH - 40) + bob;
            int logoX = this.width / 2 - logoW / 2 + (int) ((1f - introLogo) * -30f) + (int) (mxN * -4f);
            if (introLogo > 0.05f) PixelFont.draw(g, logo, logoX, logoY, cell, t);
        }

        String sub = "client-side visuals  -  Minecraft 1.21.11";
        if (introLogo > 0.6f) {
            g.drawString(this.font, sub, this.width / 2 - this.font.width(sub) / 2,
                    logoY + logoH + 10 - bob, 0xFF9999BB, false);
        }

        // ---- Buttons: staggered slide-in + eased hover ----
        float ease = 1f - (float) Math.exp(-dt * 14f);
        for (int i = 0; i < btns.size(); i++) {
            Btn b = btns.get(i);
            boolean over = UI.in(mouseX, mouseY, b.x, b.y, b.w, b.h);
            hover[i] += ((over ? 1f : 0f) - hover[i]) * ease;
            float hv = hover[i];

            float p = easeOut((t - openTime - 180 - i * 90L) / 450f);
            if (p <= 0.04f) continue;

            // Round pill buttons: they grow a little and glow while hovered.
            int growW = (int) (hv * 10f), growH = (int) (hv * 4f);
            int bw = b.w + growW, bh = b.h + growH;
            int bx = b.x - growW / 2 + (int) ((1f - p) * -60f);
            int by = b.y - growH / 2;
            int rad = bh / 2;
            int rgbAccent = accent & 0xFFFFFF;

            if (hv > 0.02f) {
                for (int k = 5; k >= 1; k--) {
                    int ga = (int) (hv * p * 0x1C * (6 - k) / 5f);
                    if (ga <= 0) continue;
                    UI.roundRect(g, bx - k * 2, by - k * 2, bw + k * 4, bh + k * 4, rad + k * 2,
                            (ga << 24) | rgbAccent);
                }
            }
            int bgAlpha = (int) ((0x22 + 0x36 * hv) * p);
            UI.roundRect(g, bx, by, bw, bh, rad, (bgAlpha << 24) | 0xFFFFFF);
            if (hv > 0.05f) {
                UI.roundRing(g, bx, by, bw, bh, rad, ((int) (0xD0 * hv * p) << 24) | rgbAccent);
            }

            if (p > 0.35f) {
                int a = (int) (255 * Mth.clamp((p - 0.35f) / 0.65f, 0f, 1f));
                int rgb = UI.lerpRgb(0xCCCCDD, 0xFFFFFF, hv);
                int tx = bx + bw / 2 - this.font.width(b.label) / 2;
                g.drawString(this.font, b.label, tx, by + (bh - this.font.lineHeight) / 2 + 1,
                        (Math.max(a, 8) << 24) | rgb, false);
            }
        }

        g.drawString(this.font, "Lo Client " + VisualClient.VERSION, 6, this.height - 12, 0xFF666680, false);

        // ---- Fade in from black ----
        float f = Mth.clamp((t - openTime) / 500f, 0f, 1f);
        int fadeA = (int) ((1f - f) * 255);
        if (fadeA >= 4) g.fill(0, 0, this.width, this.height, fadeA << 24);
    }
}
