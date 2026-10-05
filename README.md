# Pretty Meteors with Trails

Minecraft **1.21.1**, Java **21**; separate **Fabric, Forge and NeoForge** jars.
Version `2.0.2+mc1.21.1`. Never mix these with 1.20.1 or 26.x binaries.

Synchronized, server-authoritative meteor showers. Operator commands: `/prettymeteors start [single|small|medium|large]`, `/prettymeteors stop`, `/prettymeteors status`, `/prettymeteors schedule enable|disable`. Requires matching SeamlessLib 2.x.

## Build and architecture

`common` holds loader-neutral code, resources and tests; `fabric`, `forge`
and `neoforge` explicitly inject their platform services. Architectury Loom
is build tooling only, not a runtime API. Pins are in
`gradle/libs.versions.toml`. Gameplay composite builds use a sibling
`seamless-api` checkout for the matching Minecraft line; the library is not shaded.

Run Gradle with Java 25 installed; source/game tasks use Java 21:

```text
gradlew.bat clean check build
```

Distribute only remapped
`<loader>/build/libs/pretty-meteors-with-trails-2.0.2+mc1.21.1-<loader>.jar`.
Dev/QA jars are not release files. `check` runs common tests/isolation,
applicable loader GameTests with discovery guards, and all-loader jar checks.

## Icons and settings

All loaders reference the current CurseForge project PNG, bundled locally.
Source URLs and SHA-256 are in `gradle/icon-provenance.json`; do not replace
this artwork by running historical SVG generators. Fabric gameplay settings
use optional Mod Menu 11.0.5; Forge/NeoForge use native Mods-menu adapters.
SeamlessLib is a library with no gameplay settings screen.

## Status and migration

Local clean builds pass across the suite: 90 unit tests and 86 loader GameTests.
Client/UI, multiplayer, packaged-server and optional-JEI acceptance is separate:
see [.github/RELEASE_ACCEPTANCE.md](.github/RELEASE_ACCEPTANCE.md).
Build success is not production readiness, a GitHub push or a CurseForge release.
See [PORTING.md](PORTING.md) and [MIGRATION.md](MIGRATION.md).
Upgrade only backup copies of worlds/configs.

## License

Existing All Rights Reserved licensing is unchanged, as declared in loader metadata.
