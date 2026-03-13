package dev.siea.discord2fa.bungeecord.adapter;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import net.md_5.bungee.config.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** ConfigAdapter backed by BungeeCord Configuration. */
public final class BungeeConfigAdapter implements ConfigAdapter {

    private final Configuration config;

    public BungeeConfigAdapter(Configuration config) {
        this.config = config;
    }

    @Override
    public String getString(String key) {
        return config == null ? null : config.getString(key, null);
    }

    @Override
    public int getInt(String key) {
        return config == null ? 0 : config.getInt(key, 0);
    }

    @Override
    public boolean getBoolean(String key) {
        return config != null && config.getBoolean(key, false);
    }

    @Override
    public List<String> getStringList(String key) {
        if (config == null) return Collections.emptyList();
        List<?> list = config.getList(key, null);
        if (list == null) return Collections.emptyList();
        return list.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.toList());
    }
}
