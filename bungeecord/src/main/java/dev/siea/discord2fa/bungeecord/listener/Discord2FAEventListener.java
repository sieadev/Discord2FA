package dev.siea.discord2fa.bungeecord.listener;

import dev.siea.discord2fa.bungeecord.player.BungeeProxyPlayer;
import dev.siea.discord2fa.common.event.EventType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public final class Discord2FAEventListener implements Listener {

    private final dev.siea.discord2fa.proxyserver.ProxyServer server;
    private final net.md_5.bungee.api.ProxyServer proxy;

    public Discord2FAEventListener(dev.siea.discord2fa.proxyserver.ProxyServer server, net.md_5.bungee.api.ProxyServer proxy) {
        this.server = server;
        this.proxy = proxy;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        server.handlePlayerJoin(new BungeeProxyPlayer(event.getPlayer(), proxy));
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();
        if (message != null && message.trim().startsWith("/")) {
            if (!server.onCommand(uuid, parseCommandLabel(message))) event.setCancelled(true);
        } else {
            if (!server.onEvent(uuid, EventType.CHAT)) event.setCancelled(true);
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
