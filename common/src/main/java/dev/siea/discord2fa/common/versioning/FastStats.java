package dev.siea.discord2fa.common.versioning;

/**
 * FastStats plugin ID. Used by each platform to start Metrics.
 * Common does not depend on FastStats; platforms add the appropriate FastStats-* dependency and one startup line.
 */
public final class FastStats {
    private FastStats() {}


    public static final String id = "d6f963b1c383cb44c3621dead5f25255";
}
