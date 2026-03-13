package dev.siea.discord2fa.common.server;

import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.event.EventType;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ServerConfig {

    private final Set<String> allowedActions;
    private final Set<String> allowedCommands;
    private final boolean forceLink;
    private final boolean rememberSignInLocation;

    public ServerConfig(ConfigAdapter config) {
        forceLink = config.getBoolean("forceLink");
        rememberSignInLocation = config.getBoolean("rememberSignInLocation");
        List<String> actions = config.getStringList("allowedActions");
        if (actions == null) actions = Collections.emptyList();
        allowedActions = actions.stream()
            .map(s -> s == null ? "" : s.trim().toUpperCase())
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

        List<String> commands = config.getStringList("allowedCommands");
        if (commands == null) commands = Collections.emptyList();
        allowedCommands = commands.stream()
            .map(ServerConfig::normalizeCommand)
            .collect(Collectors.toSet());
    }

    private static String normalizeCommand(String cmd) {
        if (cmd == null) return "";
        String s = cmd.trim().toLowerCase();
        if (s.startsWith("/")) s = s.substring(1);
        return s;
    }

    /**
     * Returns true if this event type is in the allowed whitelist.
     * EventType is mapped to config names: BREAK, PLACE, CHAT, MOVE, DROP, INVENTORY, SERVER_SWITCH.
     */
    public boolean isEventAllowed(EventType eventType) {
        if (eventType == null) return false;
        String name = toConfigName(eventType);
        return name != null && allowedActions.contains(name);
    }

    public boolean isCommandAllowed(String commandLabel) {
        return commandLabel != null && allowedCommands.contains(normalizeCommand(commandLabel));
    }

    /** True if users must link their Discord account (force-link in config). */
    public boolean isForceLink() {
        return forceLink;
    }

    /** True if we remember sign-in locations and skip verify when same IP+version within 30 days. */
    public boolean isRememberSignInLocation() {
        return rememberSignInLocation;
    }

    private static String toConfigName(EventType type) {
        return switch (type) {
            case CHAT -> "CHAT";
            case MOVE -> "MOVE";
            case BLOCK_BREAK -> "BREAK";
            case BLOCK_PLACE -> "PLACE";
            case DROP -> "DROP";
            case INVENTORY -> "INVENTORY";
            default -> null;
        };
    }
}
