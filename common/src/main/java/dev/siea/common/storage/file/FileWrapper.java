package dev.siea.common.storage.file;

import dev.siea.common.storage.models.Account;
import dev.siea.common.util.ConfigUtil;
import java.nio.file.Path;
import java.util.Set;

public class FileWrapper {
    private final ConfigUtil config;

    public FileWrapper(Path dir){
        config = new ConfigUtil(dir, "Accounts.yml");
    }

    public Account findAccountByUUID(String uuid){
        String discordID = config.getConfig().getString(uuid);
        if (discordID == null) return null;
        else{
            return new Account(discordID,uuid);
        }
    }

    public Account findAccountByDiscordID(String discordID){
        Set<Object> uuids = config.getConfig().getKeys();
        for (Object uuid : uuids) {
            String storedDiscordID = config.getConfig().getString((String) uuid);
            if (storedDiscordID != null && storedDiscordID.equals(discordID)) {
                return new Account(discordID, (String) uuid);
            }
        }
        return null;
    }

    public void createAccount(String uuid, String discordId){
        config.getConfig().set(uuid, discordId);
        config.save();
    }

    public void deleteAccount(String uuid){
        config.getConfig().set(uuid, null);
        config.save();
    }
}
