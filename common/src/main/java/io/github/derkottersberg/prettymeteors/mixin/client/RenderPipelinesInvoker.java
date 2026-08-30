package io.github.derkottersberg.prettymeteors.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesInvoker {
    @Invoker("register")
    static RenderPipeline prettymeteors$register(RenderPipeline pipeline) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
