package dev.siea.discord2fa.spigot.adapter;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/** ConfigAdapter backed by Bukkit FileConfiguration (e.g. plugin.getConfig()). */
public final class BukkitConfigAdapter implements ConfigAdapter {

    private final FileConfiguration config;

    public BukkitConfigAdapter(FileConfiguration config) {
        this.config = config;
    }

    @Override
    public String getString(String key) {
        return config.getString(key);
    }

    @Override
    public int getInt(String key) {
        return config.getInt(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    @Override
    public List<String> getStringList(String key) {
        List<String> list = config.getStringList(key);
        return list != null ? list : java.util.Collections.emptyList();
    }
}
