package dev.visualclient;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** An invisible slider: it handles dragging, the screen draws the looks. */
public class GhostSlider extends AbstractSliderButton {
    private final float min, max;
    private final Consumer<Float> set;

    public GhostSlider(int x, int y, int w, int h, float min, float max, float current,
                       Consumer<Float> set) {
        super(x, y, w, h, Component.empty(), (current - min) / (max - min));
        this.min = min;
        this.max = max;
        this.set = set;
        setAlpha(0f);
    }

    public float frac() {
        return (float) this.value;
    }

    public void setFrac(float f) {
        this.value = f;
    }

    @Override
    protected void updateMessage() {}

    @Override
    protected void applyValue() {
        set.accept(min + (float) this.value * (max - min));
    }
}
