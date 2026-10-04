package com.derko.prettymeteors.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import java.util.List;

/** Vanilla render states: unfogged color shader, additive trails, conventional Z. */
public final class MeteorRenderTypes extends RenderType {
    private static final RenderType TRAILS = new MeteorRenderTypes(
            List.of(RenderStateShard.NO_TEXTURE, RenderStateShard.POSITION_COLOR_SHADER,
                    RenderStateShard.LIGHTNING_TRANSPARENCY, RenderStateShard.LEQUAL_DEPTH_TEST,
                    RenderStateShard.NO_CULL, RenderStateShard.NO_LIGHTMAP, RenderStateShard.NO_OVERLAY,
                    RenderStateShard.NO_LAYERING, RenderStateShard.MAIN_TARGET,
                    RenderStateShard.DEFAULT_TEXTURING, RenderStateShard.COLOR_WRITE,
                    RenderStateShard.DEFAULT_LINE, RenderStateShard.NO_COLOR_LOGIC));

    private MeteorRenderTypes(List<RenderStateShard> states) {
        super("prettymeteors:meteor", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 65536, false, true,
                () -> states.forEach(RenderStateShard::setupRenderState),
                () -> states.forEach(RenderStateShard::clearRenderState));
    }
    public static void initialize() {}
    public static RenderType trails() { return TRAILS; }
}
