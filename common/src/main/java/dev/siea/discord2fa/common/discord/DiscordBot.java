package dev.siea.discord2fa.common.discord;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.database.DatabaseAdapter;
import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.common.i18n.MessageProvider;
import dev.siea.discord2fa.common.logger.LoggerAdapter;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.MessageComponentInteraction;

import java.awt.Color;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordBot {
    /** Pending link codes: code -> Discord user who requested it. */
    private final Map<String, User> codes = new ConcurrentHashMap<>();
    /** Discord user ids that have been sent a code and not yet linked (so we don't send duplicate codes). */
    private final Map<Long, Boolean> pendingLinkUserIds = new ConcurrentHashMap<>();
    /** Pending verify requests: discord id -> (future to complete, sign-in location to add to DB on accept). */
    private final Map<Long, PendingVerify> pendingVerifies = new ConcurrentHashMap<>();

    private final DiscordConfig discordConfig;
    private final MessageProvider messageProvider;
    private final DatabaseAdapter databaseAdapter;
    /** Executor for Discord-related async work (DB state, non-blocking callbacks). */
    private final ExecutorService discordExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Discord2FA-discord");
        t.setDaemon(true);
        return t;
    });

    private volatile DiscordApi api;

    public DiscordBot(ConfigAdapter configAdapter, MessageProvider messageProvider, DatabaseAdapter databaseAdapter, LoggerAdapter loggerAdapter) {
        this.discordConfig = new DiscordConfig(configAdapter);
        this.messageProvider = messageProvider;
        this.databaseAdapter = databaseAdapter;

        if (discordConfig.isConfigured()) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return new DiscordApiBuilder()
                            .setToken(discordConfig.getToken())
                            .login()
                            .join();
                } catch (Exception e) {
                    return null;
                }
            }, discordExecutor).thenAcceptAsync(connectedApi -> {
                if (connectedApi == null) return;
                this.api = connectedApi;
                registerListeners();
                ensureLinkMessageAsync();
            }, discordExecutor);
        } else {
            loggerAdapter.warn("The Discord bot will not start because it has not been configured. Set discord.token, discord.guild, and discord.channel in your config.yml to enable it.");
        }
    }

    /**
     * Register button listeners: link button in channel, verify/deny in DMs.
     */
    private void registerListeners() {
        api.addMessageComponentCreateListener(event -> {
            MessageComponentInteraction interaction = event.getMessageComponentInteraction();
            String customId = interaction.getCustomId();

            if ("link".equals(customId)) {
                interaction.acknowledge();
                User user = interaction.getUser();
                CompletableFuture.runAsync(() -> sendLinkRequest(user), discordExecutor);
                return;
            }

            if (customId.startsWith("verify_")) {
                CompletableFuture.runAsync(() -> handleVerifyButton(interaction, customId), discordExecutor);
            }
        });
    }

    /**
     * Ensures the configured channel has the "link your account" message. Loads stored message ID from DB;
     * if that message still exists, does nothing (button listener already handles it). Otherwise sends a new
     * message and persists its ID. All work is done asynchronously; DB lookup runs off the discord thread.
     */
    private void ensureLinkMessageAsync() {
        if (api == null) return;
        ServerTextChannel channel = api.getServerTextChannelById(discordConfig.getChannelId()).orElse(null);
        if (channel == null) return;

        CompletableFuture.supplyAsync(() -> databaseAdapter.getState(DatabaseAdapter.STATE_LINK_MESSAGE_ID), discordExecutor)
                .thenAccept(storedId -> ensureLinkMessageWithStoredId(channel, storedId));
    }

    private void ensureLinkMessageWithStoredId(ServerTextChannel channel, String storedId) {
        if (storedId != null && !storedId.isBlank()) {
            try {
                long messageId = Long.parseLong(storedId.trim());
                channel.getMessageById(messageId)
                        .thenAccept(msg -> { /* message exists, nothing to do */ })
                        .exceptionally(ex -> {
                            sendNewLinkMessage(channel);
                            return null;
                        });
                return;
            } catch (NumberFormatException ignored) {
                // fall through to send new
            }
        }
        sendNewLinkMessage(channel);
    }


    /**
     * Sends a new link message to the channel and persists its message ID in the database (async).
     */
    private void sendNewLinkMessage(ServerTextChannel channel) {
        String title = messageProvider.get("link.title");
        String text = messageProvider.get("link.text");
        String footer = messageProvider.get("link.footer");
        String linkButtonLabel = messageProvider.get("link.linkButton");

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(text)
                .setFooter(footer)
                .setColor(Color.BLUE);

        new MessageBuilder()
                .setEmbed(embed)
                .addComponents(ActionRow.of(Button.primary("link", linkButtonLabel)))
                .send(channel)
                .thenAccept(msg -> discordExecutor.execute(() ->
                        databaseAdapter.setState(DatabaseAdapter.STATE_LINK_MESSAGE_ID, String.valueOf(msg.getId()))))
                .exceptionally(ex -> null);
    }

    /**
     * Handle Verify / Deny button press from the verify DM.
     */
    private void handleVerifyButton(MessageComponentInteraction interaction, String customId) {
        long discordId = interaction.getUser().getId();
        PendingVerify pending = pendingVerifies.remove(discordId);
        if (pending == null) {
            interaction.createImmediateResponder()
                    .setContent(messageProvider.get("notVerified"))
                    .setFlags(org.javacord.api.entity.message.MessageFlag.EPHEMERAL)
                    .respond();
            return;
        }

        boolean accepted = customId.startsWith("verify_accept");
        if (accepted) {
            SignInLocation loc = pending.signInLocation;
            if (loc != null) {
                discordExecutor.execute(() -> databaseAdapter.addLoginLocation(
                        loc.getIpAddress(), loc.getVersion(), loc.getMinecraftUuid(), loc.getTimeOfLogin()));
            }
            interaction.createImmediateResponder()
                    .setContent(messageProvider.get("acceptMessage"))
                    .respond();
            pending.future.complete(true);
        } else {
            interaction.createImmediateResponder()
                    .setContent(messageProvider.get("denyMessage"))
                    .respond();
            pending.future.complete(false);
        }
    }

    /**
     * Ask the Discord user to confirm the sign-in. Sends a DM with embed and Verify/Deny buttons.
     * On Verify, the sign-in location is stored as trusted and the future completes with true.
     */
    public CompletableFuture<Boolean> attemptVerify(LinkedPlayer linkedPlayer, SignInLocation signInLocation) {
        if (api == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<User> userFuture = api.getUserById(linkedPlayer.getDiscordId());
        return userFuture
                .thenCompose(user -> {
                    String ip = signInLocation != null ? signInLocation.getIpAddress() : "?";
        String title = messageProvider.get("verify.title");
        String text = messageProvider.get("verify.text").replace("%ip%", ip);
        String footer = messageProvider.get("verify.footer");
                    String verifyLabel = messageProvider.get("verify.VerifyButton");
                    String denyLabel = messageProvider.get("verify.DenyButton");

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(title)
                            .setDescription(text)
                            .setFooter(footer)
                            .setColor(Color.ORANGE);

                    String acceptId = "verify_accept";
                    String denyId = "verify_deny";

                    CompletableFuture<Boolean> result = new CompletableFuture<>();
                    pendingVerifies.put(linkedPlayer.getDiscordId(), new PendingVerify(result, signInLocation));

                    return new MessageBuilder()
                            .setEmbed(embed)
                            .addComponents(ActionRow.of(
                                    Button.success(acceptId, verifyLabel),
                                    Button.danger(denyId, denyLabel)))
                            .send(user)
                            .thenApply(msg -> result);
                })
                .thenCompose(cf -> cf)
                .exceptionally(ex -> false);
    }

    /**
     * Consumes the link code if present: removes it from the map and returns the associated Discord user.
     * Call this when the player completes /link &lt;code&gt; so the code is only valid once.
     */
    public Optional<User> consumeLinkCode(String code) {
        User user = codes.remove(code);
        if (user != null) {
            pendingLinkUserIds.remove(user.getId());
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Send the link-account DM: embed with code and store code for /link command. Runs async.
     */
    private void sendLinkRequest(User user) {
        CompletableFuture.supplyAsync(() -> databaseAdapter.getLinkedByDiscord(user.getId()), discordExecutor)
                .thenAccept(linked -> {
                    if (linked.isPresent()) {
                        sendDmEmbed(user, messageProvider.get("alreadyLinked"), null);
                        return;
                    }
                    if (pendingLinkUserIds.containsKey(user.getId())) {
                        sendDmEmbed(user, messageProvider.get("alreadyLinking"), null);
                        return;
                    }
                    String code = generateRandomString();
                    codes.put(code, user);
                    pendingLinkUserIds.put(user.getId(), Boolean.TRUE);
                    String message = messageProvider.get("codeMessage").replace("%code%", code);
                    sendDmEmbed(user, message, "Verification Code");
                });
    }

    private void sendDmEmbed(User user, String description, String title) {
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription(description)
                .setColor(Color.BLUE);
        if (title != null && !title.isEmpty()) {
            embed.setTitle(title);
        }
        new MessageBuilder().setEmbed(embed).send(user).exceptionally(ex -> null);
    }

    private static String generateRandomString() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static final class PendingVerify {
        final CompletableFuture<Boolean> future;
        final SignInLocation signInLocation;

        PendingVerify(CompletableFuture<Boolean> future, SignInLocation signInLocation) {
            this.future = future;
            this.signInLocation = signInLocation;
        }
    }
}
