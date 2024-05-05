package dev.siea.discord2fa.storage.models;
import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.discord.DiscordBot;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Account {
    private final String discordID;
    private final OfflinePlayer player;

    public Account(String discordID, String minecraftUUID) {
        this.discordID = discordID;
        this.player = Discord2FA.getPlugin().getServer().getOfflinePlayer(UUID.fromString(minecraftUUID));
    }

    public String getDiscordID() {
        return discordID;
    }

    public CompletableFuture<User> getUser() {
        CompletableFuture<User> future = new CompletableFuture<>();
        DiscordBot.getShardManager().retrieveUserById(discordID).queue(
                user -> future.complete(user),
                future::completeExceptionally
        );
        return future;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }
}
