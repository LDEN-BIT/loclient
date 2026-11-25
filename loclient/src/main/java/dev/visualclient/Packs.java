package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keeps Lo Client's resource packs enabled and on top of your other packs. Packs whose name
 * contains "font" (your own font packs) are kept above Lo Client's, so they win for fonts.
 */
public final class Packs {
    private Packs() {}

    public static void sync(boolean forceReload) {
        Minecraft mc = Minecraft.getInstance();
        Config c = Config.get();
        PackRepository repo = mc.getResourcePackRepository();
        repo.reload(); // picks up the pack folders we just wrote

        List<String> before = new ArrayList<>(repo.getSelectedIds());
        List<String> ours = new ArrayList<>();
        if (c.packsOnTop) {
            if (Files.isDirectory(LoPack.dir())) ours.add(LoPack.ID);
            if (Files.isDirectory(GlintPack.dir())) ours.add(GlintPack.ID);
        }

        List<String> others = new ArrayList<>();
        List<String> fontPacks = new ArrayList<>();
        for (String id : before) {
            if (id.equals(LoPack.ID) || id.equals(GlintPack.ID)) continue;
            if (c.packsOnTop && id.toLowerCase(Locale.ROOT).contains("font")) fontPacks.add(id);
            else others.add(id);
        }

        List<String> after = new ArrayList<>(others);
        after.addAll(ours);
        after.addAll(fontPacks);

        if (after.equals(before)) {
            if (forceReload) mc.reloadResourcePacks();
            return;
        }
        repo.setSelected(after);
        mc.options.updateResourcePacks(repo); // saves options.txt and reloads when the list changed
    }
}
