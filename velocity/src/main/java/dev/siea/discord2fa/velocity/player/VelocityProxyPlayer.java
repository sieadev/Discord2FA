package dev.siea.discord2fa.velocity.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.proxyserver.player.ProxyPlayer;

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
    public void sendMessage(String message) {
        handle.sendMessage(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().deserialize(message));
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
