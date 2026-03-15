package dev.siea.discord2fa.proxyserver;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.proxyserver.player.ProxyPlayer;

import java.util.function.Consumer;

/**
 * Static registry for the logical server names used by the proxy while a
 * player is going through verification and after they have been verified.
 * <p>
 * Expected config keys (resolved via {@link ConfigAdapter}):
 * <ul>
 *   <li><strong>proxy.verification-server</strong> – server players should be on while verifying</li>
 *   <li><strong>proxy.post-verification-server</strong> – server players should be sent to after verification</li>
 * </ul>
 *
 * The values are simple names/identifiers; the proxy platform modules
 * (BungeeCord, Velocity) are responsible for resolving them to actual
 * backend servers.
 */
public final class ProxyTargetServers {

    private static String verificationServer;
    private static String postVerificationServer;

    // Precomputed actions so per-player calls don't need null checks.
    private static Consumer<ProxyPlayer> verificationSender = p -> { };
    private static Consumer<ProxyPlayer> postVerificationSender = p -> { };

    /**
     * Initialize the target server names from configuration. Should be called
     * once during proxy bootstrap (e.g. from {@link dev.siea.discord2fa.proxyserver.ProxyServer}'s constructor).
     */
    public static void initialize(ConfigAdapter config) {
        verificationServer = trimToNull(config.getString("server.verification"));
        postVerificationServer = trimToNull(config.getString("server.post-verification"));

        verificationSender = verificationServer != null
            ? player -> player.sendToServer(verificationServer)
            : player -> { };

        postVerificationSender = postVerificationServer != null
            ? player -> player.sendToServer(postVerificationServer)
            : player -> { };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Server name players should be on while they are verifying, or null if not configured. */
    public static String getVerificationServer() {
        return verificationServer;
    }

    /** Server name players should be sent to after they have successfully verified, or null if not configured. */
    public static String getPostVerificationServer() {
        return postVerificationServer;
    }

    /**
     * Send a player to the verification server, if configured. No-op otherwise.
     */
    public static void sendPlayerToVerificationServer(ProxyPlayer player) {
        verificationSender.accept(player);
    }

    /**
     * Send a player to the post-verification server, if configured. No-op otherwise.
     */
    public static void sendPlayerToPostVerificationServer(ProxyPlayer player) {
        postVerificationSender.accept(player);
    }
}


