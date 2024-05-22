package dev.siea.common.storage.file;

import dev.siea.common.Common;
import dev.siea.common.storage.models.Account;
import dev.siea.common.util.ConfigUtil;
import org.spongepowered.configurate.serialize.SerializationException;

import java.nio.file.Path;
import java.util.Set;

public class FileWrapper {
    private final ConfigUtil config;

    public FileWrapper(Path dir){
        config = new ConfigUtil(dir, "Accounts.yml");
    }

    public Account findAccountByUUID(String uuid){
        String discordID = config.getNode().node(uuid).getString();
        if (discordID == null) return null;
        else{
            return new Account(discordID,uuid);
        }
    }

    public Account findAccountByDiscordID(String discordID){
        Set<Object> uuids = config.getNode().childrenMap().keySet();
        for (Object uuid : uuids) {
            String storedDiscordID = config.getNode().node(uuid).getString();
            if (storedDiscordID != null && storedDiscordID.equals(discordID)) {
                return new Account(discordID, (String) uuid);
            }
        }
        return null;
    }

    public void createAccount(String uuid, String discordId){
        try {
            config.getNode().node(uuid).set(discordId);
            config.save();
        } catch (SerializationException e) {
            Common.getInstance().log("Could not save account for " + discordId);
        }
    }

    public void deleteAccount(String uuid){
        try {
            config.getNode().node(uuid).set(null);
            config.save();
        } catch (SerializationException e) {
            Common.getInstance().log("Could not delete account for " + uuid);
        }
    }
}
