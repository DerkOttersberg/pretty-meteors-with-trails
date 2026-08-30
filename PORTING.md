# Porting Pretty Meteors with Trails

Minecraft, Java, loader, and build-tool versions live only in
`gradle/libs.versions.toml`. Update that catalog first, then compile `common`
against Minecraft's official names before touching loader adapters.

## Stable common behavior

- `MeteorShowerConfig.skyOriginY` preserves the legacy sky anchor.
- `MeteorShowerClientState.calculateStartPosition` preserves independent X/Z
  lane sampling, midpoint placement, and end-of-path sky clearance.
- `PrettyMeteorsMod` owns scheduling and dimension-keyed active state.
- `MeteorShowerPayload` is the sole network contract. Joining players and
  dimension changes must receive `PrettyMeteorsMod.statePayload` immediately;
  periodic broadcast is only a recovery mechanism.
- `PlatformServices` and `ClientPlatformServices` are passed explicitly. Do
  not add reflective or `ServiceLoader` discovery.

Do not add loader imports to `common`; `verifyCommonIsolation` rejects them.
Seamless API remains an external 2.x dependency and must not be shaded.

## Rendering boundary

Read [docs/meteor-rendering-parity.md](docs/meteor-rendering-parity.md) before
changing placement or geometry. Keep rendering on Minecraft/Blaze3D render
pipelines, with no raw OpenGL or backend-specific assumptions. Any visual
change must be checked on both OpenGL and Vulkan at ordinary terrain height.

## Port checklist

1. Update `gradle/libs.versions.toml` and resource pack metadata.
2. Run `gradlew.bat clean check build` on Java 25 (or the new target JDK).
3. Inspect all three jars for loader-metadata isolation and canonical names.
4. Boot dedicated servers and clients for Fabric, Forge, and NeoForge.
5. Verify start, stop, late join, reconnect, and dimension changes.
6. Compare large-shower altitude, spread, scale, color, and density with the
   pinned reference behavior under OpenGL and Vulkan.
7. Run the matching five-mod combined profiles and retain logs/screenshots.
