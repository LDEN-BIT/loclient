package dev.visualclient.mixin;

import dev.visualclient.Config;
import dev.visualclient.VisualTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    // Swaps the vanilla title screen for the VisualClient one.
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private Screen visualclient$title(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class && Config.get().customMenu) {
            return new VisualTitleScreen();
        }
        return screen;
    }
}
