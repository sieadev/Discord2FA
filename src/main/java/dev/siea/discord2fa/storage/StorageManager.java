package dev.siea.discord2fa.storage;

import dev.siea.discord2fa.discord.DiscordBot;
import dev.siea.discord2fa.storage.file.FileStorage;
import dev.siea.discord2fa.storage.mysql.MySQLStorage;
import dev.siea.discord2fa.storage.mysql.MySQLWrapper;
import dev.siea.discord2fa.storage.models.Account;
import dev.siea.discord2fa.storage.settings.UserDataStorage;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.Objects;

public class StorageManager {
    private static Storage accountStorage;
    private static UserDataStorage userDataStorage;

    public static void init(Plugin plugin) {
        try {
            StorageType storageType = StorageType.valueOf(plugin.getConfig().getString("storage"));
            switch (storageType) {
                case MYSQL:
                    MySQLWrapper.onEnable(plugin);
                    accountStorage = new MySQLStorage();
                    break;
                case FILE:
                    accountStorage = new FileStorage();
                    break;
                default:
                    throw new Exception("Storage type not supported");
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("fileAsFallback")) {
                plugin.getLogger().severe("Switching to File Storage (fileAsFallback) due to invalid Storage Type or connection failure!");
                accountStorage = new FileStorage();
            } else {
                throw new ExceptionInInitializerError(e);
            }
        }

        if (plugin.getConfig().getBoolean("rememberIPAddresses")) {
            userDataStorage = new UserDataStorage(plugin);
        }
    }

    public static void reload(Plugin plugin) {
        try{
            if (MySQLWrapper.getConnection() != null) {
                MySQLWrapper.onDisable();
            }
        } catch (SQLException ignore) {
        }
        userDataStorage = null;
        accountStorage = null;
        try {
            StorageType storageType = StorageType.valueOf(plugin.getConfig().getString("storage"));
            switch (storageType) {
                case MYSQL:
                    MySQLWrapper.onEnable(plugin);
                    accountStorage = new MySQLStorage();
                    break;
                case FILE:
                    accountStorage = new FileStorage();
                    break;
                default:
                    throw new Exception("Storage type not supported");
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("fileAsFallback")) {
                plugin.getLogger().severe("Switching to File Storage (fileAsFallback) due to invalid Storage Type or connection failure!");
                accountStorage = new FileStorage();
            } else {
                throw new ExceptionInInitializerError(e);
            }
        }

        if (plugin.getConfig().getBoolean("rememberIPAddresses")) {
            userDataStorage = new UserDataStorage(plugin);
        }
    }

    public static boolean isLinked(Player player) {
        return accountStorage.isLinked(player.getUniqueId().toString());
    }

    public static void linkAccount(Player player, String discordID) throws SQLException {
        accountStorage.linkAccount(player.getUniqueId().toString(), discordID);
    }

    public static boolean isLinkedByDiscord(Member member) {
        return accountStorage.isLinkedByDiscord(member.getId());
    }

    public static void unlinkAccount(Player player) {
        accountStorage.unlinkAccount(player.getUniqueId().toString());
    }

    public static Account findAccountByUUID(String uuid) {
        return accountStorage.findAccountByUUID(uuid);
    }

    public static Account findAccountByDiscordID(String id) {
        return accountStorage.findAccountByDiscordID(id);
    }

    public static void updateIPAddress(Player player){
        if (!rememberIPAddress()) return;
        userDataStorage.setIP(player.getUniqueId().toString(), Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress());
    }

    public static boolean isRemembered(Player player) {
        if (!rememberIPAddress()) return false;
        return Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress().equals(userDataStorage.getLatestIP(player.getUniqueId().toString()));
    }

    public static boolean rememberIPAddress(){
        return userDataStorage != null;
    }
}
