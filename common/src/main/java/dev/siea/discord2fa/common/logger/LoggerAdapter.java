package dev.siea.discord2fa.common.logger;

/**
 * Logging adapter for use in common code. Implementations wrap the platform logger
 * (e.g. {@link java.util.logging.Logger}, SLF4J, or BungeeCord's logger) and are
 * passed in by the game/proxy server so that log output goes to the correct place.
 */
public interface LoggerAdapter {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);

    /**
     * Log an error with an associated throwable.
     */
    void error(String message, Throwable throwable);
}
