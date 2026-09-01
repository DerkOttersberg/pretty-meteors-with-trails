package io.github.derkottersberg.prettymeteors.neoforge.gametest;

import com.derko.prettymeteors.gametest.MeteorGameTestScenario;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public final class PrettyMeteorsNeoForgeGameTests {
    private static final String MOD_ID = "prettymeteors";
    private static final String TEST_NAME = "synchronizes_active_shower_and_codec";
    private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(MOD_ID, TEST_NAME);
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, MOD_ID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SYNCHRONIZES_SHOWER =
            TEST_FUNCTIONS.register(TEST_NAME, () -> MeteorGameTestScenario::synchronizesActiveShowerAndCodec);

    private PrettyMeteorsNeoForgeGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(PrettyMeteorsNeoForgeGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MOD_ID, "default_environment"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                40,
                0,
                true);
        event.registerTest(TEST_ID, new FunctionGameTestInstance(SYNCHRONIZES_SHOWER.getKey(), data));
    }
}
