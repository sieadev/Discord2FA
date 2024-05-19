package dev.siea.common.storage.models;

import dev.siea.common.Common;
import net.dv8tion.jda.api.entities.User;
import java.util.concurrent.CompletableFuture;

public class Account {
    private final String discordID;
    private final String minecraftUUID;

    public Account(String discordID, String minecraftUUID) {
        this.discordID = discordID;
        this.minecraftUUID = minecraftUUID;
    }

    public String getDiscordID() {
        return discordID;
    }

    public String getMinecraftUUID() {
        return minecraftUUID;
    }

    public CompletableFuture<User> getUser() {
        CompletableFuture<User> future = new CompletableFuture<>();
        Common.getInstance().getShardManager().retrieveUserById(discordID).queue(
                future::complete,
                future::completeExceptionally
        );
        return future;
    }
}
