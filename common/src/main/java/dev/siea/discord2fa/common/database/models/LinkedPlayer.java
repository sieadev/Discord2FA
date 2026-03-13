package dev.siea.discord2fa.common.database.models;

import dev.siea.discord2fa.common.database.DatabaseAdapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A player whose Minecraft account is linked to a Discord account.
 * Provides methods to query sign-in locations and check for recent sign-ins.
 */
public final class LinkedPlayer {

    private final UUID minecraftUuid;
    private final long discordId;
    private final Instant timeLinked;

    public LinkedPlayer(UUID minecraftUuid, long discordId, Instant timeLinked) {
        this.minecraftUuid = minecraftUuid;
        this.discordId = discordId;
        this.timeLinked = timeLinked;
    }

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public long getDiscordId() {
        return discordId;
    }

    public Instant getTimeLinked() {
        return timeLinked;
    }

    /**
     * Returns all sign-in (login) locations for this player, newest first.
     */
    public List<SignInLocation> getSignInLocations(DatabaseAdapter adapter) {
        return adapter.getSignInLocations(minecraftUuid);
    }

    /**
     * Returns true if a sign-in from the given IP and version is already stored
     * and is less than 30 days old.
     */
    public boolean hasRecentSignInLocation(DatabaseAdapter adapter, String ipAddress, String version) {
        return adapter.hasRecentSignInLocation(minecraftUuid, ipAddress, version);
    }
}
