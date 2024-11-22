package net.kawaii.utils;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

import static net.kawaii.Client.mc;

public class Utils {

    public static boolean canUpdate() {
        return mc != null && mc.world != null && mc.player != null;
    }

    public static boolean canOpenGui() {
        if (canUpdate()) return mc.currentScreen == null;

        return canOpenGuiMenu();
    }

    public static boolean canOpenGuiMenu(){
        return canOpenGuiMenu(mc.currentScreen);
    }

    public static boolean canOpenGui(Screen screen) {
        if (canUpdate()) return screen == null;

        return canOpenGuiMenu(screen);
    }

    public static boolean canOpenGuiMenu(Screen screen){
        return screen == null || screen instanceof TitleScreen || screen instanceof MultiplayerScreen || screen instanceof SelectWorldScreen;
    }

}
