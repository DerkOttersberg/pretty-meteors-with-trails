# Migration to 2.0.0 for Minecraft 26.2

- Install the jar matching the active loader: Fabric, Forge, or NeoForge.
- Install Seamless API 2.x separately; it is no longer bundled or shaded.
- The compatibility ID remains `prettymeteors`, and the command root remains
  `/prettymeteors`.
- Existing worlds do not contain registered meteor blocks or entities, so no
  world registry remap is required.
- Shower behavior combines the nightly events from the historical main line
  with the Seamless API integration from the Fabric port.
- The 26.2 renderer preserves the original high, broad meteor field while
  replacing raw OpenGL assumptions with Minecraft render pipelines.

Back up copied worlds and configuration directories before testing any major
version upgrade. Do not use the only copy of a world for migration QA.
