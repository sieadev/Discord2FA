package dev.siea.discord2fa.common.versioning;

/**
 * bStats plugin ID. Used by each platform to start Metrics (e.g. {@code new Metrics(this, BStats.PLUGIN_ID)}).
 * Common does not depend on bStats; platforms add the appropriate bstats-* dependency and one startup line.
 */
public final class BStats {
    private BStats() {}

    /** bStats plugin ID (same for all platforms, or use per-platform constants if you register separately). */
    public static final int PLUGIN_ID = 21448;
}
