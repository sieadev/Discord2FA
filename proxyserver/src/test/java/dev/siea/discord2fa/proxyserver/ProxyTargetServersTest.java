package dev.siea.discord2fa.proxyserver;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.proxyserver.player.ProxyPlayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProxyTargetServersTest {

    private static ConfigAdapter config(Map<String, String> values) {
        return new ConfigAdapter() {
            @Override public String getString(String key) { return values.get(key); }
            @Override public int getInt(String key) { return 0; }
            @Override public boolean getBoolean(String key) { return false; }
            @Override public List<String> getStringList(String key) { return List.of(); }
        };
    }

    private static final class RoutedPlayer extends ProxyPlayer {
        final List<String> sentTo = new ArrayList<>();

        RoutedPlayer() {
            super(UUID.randomUUID());
        }

        @Override public void sendToServer(String serverName) { sentTo.add(serverName); }
        @Override public String getName() { return "p"; }
        @Override public void sendMessage(String message) { }
        @Override public void sendTitle(String title, String subtitle, int fadeIn, int duration, int fadeOut) { }
        @Override public void kick(String reason) { }
    }

    @Test
    void routesToConfiguredServers() {
        ProxyTargetServers.initialize(config(Map.of("server.verification", " lobby ", "server.post-verification", "survival")));
        RoutedPlayer player = new RoutedPlayer();

        ProxyTargetServers.sendPlayerToVerificationServer(player);
        ProxyTargetServers.sendPlayerToPostVerificationServer(player);

        assertEquals("lobby", ProxyTargetServers.getVerificationServer());
        assertEquals(List.of("lobby", "survival"), player.sentTo);
    }

    @Test
    void unconfiguredServersAreNoops() {
        ProxyTargetServers.initialize(config(Map.of("server.verification", "  ")));
        RoutedPlayer player = new RoutedPlayer();

        ProxyTargetServers.sendPlayerToVerificationServer(player);
        ProxyTargetServers.sendPlayerToPostVerificationServer(player);

        assertNull(ProxyTargetServers.getVerificationServer());
        assertNull(ProxyTargetServers.getPostVerificationServer());
        assertTrue(player.sentTo.isEmpty());
    }

    @Test
    void verifyingAProxyPlayerSendsThemToThePostVerificationServer() {
        ProxyTargetServers.initialize(config(Map.of("server.post-verification", "survival")));
        RoutedPlayer player = new RoutedPlayer();
        List<String> order = new ArrayList<>();
        player.setOnVerifiedCallback(() -> order.add("callback"));

        player.onVerified();

        assertEquals(List.of("survival"), player.sentTo);
        assertEquals(List.of("callback"), order);
    }
}
