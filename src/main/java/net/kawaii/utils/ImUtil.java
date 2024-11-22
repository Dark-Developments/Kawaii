package net.kawaii.utils;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.extension.implot.ImPlot;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.MinecraftClient;

public class ImUtil {
    private static final ImGuiImplGlfw ImGLFW = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 ImGL3 = new ImGuiImplGl3();

    public static void initImGUI(){
        ImGui.createContext();
        ImPlot.createContext();

        style();

        ImGLFW.init(MinecraftClient.getInstance().getWindow().getHandle(), true);
        ImGL3.init();
    }

    public static void draw(Runnable runnable){
        ImGLFW.newFrame();
        ImGui.newFrame();

        runnable.run();

        ImGui.endFrame();
        ImGui.render();

        ImGL3.renderDrawData(ImGui.getDrawData());
    }

    public static void style(){
        final ImGuiStyle style = ImGui.getStyle();

        style.setWindowRounding(3F);
        style.setFrameRounding(2F);
        style.setPopupRounding(2F);
        style.setScrollbarRounding(9F);
        style.setGrabRounding(3F);
        style.setWindowBorderSize(0F);
    }
}
