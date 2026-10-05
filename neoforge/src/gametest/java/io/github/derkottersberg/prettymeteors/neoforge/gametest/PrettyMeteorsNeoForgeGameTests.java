package io.github.derkottersberg.prettymeteors.neoforge.gametest;

import com.derko.prettymeteors.gametest.MeteorGameTestScenario;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("prettymeteors")
@PrefixGameTestTemplate(false)
public final class PrettyMeteorsNeoForgeGameTests {
    public static void register(IEventBus bus) {
        bus.addListener((RegisterGameTestsEvent event) -> event.register(PrettyMeteorsNeoForgeGameTests.class));
    }
    @GameTest(template = "empty",timeoutTicks=40)
    public static void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        MeteorGameTestScenario.synchronizesActiveShowerAndCodec(helper);
    }
    @GameTest(template = "empty",timeoutTicks=40)
    public static void commandsRequireOperator(GameTestHelper helper) {
        MeteorGameTestScenario.commandsRequireOperator(helper);
    }
}
