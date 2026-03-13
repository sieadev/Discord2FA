package dev.siea.discord2fa.common.i18n;

/**
 * Provides localized messages by key. Used by BaseServer and other common code
 * so that platforms never handle message strings directly.
 */
public interface MessageProvider {

    /**
     * Get a message by key. If no message is specified for the key, the fallback (e.g. default.yml) is used.
     * If that does not exist either, returns the key.
     */
    String get(String key);
}
