package net.kawaii.mixin;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {
    private boolean shouldOverride = true;
    private final Random random = new Random();

    private final List<String> splashes = CustomSplashes();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<SplashTextRenderer> cir) {
        if (shouldOverride) cir.setReturnValue(new SplashTextRenderer(splashes.get(random.nextInt(splashes.size()))));
        shouldOverride = random.nextBoolean();
    }

    private static List<String> CustomSplashes() {
        return Arrays.asList(
                "imGUI: Where buttons wear hats!",
                "imGUI: Turning pixels into party favors!",
                "imGUI: Making UIs dance the chicken dance!",
                "imGUI: Where even creepers appreciate the design!",
                "imGUI: Because even skeletons need a user-friendly interface!",
                "imGUI: Where pigs fly and UIs sing!",
                "imGUI: UIs with pizzazz!",
                "imGUI: Pixels partying!",
                "imGUI: Creeper-approved design!",
                "imGUI: Skeleton-friendly UI!",
                "imGUI: Where pigs fly!",
                "imGUI: UIs that sing!",
                "imGUI: Stylish sheep!",
                "imGUI: Crafting craziness!",
                "imGUI: Pixel fun!",
                "imGUI: Serious UI silliness!"
        );
    }

}