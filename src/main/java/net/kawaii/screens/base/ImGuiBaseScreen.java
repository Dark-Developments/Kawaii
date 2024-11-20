package net.kawaii.screens.base;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static net.kawaii.Client.mc;

public class ImGuiBaseScreen extends Screen {

    public ImGuiBaseScreen(String name) {
        super(Text.of(name));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (mc.world == null){
            super.render(context, mouseX, mouseY, delta);
        }
    }

}