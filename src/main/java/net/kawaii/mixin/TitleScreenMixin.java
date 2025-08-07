package net.kawaii.mixin;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kawaii.Client;
import net.kawaii.screens.EthanolServersScreen;
import net.kawaii.utils.EthanolSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.ethanol.ethanolapi.EthanolAPI;
import rocks.ethanol.ethanolapi.auth.DiscordAuthURL;
import rocks.ethanol.ethanolapi.structure.ThrowingConsumer;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {TitleScreen.class},
   priority = 1001
)
public abstract class TitleScreenMixin extends Screen {
   private CompletableFuture<Void> authFuture;

   protected TitleScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"addNormalWidgets"},
      at = @At("TAIL"), cancellable = true
   )
   private void addEthanolButton(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
      int newY = cir.getReturnValue() + spacingY; // Add below the last button

      this.addDrawableChild(ButtonWidget.builder(Text.of("Ethanol"), (button) -> {
         EthanolServersScreen multiplayerScreen = EthanolServersScreen.instance(this);
         ForkJoinPool.commonPool().submit(() -> {
            Text originalTitle = button.getMessage();

            if (EthanolSystem.apiKey.isEmpty()){
               button.active = false;
               button.setMessage(Text.of("Logging in..."));
               if (authFuture != null && !authFuture.isDone()) {
                  authFuture.cancel(true); // Cancel the previous authentication task
               }

               ThrowingConsumer<DiscordAuthURL, IOException> opener = url -> {
                  Util.getOperatingSystem().open(url.toURI());
               };

               authFuture = EthanolAPI.DEFAULT_AUTHENTICATOR.authenticateAsync(
                       60000, opener // Authentication with a timeout of 60 seconds
               ).thenAcceptAsync(result -> {
                  // Successful authentication callback
                  EthanolSystem.apiKey = result;
                  setScreen(button, originalTitle, multiplayerScreen);
               }).exceptionally(ex -> {
                  // Log or handle specific exceptions here if needed (e.g., timeout)
                  ex.printStackTrace();
                  return null; // Return null to indicate that we handled the exception
               });
            } else {
               setScreen(button, originalTitle, multiplayerScreen);
            }

         });


      }).width(200).dimensions(this.width / 2 - 100, newY, 200, 20).build());

      cir.setReturnValue(newY);
   }

   @Unique
   private void setScreen(ButtonWidget button, Text originalTitle, Screen multiplayerScreen){
      Client.mc.execute(() -> {
         button.active = true;
         button.setMessage(originalTitle);
         this.client.setScreen(multiplayerScreen);
      });
   }
}