# Changelog

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
