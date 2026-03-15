package dev.siea.discord2fa.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import dev.siea.discord2fa.common.event.EventType;
import dev.siea.discord2fa.proxyserver.ProxyServer;
import dev.siea.discord2fa.velocity.player.VelocityProxyPlayer;

public final class Discord2FAEventListener {

    private final ProxyServer server;
    private final com.velocitypowered.api.proxy.ProxyServer proxy;

    public Discord2FAEventListener(ProxyServer server, com.velocitypowered.api.proxy.ProxyServer proxy) {
        this.server = server;
        this.proxy = proxy;
    }

    @Subscribe
    public void onLogin(com.velocitypowered.api.event.connection.LoginEvent event) {
        server.handlePlayerJoin(new VelocityProxyPlayer(event.getPlayer(), proxy));
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        java.util.UUID uuid = player.getUniqueId();
        String message = event.getMessage();
        if (message != null && message.trim().startsWith("/")) {
            String label = parseCommandLabel(message);
            if (!server.onCommand(uuid, label)) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                String msg = server.getCommandDeniedMessage(uuid, label);
                if (msg != null) player.sendMessage(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().deserialize(msg));
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
