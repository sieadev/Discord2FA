package dev.siea.discord2fa.common.i18n;

import java.io.InputStream;

/**
 * Loads resources (e.g. lang files) from the plugin jar. Implemented by each
 * platform so that common can copy bundled lang files to disk without
 * depending on Bukkit/Bungee/Velocity APIs.
 */
public interface ResourceLoader {

    /**
     * Open a resource from the plugin jar. Path is relative to the jar root,
     * e.g. "lang/en.yml". Returns null if the resource does not exist.
     */
    InputStream getResource(String path);
}
