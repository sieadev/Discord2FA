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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseServer {
    private final LoggerAdapter logger;
    private final DatabaseAdapter databaseAdapter;
    private final DiscordBot discordBot;
    private final ServerConfig serverConfig;
    private final MessageProvider messageProvider;

    /** Players currently verifying. Removed only when they verify. */
    private final Map<UUID, CommonPlayer> verifyingPlayers = new ConcurrentHashMap<>();

    /** Command labels that this server handles. Platforms should register these and pass execution here. */
    public static final List<String> HANDLED_COMMANDS = Collections.unmodifiableList(Arrays.asList("link", "unlink"));

    public BaseServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider) {
        this.messageProvider = messageProvider != null ? messageProvider : k -> k;
        this.logger = logger;
        this.databaseAdapter = new DatabaseAdapter(configProvider);
        this.discordBot = new DiscordBot(configProvider, messageProvider, databaseAdapter);
        this.serverConfig = new ServerConfig(configProvider);
        purgeOldSignInLocationsAsync();
        checkForUpdates();
    }

    /** Purges sign-in locations older than 30 days asynchronously so startup is not blocked. */
    private void purgeOldSignInLocationsAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                int deleted = databaseAdapter.purgeSignInLocationsOlderThan(30);
                if (deleted > 0) {
                    logger.info("Purged " + deleted + " sign-in location(s) older than 30 days.");
                }
            } catch (Exception e) {
                logger.error("Failed to purge old sign-in locations: " + e.getMessage());
            }
        });
    }

    /**
     * Register a player as verifying. Called from platform join/login events.
     * When they verify, they are removed via the onVerified callback.
     * If force-link is disabled and the player has no linked Discord account, they are not added.
     * If rememberSignInLocation is enabled and the same IP+version signed in within 30 days, they are not added (skip verify).
     */
    protected final void addPlayer(CommonPlayer player) {
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
    public final boolean onEvent(UUID uuid, EventType eventType) {
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
     * Dispatches a command from the platform. Call this when a player runs one of {@link #HANDLED_COMMANDS}.
     * Performs the verification gate (if player is verifying and command not in allowedCommands, sends message and returns true).
     * Then runs the appropriate handler for "link" or "unlink". Returns true if the command was handled (so the platform should not pass to other handlers).
     */
    public final boolean handleCommand(CommonPlayer player, String commandLabel, List<String> args) {
        String label = commandLabel == null ? "" : commandLabel.trim().toLowerCase(java.util.Locale.ROOT);
        if (verifyingPlayers.containsKey(player.getUniqueId()) && !serverConfig.isCommandAllowed(label)) {
            LinkedPlayer linked = databaseAdapter.getLinkedPlayer(player.getUniqueId());
            player.sendMessage(linked != null ? messageProvider.get("notVerified") : messageProvider.get("forceLink"));
            return true;
        }
        return switch (label) {
            case "link" -> {
                handleLinkCommand(player, args.isEmpty() ? "" : args.get(0));
                yield true;
            }
            case "unlink" -> {
                handleUnlinkCommand(player);
                yield true;
            }
            default -> false;
        };
    }

    /**
     * Handle the /link &lt;code&gt; command. Call this when the player runs link with a code from Discord.
     * Sends the appropriate message (noCode, invalidCode, linkSuccess) to the player.
     * If the player was in the verifying state, removes them and calls onVerified so they can play.
     */
    private void handleLinkCommand(CommonPlayer player, String code) {
        if (code == null || code.isBlank()) {
            player.sendMessage(messageProvider.get("noCode"));
            return;
        }
        var discordUser = discordBot.consumeLinkCode(code);
        if (discordUser.isEmpty()) {
            player.sendMessage(messageProvider.get("invalidCode"));
            return;
        }
        long discordId = discordUser.get().getId();
        UUID playerUuid = player.getUniqueId();
        databaseAdapter.saveLinkedPlayer(new LinkedPlayer(playerUuid, discordId, Instant.now()));
        player.sendMessage(messageProvider.get("linkSuccess"));
        if (verifyingPlayers.remove(playerUuid) != null) {
            player.setLinkedPlayer(databaseAdapter.getLinkedPlayer(playerUuid));
            player.onVerified();
        }
    }

    /**
     * Handles the /unlink command. Only allowed when the player is verified (not in the verifying state).
     * If they are still verifying, sends notVerified. If not linked, sends notLinked. Otherwise removes the link and sends unlinkSuccess.
     */
    private void handleUnlinkCommand(CommonPlayer player) {
        if (verifyingPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(messageProvider.get("notVerified"));
            return;
        }
        if (databaseAdapter.getLinkedPlayer(player.getUniqueId()) == null) {
            player.sendMessage(messageProvider.get("notLinked"));
            return;
        }
        databaseAdapter.removeLinkedPlayer(player.getUniqueId());
        player.setLinkedPlayer(null);
        player.sendMessage(messageProvider.get("unlinkSuccess"));
    }

    /**
     * Called by platform listeners for command events. Not in map = allow.
     * In map = check allowedCommands whitelist.
     */
    public final boolean onCommand(UUID uuid, String commandLabel) {
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
     * Returns the message to send when a command is denied (player is verifying and command not in allowedCommands).
     * Returns null if the command is allowed or the player is not in the verifying state. Used by platforms to send the message when cancelling the command.
     */
    public final String getCommandDeniedMessage(UUID uuid, String commandLabel) {
        if (!verifyingPlayers.containsKey(uuid)) return null;
        if (serverConfig.isCommandAllowed(commandLabel == null ? "" : commandLabel.trim().toLowerCase(java.util.Locale.ROOT))) return null;
        LinkedPlayer linked = databaseAdapter.getLinkedPlayer(uuid);
        return linked != null ? messageProvider.get("notVerified") : messageProvider.get("forceLink");
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

