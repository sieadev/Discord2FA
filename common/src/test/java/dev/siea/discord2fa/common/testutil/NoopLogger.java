package dev.siea.discord2fa.common.testutil;

import dev.siea.discord2fa.common.logger.LoggerAdapter;

public final class NoopLogger implements LoggerAdapter {
    @Override public void debug(String message) { }
    @Override public void info(String message) { }
    @Override public void warn(String message) { }
    @Override public void error(String message) { }
    @Override public void error(String message, Throwable throwable) { }
}
