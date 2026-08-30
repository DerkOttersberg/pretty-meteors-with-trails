# Contributing

Use Java 25 and place loader-neutral scheduling, payload, state, and rendering
logic in `common`. Loader modules should contain only lifecycle, networking,
command, and render-hook adapters.

Before submitting a change, run:

```text
gradlew.bat clean check build
```

Add or update tests for scheduling, placement math, payload fields, and
connection synchronization. For rendering changes, run the combined Fabric,
Forge, and NeoForge profiles under OpenGL and Vulkan and compare against
[docs/meteor-rendering-parity.md](docs/meteor-rendering-parity.md). Never trade
the original wide, high sky field for more trails inside one fixed camera view.
