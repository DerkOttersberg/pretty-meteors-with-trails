# Contributing

For this branch, base changes on `1.20.1`, not `main` or a 26.x branch.
Java 25 hosts Gradle; Java 17 compiles and runs Minecraft. Supported loaders
are Fabric and Forge only. Keep a matching `1.20.1` API sibling checkout.


Use Java 25 and place loader-neutral scheduling, payload, state, and rendering
logic in `common`. Loader modules should contain only lifecycle, networking,
command, and render-hook adapters.

Before submitting a change, run:

```text
gradlew.bat clean check build
```

Add or update tests for scheduling, placement math, payload fields, and
connection synchronization. For rendering changes, run the combined Fabric,
and Forge profiles under OpenGL and compare against
[docs/meteor-rendering-parity.md](docs/meteor-rendering-parity.md). Never trade
the original wide, high sky field for more trails inside one fixed camera view.
