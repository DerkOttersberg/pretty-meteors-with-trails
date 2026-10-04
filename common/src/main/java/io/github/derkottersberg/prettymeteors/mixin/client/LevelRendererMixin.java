package io.github.derkottersberg.prettymeteors.mixin.client;

import com.derko.prettymeteors.client.MeteorShowerClientState;
import com.derko.prettymeteors.client.MeteorRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only emits geometry through vanilla buffers; no raw OpenGL calls. */
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void prettymeteors$renderTrails(PoseStack poses, float partialTick, long finishTime,
            boolean outline, Camera camera, GameRenderer renderer, LightTexture light,
            Matrix4f projection, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        var buffers = client.renderBuffers().bufferSource();
        var type = MeteorRenderTypes.trails();
        MeteorShowerClientState.INSTANCE.renderWorldPass(
                poses.last().pose(), buffers.getBuffer(type), renderer.getDepthFar());
        buffers.endBatch(type);
    }
}
