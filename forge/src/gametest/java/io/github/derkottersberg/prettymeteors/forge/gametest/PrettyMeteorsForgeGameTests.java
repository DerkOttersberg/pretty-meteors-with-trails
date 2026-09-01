package io.github.derkottersberg.prettymeteors.forge.gametest;

import com.derko.prettymeteors.gametest.MeteorGameTestScenario;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;

@GameTestNamespace("prettymeteors")
public final class PrettyMeteorsForgeGameTests {
    private PrettyMeteorsForgeGameTests() {
    }

    @GameTest(name = "synchronizes_active_shower_and_codec", maxTicks = 40)
    public static void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        MeteorGameTestScenario.synchronizesActiveShowerAndCodec(helper);
    }
}
