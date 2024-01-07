package dev.siea.discord2fa.database.models;
import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.discord.DiscordBot;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class Account {
    private final String discordID;
    private User user;
    private final OfflinePlayer player;
    public Account(String discordID, String minecraftUUID) {
        this.user = DiscordBot.getShardManager().retrieveUserById(discordID).complete();
        this.discordID = discordID;
        this.player = Discord2FA.getPlugin().getServer().getOfflinePlayer(UUID.fromString(minecraftUUID));
    }

    public String getDiscordID() {
        return discordID;
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
