package dev.siea.discord2fa.database;

import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class AccountUtil {
    public static boolean isLinked(Player player) {
        try {
            return Database.findAccountByUUID(player.getUniqueId().toString()) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void linkAccount(Player player, String discordID) throws SQLException {
        Database.createAccount(player.getUniqueId().toString(), discordID);
    }

    public static boolean isLinkedByDiscord(Member member) {
        try {
            return Database.findAccountByDiscordID(member.getId()) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void unlinkAccount(Player player) {
        try {
            Database.deleteAccount(player.getUniqueId().toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
