package dev.siea.discord2fa.storage;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.storage.database.Database;
import dev.siea.discord2fa.storage.file.FileUtil;
import dev.siea.discord2fa.storage.models.Account;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class StorageManager {
    private static StorageType storageType;
    private static FileUtil fileUtil;


    public static void init(Plugin plugin) {
        try{
            storageType = StorageType.valueOf(plugin.getConfig().getString("storage"));
        } catch (Exception e){
            if (plugin.getConfig().getBoolean("fileAsFallback")){
                storageType = StorageType.FILE;
                plugin.getLogger().severe("Switching to File Storage(fileAsFallback) due to invalid Storage Type!");
            }
            else{
                plugin.getLogger().severe(String.format("[%s] - Disabled due to invalid Storage Type! [THIS IS NOT A BUG DO NOT REPORT IT]", plugin.getDescription().getName()));
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }

        if (storageType == StorageType.MYSQL){
            try {
                Database.onEnable(plugin);
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("fileAsFallback")){
                    storageType = StorageType.FILE;
                    plugin.getLogger().severe("Switching to File Storage(fileAsFallback) due to being unable to connect to Database!");
                }
                else{
                    plugin.getLogger().severe(String.format("[%s] - Disabled due to being unable to connect to Database! [THIS IS NOT A BUG DO NOT REPORT IT]", plugin.getDescription().getName()));
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    return;
                }
            }
        }
        if (storageType == StorageType.FILE) {
            fileUtil = new FileUtil();
        }
    }

    public static boolean isLinked(Player player) {
        if (storageType == StorageType.MYSQL){
            try {
                return Database.findAccountByUUID(player.getUniqueId().toString()) != null;
            } catch (SQLException e) {
                return false;
            }
        }
        else if (storageType == StorageType.FILE){
            return fileUtil.findAccountByUUID(player.getUniqueId().toString())!= null;
        }
        return false;
    }

    public static void linkAccount(Player player, String discordID) throws SQLException {
        if (storageType == StorageType.MYSQL){
            Database.createAccount(player.getUniqueId().toString(), discordID);
        }
        else if (storageType == StorageType.FILE){
            fileUtil.createAccount(player.getUniqueId().toString(), discordID);
        }
    }

    public static boolean isLinkedByDiscord(Member member) {
        if (storageType == StorageType.MYSQL){
            try {
                return Database.findAccountByDiscordID(member.getId()) != null;
            } catch (SQLException e) {
                return false;
            }
        }
        else if (storageType == StorageType.FILE){
            fileUtil.findAccountByDiscordID(member.getId());
        }
        return false;
    }

    public static void unlinkAccount(Player player) {
        if (storageType == StorageType.MYSQL){
            try {
                Database.deleteAccount(player.getUniqueId().toString());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        else if (storageType == StorageType.FILE){
            fileUtil.deleteAccount(player.getUniqueId().toString());
        }
    }

    public static Account findAccountByUUID(String uuid) {
        if (storageType == StorageType.MYSQL){
            try {
                return Database.findAccountByUUID(uuid);
            } catch (SQLException e) {
                return null;
            }
        }
        else if (storageType == StorageType.FILE){
            return fileUtil.findAccountByUUID(uuid);
        }
        return null;
    }

    public static Account findAccountByDiscordID(String id) {
        if (storageType == StorageType.MYSQL){
            try {
                return Database.findAccountByDiscordID(id);
            } catch (SQLException e) {
                return null;
            }
        }
        else if (storageType == StorageType.FILE){
            return fileUtil.findAccountByDiscordID(id);
        }
        return null;
    }
}
