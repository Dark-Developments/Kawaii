package net.kawaii.screens;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;
import net.kawaii.Client;
import net.kawaii.screens.base.ImGuiBaseScreen;
import net.kawaii.utils.EthanolSystem;
import net.kawaii.utils.ImUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import rocks.ethanol.ethanolapi.EthanolAPI;
import rocks.ethanol.ethanolapi.auth.DiscordAuthURL;
import rocks.ethanol.ethanolapi.server.listener.EthanolServer;
import rocks.ethanol.ethanolapi.server.listener.EthanolServerListener;
import rocks.ethanol.ethanolapi.server.listener.exceptions.EthanolServerListenerConnectException;
import rocks.ethanol.ethanolapi.structure.ThrowingConsumer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static net.kawaii.Client.mc;

public class EthanolScreen extends ImGuiBaseScreen {
    public static EthanolScreen INSTANCE = new EthanolScreen();
    private boolean loaded = false, showDemoScreen = false, showConsole = false;
    private EthanolServerListener listener = Client.EthanolListener;
    private CompletableFuture<Void> authFuture;
    private EthanolConsole ethanolConsole;

    public EthanolScreen() {
        super("ClickGUI");
    }

    @Override
    public synchronized void init() {
        if (loaded) {
            return;
        }
        loaded = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        ImUtil.draw(() -> {
            show();
            if (showConsole) ethanolConsole.show();

            if (showDemoScreen) ImGui.showDemoWindow();
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_Q && modifiers == GLFW.GLFW_MOD_SHIFT){
            showDemoScreen = !showDemoScreen;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void show() {
        ImGui.setNextWindowSize(350, 400, ImGuiCond.Once);

        if (!ImGui.begin("Ethanol")) { // Start Custom window
            ImGui.end(); // Always call end if begin fails
            return;
        }

        String apikey = EthanolSystem.apiKey;

        if (apikey.isEmpty()) {
            if (ImGui.button("Login")) {
                ImGui.openPopup("DiscordLogin");

                if (authFuture != null && !authFuture.isDone()) {
                    authFuture.cancel(true); // Cancel the previous authentication task
                }

                if (ImGui.beginPopupModal("DiscordLogin")) {
                    ThrowingConsumer<DiscordAuthURL, IOException> opener = url -> {
                        Util.getOperatingSystem().open(url.toURI());
                    };

                    ImGui.text("Please authenticate with Discord in your browser.");
                    ImGui.text("The browser didn't open? Click below to copy the link and open it manually.");

                    if (ImGui.button("Copy")) {
                        String url = EthanolAPI.DEFAULT_AUTHENTICATOR.getUrl().toString();
                        mc.keyboard.setClipboard(url);
                    }

                    if (ImGui.button("Close")) {
                        ImGui.closeCurrentPopup(); // Ensure proper popup closure
                    }

                    authFuture = EthanolAPI.DEFAULT_AUTHENTICATOR.authenticateAsync(
                            60000, opener
                    ).thenAccept(result -> {
                        EthanolSystem.apiKey = result;
                        ImGui.closeCurrentPopup(); // Close the popup after authentication
                    }).exceptionally(ex -> {
                        ImGui.text("Failed to authenticate with Discord.");
                        return null;
                    });

                    ImGui.endPopup();
                }
            }
        }

        if (listener == null && !apikey.isEmpty()) {
            listener = EthanolAPI.createServerListener(apikey);
            Client.EthanolListener = listener;
            try {
                listener.run();
            } catch (IOException | EthanolServerListenerConnectException ignored) {
            }
        }

        if (apikey.isEmpty() || listener == null || listener.getServers() == null || listener.getServers().length == 0) {
            ImGui.text("No Infected Servers Found.");
        } else {
            ImGui.text("Found %s server(s):".formatted(listener.getServers().length));

            if (ImGui.beginTable("servers", 3,  ImGuiTableFlags.Reorderable | ImGuiTableFlags.Hideable | ImGuiTableFlags.BordersOuter | ImGuiTableFlags.RowBg| ImGuiTableFlags.BordersInnerV)) {
                ImGui.tableSetupColumn("IP");
                ImGui.tableSetupColumn("Players");
                ImGui.tableSetupColumn("Actions");
                ImGui.tableSetupScrollFreeze(0, 1); // Make top row always visible
                ImGui.tableHeadersRow();

                for (EthanolServer server : listener.getServers()) {
                    ImGui.tableNextRow();
                    String serverIP = server.getAddress().toString().substring(1);
                    String Players = "%s/%s".formatted(server.getOnlinePlayers(), server.getMaxPlayers());

                    ImGui.tableSetColumnIndex(0);
                    ImGui.text(serverIP);
                    ImGui.tableSetColumnIndex(1);
                    ImGui.text(Players);
                    ImGui.tableSetColumnIndex(2);

                    if (ImGui.button("Join")) {
                        ServerInfo info = new ServerInfo(serverIP, serverIP, ServerInfo.ServerType.OTHER);
                        ConnectScreen.connect(this, mc, ServerAddress.parse(serverIP), info, false, null);
                    }

                    ImGui.sameLine();

                    if (ImGui.button("Console")) {
                        ethanolConsole = new EthanolConsole(server.getAuthentication());
                        showConsole = true;
                    }
                }

                ImGui.endTable();
            }
        }

        ImGui.end(); // End main window
    }
}