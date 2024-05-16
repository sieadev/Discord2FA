package dev.siea.discord2fa.storage.settings;

import dev.siea.discord2fa.util.ConfigUtil;
import org.bukkit.plugin.Plugin;

public class UserDataStorage {
    private final ConfigUtil config;

    public UserDataStorage(Plugin plugin) {
        config = new ConfigUtil(plugin, "ipaddresses.yml");
    }

    public String getLatestIP(String uuid){
        return config.getConfig().getString(uuid);
    }

    public void setIP(String uuid, String ip){
        config.getConfig().set(uuid, ip);
        config.save();
    }
}
