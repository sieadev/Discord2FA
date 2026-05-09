package dev.siea.discord2fa.common.player;

import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.database.models.SignInLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * Minimal, platform-agnostic representation of a player.
 * Verification state is not stored here; the server tracks "verifying" players
 * in a map and removes them when verification succeeds.
 */
public abstract class CommonPlayer {

    private final UUID uniqueId;
    private LinkedPlayer linkedPlayer;
    private final SignInLocation signinLocation;
    private Runnable onVerifiedCallback;

    /**
     * Create a CommonPlayer from a UUID (no sign-in location).
     */
    protected CommonPlayer(UUID uniqueId) {
        this(uniqueId, null);
    }

    /**
     * Create a CommonPlayer from a UUID and optional sign-in location (null if not available).
     */
    protected CommonPlayer(UUID uniqueId, SignInLocation signinLocation) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.signinLocation = signinLocation;
    }

    /**
     * Create a CommonPlayer from a UUID string (parsed).
     */
    protected CommonPlayer(String uuidString) {
        this(UUID.fromString(Objects.requireNonNull(uuidString, "uuidString")), null);
    }

    public final UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Called by the server when adding the player. Do not call from application code.
     */
    public final void setOnVerifiedCallback(Runnable onVerifiedCallback) {
        this.onVerifiedCallback = onVerifiedCallback;
    }

    /**
     * Call when verification succeeds. Runs the server callback (removes from verifying map)
     * then subclass logic (e.g. send to post-verification server).
     */
    public final void onVerified() {
        onVerifiedImpl();
        if (onVerifiedCallback != null) onVerifiedCallback.run();
    }

    /**
     * Override for post-verification behaviour (e.g. proxy sends player to next server).
     */
    protected void onVerifiedImpl() {
    }

    /**
     * Set the LinkedPlayer associated with this player, if any.
     * Intended to be called by server code when registering the player.
     */
    public final void setLinkedPlayer(LinkedPlayer linkedPlayer) {
        this.linkedPlayer = linkedPlayer;
    }

    /**
     * Returns true if this player has a LinkedPlayer entry.
     */
    public final boolean isLinked() {
        return linkedPlayer != null;
    }

    /**
     * Returns the LinkedPlayer for this player, or null if not linked.
     */
    public final LinkedPlayer getLinkedPlayer() {
        return linkedPlayer;
    }

    /**
     * Where this player is currently signing in from (IP, version), or null if not available.
     */
    public final SignInLocation getSigninLocation() {
        return signinLocation;
    }

    public abstract String getName();

    public abstract boolean hasPermission(String permission);

    public abstract void sendMessage(String message);

    public abstract void sendUrlButton(String text, String url);

    public abstract void sendTitle(String title, String subtitle, int fadeIn, int duration, int fadeOut);

    public final void sendTitle(String title) {
        sendTitle(title, "", 10, 60, 10);
    }

    public abstract void kick(String reason);
}


