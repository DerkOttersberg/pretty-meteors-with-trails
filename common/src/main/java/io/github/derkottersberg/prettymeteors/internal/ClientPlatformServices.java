package io.github.derkottersberg.prettymeteors.internal;

/** Identifies the client loader adapter passed explicitly into common bootstrap code. */
public interface ClientPlatformServices {
    String loaderName();
}
