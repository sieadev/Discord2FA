package dev.siea.common.storage.settings;

import dev.siea.common.Common;
import dev.siea.common.util.ConfigUtil;
import org.spongepowered.configurate.serialize.SerializationException;

import java.nio.file.Path;

public class UserDataStorage {
    private final ConfigUtil config;

    public UserDataStorage(Path dir) {
        config = new ConfigUtil(dir, "ipaddresses.yml");
    }

    public String getLatestIP(String uuid){
        return config.getNode().node(uuid).getString();
    }

    public void setIP(String uuid, String ip){
        try {
            config.getNode().node(uuid).set(ip);
            config.save();
        } catch (SerializationException e) {
            Common.getInstance().log("Could not save ip for " + uuid);
        }
    }
}
