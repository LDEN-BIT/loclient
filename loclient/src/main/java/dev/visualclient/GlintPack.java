package dev.visualclient;

import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Item glint color: Minecraft has no setting for it, so we generate a tiny resource pack
 * ("Lo Client Glint") with a recolored glint texture. Enable it once in Options > Resource Packs
 * and press F3+T after changing the color.
 */
public final class GlintPack {
    public static final String NAME = "Lo Client Glint";
    public static final String ID = "file/" + NAME;

    private GlintPack() {}

    public static Path dir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(NAME);
    }

    public static boolean write(int rgb) {
        try {
            Path root = dir();
            Path tex = root.resolve("assets").resolve("minecraft").resolve("textures").resolve("misc");
            Files.createDirectories(tex);
            Files.writeString(root.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":75,\"description\":\"Lo Client glint color\","
                            + "\"supported_formats\":{\"min_inclusive\":46,\"max_inclusive\":99}}}");
            BufferedImage img = pattern(rgb);
            ImageIO.write(img, "png", tex.resolve("enchanted_glint_item.png").toFile());
            ImageIO.write(img, "png", tex.resolve("enchanted_glint_entity.png").toFile());
            return true;
        } catch (Exception e) {
            System.err.println("[Lo Client] Could not write glint pack: " + e);
            return false;
        }
    }

    public static boolean delete() {
        Path root = dir();
        if (!Files.exists(root)) return false;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** A 64x64 tileable shimmer: diagonal bands with a little sparkle, tinted with the color. */
    private static BufferedImage pattern(int rgb) {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double w1 = 0.5 + 0.5 * Math.sin((x + y) * 2 * Math.PI / 16.0);
                double w2 = 0.5 + 0.5 * Math.sin((x - 2 * y) * 2 * Math.PI / 32.0);
                int h = (x * 73856093) ^ (y * 19349663);
                double n = ((h >>> 3) & 255) / 255.0;
                double v = 0.12 + 0.62 * w1 * w1 + 0.30 * w2 * n;
                v = Math.max(0, Math.min(1, v));
                int pr = (int) (r * v), pg = (int) (g * v), pb = (int) (b * v);
                img.setRGB(x, y, 0xFF000000 | (pr << 16) | (pg << 8) | pb);
            }
        }
        return img;
    }
}
