package dev.siea.discord2fa.common.config;

import java.util.List;

public interface ConfigAdapter {
    String getString(String key);
    int getInt(String key);
    boolean getBoolean(String key);

    /**
     * List value (e.g. from config allowedActions, allowedCommands).
     * Return empty list if key is missing or not a list.
     */
    List<String> getStringList(String key);
}
