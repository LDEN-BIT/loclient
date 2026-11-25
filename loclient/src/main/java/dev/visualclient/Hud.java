package dev.visualclient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** All HUD drawing lives here. Read-only: nothing here changes gameplay. */
public final class Hud {
    public static final int PANEL = 0, KEYS = 1, COMPASS = 2;
    /** Last drawn rectangle of each movable element: {x, y, w, h, visible}. */
    public static final int[][] BOXES = new int[3][5];

    private static final int PAD = 4;
    private static final int KEY = 18;
    private static final int KEYS_W = KEY * 3 + 4;
    private static final int KEYS_H = (KEY + 2) * 2 + 10;
    private static final int COMPASS_W = 180;
    private static final int COMPASS_H = 14;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean lastLeft, lastRight;

    /** Best-effort drag while the chat is open (the layout editor is the reliable way). */
    private static final class Elem {
        boolean dragging, wasDown;
        int ox, oy;
    }

    private static final Elem[] ELEMS = {new Elem(), new Elem(), new Elem()};

    private record Row(ItemStack icon, String text, int color) {
        static Row text(String s) { return new Row(ItemStack.EMPTY, s, 0); }
        boolean hasIcon() { return !icon.isEmpty(); }
    }

    private Hud() {}

    public static int accent(Config c) {
        if (c.rainbow) {
            float hue = (System.currentTimeMillis() % 6000L) / 6000f;
            return 0xFF000000 | Mth.hsvToRgb(hue, 0.55f, 1.0f);
        }
        return 0xFF000000 | c.accentColor;
    }

