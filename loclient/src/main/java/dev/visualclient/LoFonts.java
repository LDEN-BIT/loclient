package dev.visualclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Font list for Lo Client: a few bundled open-license fonts (SIL OFL, licenses are next to the
 * font files in the jar) plus any .ttf / .otf you drop into  .minecraft/loclient/fonts/
 */
public final class LoFonts {
    public static final String DEFAULT = "Default";

    private record Bundled(String name, String file) {}

    private static final Bundled[] BUNDLED = {
            new Bundled("Outfit", "Outfit-Regular.ttf"),
            new Bundled("Jura", "Jura-Medium.ttf"),
            new Bundled("Tektur", "Tektur-Regular.ttf"),
            new Bundled("Pixelify Sans", "PixelifySans-Medium.ttf"),
            new Bundled("Instrument Sans", "InstrumentSans-Regular.ttf"),
    };

    private LoFonts() {}

    public static Path userDir() {
        return FabricLoader.getInstance().getGameDir().resolve("loclient").resolve("fonts");
    }

    public static void initFolders() {
        try {
            Files.createDirectories(userDir());
        } catch (IOException e) {
            System.err.println("[Lo Client] Could not create fonts folder: " + e);
        }
    }

    public static String[] names() {
        List<String> list = new ArrayList<>();
        list.add(DEFAULT);
        for (Bundled b : BUNDLED) list.add(b.name);
        for (Path p : userFiles()) {
            String s = stem(p);
            if (!list.contains(s)) list.add(s);
        }
        return list.toArray(new String[0]);
    }

    /** True when a non-default font is chosen and its file can actually be found. */
    public static boolean isCustom(Config c) {
        return c.font != null && !DEFAULT.equals(c.font) && bytes(c.font) != null;
    }

    public static byte[] bytes(String name) {
        for (Bundled b : BUNDLED) {
            if (b.name.equals(name)) {
                try (InputStream in = LoFonts.class.getResourceAsStream("/assets/loclient/fontfiles/" + b.file)) {
                    return in == null ? null : in.readAllBytes();
                } catch (IOException e) {
                    return null;
                }
            }
        }
        for (Path p : userFiles()) {
            if (stem(p).equals(name)) {
                try {
                    return Files.readAllBytes(p);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** ".ttf" or ".otf" */
    public static String ext(String name) {
        for (Path p : userFiles()) {
            if (stem(p).equals(name)) {
                return p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".otf") ? ".otf" : ".ttf";
            }
        }
        return ".ttf";
    }

    private static List<Path> userFiles() {
        Path d = userDir();
        if (!Files.isDirectory(d)) return List.of();
        try (Stream<Path> s = Files.list(d)) {
            return s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.endsWith(".ttf") || n.endsWith(".otf");
            }).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String stem(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
