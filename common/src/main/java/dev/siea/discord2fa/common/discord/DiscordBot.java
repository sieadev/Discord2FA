package dev.siea.discord2fa.common.discord;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.database.models.SignInLocation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DiscordBot {
    private final DiscordConfig discordConfig;

    public DiscordBot(ConfigAdapter configAdapter) {
        this.discordConfig = new DiscordConfig(configAdapter);
    }

    public Future<Boolean> attemptVerify(LinkedPlayer linkedPlayer, SignInLocation signInLocation) {
        return CompletableFuture.completedFuture(false);
    }
}
