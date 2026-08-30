# Meteor rendering parity for Minecraft 26.2

This port treats the meteor appearance in Fabric branch tip
`eeaf42b87c4053a57d782c998393a183c85f879b` as the visual reference. The
network/API integration in
`3065b77ff8dfdd6bb5adea6d0bb6955cddb33955` is retained, but it does not
replace the reference branch's shower generation or rendering calculations.

## Root cause of the low, small, clustered regression

Two temporary diagnostics changed spatial calculations while the Minecraft
26.2 render hook was being isolated:

| Calculation | Reference behavior | Diagnostic behavior | Visible consequence |
| --- | --- | --- | --- |
| Shower origin Y | `max(observerY + 150, 292)` | Raw observer Y | At ordinary terrain height, every trail moved about 228 blocks downward. |
| Horizontal placement | Independent X and Z samples in `[-laneSpread, +laneSpread]` | A direction-aligned band capped by `shellRadius` | A large shower changed from a 2,200 by 2,200 block field to a roughly 460-block-wide band. |
| Ribbon width | `trailWidth * clamp(distance / 100, 1, 10)` | Formula unchanged, but trails were pulled much closer | Distance scaling produced physically thinner ribbons, making the meteors look smaller as a secondary effect. |
| Depth comparison | Minecraft 26.2 reversed-Z `GREATER_THAN_OR_EQUAL` | Legacy normal-Z `LESS_THAN_OR_EQUAL` | Submitted quads failed against the depth buffer, leaving no visible trails. |
| Distant sky depth | Preserve the field's angular placement inside the camera far plane | Submit literal distances of up to 3,448 blocks | Render distance clipped the broad field and left only a sparse central subset. |

`shellRadius` is part of the legacy payload and remains available for
compatibility, but the reference implementation does **not** use it to clamp
trail placement. Large showers use `laneSpread=1100` and
`heightOffset=160`.

The port now centralizes the first formula in
`MeteorShowerConfig.skyOriginY` and the second in
`MeteorShowerClientState.calculateStartPosition`. Regression tests pin the
minimum Y anchor, full lane extent, midpoint back-offset, and end-of-path sky
clearance.

## Calculations retained from the reference renderer

The following behavior remains unchanged apart from official-name mappings:

- 70 percent fast/thin and 30 percent slower/longer style buckets;
- speed sampling, nonlinear lifetime selection, and quarter-life burnout;
- shallow pitch, shared shower yaw, and per-trail angular variation;
- path-midpoint back-offset and independent X/Z placement;
- teardrop width curve, trail opacity curve, warm head/deep-orange tail;
- distance-scaled ribbon width, two-sided quads, and the forward needle cap;
- spawn accumulation, active-trail limit, dimension cleanup, and seeded
  deterministic client generation.

For a large shower started near normal terrain, the restored runtime probe
reported an origin at Y=292 and initial trail starts at Y=447 through Y=578.
The first twelve sampled trails were 626 through 3,448 blocks from the camera,
which is consistent with the reference branch's intentionally broad and
sparse sky field.

## Required Minecraft 26.2 adaptation

The visual formulas did not need redesigning. The rendering backend did:

- Minecraft 26.2 receives the same position/color quad geometry through a
  backend-neutral `RenderPipeline` and `RenderType` rather than raw OpenGL.
- The pipeline keeps lightning-style alpha blending, reversed-Z depth test
  `GREATER_THAN_OR_EQUAL`, no depth writes, no culling, and no terrain fog.
- The pipeline is initialized during client bootstrap, before Minecraft's
  first shader/resource reload. Registering it lazily when the first meteor
  appeared caused the initial invisible-render failure.
- Minecraft 26.2 ties its far plane to render distance. For each trail, the
  renderer uniformly scales camera-relative head, tail, width, and segment
  geometry only when necessary to place it within 82 percent of that plane.
  Uniform scaling preserves screen angle, apparent size, shape, and motion;
  the server-authoritative world-space simulation and 2,200-by-2,200 spawn
  envelope remain unchanged. It also removes unwanted render-distance-driven
  density and altitude bias.
- Loader adapters only select lifecycle, packet, and world-render hooks. The
  meteor state, generation, and geometry remain in `common`.
