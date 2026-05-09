package dev.siea.discord2fa.bungeecord.player;

import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.proxyserver.player.ProxyPlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.net.InetSocketAddress;
import java.time.Instant;

/** ProxyPlayer implementation wrapping a BungeeCord ProxiedPlayer. */
public final class BungeeProxyPlayer extends ProxyPlayer {

    private final ProxiedPlayer handle;
    private final ProxyServer proxy;

    public BungeeProxyPlayer(ProxiedPlayer handle, ProxyServer proxy) {
        super(handle.getUniqueId(), parseSigninLocation(handle));
        this.handle = handle;
        this.proxy = proxy;
    }

    private static SignInLocation parseSigninLocation(ProxiedPlayer p) {
        String ip = ipFrom(p);
        if (ip == null) return null;
        int ver = p.getPendingConnection() != null ? p.getPendingConnection().getVersion() : 0;
        return new SignInLocation(0, ip, String.valueOf(ver), p.getUniqueId(), Instant.now());
    }

    private static String ipFrom(ProxiedPlayer p) {
        if (p.getSocketAddress() == null) return null;
        if (!(p.getSocketAddress() instanceof InetSocketAddress a)) return null;
        if (a.getAddress() == null) return null;
        return a.getAddress().getHostAddress();
    }

    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(message);
    }

    @Override
    public void sendUrlButton(String text, String url) {
        TextComponent component = new TextComponent(text);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

        handle.sendMessage(component);
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int duration, int fadeOut) {
        handle.sendTitle(new BungeeTitle().title(title).subTitle(subtitle).fadeIn(fadeIn).fadeOut(fadeOut).stay(duration));
    }

    @Override
    public void kick(String reason) {
        handle.disconnect(reason);
    }

    @Override
    public void sendToServer(String serverName) {
        if (proxy != null) {
            net.md_5.bungee.api.config.ServerInfo info = proxy.getServerInfo(serverName);
            if (info != null) handle.connect(info);
        }
    }

    public ProxiedPlayer getHandle() {
        return handle;
    }
}
