package dev.siea.discord2fa.paper.player;

import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.common.player.CommonPlayer;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.time.Instant;

/** CommonPlayer implementation wrapping a Bukkit Player. */
public final class PaperPlayer extends CommonPlayer {

    private final Player handle;

    public PaperPlayer(Player handle) {
        super(handle.getUniqueId(), parseSigninLocation(handle));
        this.handle = handle;
    }

    private static SignInLocation parseSigninLocation(Player p) {
        String ip = ipFrom(p);
        if (ip == null) return null;
        String version = versionFrom(p);
        return new SignInLocation(0, ip, version, p.getUniqueId(), Instant.now());
    }

    private static String ipFrom(Player p) {
        if (p.getAddress() == null) return null;
        InetSocketAddress a = (InetSocketAddress) p.getAddress();
        if (a.getAddress() == null) return null;
        return a.getAddress().getHostAddress();
    }

    @SuppressWarnings("deprecation")
    private static String versionFrom(Player p) {
        try {
            int v = p.getProtocolVersion();
            return v > 0 ? String.valueOf(v) : "unknown";
        } catch (ClassCastException e) {
            return "unknown";
        }
    }

    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(message);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void sendTitle(String title, String subtitle, int fadeIn, int duration, int fadeOut) {
        handle.sendTitle(title, subtitle, fadeIn, duration, fadeOut);
    }

    @Override
    public void kick(String reason) {
        handle.kickPlayer(reason);
    }

    public Player getHandle() {
        return handle;
    }
}
