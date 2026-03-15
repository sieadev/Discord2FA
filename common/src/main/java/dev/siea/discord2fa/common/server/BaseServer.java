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

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseServer {
    private final LoggerAdapter logger;
    private final DatabaseAdapter databaseAdapter;
    private final DiscordBot discordBot;
    private final ServerConfig serverConfig;
    private final MessageProvider messageProvider;

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
     * DB lookups run asynchronously so the join thread is not blocked; the actual add and messages run on the server executor.
     */
    protected final void addPlayer(CommonPlayer player) {
        if (player == null) return;
        SignInLocation current = player.getSigninLocation();

        if (!discordBot.isConnected() || databaseAdapter == null) return;

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
            if (skip) return;

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
                p.sendMessage(messageProvider.get("forceLink"));
            }
        }, serverExecutor);
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
     * Dispatches a command asynchronously. DB work runs off the server thread; messages and onVerified run on the server executor.
     * Returns a future that completes with true if the command was handled (platform should cancel the command event when true).
     */
    public final CompletableFuture<Boolean> handleCommand(CommonPlayer player, String commandLabel, List<String> args) {
        String label = commandLabel == null ? "" : commandLabel.trim().toLowerCase(java.util.Locale.ROOT);
        if (verifyingPlayers.containsKey(player.getUniqueId()) && !serverConfig.isCommandAllowed(label)) {
            return CompletableFuture.supplyAsync(() -> databaseAdapter.getLinkedPlayer(player.getUniqueId()), dbExecutor)
                    .thenApplyAsync(linked -> {
                        player.sendMessage(linked != null ? messageProvider.get("notVerified") : messageProvider.get("forceLink"));
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
                            }, serverExecutor)
                            .thenApply(v -> true);
                }, serverExecutor);
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
     * Returns a future with the message to send when a command is denied (player is verifying and command not in allowedCommands).
     * Completes with null if the command is allowed or the player is not in the verifying state. Run the lookup async so the event thread is not blocked.
     */
    public final CompletableFuture<String> getCommandDeniedMessage(UUID uuid, String commandLabel) {
        if (!verifyingPlayers.containsKey(uuid)) return CompletableFuture.completedFuture(null);
        if (serverConfig.isCommandAllowed(commandLabel == null ? "" : commandLabel.trim().toLowerCase(java.util.Locale.ROOT))) return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(() -> {
            LinkedPlayer linked = databaseAdapter.getLinkedPlayer(uuid);
            return linked != null ? messageProvider.get("notVerified") : messageProvider.get("forceLink");
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
        } else {
            logger.warn("You are " + result.getVersionsBehind() + " version(s) behind (Current version: " + PluginVersion.get() + "). Download the latest at " + result.getDownloadUrl());
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

