package net.kawaii.mixin;

import net.kawaii.Client;
import net.kawaii.screens.EthanolScreen;
import net.kawaii.utils.Utils;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(at = @At("HEAD"), method = "onKey", cancellable = true)
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci){
        if (Client.ScreenKeyBinding.matchesKey(key, scancode)){
            if (Utils.canOpenGuiMenu()) Client.mc.setScreen(EthanolScreen.INSTANCE);
        }
    }
}