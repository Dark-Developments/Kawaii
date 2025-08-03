package net.kawaii.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.extension.implot.ImPlot;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImInt;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

public class ImUtil {
    private static final ImGuiImplGlfw ImGLFW = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 ImGL3 = new ImGuiImplGl3();
    private static boolean initialized = false;

    public static void initImGUI(){
        if (initialized) return;
        initialized = true;

        ImGui.createContext();
        ImPlot.createContext();

        ImGuiIO io = ImGui.getIO();
        ImFontAtlas fontAtlas = io.getFonts();
        fontAtlas.addFontDefault();
        fontAtlas.getTexDataAsRGBA32(new ImInt(), new ImInt()); // ← Required
        style();

        ImGLFW.init(MinecraftClient.getInstance().getWindow().getHandle(), true);
        ImGL3.init();
    }

    public static void draw(Runnable runnable){
        // DO NOT recreate the context here!
        ImGLFW.newFrame();
        ImGL3.newFrame();
        ImGui.newFrame();

        runnable.run();

        ImGui.endFrame();
        ImGui.render();

        RenderSystem.assertOnRenderThread();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

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
