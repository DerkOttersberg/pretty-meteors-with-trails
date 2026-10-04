# Changelog

## 2.0.2+mc1.20.1

- Meteor commands require operator permission (level 2 / singleplayer cheats).
  Ordinary multiplayer clients cannot start/stop showers or change server settings.
- Native Fabric/Forge regression tests cover command authorization.
- Backport the current shared gameplay architecture to Minecraft 1.20.1, Java 17,
  Fabric and Forge. NeoForge is intentionally excluded from this line.
- Restore remapped loader jars, mixin refmaps, legacy NBT/data formats and bounded
  networking without changing public compatibility or registry namespaces.
- Preserve current config migration, UI clarity and item-conservation safeguards.


## 2.0.2+mc26.3

- Adapt meteor rendering to Renderpearl pipelines and the required dynamic-transform/projection bindings. Preserve geometry, altitude, spread, scheduling, and synchronization. Fix Forge GameTest discovery.
- Minecraft 26.3 only, Java 25; Fabric, Forge, and NeoForge.
- Forge 66.0.9 and NeoForge 26.3.0.48-beta are upstream beta loaders.
- Existing 26.2 releases remain separate; no blanket 26.* compatibility.

## 2.0.1+mc26.2

- Added a dedicated Mods-menu icon on all loaders.
- Rebuilt the settings screen with responsive pages, visible ARGB labels, full
  hover explanations, explicit probability units, a live 100% total, and safe
  draft/Save/Cancel behavior across page changes and window resizing.
- Discarded paused or stalled world-time backlog instead of spawning it as a
  burst when client ticking resumes.
- Added a bounded, unit-tested meteor spawn budget and strict Fabric API
  metadata.
- Normalized the Seamless API checkout path for case-sensitive CI runners.

## 2.0.0+mc26.2

- Ported to Minecraft Java 26.2 and Java 25 on Fabric, Forge, and NeoForge.
- Unified scheduling, commands, active state, payloads, generation, and
  rendering calculations in a shared `common` module.
- Combined nightly shooting stars with Seamless API 2.x shower registration.
- Restored the original minimum sky anchor and independent 2,200-by-2,200
  large-shower placement envelope after diagnosing the temporary low,
  clustered rendering regression.
- Replaced raw OpenGL behavior with backend-neutral Minecraft render
  pipelines for OpenGL and Vulkan.
- Corrected the Minecraft 26.2 reversed-Z depth comparison that discarded
  valid meteor fragments, and projected distant trails to a safe sky depth so
  the full legacy field no longer depends on the player's render distance.
- Added immediate login and dimension-change synchronization on all loaders,
  retaining periodic state broadcast as recovery.
- Added unit tests for scheduling and meteor math plus a live Fabric GameTest
  for join synchronization and payload codec integrity.
