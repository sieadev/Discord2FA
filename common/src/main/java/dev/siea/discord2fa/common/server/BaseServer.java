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
import dev.siea.discord2fa.common.versioning.PluginVersion;
import dev.siea.discord2fa.common.versioning.UpdateCheckResult;
import dev.siea.discord2fa.common.versioning.UpdateChecker;

import java.awt.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class BaseServer {
    private final LoggerAdapter logger;
    private final DatabaseAdapter databaseAdapter;
    private final DiscordBot discordBot;
    private final ServerConfig serverConfig;
    private final MessageProvider messageProvider;
    private boolean isUpToDate;

    /** Executor for blocking DB work so it doesn't block the server thread. */
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Discord2FA-db");
        t.setDaemon(true);
        return t;
    });
    /** Optional executor for running callbacks on the server/main thread (e.g. Bukkit main thread). If null, callbacks run on dbExecutor. */
    private final Executor serverExecutor;

    /** Players currently verifying. Removed only when they verify. */
    private final Map<UUID, CommonPlayer> verifyingPlayers = new ConcurrentHashMap<>();
    /** Players whose join is still being processed (async). Block actions until we decide skip or verify. */
    private final Set<UUID> pendingVerification = ConcurrentHashMap.newKeySet();

    /** Cached result of the last update check (startup); used by /discord2fa version. */
    private volatile UpdateCheckResult lastUpdateCheckResult;

    /** Command labels that this server handles. Platforms should register these and pass execution here. */
    public static final List<String> HANDLED_COMMANDS = Collections.unmodifiableList(Arrays.asList("link", "unlink"));

    public BaseServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider) {
        this(configProvider, logger, messageProvider, null);
    }

    /**
     * @param serverExecutor optional executor for running player-facing callbacks (sendMessage, onVerified) on the server/main thread. If null, callbacks run on the internal DB thread.
     * @param dataFolder     plugin data folder (where config.yml lives). When non-null, SQLite DB is stored here by default so no path need be set in config.
     */
    public BaseServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider, Executor serverExecutor, Path dataFolder) {
        this.messageProvider = messageProvider != null ? messageProvider : k -> k;
        this.logger = logger;
        this.databaseAdapter = new DatabaseAdapter(configProvider, dataFolder);
        this.discordBot = new DiscordBot(configProvider, messageProvider, databaseAdapter, logger);
        this.serverConfig = new ServerConfig(configProvider);
        this.serverExecutor = serverExecutor != null ? serverExecutor : dbExecutor;
        purgeOldSignInLocationsAsync();
        checkForUpdates();
    }

    /**
     * @param serverExecutor optional executor for running player-facing callbacks on the server/main thread. If null, callbacks run on the internal DB thread.
     */
    public BaseServer(ConfigAdapter configProvider, LoggerAdapter logger, MessageProvider messageProvider, Executor serverExecutor) {
        this(configProvider, logger, messageProvider, serverExecutor, null);
    }

    /**
     * Shuts down the Discord bot, closes the database, and stops executors. Call from plugin onDisable()
     * so the Discord API disconnects before the plugin classloader is closed (avoids "zip file closed" errors).
     * Safe to call multiple times.
     */
    public final void shutdown() {
        discordBot.shutdown();
        databaseAdapter.close();
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
                dbExecutor.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dbExecutor.shutdownNow();
        }
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
     * Returns true if the player is either still pending (join being processed) or in the verifying set.
     * Used by platforms to block commands/events until we either skip or complete verification.
     */
    public final boolean isPlayerPendingOrVerifying(UUID uuid) {
        return pendingVerification.contains(uuid) || verifyingPlayers.containsKey(uuid);
    }

    /** Whether the given command label is in the allowed-while-unverified list (e.g. /link). */
    public final boolean isCommandAllowed(String commandLabel) {
        return serverConfig.isCommandAllowed(commandLabel);
    }

    /**
     * Runs the same "skip verification?" logic as addPlayer on the DB executor and blocks until done.
     * Returns true if the player should skip verification (e.g. not linked and forceLink off, or location remembered).
     * Proxy platforms use this to send the player directly to the post-verification server instead of the verification server.
     * If the bot is not connected or the database is unavailable, returns true (skip) so the player is not stuck.
     */
    public final boolean shouldSkipVerificationBlocking(CommonPlayer player) {
        if (player == null) return true;
        if (!discordBot.isConnected() || databaseAdapter == null) return true;
        SignInLocation current = player.getSigninLocation();
        return CompletableFuture.supplyAsync(() -> {
            LinkedPlayer linked = databaseAdapter.getLinkedPlayer(player.getUniqueId());
            if (linked != null) player.setLinkedPlayer(linked);
            boolean skip = !serverConfig.isForceLink() && linked == null;
            if (!skip && current != null && serverConfig.isRememberSignInLocation()
                && databaseAdapter.hasRecentSignInLocation(player.getUniqueId(), current.getIpAddress(), current.getVersion())) {
                skip = true;
            }
            return skip;
        }, dbExecutor).join();
    }

    /**
     * Register a player as verifying. Called from platform join/login events.
     * DB lookups run asynchronously so the join thread is not blocked; the actual add and messages run on the server executor.
     * @param onSkippedVerification optional callback when the player is skipped (e.g. not linked and forceLink off, or location remembered). Proxy platforms use this to send the player to the post-verification server.
     */
    protected final void addPlayer(CommonPlayer player, Runnable onSkippedVerification) {
        if (player == null) return;

        if (player.hasPermission("discord2fa.admin")) {
            player.sendMessage("&cA new version of Discord2FA is available!");
            player.sendUrlButton("§b[DOWNLOAD]", "https://modrinth.com/plugin/discord2fa");
        }

        SignInLocation current = player.getSigninLocation();

        if (!discordBot.isConnected() || databaseAdapter == null) return;

        pendingVerification.add(player.getUniqueId());

        CompletableFuture.supplyAsync(() -> {
            LinkedPlayer linked = databaseAdapter.getLinkedPlayer(player.getUniqueId());
            if (linked != null) player.setLinkedPlayer(linked);
            boolean skip = !serverConfig.isForceLink() && linked == null;
            if (!skip && current != null && serverConfig.isRememberSignInLocation()
                && databaseAdapter.hasRecentSignInLocation(player.getUniqueId(), current.getIpAddress(), current.getVersion())) {
                skip = true;
            }
            return new Object[]{ linked, skip, player };
        }, dbExecutor).thenAcceptAsync(result -> {
            boolean skip = (Boolean) ((Object[]) result)[1];
            CommonPlayer p = (CommonPlayer) ((Object[]) result)[2];
            pendingVerification.remove(p.getUniqueId());

            if (skip) {
                if (onSkippedVerification != null) onSkippedVerification.run();
                return;
            }

            p.setOnVerifiedCallback(() -> verifyingPlayers.remove(p.getUniqueId()));
            verifyingPlayers.put(p.getUniqueId(), p);

            if (p.isLinked()) {
                discordBot.attemptVerify(p.getLinkedPlayer(), p.getSigninLocation())
                        .thenAcceptAsync(verified -> {
                            if (verified) {
                                p.sendMessage(messageProvider.get("verifySuccess"));
                                p.onVerified();
                            } else {
                                p.kick(messageProvider.get("verifyDenied"));
                            }
                        }, serverExecutor);
            } else {
                p.sendMessage(messageProvider.get("forceLink").replace("%player%", player.getName()));
            }
        }, serverExecutor);
    }

    /** Overload for non-proxy platforms; no callback when skipped. */
    protected final void addPlayer(CommonPlayer player) {
        addPlayer(player, null);
    }

    /**
     * Called by platform listeners for non-command events. Returns true to allow,
     * false to deny (e.g. cancel). Pending or verifying = block unless event is allowed.
     */
    public final boolean onEvent(UUID uuid, EventType eventType) {
        if (!isPlayerPendingOrVerifying(uuid)) return true;
        if (serverConfig.isEventAllowed(eventType)) return true;
        CommonPlayer player = verifyingPlayers.get(uuid);
        if (player != null) {
            if (player.isLinked()) {
                player.sendTitle(messageProvider.get("verifyTitle"));
            } else {
                player.sendMessage(messageProvider.get("forceLink").replace("%player%", player.getName()));
            }
        }
        return false;
    }

    /**
     * Dispatches a command asynchronously. DB work runs off the server thread; messages and onVerified run on the server executor.
     * Returns a future that completes with true if the command was handled (platform should cancel the command event when true).
     */
    public final CompletableFuture<Boolean> handleCommand(CommonPlayer player, String commandLabel, List<String> args) {
        String label = commandLabel == null ? "" : commandLabel.trim().toLowerCase(java.util.Locale.ROOT);
        if (isPlayerPendingOrVerifying(player.getUniqueId()) && !serverConfig.isCommandAllowed(label)) {
            return CompletableFuture.supplyAsync(() -> databaseAdapter.getLinkedPlayer(player.getUniqueId()), dbExecutor)
                    .thenApplyAsync(linked -> {
                        player.sendMessage(linked != null ? messageProvider.get("notVerified") : messageProvider.get("forceLink").replace("%player%", player.getName()));
                        return true;
                    }, serverExecutor);
        }
        return switch (label) {
            case "link" -> handleLinkCommandAsync(player, args.isEmpty() ? "" : args.get(0));
            case "unlink" -> handleUnlinkCommandAsync(player);
            default -> CompletableFuture.completedFuture(false);
        };
    }

    private CompletableFuture<Boolean> handleLinkCommandAsync(CommonPlayer player, String code) {
        if (databaseAdapter.getLinkedPlayer(player.getUniqueId()) != null) {
            serverExecutor.execute(() -> player.sendMessage(messageProvider.get("alreadyLinked")));
            return CompletableFuture.completedFuture(true);
        }
        if (code == null || code.isBlank()) {
            serverExecutor.execute(() -> player.sendMessage(messageProvider.get("noCode")));
            return CompletableFuture.completedFuture(true);
        }
        var discordUser = discordBot.consumeLinkCode(code);
        if (discordUser.isEmpty()) {
            serverExecutor.execute(() -> player.sendMessage(messageProvider.get("invalidCode")));
            return CompletableFuture.completedFuture(true);
        }
        long discordId = discordUser.get().getId();
        UUID playerUuid = player.getUniqueId();
        return CompletableFuture.runAsync(() -> {
            databaseAdapter.saveLinkedPlayer(new LinkedPlayer(playerUuid, discordId, Instant.now()));
        }, dbExecutor).thenApplyAsync(v -> {
            LinkedPlayer linked = databaseAdapter.getLinkedPlayer(playerUuid);
            player.sendMessage(messageProvider.get("linkSuccess"));
            if (verifyingPlayers.remove(playerUuid) != null) {
                player.setLinkedPlayer(linked);
                player.onVerified();
            }
            discordBot.giveVerifiedRole(discordUser.get());
            return true;
        }, serverExecutor);
    }

    private CompletableFuture<Boolean> handleUnlinkCommandAsync(CommonPlayer player) {
        if (verifyingPlayers.containsKey(player.getUniqueId())) {
            serverExecutor.execute(() -> player.sendMessage(messageProvider.get("notVerified")));
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.supplyAsync(() -> databaseAdapter.getLinkedPlayer(player.getUniqueId()), dbExecutor)
                .thenComposeAsync(linked -> {
                    if (linked == null) {
                        player.sendMessage(messageProvider.get("notLinked"));
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.runAsync(() -> databaseAdapter.removeLinkedPlayer(player.getUniqueId()), dbExecutor)
                            .thenRunAsync(() -> {
                                player.setLinkedPlayer(null);
                                player.sendMessage(messageProvider.get("unlinkSuccess"));
                                discordBot.revokeVerifiedRole(linked.getDiscordId());
                            }, serverExecutor)
                            .thenApply(v -> true);
                }, serverExecutor);
    }

    /**
     * Called by platform listeners for command events. Pending or verifying = block unless command is allowed.
     */
    public final boolean onCommand(UUID uuid, String commandLabel) {
        if (!isPlayerPendingOrVerifying(uuid)) return true;
        if (serverConfig.isCommandAllowed(commandLabel)) return true;
        CommonPlayer player = verifyingPlayers.get(uuid);
        if (player != null) {
            if (player.isLinked()) {
                player.sendTitle(messageProvider.get("verifyTitle"));
            } else {
                player.sendMessage(messageProvider.get("forceLink").replace("%player%", player.getName()));
            }
        }
        return false;
    }

    /**
     * Returns a future with the message to send when a command is denied (player is verifying and command not in allowedCommands).
     * Completes with null if the command is allowed or the player is not in the verifying state. Run the lookup async so the event thread is not blocked.
     */
    public final CompletableFuture<String> getCommandDeniedMessage(UUID uuid, String commandLabel) {
        if (!isPlayerPendingOrVerifying(uuid)) return CompletableFuture.completedFuture(null);
        if (serverConfig.isCommandAllowed(commandLabel == null ? "" : commandLabel.trim().toLowerCase(java.util.Locale.ROOT))) return CompletableFuture.completedFuture(null);
        CommonPlayer player = verifyingPlayers.get(uuid);
        if (player == null) return CompletableFuture.completedFuture(messageProvider.get("notVerified"));
        return CompletableFuture.supplyAsync(() -> {
            LinkedPlayer linked = databaseAdapter.getLinkedPlayer(uuid);
            return linked != null ? messageProvider.get("notVerified") : messageProvider.get("forceLink").replace("%player%", player.getName());
        }, dbExecutor);
    }

    /**
     * Checks for updates and logs the result to the console. Called automatically from the constructor.
     * Version is read from the JAR manifest (Implementation-Version).
     */
    private void checkForUpdates() {
        UpdateCheckResult result = UpdateChecker.check();
        this.lastUpdateCheckResult = result;
        if (result == null) logger.error("Unable to retrieve version. Please report this!!!");
        else if (result.isNewerThanLatest()) {
            logger.warn("You are using an experimental build of Discord2FA (version " + PluginVersion.get() + "). Latest release is " + result.getLatestReleaseVersion() + ".");
        } else if (result.isUpToDate()) {
            logger.info("You are running the latest release of Discord2FA.");
            isUpToDate = true;
        } else {
            logger.warn("You are " + result.getVersionsBehind() + " version(s) behind (Current version: " + PluginVersion.get() + "). Download the latest at " + result.getDownloadUrl());
            isUpToDate = false;
        }
    }

    /**
     * Checks for updates and logs the result to the permitted Player. Called automatically from the constructor.
     * Version is read from the JAR manifest (Implementation-Version).
     */
    private void sendAdminUpdateAlert(CommonPlayer player){
        if (!isUpToDate){
            player.sendMessage(messageProvider.get("outdatedPlugin"));
        }else{
            return;
        }
    }

    /**
     * Returns lines for the /discord2fa version subcommand.
     */
    public List<String> getVersionInfoMessage() {
        String current = PluginVersion.get();
        if (current.isBlank()) current = "unknown";
        List<String> lines = new java.util.ArrayList<>();
        lines.add("§eDiscord2FA §fversion §a" + current);
        UpdateCheckResult result = lastUpdateCheckResult;
        if (result == null) {
            lines.add("§7(Update check unavailable)");
        } else if (result.isNewerThanLatest()) {
            lines.add("§7Experimental build. Latest release: §f" + result.getLatestReleaseVersion());
        } else if (result.isUpToDate()) {
            lines.add("§7Latest release.");
        } else {
            lines.add("§c" + result.getVersionsBehind() + " version(s) behind. §7Download: §f" + result.getDownloadUrl());
        }
        return lines;
    }

    /**
     * Returns lines for the /discord2fa status subcommand.
     */
    public List<String> getStatusInfoMessage() {
        List<String> lines = new java.util.ArrayList<>();
        int ok = 0;
        int total = 2;
        lines.add("§8[§a✓§8] §7Database §fconnected");
        ok++;
        if (discordBot.isConnected()) {
            lines.add("§8[§a✓§8] §7Discord bot §fconnected");
            ok++;
        } else if (discordBot.isConfigured()) {
            lines.add("§8[§c✗§8] §7Discord bot §cconfigured but failed to connect §7(check token and network)");
        } else {
            lines.add("§8[§c✗§8] §7Discord bot §cnot configured §7(set discord.token, discord.guild, discord.channel in config.yml)");
        }
        lines.add("");
        if (ok == total) {
            lines.add("§a" + ok + "/" + total + " services running.");
        } else {
            lines.add("§e" + ok + "/" + total + " services running.");
        }
        return lines;
    }
}

