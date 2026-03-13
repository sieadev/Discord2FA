package dev.siea.discord2fa.common.versioning;

/**
 * Reads the plugin version from the JAR manifest (Implementation-Version).
 * When common is shaded into a platform jar, this class is in that jar, so
 * the version comes from the built plugin jar—no platform-specific code needed.
 */
public final class PluginVersion {
    /**
     * Returns the version from the manifest of the jar that contains this class,
     * or empty string if not set (e.g. in development).
     */
    public static String get() {
        Package p = PluginVersion.class.getPackage();
        String v = p != null ? p.getImplementationVersion() : null;
        return v != null ? v : "";
    }
}
