package dev.siea.discord2fa.velocity.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.proxyserver.player.ProxyPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.Optional;

/** ProxyPlayer implementation wrapping a Velocity Player. */
public final class VelocityProxyPlayer extends ProxyPlayer {

    private final Player handle;
    private final ProxyServer proxy;

    public VelocityProxyPlayer(Player handle, ProxyServer proxy) {
        super(handle.getUniqueId(), parseSigninLocation(handle));
        this.handle = handle;
        this.proxy = proxy;
    }

    private static SignInLocation parseSigninLocation(Player p) {
        String ip = ipFrom(p.getRemoteAddress());
        if (ip == null) return null;
        String version = p.getProtocolVersion() != null ? p.getProtocolVersion().getName() : "unknown";
        return new SignInLocation(0, ip, version, p.getUniqueId(), Instant.now());
    }

    private static String ipFrom(SocketAddress addr) {
        if (!(addr instanceof InetSocketAddress a)) return null;
        if (a.getAddress() == null) return null;
        return a.getAddress().getHostAddress();
    }

    @Override
    public String getName() {
        return handle.getUsername();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().deserialize(message));
    }

    @Override
    public void sendUrlButton(String text, String url) {
        handle.sendMessage(
                Component.text(text)
                        .clickEvent(ClickEvent.openUrl(url))
        );
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int duration, int fadeOut) {
        handle.showTitle(
                net.kyori.adventure.title.Title.title(
                        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                                .plainText().deserialize(title),
                        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                                .plainText().deserialize(subtitle),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(fadeIn * 50L),
                                java.time.Duration.ofMillis(duration * 50L),
                                java.time.Duration.ofMillis(fadeOut * 50L)
                        )
                )
        );
    }

    @Override
    public void kick(String reason) {
        handle.disconnect(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().deserialize(reason));
    }

    @Override
    public void sendToServer(String serverName) {
        Optional<RegisteredServer> server = proxy.getServer(serverName);
        server.ifPresent(s -> handle.createConnectionRequest(s).fireAndForget());
    }
}
