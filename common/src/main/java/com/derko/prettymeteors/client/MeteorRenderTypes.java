package com.derko.prettymeteors.client;

import com.derko.prettymeteors.PrettyMeteorsMod;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.derkottersberg.prettymeteors.mixin.client.RenderPipelinesInvoker;
import io.github.derkottersberg.prettymeteors.mixin.client.RenderTypeInvoker;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Backend-neutral, fog-free pipeline for distant sky trails. */
public final class MeteorRenderTypes {
    private static final RenderType TRAILS = createTrails();

    private MeteorRenderTypes() {
    }

    /** Forces pipeline registration before Minecraft's first shader reload. */
    public static void initialize() {
        // Static initialization performs the registration. Keeping this method explicit
        // makes the required lifecycle ordering clear at the common client bootstrap.
    }

    public static RenderType trails() {
        return TRAILS;
    }

    private static RenderType createTrails() {
        RenderPipeline pipeline = RenderPipeline.builder()
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withLocation(PrettyMeteorsMod.id("pipeline/meteor"))
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                // Minecraft 26.2 uses a reversed-Z projection and clears depth to zero.
                // Match vanilla's world pipelines: larger depth values are closer.
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .withCull(false)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .build();

        RenderPipelinesInvoker.prettymeteors$register(pipeline);
        RenderSetup setup = RenderSetup.builder(pipeline).createRenderSetup();
        return RenderTypeInvoker.prettymeteors$create("prettymeteors:meteor", setup);
    }
}
