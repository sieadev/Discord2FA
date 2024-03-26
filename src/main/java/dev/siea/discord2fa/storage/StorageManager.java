package dev.siea.discord2fa.storage;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.storage.database.Database;
import dev.siea.discord2fa.storage.file.FileUtil;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class StorageManager {
    private static StorageType storageType;
    private static FileUtil fileUtil;


    public static void init(Plugin plugin) {
        storageType = StorageType.valueOf(plugin.getConfig().getString("storage"));
        fileUtil = new FileUtil();
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
}
