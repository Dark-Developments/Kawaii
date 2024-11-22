package net.kawaii.screens;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import net.kawaii.screens.base.ImGuiBaseScreen;
import net.minecraft.client.gui.DrawContext;
import rocks.ethanol.ethanolapi.EthanolAPI;
import rocks.ethanol.ethanolapi.server.connector.EthanolServerConnector;

import java.util.ArrayList;
import java.util.List;

public class EthanolConsole extends ImGuiBaseScreen {
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 300;
    private static final int INPUT_TEXT_LENGTH = 256;

    private final ImString consoleInputText = new ImString(INPUT_TEXT_LENGTH);
    private final List<String> consoleOutput = new ArrayList<>();
    private final String authKey;
    private EthanolServerConnector connector;


    public EthanolConsole(String authKey) {
        super("NewImGuiScreen");
        this.authKey = authKey;
        connector = EthanolAPI.connect(authKey);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    public void show() {
        if (authKey == null) return;

        ImGui.setNextWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT, ImGuiCond.FirstUseEver);

        if (!ImGui.begin("Console")) {
            ImGui.end();
            return;
        }

        ImGui.beginChild("Output", 0, -ImGui.getFrameHeightWithSpacing() - 10, true);
        for (String line : consoleOutput) {
            ImGui.text(line);
        }
        ImGui.endChild();

        if (ImGui.inputText("Input", consoleInputText, ImGuiInputTextFlags.EnterReturnsTrue)) {
            String newText = consoleInputText.get();

            consoleOutput.add(newText);
            consoleInputText.set("");
        }

        ImGui.end();
    }
}