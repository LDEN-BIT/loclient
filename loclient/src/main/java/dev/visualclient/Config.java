package dev.visualclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** All settings, saved to config/visualclient.json. */
public class Config {
    // HUD elements
    public boolean hud = true;
    public boolean keystrokes = true;
    public boolean fps = true;
    public boolean coords = true;
    public boolean facing = true;
    public boolean ping = true;
    public boolean clock = false;
    public boolean biome = false;
    public boolean light = false;
    public boolean cps = true;
    public boolean armor = true;
    public boolean effects = true;
    public boolean compass = true;
    public boolean sprintIndicator = true;

    // Layout: -1 means "use the default spot for the chosen corner"
    public int corner = 0;
    public int panelX = -1, panelY = -1;
    public int keysX = -1, keysY = -1;
    public int compassX = -1, compassY = -1;

    // Theme (colors are RGB ints, e.g. 0x8F6BFF)
    public boolean rainbow = true;
    public int accentColor = 0x8F6BFF;
    public int hudTextColor = 0xFFFFFF;
    public int panelColor = 0x000000;
    public float bgOpacity = 0.56f;
    public boolean customMenu = true;

    // Crosshair: 0 vanilla, 1 dot, 2 cross, 3 circle
    public int crosshair = 0;
    public int crosshairColor = 0xFFFFFF;
    public int crosshairTargetColor = 0xFF5555;

    // World visuals
    public boolean skyTint = false;
    public int skyColor = 0x7A5CFF;
    public float skyStrength = 0.5f;
    public boolean cloudTint = false;
    public int cloudColor = 0xFF8AD8;
    public float particles = 1.0f;
    public float starBoost = 0.0f;      // 0 = vanilla stars
    public boolean outlineEnabled = false;
    public int outlineColor = 0x8F6BFF;
    public int glintColor = 0x55AAFF;   // written to a generated resource pack

    // Fonts / packs / light
    public String font = "Default";      // "Default" = vanilla Minecraft font
    public float fontSize = 10f;
    public boolean packsOnTop = true;    // keep Lo Client's resource packs enabled above other packs
    public boolean fullbright = false;

    // Camera
    public float zoom = 0.25f;
    public boolean freelook = true;
    public boolean freelookThirdPerson = true;
    public float thirdPersonDistance = 1.0f;

    // Waypoints (per server / singleplayer)
    public boolean showWaypoints = true;
    public Map<String, List<Waypoint>> waypoints = new HashMap<>();

    // Saved profiles ("1", "2", "3") as JSON snapshots
    public Map<String, String> profiles = new HashMap<>();

    public static class Waypoint {
        public String name;
        public double x, y, z;
        public int color;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("loclient.json");
    }

    private static Path oldPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("visualclient.json");
    }

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            Path p = Files.exists(path()) ? path() : oldPath(); // migrate old settings
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    instance = GSON.fromJson(r, Config.class);
                }
            }
        } catch (Exception e) {
            System.err.println("[VisualClient] Could not read config, using defaults: " + e);
        }
        if (instance == null) instance = new Config();
        if (instance.waypoints == null) instance.waypoints = new HashMap<>();
        if (instance.profiles == null) instance.profiles = new HashMap<>();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(get(), w);
        } catch (Exception e) {
            System.err.println("[VisualClient] Could not save config: " + e);
        }
    }

    public void resetPositions() {
        panelX = panelY = keysX = keysY = compassX = compassY = -1;
    }

    // ---- Profiles ----

    public static boolean hasProfile(int slot) {
        return get().profiles.containsKey(String.valueOf(slot));
    }

    public static void saveProfile(int slot) {
        Config cur = get();
        Config copy = GSON.fromJson(GSON.toJson(cur), Config.class);
        copy.waypoints = null;
        copy.profiles = null;
        cur.profiles.put(String.valueOf(slot), GSON.toJson(copy));
        save();
    }

    public static boolean loadProfile(int slot) {
        Config cur = get();
        String json = cur.profiles.get(String.valueOf(slot));
        if (json == null) return false;
        Config p = GSON.fromJson(json, Config.class);
        p.waypoints = cur.waypoints;
        p.profiles = cur.profiles;
        instance = p;
        save();
        return true;
    }
}
