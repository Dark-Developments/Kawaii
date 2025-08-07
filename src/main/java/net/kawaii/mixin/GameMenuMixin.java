package net.kawaii.mixin;

import net.kawaii.Client;
import net.kawaii.screens.EthanolServersScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuMixin extends Screen {

    protected GameMenuMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/GridWidget$Adder;add(Lnet/minecraft/client/gui/widget/Widget;I)Lnet/minecraft/client/gui/widget/Widget;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void oninitWidgets(CallbackInfo ci, GridWidget gridWidget, GridWidget.Adder adder){

        adder.add(ButtonWidget.builder(Text.literal("Ethanol"), (button) -> {
            Client.mc.setScreen(EthanolServersScreen.instance(this));
        }).width(204).build(), 2);
    }


}