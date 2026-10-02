package dev.siea.discord2fa.common.server;

import dev.siea.discord2fa.common.event.EventType;
import dev.siea.discord2fa.common.testutil.MapConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {

    @Test
    void emptyConfigAllowsNothingAndDisablesFlags() {
        ServerConfig config = new ServerConfig(new MapConfig());

        assertFalse(config.isForceLink());
        assertFalse(config.isRememberSignInLocation());
        for (EventType type : EventType.values()) {
            assertFalse(config.isEventAllowed(type), type.name());
        }
        assertFalse(config.isCommandAllowed("link"));
    }

    @Test
    void commandsAreNormalizedForSlashCaseAndWhitespace() {
        ServerConfig config = new ServerConfig(new MapConfig().set("allowedCommands", List.of(" /Link ", "help")));

        assertTrue(config.isCommandAllowed("link"));
        assertTrue(config.isCommandAllowed("/LINK"));
        assertTrue(config.isCommandAllowed("  link "));
        assertTrue(config.isCommandAllowed("/help"));
        assertFalse(config.isCommandAllowed("linkx"));
        assertFalse(config.isCommandAllowed("unlink"));
        assertFalse(config.isCommandAllowed(null));
    }

    @Test
    void namespacedCommandIsNotTheSameAsAllowedCommand() {
        ServerConfig config = new ServerConfig(new MapConfig().set("allowedCommands", List.of("/link")));

        assertFalse(config.isCommandAllowed("discord2fa:link"));
        assertFalse(config.isCommandAllowed("minecraft:tp"));
    }

    @Test
    void blankAndNullCommandEntriesDoNotAllowEmptyCommand() {
        ServerConfig config = new ServerConfig(new MapConfig().set("allowedCommands", Arrays.asList("/link", null)));

        assertTrue(config.isCommandAllowed("link"));
        // a null entry normalizes to "" — make sure that does not open up anything meaningful
        assertFalse(config.isCommandAllowed("spawn"));
    }

    @Test
    void actionsMapToTheirConfigNames() {
        ServerConfig config = new ServerConfig(new MapConfig()
                .set("allowedActions", List.of("chat", " Move ", "BREAK", "PLACE", "DROP", "INVENTORY")));

        assertTrue(config.isEventAllowed(EventType.CHAT));
        assertTrue(config.isEventAllowed(EventType.MOVE));
        assertTrue(config.isEventAllowed(EventType.BLOCK_BREAK));
        assertTrue(config.isEventAllowed(EventType.BLOCK_PLACE));
        assertTrue(config.isEventAllowed(EventType.DROP));
        assertTrue(config.isEventAllowed(EventType.INVENTORY));
    }

    @Test
    void commandEventCanNeverBeWhitelistedAsAnAction() {
        ServerConfig config = new ServerConfig(new MapConfig().set("allowedActions", List.of("COMMAND", "BLOCK_BREAK")));

        assertFalse(config.isEventAllowed(EventType.COMMAND));
        assertFalse(config.isEventAllowed(EventType.BLOCK_BREAK), "only the documented name BREAK is accepted");
        assertFalse(config.isEventAllowed(null));
    }

    @Test
    void flagsAreRead() {
        ServerConfig config = new ServerConfig(new MapConfig().set("forceLink", true).set("rememberSignInLocation", true));

        assertTrue(config.isForceLink());
        assertTrue(config.isRememberSignInLocation());
    }
}
