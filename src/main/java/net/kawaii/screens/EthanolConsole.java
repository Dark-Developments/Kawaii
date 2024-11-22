package net.kawaii.screens;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import rocks.ethanol.ethanolapi.EthanolAPI;
import rocks.ethanol.ethanolapi.server.connector.EthanolServerConnector;
import rocks.ethanol.ethanolapi.server.listener.EthanolServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EthanolConsole {
    private static final int WINDOW_WIDTH = 500, WINDOW_HEIGHT = 300, INPUT_TEXT_LENGTH = 256;
    private final ImString consoleInputText = new ImString(INPUT_TEXT_LENGTH);
    private final List<String> consoleOutput = new ArrayList<>();
    private final EthanolServer server;
    private EthanolServerConnector connector;
    private ImBoolean isOpen;
    private ImBoolean autoScroll = new ImBoolean(true); // Toggle for auto-scrolling

    public EthanolConsole(EthanolServer server) {
        this.server = server;
        this.connector = EthanolAPI.connect(server.getAuthentication());

        // Connect to the server asynchronously
        CompletableFuture<EthanolServerConnector> future = connector.startAsync();

        future.thenAccept(conn -> {
            appendConsoleOutput("Connected to the server!");

            // Listen for messages from the server
            conn.listen(message -> appendConsoleOutput("Server: " + message));

        }).exceptionally(ex -> {
            appendConsoleOutput("Failed to connect: " + ex.getMessage());
            return null;
        });

        // Add a shutdown hook to close the connector gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (!connector.isClosed()) {
                    connector.close();
                    appendConsoleOutput("Connector closed!");
                }
            } catch (Exception e) {
                appendConsoleOutput("Error closing connector: " + e.getMessage());
            }
        }));

        isOpen = new ImBoolean(true);
    }

    public void show() {
        if (!isOpen.get()) return;

        ImGui.setNextWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT, ImGuiCond.FirstUseEver);

        if (!ImGui.begin("Console - " + server.getAddress(), isOpen)) {
            EthanolScreen.INSTANCE.showConsole = false;
            ImGui.end();
            return;
        }

        // Create a scrollable area for the console output
        ImGui.beginChild("Output", 0, -ImGui.getFrameHeightWithSpacing() - 10, true);

        synchronized (consoleOutput) {
            for (String line : consoleOutput) {
                ImGui.text(line);
            }
        }

        // Automatically scroll to the bottom if auto-scrolling is enabled
        if (autoScroll.get()) {
            ImGui.setScrollY(ImGui.getScrollMaxY()); // Scroll to the bottom
        }

        ImGui.endChild();

        // Input box for user commands
        if (ImGui.inputText("Input", consoleInputText, ImGuiInputTextFlags.EnterReturnsTrue)) {
            String newText = consoleInputText.get();
            consoleInputText.set("");

            // Add input to the console output
            appendConsoleOutput("You: " + newText);

            // Send input to the server
            if (connector != null && !newText.trim().isEmpty()) {
                try {
                    connector.writeLine(newText);
                } catch (Exception e) {
                    appendConsoleOutput("Error sending message: " + e.getMessage());
                }
            }
        }

        ImGui.sameLine();
        ImGui.checkbox("Auto Scroll", autoScroll);

        ImGui.end();
    }

    private void appendConsoleOutput(String message) {
        synchronized (consoleOutput) {
            consoleOutput.add(message);
        }
    }
}
