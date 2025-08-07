package net.kawaii.mixin;

import net.kawaii.Client;
import net.kawaii.screens.ServerConsoleScreen;
import net.kawaii.utils.EthanolSystem;
import net.kawaii.utils.Utils;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.ethanol.ethanolapi.server.listener.EthanolServer;
import rocks.ethanol.ethanolapi.server.listener.EthanolServerListener;

import java.util.Objects;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(at = @At("HEAD"), method = "onKey", cancellable = true)
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci){
        if (action != GLFW.GLFW_PRESS) return;
        if (!Client.ScreenKeyBinding.matchesKey(key, scancode)) return;
        if (Client.mc.player == null || Client.mc.world == null) return;
        if (Client.mc.getNetworkHandler() == null || !Client.mc.getNetworkHandler().isConnectionOpen()) return;
        if (!Utils.canOpenGuiMenu()) return;
        Client.LOGGER.info("Can Open");

        if (EthanolSystem.apiKey != null);

        Client.LOGGER.info("Got Key");

        EthanolServerListener listener = Client.EthanolListener;
        if (listener == null) return;

        Client.LOGGER.info("Listener Alive");

        for (EthanolServer server : listener.getServers()) {
            if (server.getAddress().toString().substring(1).equals(Objects.requireNonNull(Client.mc.getNetworkHandler().getServerInfo()).address)) {
                Client.LOGGER.info("Got Server");
                Client.mc.setScreen(new ServerConsoleScreen(server));
                break;
            }
        }
    }
}