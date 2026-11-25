package dev.visualclient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Your own waypoint markers, drawn on screen. They live only on your client. */
public final class Waypoints {
    private static final int[] PALETTE = {
            0x55FFFF, 0xFF5555, 0x55FF55, 0xFFFF55, 0xFF55FF, 0xFFAA00
    };

    private Waypoints() {}

    private static String key() {
        ServerData sd = Minecraft.getInstance().getCurrentServer();
        return sd != null ? sd.ip : "singleplayer";
    }

    public static List<Config.Waypoint> list() {
        return Config.get().waypoints.computeIfAbsent(key(), k -> new ArrayList<>());
    }

    public static void addHere(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        List<Config.Waypoint> l = list();
        Config.Waypoint w = new Config.Waypoint();
        w.name = (name == null || name.isBlank()) ? "Waypoint " + (l.size() + 1) : name;
        w.x = mc.player.getX();
        w.y = mc.player.getY();
        w.z = mc.player.getZ();
        w.color = PALETTE[l.size() % PALETTE.length];
        l.add(w);
        Config.save();
    }

    public static boolean remove(String name) {
        boolean removed = list().removeIf(w -> w.name.equalsIgnoreCase(name));
        if (removed) Config.save();
        return removed;
    }

    public static void render(GuiGraphics g, Minecraft mc, DeltaTracker delta) {
        Config c = Config.get();
        Player p = mc.player;
        if (!c.showWaypoints || p == null) return;
        List<Config.Waypoint> l = list();
        if (l.isEmpty()) return;

        float pt = delta.getGameTimeDeltaPartialTick(false);
        float yaw = Freelook.active ? Freelook.yaw : Mth.lerp(pt, p.yRotO, p.getYRot());
        float pitch = Freelook.active ? Freelook.pitch : Mth.lerp(pt, p.xRotO, p.getXRot());
        Vec3 eye = p.getEyePosition(pt);

        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
        double rx = -Math.cos(yr), rz = -Math.sin(yr);
        // up = right x forward
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;

        int sw = g.guiWidth(), sh = g.guiHeight();
        double fov = Math.toRadians(mc.options.fov().get() * Zoom.current());
        double tanHalf = Math.tan(fov / 2.0);
        double aspect = sw / (double) sh;
        Font font = mc.font;

        for (Config.Waypoint w : l) {
            double dx = w.x - eye.x, dy = w.y + 1.0 - eye.y, dz = w.z - eye.z;
            double depth = dx * fx + dy * fy + dz * fz;
            if (depth < 0.5) continue;
            double right = dx * rx + dz * rz;
            double up = dx * ux + dy * uy + dz * uz;
            int sx = (int) Math.round(sw / 2.0 + (right / depth) / (tanHalf * aspect) * (sw / 2.0));
            int sy = (int) Math.round(sh / 2.0 - (up / depth) / tanHalf * (sh / 2.0));
            if (sx < -30 || sx > sw + 30 || sy < -30 || sy > sh + 30) continue;

            int color = 0xFF000000 | w.color;
            for (int i = -3; i <= 3; i++) {
                int half = 3 - Math.abs(i);
                g.fill(sx - half, sy + i, sx + half + 1, sy + i + 1, color);
            }
            double dist = Math.sqrt(dx * dx + (dy - 1.0) * (dy - 1.0) + dz * dz);
            String label = w.name + "  " + Math.round(dist) + "m";
            g.drawString(font, label, sx - font.width(label) / 2, sy - 14, color, true);
        }
    }
}
