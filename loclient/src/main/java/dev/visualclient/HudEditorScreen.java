package dev.visualclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * HUD layout editor. Click and drag the panel, keystrokes and compass anywhere.
 * Esc or Done to finish. Everything is saved automatically.
 */
public class HudEditorScreen extends Screen {
    private static final String[] NAMES = {"Info panel", "Keystrokes", "Compass"};

    private final Screen parent;
    private int dragging = -1;
    private int ox, oy;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Edit HUD layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 90, h = 20, y = this.height - 34;
        addRenderableWidget(UI.ghost(this.width / 2 - w - 4, y, w, h, () -> {
            Config.get().resetPositions();
            Config.save();
        }));
        addRenderableWidget(UI.ghost(this.width / 2 + 4, y, w, h, this::onClose));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick); // background + invisible buttons
        Config c = Config.get();
        int accent = Hud.accent(c);

        g.fill(0, 0, this.width, this.height, 0x55000000);
        Hud.renderEditor(g);

        // Outlines + names so you can see what is draggable
        for (int i = 0; i < 3; i++) {
            int[] b = Hud.BOXES[i];
            if (b[4] == 0) continue;
            boolean hover = dragging == i || (dragging < 0 && UI.in(mx, my, b[0], b[1], b[2], b[3]));
            UI.border(g, b[0] - 2, b[1] - 2, b[2] + 4, b[3] + 4, hover ? 0xFFFFFFFF : accent);
            g.drawString(this.font, NAMES[i], b[0], Math.max(2, b[1] - 12), hover ? 0xFFFFFFFF : accent, true);
        }

        String title = "Edit HUD layout";
        g.drawString(this.font, title, this.width / 2 - this.font.width(title) / 2, 10, 0xFFFFFFFF, true);
        String hint = "Drag the highlighted boxes. Turn elements on or off in Lo Client Settings.";
        g.drawString(this.font, hint, this.width / 2 - this.font.width(hint) / 2, 24, 0xFFAAAABB, false);

        // Buttons
        int w = 90, h = 20, y = this.height - 34;
        drawChip(g, "Reset", this.width / 2 - w - 4, y, w, h, mx, my, accent);
        drawChip(g, "Done", this.width / 2 + 4, y, w, h, mx, my, accent);
    }

    private void drawChip(GuiGraphics g, String label, int x, int y, int w, int h, int mx, int my, int accent) {
        boolean hover = UI.in(mx, my, x, y, w, h);
        UI.rect(g, x, y, w, h, hover ? 0x66FFFFFF : 0x33FFFFFF);
        g.drawString(this.font, label, x + w / 2 - this.font.width(label) / 2, y + 6, hover ? 0xFFFFFFFF : accent, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            for (int i = 2; i >= 0; i--) {
                int[] b = Hud.BOXES[i];
                if (b[4] == 1 && UI.in(mx, my, b[0] - 2, b[1] - 2, b[2] + 4, b[3] + 4)) {
                    dragging = i;
                    ox = mx - b[0];
                    oy = my - b[1];
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging >= 0) {
            Hud.setPos(dragging, (int) event.x() - ox, (int) event.y() - oy, this.width, this.height);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging >= 0) {
            dragging = -1;
            Config.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
