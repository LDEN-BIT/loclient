package dev.visualclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * The generated "Lo Client Pack" resource pack (lives in the resourcepacks folder):
 * the dark button/list/scrollbar textures plus the font you picked. Packs.sync() keeps it enabled
 * above your other packs (but below packs with "font" in their name).
 */
public final class LoPack {
    public static final String NAME = "Lo Client Pack";
    public static final String ID = "file/" + NAME;

    private static final String MCMETA =
            "{\"pack\":{\"pack_format\":75,\"description\":\"Lo Client theme and fonts\","
                    + "\"supported_formats\":{\"min_inclusive\":46,\"max_inclusive\":99}}}";

    private static final String[] GUI_FILES = {
            "footer_separator.png", "header_separator.png",
            "inworld_footer_separator.png", "inworld_header_separator.png",
            "inworld_menu_list_background.png", "menu_list_background.png"
    };
    private static final String[] SPRITES = {
            "button", "button_disabled", "button_highlighted", "scroller", "scroller_background",
            "slider", "slider_handle", "slider_handle_highlighted", "slider_highlighted",
            "text_field", "text_field_highlighted"
    };

    private LoPack() {}

    public static Path dir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(NAME);
    }

    public static boolean write(Config c) {
        try {
            Path root = dir();
            deleteTree(root);
            Files.createDirectories(root);
            Files.writeString(root.resolve("pack.mcmeta"), MCMETA);

            for (String f : GUI_FILES) copyTheme(root, f);
            for (String s : SPRITES) {
                copyTheme(root, "sprites/widget/" + s + ".png");
                copyTheme(root, "sprites/widget/" + s + ".png.mcmeta");
            }

            if (LoFonts.isCustom(c)) {
                byte[] ttf = LoFonts.bytes(c.font);
                String file = "lofont" + LoFonts.ext(c.font);
                Path ownFonts = root.resolve("assets").resolve("loclient").resolve("font");
                Files.createDirectories(ownFonts);
                Files.write(ownFonts.resolve(file), ttf);

                // Providers from packs are merged, so characters this font lacks fall back to vanilla.
                Path mcFonts = root.resolve("assets").resolve("minecraft").resolve("font");
                Files.createDirectories(mcFonts);
                Files.writeString(mcFonts.resolve("default.json"),
                        "{\"providers\":[{\"type\":\"ttf\",\"file\":\"loclient:" + file + "\","
                                + "\"shift\":[0,0],\"size\":" + c.fontSize + ",\"oversample\":4.0}]}");
            }
            return true;
        } catch (Exception e) {
            System.err.println("[Lo Client] Could not write Lo Client Pack: " + e);
            return false;
        }
    }

    private static void copyTheme(Path root, String rel) throws IOException {
        try (InputStream in = LoPack.class.getResourceAsStream("/assets/minecraft/textures/gui/" + rel)) {
            if (in == null) return;
            Path out = root.resolve("assets").resolve("minecraft").resolve("textures").resolve("gui").resolve(rel);
            Files.createDirectories(out.getParent());
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }
}
