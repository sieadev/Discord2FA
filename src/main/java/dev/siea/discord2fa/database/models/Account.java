package dev.siea.discord2fa.database.models;
import dev.siea.discord2fa.Discord2FA;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Account {
    private final User user;
    private final OfflinePlayer player;
    public Account(String discordID, String minecraftUUID) {
        this.user = Discord2FA.getDiscordBot().getShardManager().getUserById(discordID);
        this.player = Discord2FA.getPlugin().getServer().getOfflinePlayer(UUID.fromString(minecraftUUID));
    }

    public String getDiscordID() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    public String getMinecraftUUID() {
        return player.getUniqueId().toString();
    }

    public OfflinePlayer getPlayer() {
        return player;
    }
}
