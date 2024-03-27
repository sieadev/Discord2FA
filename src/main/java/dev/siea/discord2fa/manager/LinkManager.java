package dev.siea.discord2fa.manager;

import dev.siea.discord2fa.discord.DiscordUtils;
import dev.siea.discord2fa.message.Messages;
import dev.siea.discord2fa.storage.StorageManager;
import dev.siea.discord2fa.storage.models.Account;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;
import java.sql.SQLException;
import java.util.HashMap;

public class LinkManager {
    private static final HashMap<String, Member> linking = new HashMap<>();
    public static void queLink(Member member, String code) {
        linking.put(code, member);
    }

    public static void tryLink(Player player, String code) {
        if (linking.containsKey(code)) {
            Member member = linking.get(code);
            linking.remove(code);
            try {
                StorageManager.linkAccount(player, member.getId());
                DiscordUtils.giveRole(member.getId());
                player.sendMessage(Messages.get("linkSuccess").replace("%member%", member.getEffectiveName()));
            } catch (SQLException e) {
                player.sendMessage("§cAn error occurred while linking your account! Contact an administrator!: " + e.getMessage());
            }
        } else {
           player.sendMessage(Messages.get("invalidCode"));
        }
    }

    public static HashMap<String, Member> getLinking() {
        return linking;
    }

    public static void unlink(Player player) {
        Account account = StorageManager.findAccountByUUID(player.getUniqueId().toString());
        StorageManager.unlinkAccount(player);
        DiscordUtils.giveRole(account.getDiscordID(),false);
    }
}
