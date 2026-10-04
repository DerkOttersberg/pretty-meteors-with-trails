package io.github.derkottersberg.prettymeteors.fabric.gametest;

import com.derko.prettymeteors.gametest.MeteorGameTestScenario;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("removal")
public final class PrettyMeteorsGameTests {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        MeteorGameTestScenario.synchronizesActiveShowerAndCodec(helper);
    }
}
