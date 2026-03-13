package dev.siea.discord2fa.common.server;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.logger.LoggerAdapter;
import dev.siea.discord2fa.common.database.DatabaseAdapter;
import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.common.discord.DiscordBot;
import dev.siea.discord2fa.common.event.EventType;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.common.versioning.UpdateCheckResult;
import dev.siea.discord2fa.common.versioning.UpdateChecker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseServer {
    private final LoggerAdapter logger;
    private final DatabaseAdapter databaseAdapter;
    private final DiscordBot discordBot;
    private final ServerConfig serverConfig;
    private final MessageProvider messageProvider;

    /** Players currently verifying. Removed only when they verify. */
    private final Map<UUID, CommonPlayer> verifyingPlayers = new ConcurrentHashMap<>();

    public BaseServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider) {
        this.logger = logger;
        this.databaseAdapter = new DatabaseAdapter(configProvider);
        this.discordBot = new DiscordBot(configProvider);
        this.serverConfig = new ServerConfig(configProvider);
        this.messageProvider = messageProvider != null ? messageProvider : k -> k;
        checkForUpdates();
    }

    /**
     * Register a player as verifying. Called from platform join/login events.
     * When they verify, they are removed via the onVerified callback.
     * If force-link is disabled and the player has no linked Discord account, they are not added.
     * If rememberSignInLocation is enabled and the same IP+version signed in within 30 days, they are not added (skip verify).
     */
    protected void addPlayer(CommonPlayer player) {
        if (player == null) return;

        LinkedPlayer linked = databaseAdapter.getLinkedPlayer(player.getUniqueId());
        if (linked != null) player.setLinkedPlayer(linked);

        if (!serverConfig.isForceLink() && linked == null) return;

        SignInLocation current = player.getSigninLocation();
        if (current != null && serverConfig.isRememberSignInLocation()
            && databaseAdapter.hasRecentSignInLocation(player.getUniqueId(), current.getIpAddress(), current.getVersion())) {
            return;
        }

        player.setOnVerifiedCallback(() -> verifyingPlayers.remove(player.getUniqueId()));
        verifyingPlayers.put(player.getUniqueId(), player);

        if (player.isLinked()) {
            discordBot.attemptVerify(player.getLinkedPlayer(), player.getSigninLocation())
                    .thenAcceptAsync(verified -> {
                        if (verified) {
                            player.sendMessage(messageProvider.get("verifySuccess"));
                            player.onVerified();
                        } else {
                            player.kick(messageProvider.get("verifyDenied"));
                        }
                    });
        } else {
            player.sendMessage(messageProvider.get("forceLink"));
        }
    }

    /**
     * Called by platform listeners for non-command events. Returns true to allow,
     * false to deny (e.g. cancel). Not in map = verified or unknown → allow.
     */
    public boolean onEvent(UUID uuid, EventType eventType) {
        CommonPlayer player = verifyingPlayers.get(uuid);
        if (player == null) return true;
        if (serverConfig.isEventAllowed(eventType)) return true;
        if (player.isLinked()) {
            player.sendMessage(messageProvider.get("notVerified"));
        } else {
            player.sendMessage(messageProvider.get("forceLink"));
        }
        return false;
    }

    /**
     * Called by platform listeners for command events. Not in map = allow.
     * In map = check allowedCommands whitelist.
     */
    public boolean onCommand(UUID uuid, String commandLabel) {
        CommonPlayer player = verifyingPlayers.get(uuid);
        if (player == null) return true;
        if (serverConfig.isCommandAllowed(commandLabel)) return true;
        if (player.isLinked()) {
            player.sendMessage(messageProvider.get("notVerified"));
        } else {
            player.sendMessage(messageProvider.get("forceLink"));
        }
        return false;
    }

    /**
     * Checks for updates and logs the result to the console. Called automatically from the constructor.
     * Version is read from the JAR manifest (Implementation-Version).
     */
    private void checkForUpdates() {
        UpdateCheckResult result = UpdateChecker.check();
        if (result == null) logger.error("Unable to retrieve version. Please report this!!!");
        else if (result.isUpToDate()) {
            logger.info("You are running the latest release of Discord2FA.");
        } else {
            logger.warn("You are " + result.getVersionsBehind() + " version(s) behind. Download the latest at " + result.getDownloadUrl());
        }
    }
}

