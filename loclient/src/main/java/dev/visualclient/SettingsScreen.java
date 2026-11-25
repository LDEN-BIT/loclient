package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Lo Client settings. Draws its own panel, tabs, switches, sliders, color swatches and a
 * smooth scrollbar over invisible vanilla widgets that do the clicking.
 */
public class SettingsScreen extends Screen {
    private static final String[] TABS = {"HUD", "Style", "World", "Camera", "Waypoints", "Profiles"};
    private static final String[] CROSSHAIRS = {"Vanilla", "Dot", "Cross", "Circle"};

    private static final float[] SIZES = {8f, 9f, 10f, 11f, 12f, 13f, 14f};
    private static final String[] SIZE_NAMES = {"8", "9", "10", "11", "12", "13", "14"};

    private static int tab = 0;
    private static String status = "";

    private final Screen parent;
    private int px, py, pw, ph;
    private int cx, cy, colW, viewH;
    private int slot;
    private boolean rebuild;

    // Smooth scrolling
    private float scroll = 0f, scrollTarget = 0f;
    private boolean draggingBar = false;

    private long openTime = 0L, lastFrame = 0L;

    private enum Kind { TOGGLE, SLIDER, CYCLE, ACTION, COLOR }

    private static final class Item {
        Kind kind;
        String label;
        int x, y, w, h;
        Supplier<Boolean> on;
        Supplier<String> text;
        IntSupplier color;
        GhostSlider slider;
        AbstractWidget widget;
        float min, max;
        float hover;
    }

    private final List<Item> items = new ArrayList<>();

