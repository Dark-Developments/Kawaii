package net.kawaii;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.ethanol.ethanolapi.server.listener.EthanolServerListener;

import java.io.File;

public class Client implements ClientModInitializer {
    public static final Client INSTANCE = new Client();

    public static MinecraftClient mc = MinecraftClient.getInstance();

    public final String NAME = "Kawaii";
    public static final File BASEFILE = new File(MinecraftClient.getInstance().runDirectory, INSTANCE.NAME);
    public static final Logger LOGGER = LoggerFactory.getLogger(INSTANCE.NAME);

    public static EthanolServerListener EthanolListener;

    public static final KeyBinding ScreenKeyBinding = new KeyBinding(
            "Ethanol",
            GLFW.GLFW_KEY_HOME,
            "Ethanol"
    );

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ScreenKeyBinding);

        System.out.println("""
               
               _
              {_}
              |(|
              |=|
             /   \\
             |.--|  
             ||  | 
             ||  | 
             |'--|
             '-=-'
             
             """);

    }
}
