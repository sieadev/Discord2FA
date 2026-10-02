package dev.siea.discord2fa.bungeecord.listener;

import dev.siea.discord2fa.bungeecord.player.BungeeProxyPlayer;
import dev.siea.discord2fa.common.event.EventType;
import dev.siea.discord2fa.proxyserver.ProxyTargetServers;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Discord2FAEventListener implements Listener {

    private final dev.siea.discord2fa.proxyserver.ProxyServer server;
    private final net.md_5.bungee.api.ProxyServer proxy;
    /** Login wrappers keyed by platform player handle so disconnect can match the correct session. */
    private final Map<ProxiedPlayer, BungeeProxyPlayer> wrappersByHandle = new ConcurrentHashMap<>();

    public Discord2FAEventListener(dev.siea.discord2fa.proxyserver.ProxyServer server, net.md_5.bungee.api.ProxyServer proxy) {
        this.server = server;
        this.proxy = proxy;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer handle = event.getPlayer();
        BungeeProxyPlayer player = new BungeeProxyPlayer(handle, proxy);
        wrappersByHandle.put(handle, player);

        boolean forceVerify = isSessionTakeover(handle);
        boolean skip = server.shouldSkipVerificationBlocking(player, forceVerify);
        if (skip) {
            ProxyTargetServers.sendPlayerToPostVerificationServer(player);
        } else {
            ProxyTargetServers.sendPlayerToVerificationServer(player);
        }
        server.handlePlayerJoin(player, forceVerify,
                () -> ProxyTargetServers.sendPlayerToPostVerificationServer(player),
                () -> ProxyTargetServers.sendPlayerToVerificationServer(player));
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer handle = event.getPlayer();
        BungeeProxyPlayer wrapper = wrappersByHandle.remove(handle);
        if (wrapper != null) {
            server.endPlayerSession(handle.getUniqueId(), wrapper);
        }
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();
        if (message != null && message.trim().startsWith("/")) {
            String label = parseCommandLabel(message);
            if (!server.onCommand(uuid, label)) {
                event.setCancelled(true);
                server.getCommandDeniedMessage(uuid, label)
                        .thenAccept(msg -> { if (msg != null) player.sendMessage(msg); });
            }
        } else {
            if (!server.onEvent(uuid, EventType.CHAT)) event.setCancelled(true);
        }
    }

    private boolean isSessionTakeover(ProxiedPlayer newPlayer) {
        ProxiedPlayer existing = proxy.getPlayer(newPlayer.getUniqueId());
        return existing != null && existing != newPlayer;
    }

    private static String parseCommandLabel(String message) {
        if (message == null || message.isEmpty()) return "";
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
