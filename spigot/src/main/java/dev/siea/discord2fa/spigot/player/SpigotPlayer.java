package dev.siea.discord2fa.spigot.player;

import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.common.player.CommonPlayer;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.time.Instant;

/** CommonPlayer implementation wrapping a Bukkit Player. */
public final class SpigotPlayer extends CommonPlayer {

    private final Player handle;

    public SpigotPlayer(Player handle) {
        super(handle.getUniqueId(), parseSigninLocation(handle));
        this.handle = handle;
    }

    private static SignInLocation parseSigninLocation(Player p) {
        String ip = ipFrom(p);
        if (ip == null) return null;
        return new SignInLocation(0, ip, "unknown", p.getUniqueId(), Instant.now());
    }

    private static String ipFrom(Player p) {
        if (p.getAddress() == null) return null;
        InetSocketAddress a = p.getAddress();
        if (a.getAddress() == null) return null;
        return a.getAddress().getHostAddress();
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
    public void kick(String reason) {
        handle.kickPlayer(reason);
    }
}
