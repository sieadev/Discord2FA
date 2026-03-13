package dev.siea.discord2fa.common.database.models;

import java.time.Instant;
import java.util.UUID;

/**
 * A single login/sign-in event: IP, client version, and time for a Minecraft player.
 */
public final class SignInLocation {

    private final long id;
    private final String ipAddress;
    private final String version;
    private final UUID minecraftUuid;
    private final Instant timeOfLogin;

    public SignInLocation(long id, String ipAddress, String version, UUID minecraftUuid, Instant timeOfLogin) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.version = version;
        this.minecraftUuid = minecraftUuid;
        this.timeOfLogin = timeOfLogin;
    }

    public long getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getVersion() {
        return version;
    }

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public Instant getTimeOfLogin() {
        return timeOfLogin;
    }
}
