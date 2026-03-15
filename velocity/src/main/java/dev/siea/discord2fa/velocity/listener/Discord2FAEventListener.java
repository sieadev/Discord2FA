package dev.siea.discord2fa.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import dev.siea.discord2fa.common.event.EventType;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import dev.siea.discord2fa.proxyserver.ProxyTargetServers;
import dev.siea.discord2fa.velocity.player.VelocityProxyPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Discord2FAEventListener {

    private final ProxyServer server;
    private final com.velocitypowered.api.proxy.ProxyServer proxy;
    /** Result of the blocking skip check per player, used in ServerPreConnectEvent to pick initial server. */
    private final Map<UUID, Boolean> initialServerSkip = new ConcurrentHashMap<>();

    public Discord2FAEventListener(ProxyServer server, com.velocitypowered.api.proxy.ProxyServer proxy) {
        this.server = server;
        this.proxy = proxy;
    }

    @Subscribe
    public void onLogin(com.velocitypowered.api.event.connection.LoginEvent event) {
        VelocityProxyPlayer player = new VelocityProxyPlayer(event.getPlayer(), proxy);
        boolean skip = server.shouldSkipVerificationBlocking(player);
        initialServerSkip.put(player.getUniqueId(), skip);
        server.handlePlayerJoin(player, () -> ProxyTargetServers.sendPlayerToPostVerificationServer(player));
    }

    /**
     * Redirect the initial connection: skipped players go to post-verification, others to verification server.
     * Uses the result of the blocking check from onLogin so we never send to verification then immediately transfer.
     */
    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (event.getPreviousServer() != null) return;
        Boolean skip = initialServerSkip.remove(event.getPlayer().getUniqueId());
        if (skip == null) return;
        if (skip) {
            String postVerification = ProxyTargetServers.getPostVerificationServer();
            if (postVerification != null) proxy.getServer(postVerification).ifPresent(reg -> event.setResult(ServerPreConnectEvent.ServerResult.allowed(reg)));
        } else {
            String verification = ProxyTargetServers.getVerificationServer();
            if (verification != null) proxy.getServer(verification).ifPresent(reg -> event.setResult(ServerPreConnectEvent.ServerResult.allowed(reg)));
        }
    }

    /**
     * Block command execution for unverified players (except allowed commands like /link).
     * Commands executed on the proxy fire this event; some forwarded commands may not.
     */
    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        String full = event.getCommand();
        String label = full == null || full.isBlank() ? "" : full.trim().split("\\s+", 2)[0].toLowerCase(java.util.Locale.ROOT);
        if (!server.onCommand(uuid, label)) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            server.getCommandDeniedMessage(uuid, label)
                    .thenAccept(msg -> {
                        if (msg != null) player.sendMessage(PlainTextComponentSerializer.plainText().deserialize(msg));
                    });
        }
    }

    /**
     * Chat and commands are the only events the proxy sees. In-game actions (break, place, move) happen
     * on the backend; secure the verification server with e.g. WorldGuard instead of installing Discord2FA there.
     */
    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        java.util.UUID uuid = player.getUniqueId();
        String message = event.getMessage();
        if (message != null && message.trim().startsWith("/")) {
            String label = parseCommandLabel(message);
            if (!server.onCommand(uuid, label)) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                server.getCommandDeniedMessage(uuid, label)
                        .thenAccept(msg -> {
                            if (msg != null) player.sendMessage(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().deserialize(msg));
                        });
            }
        } else {
            if (!server.onEvent(uuid, EventType.CHAT)) event.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }

    private static String parseCommandLabel(String message) {
        if (message == null || message.isEmpty()) return "";
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
