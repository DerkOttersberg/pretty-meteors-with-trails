# Migration to 2.0.0 for Minecraft 26.2

## Minecraft 1.20.1 backport

This branch targets Java 17, Fabric and Forge only. Its binary/network/Minecraft
types are not compatible with 26.x jars. Install the matching 1.20.1 Seamless API
2.x dependency; never mix Minecraft lines even when a mod's semantic version is
the same. Config/registry names are retained, but this is **not** a world-downgrade
tool. Do not open a 26.x save in 1.20.1. Use a copied existing 1.20.1 world for
upgrade testing and keep original saves/configs backed up.

The notes below describe the shared modernization and retained compatibility
contracts; references to newer loader/version behavior belong to those branches.


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
