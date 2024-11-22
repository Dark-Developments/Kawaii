package net.kawaii.screens;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import net.kawaii.screens.base.ImGuiBaseScreen;
import net.minecraft.client.gui.DrawContext;
import rocks.ethanol.ethanolapi.EthanolAPI;
import rocks.ethanol.ethanolapi.server.connector.EthanolServerConnector;
import rocks.ethanol.ethanolapi.server.listener.EthanolServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EthanolConsole extends ImGuiBaseScreen {
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 300;
    private static final int INPUT_TEXT_LENGTH = 256;

    private final ImString consoleInputText = new ImString(INPUT_TEXT_LENGTH);
    private final List<String> consoleOutput = new ArrayList<>();
    private final String authKey;
    private final EthanolServer server;
    private EthanolServerConnector connector;

    public EthanolConsole(EthanolServer server) {
        super("EthanolConsole");
        this.server = server;
        this.authKey = server.getAuthentication();
        this.connector = EthanolAPI.connect(authKey);

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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    public void show() {
        if (authKey == null) return;

        ImGui.setNextWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT, ImGuiCond.FirstUseEver);

        if (!ImGui.begin("Console - " + server.getAddress())) {
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

            // Keep the input box focused and selected
            ImGui.setKeyboardFocusHere(0);
        }

        ImGui.end();
    }


    private void appendConsoleOutput(String message) {
        synchronized (consoleOutput) {
            consoleOutput.add(message);
        }
    }
}
