package dev.siea.discord2fa.paper.adapter;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/** ConfigAdapter backed by Bukkit FileConfiguration (e.g. plugin.getConfig()). */
public final class PaperConfigAdapter implements ConfigAdapter {

    private final FileConfiguration config;

    public PaperConfigAdapter(FileConfiguration config) {
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
        return config.getStringList(key);
    }
}
