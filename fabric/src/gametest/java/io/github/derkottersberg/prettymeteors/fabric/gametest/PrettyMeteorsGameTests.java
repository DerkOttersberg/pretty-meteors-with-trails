package io.github.derkottersberg.prettymeteors.fabric.gametest;

import com.derko.prettymeteors.gametest.MeteorGameTestScenario;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("removal")
public final class PrettyMeteorsGameTests {
    @GameTest(maxTicks = 40)
    public void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        MeteorGameTestScenario.synchronizesActiveShowerAndCodec(helper);
    }
}
