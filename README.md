# Pretty Meteors with Trails

Pretty Meteors with Trails adds deterministic, server-authoritative meteor
showers rendered high across the night sky. Version `2.0.0+mc26.2` supports
Minecraft Java 26.2 on Fabric, Forge, and NeoForge and requires Seamless API
2.x.

The project is split into loader-neutral `common` gameplay/rendering code and
small `fabric`, `forge`, and `neoforge` adapters. Architectury Loom is build
tooling only; Architectury API is not a runtime dependency.

## Commands

- `/prettymeteors start [single|small|medium|large]`
- `/prettymeteors stop`
- `/prettymeteors status`
- `/prettymeteors schedule enable|disable`

Commands require permission level 2. Nightly automatic showers can also be
configured through the loader-specific configuration adapter.

## Build

Use Java 25 and run:

```text
gradlew.bat clean check build
```

The build uses the sibling `Seamless-API` repository as a pinned Gradle
composite. Loader jars are written to each loader module's `build/libs`
directory, and the root verification task rejects mixed loader metadata.

See [meteor rendering parity](docs/meteor-rendering-parity.md) for the
reference behavior, Minecraft 26.2 rendering adaptations, and the regression
tests that protect the shower's altitude, scale, and spread.

`check` also starts an isolated Fabric GameTest server. It verifies immediate
join synchronization through a real embedded connection, exact active-state
payload conversion, codec round trips, and stop-state synchronization. Forge
and NeoForge compile the same common state path with loader-native login and
dimension-change hooks.

See [PORTING.md](PORTING.md) before changing Minecraft or loader versions and
[MIGRATION.md](MIGRATION.md) for the 2.0 compatibility notes.
