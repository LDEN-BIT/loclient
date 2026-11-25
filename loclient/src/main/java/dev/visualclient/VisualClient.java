package dev.visualclient;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Lo Client: cosmetic HUD, menus, camera and world visuals. It only reads
 * your own game state and draws on your own screen, so it is safe on any server.
 */
public class VisualClient implements ClientModInitializer {
    public static final String MOD_ID = "loclient";
    public static final String VERSION = "1.5.0";

    private static KeyMapping toggleHud, toggleKeys, cycleCorner, zoomKey, settingsKey,
            freelookKey, waypointKey, editorKey, fullbrightKey;
    private static boolean packsSynced;

    @Override
    public void onInitializeClient() {
        Config.load();
        LoFonts.initFolders();
        if (Config.get().packsOnTop) LoPack.write(Config.get());

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        toggleHud = reg("key.loclient.toggle_hud", InputConstants.KEY_H, category);
        toggleKeys = reg("key.loclient.toggle_keys", InputConstants.KEY_K, category);
        cycleCorner = reg("key.loclient.cycle_corner", InputConstants.KEY_J, category);
        zoomKey = reg("key.loclient.zoom", InputConstants.KEY_C, category);
        settingsKey = reg("key.loclient.settings", InputConstants.KEY_RSHIFT, category);
        freelookKey = reg("key.loclient.freelook", InputConstants.KEY_LALT, category);
        waypointKey = reg("key.loclient.waypoint", InputConstants.KEY_B, category);
        editorKey = reg("key.loclient.hud_editor", InputConstants.KEY_U, category);
        fullbrightKey = reg("key.loclient.fullbright", InputConstants.KEY_G, category);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Config c = Config.get();
            // Once the game has finished loading, make sure Lo Client's packs are enabled on top.
            if (!packsSynced && client.getOverlay() == null) {
                packsSynced = true;
                if (c.packsOnTop) Packs.sync(false);
            }

            Zoom.active = zoomKey.isDown() && client.screen == null;

            boolean wantFree = c.freelook && freelookKey.isDown()
                    && client.screen == null && client.player != null;
            if (wantFree && !Freelook.active) Freelook.start();
            else if (!wantFree && Freelook.active) Freelook.stop();

            while (toggleHud.consumeClick()) {
                c.hud = !c.hud;
                Config.save();
                notice(client, "HUD " + (c.hud ? "on" : "off"));
            }
            while (toggleKeys.consumeClick()) {
                c.keystrokes = !c.keystrokes;
                Config.save();
                notice(client, "Keystrokes " + (c.keystrokes ? "on" : "off"));
            }
            while (cycleCorner.consumeClick()) {
                c.corner = (c.corner + 1) % 4;
                c.resetPositions();
                Config.save();
            }
            while (fullbrightKey.consumeClick()) {
                c.fullbright = !c.fullbright;
                Config.save();
                notice(client, "Fullbright " + (c.fullbright ? "on" : "off"));
            }
            while (waypointKey.consumeClick()) {
                Waypoints.addHere(null);
                notice(client, "Waypoint added");
            }
            while (editorKey.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new HudEditorScreen(null));
                }
            }
            while (settingsKey.consumeClick()) {
                client.setScreen(new SettingsScreen(client.screen));
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                Hud::render);

        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (g, delta) -> {
            if (Config.get().crosshair == 0) {
                original.render(g, delta);
            } else {
                Hud.renderCrosshair(g);
            }
        });

        // /wp add <name>, /wp remove <name>, /wp list
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("wp")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            Waypoints.addHere(StringArgumentType.getString(ctx, "name"));
                                            ctx.getSource().sendFeedback(Component.literal("Waypoint added."));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            boolean ok = Waypoints.remove(StringArgumentType.getString(ctx, "name"));
                                            ctx.getSource().sendFeedback(Component.literal(
                                                    ok ? "Waypoint removed." : "No waypoint with that name."));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("list")
                                .executes(ctx -> {
                                    List<Config.Waypoint> l = Waypoints.list();
                                    if (l.isEmpty()) {
                                        ctx.getSource().sendFeedback(Component.literal("No waypoints here."));
                                    }
                                    for (Config.Waypoint w : l) {
                                        ctx.getSource().sendFeedback(Component.literal(String.format(
                                                "%s: %.0f, %.0f, %.0f", w.name, w.x, w.y, w.z)));
                                    }
                                    return 1;
                                }))));
    }

    private static KeyMapping reg(String name, int key, KeyMapping.Category category) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(name, InputConstants.Type.KEYSYM, key, category));
    }

    private static void notice(Minecraft mc, String text) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Lo Client] " + text), true);
        }
    }
}
