package dev.siea.discord2fa.proxyserver.player;

import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.proxyserver.ProxyTargetServers;

import java.util.UUID;

/**
 * Proxy-specific extension of {@link CommonPlayer}. Implementations live in
 * the proxy platform modules (BungeeCord, Velocity) and can move players
 * between backend servers in the network.
 */
public abstract class ProxyPlayer extends CommonPlayer {

    protected ProxyPlayer(UUID uniqueId) {
        super(uniqueId, null);
    }

    protected ProxyPlayer(UUID uniqueId, SignInLocation signinLocation) {
        super(uniqueId, signinLocation);
    }

    @Override
    protected void onVerifiedImpl() {
        ProxyTargetServers.sendPlayerToPostVerificationServer(this);
    }

    /**
     * Send this player to another server on the proxy network.
     *
     * @param serverName logical/server name as understood by the proxy
     */
    public abstract void sendToServer(String serverName);
}




