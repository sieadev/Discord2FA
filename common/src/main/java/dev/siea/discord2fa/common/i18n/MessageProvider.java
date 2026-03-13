package dev.siea.discord2fa.common.i18n;

/**
 * Provides localized messages by key. Used by BaseServer and other common code
 * so that platforms never handle message strings directly.
 */
public interface MessageProvider {

    /**
     * Get a message by key. If the key is missing, returns the default value.
     */
    String get(String key, String defaultValue);
}
