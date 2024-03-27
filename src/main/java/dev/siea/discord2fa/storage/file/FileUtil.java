package dev.siea.discord2fa.storage.file;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.storage.models.Account;
import dev.siea.discord2fa.util.ConfigUtil;

import java.util.Set;

public class FileUtil {
    private final ConfigUtil config = new ConfigUtil(Discord2FA.getPlugin(), "Accounts.yml");
    public Account findAccountByUUID(String uuid){
        String discordID = config.getConfig().getString(uuid);
        if (discordID == null) return null;
        else{
            return new Account(discordID,uuid);
        }
    }

    public Account findAccountByDiscordID(String discordID){
        Set<String> uuids = config.getConfig().getKeys(false);
        for (String uuid : uuids) {
            String storedDiscordID = config.getConfig().getString(uuid);
            if (storedDiscordID != null && storedDiscordID.equals(discordID)) {
                return new Account(discordID, uuid);
            }
        }
        return null;
    }

    public void createAccount(String uuid, String discordid){
        config.getConfig().set(uuid, discordid);
        config.save();
    }

    public void deleteAccount(String uuid){
        config.getConfig().set(uuid, null);
        config.save();
    }
}
