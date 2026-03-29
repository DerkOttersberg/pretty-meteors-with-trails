# Pretty Meteors With Trails

This Fabric 1.21.11 mod implements a command-triggered meteor shower using a sky-layer renderer rather than physical meteor entities.

Design notes:
- Meteors are rendered around the camera, so they are not limited by chunk or entity render distance.
- The effect is intentionally above-cloud and skybox-like.
- The trail look is inspired by the observed behavior of the reference mod, but the code and rendering assets here are original.

Commands:
- `/prettymeteors start`
- `/prettymeteors start <durationSeconds> <meteorsPerSecond> <baseSpeed> <yawDegrees> <pitchDegrees> <spreadDegrees>`
- `/prettymeteors stop`
- `/prettymeteors status`