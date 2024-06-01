package dev.siea.common.storage;

import dev.siea.common.Common;
import dev.siea.common.storage.file.FileStorage;
import dev.siea.common.storage.models.Account;
import dev.siea.common.storage.mysql.MySQLStorage;
import dev.siea.common.storage.mysql.MySQLWrapper;
import dev.siea.common.storage.settings.UserDataStorage;
import net.dv8tion.jda.api.entities.Member;
import org.simpleyaml.configuration.ConfigurationSection;

import java.nio.file.Path;
import java.sql.SQLException;

public class CommonStorageManager {
    private Storage accountStorage;
    private UserDataStorage userDataStorage;
    private MySQLWrapper mySQLWrapper;
    private final Common common;

    public CommonStorageManager(Common common, Path dir) {
        ConfigurationSection config = common.getConfig().getConfig();
        this.common = common;
        try {
            String storageString = common.getConfigString("storage");

            if (storageString == null) {
                throw new Exception("Storage type not supported");
            }

            StorageType storageType = StorageType.valueOf(storageString);

            switch (storageType) {
                case MYSQL:
                    mySQLWrapper = new MySQLWrapper(common);
                    accountStorage = new MySQLStorage(mySQLWrapper);
                    break;
                case FILE:
                    accountStorage = new FileStorage(dir);
                    break;
                default:
                    throw new Exception("Storage type not supported");
            }
        } catch (Exception e) {
            if (config.getBoolean("fileAsFallback")) {
                common.log("Switching to File Storage (fileAsFallback) due to invalid Storage Type or connection failure!");
                accountStorage = new FileStorage(dir);
            } else {
                throw new ExceptionInInitializerError(e);
            }
        }

        if (config.getBoolean("rememberIPAddresses")) {
            userDataStorage = new UserDataStorage(dir);
        }
    }

    public void reload(Common common, Path dir) {
        try {
            StorageType storageType = StorageType.valueOf(common.getConfigString("storage"));
            switch (storageType) {
                case MYSQL:
                    mySQLWrapper = new MySQLWrapper(common);
                    accountStorage = new MySQLStorage(mySQLWrapper);
                    break;
                case FILE:
                    accountStorage = new FileStorage(dir);
                    break;
                default:
                    throw new Exception("Storage type not supported");
            }
        } catch (Exception e) {
            if (common.getConfig().getConfig().getBoolean("fileAsFallback")) {
                common.log("Switching to File Storage (fileAsFallback) due to invalid Storage Type or connection failure!");
                accountStorage = new FileStorage(dir);
            } else {
                throw new ExceptionInInitializerError(e);
            }
        }

        if (common.getConfig().getConfig().getBoolean("rememberIPAddresses")) {
            userDataStorage = new UserDataStorage(dir);
        }
    }

    public void disable(){
        try{
            if (mySQLWrapper.getConnection() != null) {
                mySQLWrapper.onDisable();
                common.log("Disabled Database wrapper...");
            }
        } catch (SQLException ignore) {
        }
    }

    public boolean isLinked(String uuid) {
        return accountStorage.isLinked(uuid);
    }

    public void linkAccount(String uuid, String discordID) {
        accountStorage.linkAccount(uuid, discordID);
    }

    public boolean isLinkedByDiscord(Member member) {
        return accountStorage.isLinkedByDiscord(member.getId());
    }

    public void unlinkAccount(String uuid) {
        accountStorage.unlinkAccount(uuid);
    }

    public Account findAccountByUUID(String uuid) {
        return accountStorage.findAccountByUUID(uuid);
    }

    public Account findAccountByDiscordID(String id) {
        return accountStorage.findAccountByDiscordID(id);
    }

    public void updateIPAddress(String uuid, String ip){
        if (!rememberIPAddress()) return;
        userDataStorage.setIP(uuid, ip);
    }

    public  boolean isRemembered(String uuid, String ip) {
        if (!rememberIPAddress()) return false;
        return ip.equals(userDataStorage.getLatestIP(uuid));
    }

    public boolean rememberIPAddress(){
        return userDataStorage != null;
    }
}