    public SettingsScreen(Screen parent) {
        super(Component.literal("Lo Client"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------- layout

    @Override
    protected void init() {
        items.clear();
        slot = 0;
        if (openTime == 0L) openTime = System.currentTimeMillis();
        Config c = Config.get();

        pw = Math.min(this.width - 16, 470);
        ph = Math.min(this.height - 16, 280);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        cx = px + 100;
        cy = py + 40;
        colW = (pw - 110 - 14) / 2;
        viewH = ph - 40 - 24;

        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            addRenderableWidget(UI.ghost(px + 8, py + 36 + i * 24, 76, 20, () -> {
                tab = index;
                scroll = 0f;
                scrollTarget = 0f;
                rebuild = true;
            }));
        }
        addRenderableWidget(UI.ghost(px + pw - 24, py + 6, 18, 18, this::onClose));

        switch (tab) {
            case 0 -> {
                action("Move HUD elements", "Edit layout", () -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().setScreen(new HudEditorScreen(this));
                    } else {
                        status = "Join a world first to edit the layout.";
                    }
                });
                action("Reset positions", "Reset", () -> {
                    c.resetPositions();
                    Config.save();
                });
                toggle("HUD", () -> c.hud, v -> c.hud = v);
                toggle("Keystrokes", () -> c.keystrokes, v -> c.keystrokes = v);
                toggle("FPS", () -> c.fps, v -> c.fps = v);
                toggle("Coordinates", () -> c.coords, v -> c.coords = v);
                toggle("Facing", () -> c.facing, v -> c.facing = v);
                toggle("Ping", () -> c.ping, v -> c.ping = v);
                toggle("Clock", () -> c.clock, v -> c.clock = v);
                toggle("Biome", () -> c.biome, v -> c.biome = v);
                toggle("Light level", () -> c.light, v -> c.light = v);
                toggle("CPS", () -> c.cps, v -> c.cps = v);
                toggle("Armor durability", () -> c.armor, v -> c.armor = v);
                toggle("Potion effects", () -> c.effects, v -> c.effects = v);
                toggle("Compass", () -> c.compass, v -> c.compass = v);
                toggle("Sprint / sneak", () -> c.sprintIndicator, v -> c.sprintIndicator = v);
            }
            case 1 -> {
                toggle("Rainbow accent", () -> c.rainbow, v -> c.rainbow = v);
                color("Theme color", () -> c.accentColor, v -> { c.accentColor = v; c.rainbow = false; }, null);
                color("HUD text color", () -> c.hudTextColor, v -> c.hudTextColor = v, null);
                color("Panel color", () -> c.panelColor, v -> c.panelColor = v, null);
                slider("Panel opacity", 0f, 1f, () -> c.bgOpacity, v -> c.bgOpacity = v);
                cycle("Crosshair", CROSSHAIRS, () -> c.crosshair, v -> c.crosshair = v);
                color("Crosshair color", () -> c.crosshairColor, v -> c.crosshairColor = v, null);
                color("Crosshair on target", () -> c.crosshairTargetColor, v -> c.crosshairTargetColor = v, null);
                toggle("Custom main menu", () -> c.customMenu, v -> c.customMenu = v);
                String[] fonts = LoFonts.names();
                cycle("Font", fonts, () -> Math.max(0, java.util.Arrays.asList(fonts).indexOf(c.font)), v -> {
                    c.font = fonts[v];
                    if (v != 0) c.packsOnTop = true;
                    applyPacks(c);
                    status = v == 0 ? "Vanilla font restored."
                            : "Font applied. Add your own .ttf/.otf files to: " + LoFonts.userDir();
                });
                cycle("Font size", SIZE_NAMES, () -> sizeIndex(c.fontSize), v -> {
                    c.fontSize = SIZES[v];
                    applyPacks(c);
                });
                toggle("Lo Client packs on top", () -> c.packsOnTop, v -> {
                    c.packsOnTop = v;
                    applyPacks(c);
                });
                action("Fonts folder", "Show path", () -> status = "Fonts folder: " + LoFonts.userDir());
            }
            case 2 -> {
                toggle("Fullbright", () -> c.fullbright, v -> c.fullbright = v);
                toggle("Sky tint", () -> c.skyTint, v -> c.skyTint = v);
                color("Sky color", () -> c.skyColor, v -> c.skyColor = v, null);
                slider("Sky strength", 0f, 1f, () -> c.skyStrength, v -> c.skyStrength = v);
                toggle("Cloud tint", () -> c.cloudTint, v -> c.cloudTint = v);
                color("Cloud color", () -> c.cloudColor, v -> c.cloudColor = v, null);
                slider("Star field boost", 0f, 1f, () -> c.starBoost, v -> c.starBoost = v);
                toggle("Block outline color", () -> c.outlineEnabled, v -> c.outlineEnabled = v);
                color("Outline color", () -> c.outlineColor, v -> c.outlineColor = v, null);
                color("Item glint color", () -> c.glintColor, v -> c.glintColor = v, () -> {
                    boolean ok = GlintPack.write(c.glintColor);
                    if (ok && c.packsOnTop) {
                        Packs.sync(true);
                        status = "Glint color applied.";
                    } else {
                        status = ok
                                ? "Glint pack saved. Enable '" + GlintPack.NAME + "' in Options > Resource Packs, then press F3+T."
                                : "Could not write the glint pack.";
                    }
                });
                action("Remove glint pack", "Delete", () -> {
                    boolean gone = GlintPack.delete();
                    if (gone && c.packsOnTop) Packs.sync(true);
                    status = gone ? "Glint pack deleted." : "No glint pack found.";
                });
                slider("Particles", 0f, 1f, () -> c.particles, v -> c.particles = v);
            }
            case 3 -> {
                toggle("Freelook", () -> c.freelook, v -> c.freelook = v);
                toggle("Freelook third person", () -> c.freelookThirdPerson, v -> c.freelookThirdPerson = v);
                slider("Zoom strength", 0.05f, 0.8f, () -> c.zoom, v -> c.zoom = v);
                slider("Third-person distance", 0.5f, 3f, () -> c.thirdPersonDistance, v -> c.thirdPersonDistance = v);
            }
            case 4 -> {
                toggle("Show waypoints", () -> c.showWaypoints, v -> c.showWaypoints = v);
                action("New waypoint here", "Add", () -> {
                    Waypoints.addHere(null);
                    rebuild = true;
                });
                List<Config.Waypoint> list = Waypoints.list();
                for (int i = 0; i < Math.min(list.size(), 40); i++) {
                    final Config.Waypoint w = list.get(i);
                    action(w.name + " (" + Math.round(w.x) + ", " + Math.round(w.z) + ")", "Remove", () -> {
                        Waypoints.remove(w.name);
                        rebuild = true;
                    });
                }
            }
            case 5 -> {
                for (int n = 1; n <= 3; n++) {
                    final int s = n;
                    action("Profile " + n, "Save", () -> {
                        Config.saveProfile(s);
                        rebuild = true;
                    });
                    action("Profile " + n + (Config.hasProfile(n) ? "" : " (empty)"), "Load", () -> {
                        if (Config.loadProfile(s)) rebuild = true;
                    });
                }
            }
            default -> {}
        }

        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll());
        scroll = Mth.clamp(scroll, 0f, maxScroll());
    }

    private static int sizeIndex(float size) {
        int best = 0;
        for (int i = 0; i < SIZES.length; i++) {
            if (Math.abs(SIZES[i] - size) < Math.abs(SIZES[best] - size)) best = i;
        }
        return best;
    }

    /** Rewrites Lo Client's resource pack and reloads resources so the change shows up. */
    private static void applyPacks(Config c) {
        Config.save();
        if (c.packsOnTop) LoPack.write(c);
        Packs.sync(true);
    }

    private int contentHeight() {
        return ((items.size() + 1) / 2) * 25;
    }

    private float maxScroll() {
        return Math.max(0, contentHeight() - viewH);
    }

    private Item next(Kind kind, String label) {
        Item it = new Item();
        it.kind = kind;
        it.label = label;
        it.x = cx + (slot % 2) * (colW + 8);
        it.y = cy + (slot / 2) * 25;
        it.w = colW;
        it.h = 21;
        slot++;
        items.add(it);
        return it;
    }

    private void toggle(String label, Supplier<Boolean> get, Consumer<Boolean> set) {
        Item it = next(Kind.TOGGLE, label);
        it.on = get;
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () -> set.accept(!get.get()));
        addRenderableWidget(it.widget);
    }

    private void cycle(String label, String[] names, Supplier<Integer> get, Consumer<Integer> set) {
        Item it = next(Kind.CYCLE, label);
        it.text = () -> names[get.get()];
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () -> set.accept((get.get() + 1) % names.length));
        addRenderableWidget(it.widget);
    }

    private void slider(String label, float min, float max, Supplier<Float> get, Consumer<Float> set) {
        Item it = next(Kind.SLIDER, label);
        it.min = min;
        it.max = max;
        it.slider = new GhostSlider(it.x, it.y, it.w, it.h, min, max, get.get(), set);
        it.widget = it.slider;
        addRenderableWidget(it.widget);
    }

    private void action(String label, String chip, Runnable run) {
        Item it = next(Kind.ACTION, label);
        it.text = () -> chip;
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, run);
        addRenderableWidget(it.widget);
    }

    private void color(String label, IntSupplier get, IntConsumer set, Runnable onDone) {
        Item it = next(Kind.COLOR, label);
        it.color = get;
        it.widget = UI.ghost(it.x, it.y, it.w, it.h, () ->
                Minecraft.getInstance().setScreen(new ColorPickerScreen(this, label, get, set, onDone)));
        addRenderableWidget(it.widget);
    }

    // ------------------------------------------------------------------- input

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 28f, 0f, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int trackX() {
        return px + pw - 10;
    }

    private int thumbH() {
        return maxScroll() > 0
                ? Math.max(18, (int) (viewH * (viewH / (float) contentHeight())))
                : viewH;
    }

    private void dragBarTo(double my) {
        float max = maxScroll();
        float frac = ((float) my - cy - thumbH() / 2f) / Math.max(1f, (viewH - thumbH()));
        scrollTarget = Mth.clamp(frac * max, 0f, max);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && maxScroll() > 0
                && UI.in((int) event.x(), (int) event.y(), trackX() - 3, cy, 12, viewH)) {
            draggingBar = true;
            dragBarTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingBar) {
            dragBarTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingBar = false;
        return super.mouseReleased(event);
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (rebuild) {
            rebuild = false;
            rebuildWidgets();
        }

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0L ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float ease = 1f - (float) Math.exp(-dt * 14f);

        float max = maxScroll();
        scrollTarget = Mth.clamp(scrollTarget, 0f, max);
        scroll += (scrollTarget - scroll) * (draggingBar ? 0.6f : ease);
        if (Math.abs(scrollTarget - scroll) < 0.05f) scroll = scrollTarget;

        // Move the invisible widgets with the content; hide the ones outside the view
        int sOff = Math.round(scroll);
        for (Item it : items) {
            int y = it.y - sOff;
            it.widget.setY(y);
            it.widget.visible = y >= cy && y + it.h <= cy + viewH;
        }

        super.render(g, mx, my, partialTick); // vanilla background + invisible widgets

        Config c = Config.get();
        int accent = Hud.accent(c);
        float open = Mth.clamp((now - openTime) / 260f, 0f, 1f);
        open = 1f - (1f - open) * (1f - open);

        g.fill(0, 0, this.width, this.height, ((int) (0x88 * open)) << 24);
        UI.rect(g, px, py, pw, ph, 0xF2101018);
        UI.border(g, px, py, pw, ph, 0xFF2A2A3C);
        g.fill(px + 92, py + 12, px + 93, py + ph - 12, 0xFF2A2A3C);

        g.drawString(this.font, "Lo Client", px + 10, py + 12, accent, true);
        g.drawString(this.font, "x", px + pw - 19, py + 11,
                UI.in(mx, my, px + pw - 24, py + 6, 18, 18) ? 0xFFFFFFFF : 0xFF888899, false);

        // Tabs
        for (int i = 0; i < TABS.length; i++) {
            int x = px + 8, y = py + 36 + i * 24;
            boolean sel = i == tab;
            boolean hover = UI.in(mx, my, x, y, 76, 20);
            if (sel) {
                UI.rect(g, x, y, 76, 20, 0x33FFFFFF);
                g.fill(x, y + 3, x + 2, y + 17, accent);
            } else if (hover) {
                UI.rect(g, x, y, 76, 20, 0x18FFFFFF);
            }
            g.drawString(this.font, TABS[i], x + 9, y + 6, sel ? 0xFFFFFFFF : 0xFFAAAABB, false);
        }

        // Items (clipped to the scroll view)
        g.enableScissor(cx - 2, cy, px + pw - 14, cy + viewH);
        for (Item it : items) {
            int iy = it.y - sOff;
            if (iy + it.h < cy || iy > cy + viewH) continue;
            boolean hoverNow = UI.in(mx, my, it.x, iy, it.w, it.h) && my >= cy && my < cy + viewH;
            it.hover += ((hoverNow ? 1f : 0f) - it.hover) * ease;
            int bgA = (int) (0x18 + 0x18 * it.hover);
            UI.rect(g, it.x, iy, it.w, it.h, (bgA << 24) | 0xFFFFFF);

            switch (it.kind) {
                case TOGGLE -> {
                    g.drawString(this.font, it.label, it.x + 8, iy + 7, 0xFFFFFFFF, false);
                    boolean on = it.on.get();
                    int sx = it.x + it.w - 30, sy = iy + 5;
                    UI.rect(g, sx, sy, 22, 11, on ? accent : 0xFF3A3A4A);
                    int kx = on ? sx + 12 : sx + 1;
                    g.fill(kx, sy + 1, kx + 9, sy + 10, 0xFFFFFFFF);
                }
                case SLIDER -> {
                    float frac = it.slider.frac();
                    float val = it.min + frac * (it.max - it.min);
                    g.drawString(this.font, it.label, it.x + 8, iy + 3, 0xFFFFFFFF, false);
                    String vs = String.format("%.2f", val);
                    g.drawString(this.font, vs, it.x + it.w - 8 - this.font.width(vs), iy + 3,
                            0xFFAAAABB, false);
                    int tx = it.x + 8, tw = it.w - 16, ty = iy + it.h - 6;
                    g.fill(tx, ty, tx + tw, ty + 2, 0xFF3A3A4A);
                    g.fill(tx, ty, tx + (int) (tw * frac), ty + 2, accent);
                    int kx = tx + (int) (tw * frac);
                    g.fill(kx - 2, ty - 2, kx + 2, ty + 4, 0xFFFFFFFF);
                }
                case COLOR -> {
                    String label = trim(it.label, it.w - 80);
                    g.drawString(this.font, label, it.x + 8, iy + 7, 0xFFFFFFFF, false);
                    int rgb = it.color.getAsInt() & 0xFFFFFF;
                    int swX = it.x + it.w - 26;
                    UI.rect(g, swX, iy + 4, 20, 13, 0xFF000000 | rgb);
                    UI.border(g, swX, iy + 4, 20, 13, hoverNow ? 0xFFFFFFFF : 0xFF4A4A5C);
                    String hx = String.format("#%06X", rgb);
                    g.drawString(this.font, hx, swX - 4 - this.font.width(hx), iy + 7, 0xFFAAAABB, false);
                }
                case CYCLE, ACTION -> {
                    String label = trim(it.label, it.w - 70);
                    g.drawString(this.font, label, it.x + 8, iy + 7, 0xFFFFFFFF, false);
                    String chip = it.text.get();
                    int cw = this.font.width(chip) + 12;
                    int chx = it.x + it.w - cw - 4;
                    int chA = (int) (0x30 + 0x25 * it.hover);
                    UI.rect(g, chx, iy + 3, cw, it.h - 6, (chA << 24) | 0xFFFFFF);
                    g.drawString(this.font, chip, chx + 6, iy + 7, accent, false);
                }
            }
        }
        g.disableScissor();

        // Scrollbar
        if (max > 0) {
            int trackX = trackX(), thumbH = thumbH();
            UI.rect(g, trackX, cy, 4, viewH, 0x30FFFFFF);
            int thumbY = cy + (int) ((viewH - thumbH) * (scroll / max));
            boolean thumbHover = draggingBar || UI.in(mx, my, trackX - 3, cy, 12, viewH);
            UI.rect(g, trackX, thumbY, 4, thumbH, thumbHover ? 0xFFFFFFFF : accent);
        }

        // Hints
        String hint = switch (tab) {
            case 0 -> "Edit layout lets you drag HUD elements (or press U in game).";
            case 1 -> "Fonts apply through the Lo Client resource pack (the game reloads briefly).";
            case 2 -> "Fullbright: key G. Glint color is applied through a Lo Client resource pack.";
            case 3 -> "Hold Left Alt to freelook, hold C to zoom.";
            case 4 -> "Press B in game to drop a waypoint. Commands: /wp add, /wp remove, /wp list";
            case 5 -> "Profiles store all your look and camera settings. Waypoints are kept separate.";
            default -> "";
        };
        if (!status.isEmpty()) hint = status;
        if (!hint.isEmpty()) {
            g.drawString(this.font, trim(hint, pw - 116), cx, py + ph - 16, 0xFF8888A0, false);
        }
    }

    private String trim(String s, int maxWidth) {
        while (this.font.width(s) > maxWidth && s.length() > 3) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public void onClose() {
        status = "";
        Minecraft.getInstance().setScreen(parent);
    }
}
