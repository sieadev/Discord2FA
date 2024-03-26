package dev.siea.discord2fa.manager;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.storage.StorageManager;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

public class LinkManager {
    private static final String invalidCode = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.invalidCode")).replace("&", "§");
    private static final String linkSuccess = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.linkSuccess")).replace("&", "§");
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
                player.sendMessage(linkSuccess.replace("%member%", member.getEffectiveName()));
            } catch (SQLException e) {
                player.sendMessage("§cAn error occurred while linking your account! Contact an administrator!: " + e.getMessage());
            }
        } else {
           player.sendMessage(invalidCode);
        }
    }

    public static HashMap<String, Member> getLinking() {
        return linking;
    }

    public static void unlink(Player player) {
        StorageManager.unlinkAccount(player);
    }
}
