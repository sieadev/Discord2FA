package dev.siea.common.storage.settings;

import dev.siea.common.util.ConfigUtil;

import java.nio.file.Path;

public class UserDataStorage {
    private final ConfigUtil config;

    public UserDataStorage(Path dir) {
        config = new ConfigUtil(dir, "ipaddresses.yml");
    }

    public String getLatestIP(String uuid){
        return config.getConfig().getString(uuid);
    }

    public void setIP(String uuid, String ip){
        config.getConfig().set(uuid, ip);
        config.save();
    }
}
