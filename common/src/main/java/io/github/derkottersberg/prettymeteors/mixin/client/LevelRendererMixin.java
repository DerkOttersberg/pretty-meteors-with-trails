package io.github.derkottersberg.prettymeteors.mixin.client;

import com.derko.prettymeteors.client.MeteorShowerClientState;
import com.derko.prettymeteors.client.MeteorRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits meteor trails through vanilla's backend-neutral feature pipeline. */
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Inject(method = "submitFeatures", at = @At("TAIL"))
    private void prettymeteors$submitTrails(
            LevelRenderState state,
            SubmitNodeCollector collector,
            boolean outlines,
            CallbackInfo callbackInfo) {
        collector.submitCustomGeometry(
                new PoseStack(),
                MeteorRenderTypes.trails(),
                (pose, consumer) -> MeteorShowerClientState.INSTANCE.renderWorldPass(
                        pose.pose(),
                        consumer,
                        state.cameraRenderState.depthFar));
    }
}