    // ------------------------------------------------------------------ entry points

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Config c = Config.get();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !c.hud) return;
        if (mc.screen instanceof HudEditorScreen) return; // the editor draws its own copy
        draw(g, mc, c, delta);
    }

    /** Used by the layout editor: draws everything except waypoints. */
    public static void renderEditor(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        draw(g, mc, Config.get(), null);
    }

    /** Moves an element (used by the layout editor). */
    public static void setPos(int which, int x, int y, int sw, int sh) {
        int[] b = BOXES[which];
        x = Mth.clamp(x, 0, Math.max(0, sw - b[2]));
        y = Mth.clamp(y, 0, Math.max(0, sh - b[3]));
        Config c = Config.get();
        switch (which) {
            case PANEL -> { c.panelX = x; c.panelY = y; }
            case KEYS -> { c.keysX = x; c.keysY = y; }
            default -> { c.compassX = x; c.compassY = y; }
        }
    }

    private static void setBox(int which, int x, int y, int w, int h) {
        int[] b = BOXES[which];
        b[0] = x; b[1] = y; b[2] = w; b[3] = h; b[4] = 1;
    }

    private static void draw(GuiGraphics g, Minecraft mc, Config c, DeltaTracker delta) {
        for (int[] b : BOXES) b[4] = 0;

        trackClicks(mc);
        Font font = mc.font;
        int sw = g.guiWidth(), sh = g.guiHeight();
        int accent = accent(c);

        List<Row> rows = buildRows(mc, c);
        if (!rows.isEmpty()) drawPanel(g, mc, font, rows, c, accent, sw, sh);
        if (c.keystrokes) drawKeystrokes(g, mc, font, c, accent, sw, sh);
        if (c.compass) drawCompass(g, mc, font, c, accent, sw, sh);
        if (delta != null) Waypoints.render(g, mc, delta);
        if (delta != null && mc.screen instanceof ChatScreen) {
            String hint = "Drag the outlined HUD boxes to move them";
            g.drawString(font, hint, sw / 2 - font.width(hint) / 2, 26, 0xCCFFFFFF, true);
        }
    }

    // ------------------------------------------------------------------ rows

    private static List<Row> buildRows(Minecraft mc, Config c) {
        List<Row> rows = new ArrayList<>();
        BlockPos pos = mc.player.blockPosition();

        if (c.fps) rows.add(Row.text("FPS: " + mc.getFps()));
        if (c.coords) rows.add(Row.text(String.format("XYZ: %.1f / %.1f / %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        if (c.facing) {
            String d = mc.player.getDirection().getName();
            rows.add(Row.text("Facing: " + Character.toUpperCase(d.charAt(0)) + d.substring(1)));
        }
        if (c.ping) {
            ClientPacketListener conn = mc.getConnection();
            if (conn != null) {
                PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
                if (info != null) rows.add(Row.text("Ping: " + info.getLatency() + " ms"));
            }
        }
        if (c.clock) rows.add(Row.text("Time: " + LocalTime.now().format(TIME)));
        if (c.biome) {
            String id = mc.level.getBiome(pos).getRegisteredName();
            int colon = id.indexOf(':');
            if (colon >= 0) id = id.substring(colon + 1);
            id = id.replace('_', ' ');
            if (!id.isEmpty()) id = Character.toUpperCase(id.charAt(0)) + id.substring(1);
            rows.add(Row.text("Biome: " + id));
        }
        if (c.light) {
            int b = mc.level.getBrightness(LightLayer.BLOCK, pos);
            int s = mc.level.getBrightness(LightLayer.SKY, pos);
            rows.add(Row.text("Light: " + b + " block / " + s + " sky"));
        }
        if (c.cps) rows.add(Row.text("CPS: " + cps(leftClicks) + " | " + cps(rightClicks)));
        if (c.sprintIndicator) {
            if (mc.player.isShiftKeyDown()) rows.add(Row.text("Sneaking"));
            else if (mc.player.isSprinting()) rows.add(Row.text("Sprinting"));
        }
        if (c.effects) {
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
                String name = e.getEffect().value().getDisplayName().getString();
                String time = e.isInfiniteDuration() ? "inf" : clock(e.getDuration() / 20);
                rows.add(Row.text(name + " " + roman(e.getAmplifier() + 1) + "  " + time));
            }
        }
        if (c.armor) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
            for (EquipmentSlot slot : slots) {
                ItemStack st = mc.player.getItemBySlot(slot);
                if (st.isEmpty() || !st.isDamageableItem()) continue;
                int max = st.getMaxDamage();
                int left = max - st.getDamageValue();
                float pct = Mth.clamp(left / (float) max, 0f, 1f);
                int color = 0xFF000000 | Mth.hsvToRgb(pct / 3f, 0.85f, 1f);
                rows.add(new Row(st, left + "/" + max, color));
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------ panel

    private static void drawPanel(GuiGraphics g, Minecraft mc, Font font, List<Row> rows,
                                  Config c, int accent, int sw, int sh) {
        int textLineH = font.lineHeight + 2;
        int textW = 0, contentH = 0;
        for (Row r : rows) {
            textW = Math.max(textW, font.width(r.text) + (r.hasIcon() ? 20 : 0));
            contentH += r.hasIcon() ? 18 : textLineH;
        }
        int boxW = textW + PAD * 2 + 3;
        int boxH = contentH + PAD * 2 - 2;

        boolean right = c.corner == 1 || c.corner == 3;
        boolean bottom = c.corner == 2 || c.corner == 3;
        int x = c.panelX >= 0 ? c.panelX : (right ? sw - boxW - 6 : 6);
        int y = c.panelY >= 0 ? c.panelY : (bottom ? sh - boxH - 6 : 6);
        x = Mth.clamp(x, 0, Math.max(0, sw - boxW));
        y = Mth.clamp(y, 0, Math.max(0, sh - boxH));

        int[] r = chatDrag(g, mc, ELEMS[PANEL], x, y, boxW, boxH, sw, sh, accent);
        x = r[0];
        y = r[1];
        if (r[2] == 1) { c.panelX = x; c.panelY = y; }
        setBox(PANEL, x, y, boxW, boxH);

        int bg = ((int) (Mth.clamp(c.bgOpacity, 0f, 1f) * 255) << 24) | (c.panelColor & 0xFFFFFF);
        g.fill(x, y, x + boxW, y + boxH, bg);
        g.fill(x, y, x + 2, y + boxH, accent);

        int textColor = 0xFF000000 | c.hudTextColor;
        int ty = y + PAD;
        for (Row row : rows) {
            int col = row.color == 0 ? textColor : row.color;
            if (row.hasIcon()) {
                g.renderItem(row.icon, x + PAD + 3, ty - 1);
                g.drawString(font, row.text, x + PAD + 3 + 20, ty + 4, col, true);
                ty += 18;
            } else {
                g.drawString(font, row.text, x + PAD + 3, ty, col, true);
                ty += textLineH;
            }
        }
    }

    // --------------------------------------------------------------- keystrokes

    private static void drawKeystrokes(GuiGraphics g, Minecraft mc, Font font, Config c,
                                       int accent, int sw, int sh) {
        boolean panelRight = c.corner == 1 || c.corner == 3;
        int kx = c.keysX >= 0 ? c.keysX : (panelRight ? 6 : sw - KEYS_W - 6);
        int ky = c.keysY >= 0 ? c.keysY : sh / 2 - KEY;
        kx = Mth.clamp(kx, 0, Math.max(0, sw - KEYS_W));
        ky = Mth.clamp(ky, 0, Math.max(0, sh - KEYS_H));

        int[] r = chatDrag(g, mc, ELEMS[KEYS], kx, ky, KEYS_W, KEYS_H, sw, sh, accent);
        kx = r[0];
        ky = r[1];
        if (r[2] == 1) { c.keysX = kx; c.keysY = ky; }
        setBox(KEYS, kx, ky, KEYS_W, KEYS_H);

        int bg = 0x90000000;
        int idle = 0xFF000000 | c.hudTextColor;
        key(g, font, "W", mc.options.keyUp.isDown(), kx + KEY + 2, ky, accent, bg, idle);
        key(g, font, "A", mc.options.keyLeft.isDown(), kx, ky + KEY + 2, accent, bg, idle);
        key(g, font, "S", mc.options.keyDown.isDown(), kx + KEY + 2, ky + KEY + 2, accent, bg, idle);
        key(g, font, "D", mc.options.keyRight.isDown(), kx + (KEY + 2) * 2, ky + KEY + 2, accent, bg, idle);

        int sy = ky + (KEY + 2) * 2;
        boolean jump = mc.options.keyJump.isDown();
        g.fill(kx, sy, kx + KEYS_W, sy + 10, jump ? accent : bg);
        g.fill(kx + 10, sy + 4, kx + KEYS_W - 10, sy + 6, jump ? 0xFF000000 : idle);
    }

    private static void key(GuiGraphics g, Font font, String label, boolean down,
                            int x, int y, int accent, int bg, int idle) {
        g.fill(x, y, x + KEY, y + KEY, down ? accent : bg);
        g.drawString(font, label, x + (KEY - font.width(label)) / 2,
                y + (KEY - font.lineHeight) / 2 + 1, down ? 0xFF000000 : idle, false);
    }

    // ------------------------------------------------------------------ compass

    private static void drawCompass(GuiGraphics g, Minecraft mc, Font font, Config c,
                                    int accent, int sw, int sh) {
        int x = c.compassX >= 0 ? c.compassX : sw / 2 - COMPASS_W / 2;
        int y = c.compassY >= 0 ? c.compassY : 6;
        x = Mth.clamp(x, 0, Math.max(0, sw - COMPASS_W));
        y = Mth.clamp(y, 0, Math.max(0, sh - COMPASS_H));

        int[] r = chatDrag(g, mc, ELEMS[COMPASS], x, y, COMPASS_W, COMPASS_H, sw, sh, accent);
        x = r[0];
        y = r[1];
        if (r[2] == 1) { c.compassX = x; c.compassY = y; }
        setBox(COMPASS, x, y, COMPASS_W, COMPASS_H);

        int cx = x + COMPASS_W / 2;
        g.fill(x, y, x + COMPASS_W, y + COMPASS_H, 0x70000000);
        g.fill(cx - 1, y, cx, y + COMPASS_H, accent);

        float yaw = Freelook.active ? Freelook.yaw : mc.player.getYRot();
        float bearing = Mth.wrapDegrees(yaw + 180f);
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int main = 0xFF000000 | c.hudTextColor;
        int dim = 0xFF000000 | UI.lerpRgb(c.hudTextColor, 0x000000, 0.35f);
        for (int i = 0; i < 8; i++) {
            float diff = Mth.wrapDegrees(i * 45f - bearing);
            if (Math.abs(diff) > 88f) continue;
            int px = cx + Math.round(diff * (COMPASS_W / 180f));
            g.drawString(font, names[i], px - font.width(names[i]) / 2, y + 3,
                    (i % 2 == 0) ? main : dim, false);
        }
    }

    // ----------------------------------------------------------------- chat drag

    private static boolean anyDragging() {
        for (Elem e : ELEMS) if (e.dragging) return true;
        return false;
    }

    /** Extra convenience: drag while chat is open. Returns {x, y, moved}. */
    private static int[] chatDrag(GuiGraphics g, Minecraft mc, Elem e, int x, int y, int w, int h,
                                  int sw, int sh, int accent) {
        if (!(mc.screen instanceof ChatScreen)) {
            if (e.dragging) { e.dragging = false; Config.save(); }
            e.wasDown = false;
            return new int[]{x, y, 0};
        }

        int mx = (int) (mc.mouseHandler.xpos() * sw / (double) mc.getWindow().getScreenWidth());
        int my = (int) (mc.mouseHandler.ypos() * sh / (double) mc.getWindow().getScreenHeight());
        boolean down = mc.mouseHandler.isLeftPressed();

        if (down && !e.wasDown && UI.in(mx, my, x - 3, y - 3, w + 6, h + 6) && !anyDragging()) {
            e.dragging = true;
            e.ox = mx - x;
            e.oy = my - y;
        }
        if (!down && e.dragging) {
            e.dragging = false;
            Config.save();
        }
        e.wasDown = down;

        int moved = 0;
        if (e.dragging) {
            x = Mth.clamp(mx - e.ox, 0, Math.max(0, sw - w));
            y = Mth.clamp(my - e.oy, 0, Math.max(0, sh - h));
            moved = 1;
        }
        UI.border(g, x - 2, y - 2, w + 4, h + 4, e.dragging ? 0xFFFFFFFF : (accent & 0x00FFFFFF) | 0xAA000000);
        return new int[]{x, y, moved};
    }

    // ---------------------------------------------------------------- crosshair

    public static void renderCrosshair(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Config c = Config.get();
        if (mc.player == null || mc.options.hideGui) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;

        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int color = 0xFF000000 | c.crosshairColor;
        if (mc.player.getHealth() <= 6f) color = 0xFFFF5555;
        if (mc.crosshairPickEntity != null) color = 0xFF000000 | c.crosshairTargetColor;

        switch (c.crosshair) {
            case 1 -> g.fill(cx - 1, cy - 1, cx + 1, cy + 1, color);
            case 2 -> {
                g.fill(cx - 7, cy - 1, cx - 2, cy, color);
                g.fill(cx + 2, cy - 1, cx + 7, cy, color);
                g.fill(cx - 1, cy - 7, cx, cy - 2, color);
                g.fill(cx - 1, cy + 2, cx, cy + 7, color);
            }
            case 3 -> {
                for (int deg = 0; deg < 360; deg += 20) {
                    double a = Math.toRadians(deg);
                    int px = cx + (int) Math.round(Math.cos(a) * 5);
                    int py = cy + (int) Math.round(Math.sin(a) * 5);
                    g.fill(px - 1, py - 1, px, py, color);
                }
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void trackClicks(Minecraft mc) {
        long now = System.currentTimeMillis();
        boolean l = mc.options.keyAttack.isDown();
        boolean r = mc.options.keyUse.isDown();
        if (l && !lastLeft) leftClicks.add(now);
        if (r && !lastRight) rightClicks.add(now);
        lastLeft = l;
        lastRight = r;
    }

    private static int cps(Deque<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
        return clicks.size();
    }

    private static String clock(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return (n >= 0 && n < r.length) ? r[n] : String.valueOf(n);
    }
}
