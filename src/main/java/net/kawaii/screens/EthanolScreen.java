package net.kawaii.screens;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;
import net.kawaii.Client;
import net.kawaii.screens.base.ImGuiBaseScreen;
import net.kawaii.utils.EthanolSystem;
import net.kawaii.utils.ImUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
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
    public boolean loaded = false, showDemoScreen = false, showConsole = false;
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
        if (keyCode == GLFW.GLFW_KEY_TAB && modifiers == GLFW.GLFW_MOD_SHIFT){
            showDemoScreen = !showDemoScreen;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void show() {
        ImGui.setNextWindowSize(500, 520, ImGuiCond.FirstUseEver);

        if (!ImGui.begin("Ethanol")) { // Start Custom window
            ImGui.end(); // Always call end if begin fails
            return;
        }

        String apikey = EthanolSystem.apiKey;

        if (apikey.isEmpty()) {
            ImGui.text("Please authenticate with Discord in your browser.");
            ImGui.text("The browser didn't open? Click below to copy the link and open it manually.");

            if (ImGui.button("Login")) {
                if (authFuture != null && !authFuture.isDone()) {
                    authFuture.cancel(true); // Cancel the previous authentication task
                }

                ThrowingConsumer<DiscordAuthURL, IOException> opener = url -> {
                    Util.getOperatingSystem().open(url.toURI());
                };

                authFuture = EthanolAPI.DEFAULT_AUTHENTICATOR.authenticateAsync(
                        60000, opener // Authentication with a timeout of 60 seconds
                ).thenAccept(result -> {
                    // Successful authentication callback
                    EthanolSystem.apiKey = result;
                }).exceptionally(ex -> {
                    // Log or handle specific exceptions here if needed (e.g., timeout)
                    ex.printStackTrace();
                    return null; // Return null to indicate that we handled the exception
                });
            }

            ImGui.sameLine();

            if (ImGui.button("Copy")) {
                String url = EthanolAPI.DEFAULT_AUTHENTICATOR.getUrl().toString();
                mc.keyboard.setClipboard(url);
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

            if (ImGui.beginTable("servers", 3, ImGuiTableFlags.Reorderable | ImGuiTableFlags.Hideable | ImGuiTableFlags.BordersOuter | ImGuiTableFlags.RowBg | ImGuiTableFlags.BordersInnerV)) {
                ImGui.tableSetupColumn("IP");
                ImGui.tableSetupColumn("Players");
                ImGui.tableSetupColumn("Actions");
//                ImGui.tableSetupScrollFreeze(0, 1); // Make top row always visible
                ImGui.tableHeadersRow();

                for (EthanolServer server : listener.getServers()) {
                    ImGui.tableNextRow();

                    String serverIP = server.getAddress().toString().substring(1);
                    String Players = "%s/%s".formatted(server.getOnlinePlayers(), server.getMaxPlayers());

                    // Column 1 - IP
                    ImGui.tableSetColumnIndex(0);
                    ImGui.text(serverIP);

                    // Column 2 - Players
                    ImGui.tableSetColumnIndex(1);
                    ImGui.text(Players);

                    // Column 3 - Actions
                    ImGui.tableSetColumnIndex(2);

                    ImGui.pushID("join_" + serverIP);  // Unique ID for the "Join" button
                    if (ImGui.button("Join")) {  // Same label for all buttons, but unique IDs
                        ServerInfo info = new ServerInfo(serverIP, serverIP, ServerInfo.ServerType.OTHER);
                        ConnectScreen.connect(this, mc, ServerAddress.parse(serverIP), info, false, null);
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(String.format("Join the server %s.", serverIP));  // Show tooltip when hovered
                    }
                    ImGui.popID();  // Reset the ID stack

                    ImGui.sameLine();

                    ImGui.pushID("console_" + serverIP);  // Unique ID for the "Console" button
                    if (ImGui.button("Console")) {  // Same label for all buttons, but unique IDs
                        ethanolConsole = new EthanolConsole(server);
                        showConsole = true;
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(String.format("Open the ethanol console for %s.", serverIP));  // Show tooltip when hovered
                    }
                    ImGui.popID();  // Reset the ID stack

                    ImGui.sameLine();

                    ImGui.pushID("add_" + serverIP);  // Unique ID for the "Console" button
                    if (ImGui.button("Add")) {  // Same label for all buttons, but unique IDs
                        final ServerList serverList = new ServerList(MinecraftClient.getInstance());
                        serverList.loadFile();
                        serverList.add(new ServerInfo(
                                serverIP,
                                serverIP,
                                ServerInfo.ServerType.OTHER
                        ), false);
                        serverList.saveFile();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(String.format("Add %s to your multiplayer server list.", serverIP));  // Show tooltip when hovered
                    }
                    ImGui.popID();  // Reset the ID stack

                    ImGui.sameLine();

                    ImGui.pushID("copy_" + serverIP);  // Unique ID for the "Console" button
                    if (ImGui.button("Copy")) {  // Same label for all buttons, but unique IDs
                        mc.keyboard.setClipboard(serverIP);
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(String.format("Copy %s to your clipboard", serverIP));  // Show tooltip when hovered
                    }
                    ImGui.popID();  // Reset the ID stack
                }

                ImGui.endTable();
            }
        }

        ImGui.end(); // End main window
    }
}