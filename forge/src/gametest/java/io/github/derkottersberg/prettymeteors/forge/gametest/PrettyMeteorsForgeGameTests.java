package io.github.derkottersberg.prettymeteors.forge.gametest;

import com.derko.prettymeteors.gametest.MeteorGameTestScenario;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;

@GameTestNamespace("prettymeteors")
public final class PrettyMeteorsForgeGameTests {
    public static void register(net.minecraftforge.eventbus.api.bus.BusGroup modBus) {
        net.minecraftforge.registries.RegisterEvent.getBus(modBus).addListener(event -> {
            if (event.getRegistryKey() == net.minecraft.core.registries.Registries.TEST_FUNCTION) {
                event.register(net.minecraft.core.registries.Registries.TEST_FUNCTION,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("prettymeteors", "synchronizes_active_shower_and_codec"),
                        () -> MeteorGameTestScenario::synchronizesActiveShowerAndCodec);
            }
        });
    }

    private PrettyMeteorsForgeGameTests() {
    }

    @GameTest(name = "synchronizes_active_shower_and_codec", maxTicks = 40)
    public static void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        MeteorGameTestScenario.synchronizesActiveShowerAndCodec(helper);
    }
}
