# Pretty Meteors with Trails

This is the `26.3` source branch. For Minecraft 26.2, use the `26.2` branch;
all three modloaders are included in each version branch. Forge/NeoForge's
pinned 26.3 loaders are upstream beta builds. See [REPOSITORY_WORKFLOW.md](REPOSITORY_WORKFLOW.md).

Pretty Meteors with Trails adds deterministic, server-authoritative meteor
showers rendered high across the night sky. Version `2.0.2+mc26.3` supports
Minecraft Java 26.3 on Fabric, Forge, and NeoForge and requires Seamless API
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

The build uses the sibling `seamless-api` repository as a pinned Gradle
composite. Loader jars are written to each loader module's `build/libs`
directory, and the root verification task rejects mixed loader metadata.

See [meteor rendering parity](docs/meteor-rendering-parity.md) for the
reference behavior, Minecraft 26.3 rendering adaptations, and the regression
tests that protect the shower's altitude, scale, and spread.

`check` also starts the configured gameplay-test servers. They drive active and
inactive state plus a mock late-player sync through each loader's real
server-side network adapter (including channel negotiation), and verify exact
state conversion and codec round trips. Unit tests cover the pause-safe client
spawn budget; loader event hooks are exercised by client/server smoke testing.

See [PORTING.md](PORTING.md) before changing Minecraft or loader versions and
[MIGRATION.md](MIGRATION.md) for the 2.0 compatibility notes.

## License

**All Rights Reserved** for new original material owned by Derk Ottersberg.
See [LICENSE](LICENSE) and [licensing history](LICENSES/README.md) for prior-license and third-party exceptions.

Public source may be viewed and forked on GitHub. Issues and pull requests are welcome;
write access to this repository is reserved for the owner.
