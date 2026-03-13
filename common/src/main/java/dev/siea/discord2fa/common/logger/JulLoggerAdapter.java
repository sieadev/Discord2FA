package dev.siea.discord2fa.common.logger;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link LoggerAdapter} that delegates to a {@link java.util.logging.Logger}.
 * Use this for Spigot, Paper, and BungeeCord, which provide a JUL logger via the plugin API.
 */
public final class JulLoggerAdapter implements LoggerAdapter {

    private final Logger logger;

    public JulLoggerAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String message) {
        logger.log(Level.FINE, message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void error(String message) {
        logger.severe(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
