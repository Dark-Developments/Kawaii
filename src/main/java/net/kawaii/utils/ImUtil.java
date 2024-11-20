package net.kawaii.utils;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.extension.implot.ImPlot;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static net.kawaii.Client.mc;

public class ImUtil {
    private static final ImGuiImplGlfw IM_GUI_IMPL_GLFW = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 IM_GUI_IMPL_GL_3 = new ImGuiImplGl3();

    public static void initImGUI(){
        ImGui.createContext();
        ImPlot.createContext();

        final ImGuiIO io = ImGui.getIO();
        io.setFontGlobalScale(1f);

        style();

        IM_GUI_IMPL_GLFW.init(MinecraftClient.getInstance().getWindow().getHandle(), true);
        IM_GUI_IMPL_GL_3.init();
    }

    public static void draw(Runnable runnable){
        IM_GUI_IMPL_GLFW.newFrame();
        ImGui.newFrame();

        runnable.run();

        ImGui.endFrame();
        ImGui.render();

        IM_GUI_IMPL_GL_3.renderDrawData(ImGui.getDrawData());
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
