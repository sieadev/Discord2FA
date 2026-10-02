package dev.siea.discord2fa.common.discord;

import dev.siea.discord2fa.common.database.models.SignInLocation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-flight sign-in verification requests, one per Discord user. Each request is bound to the player's
 * session generation; the generation is embedded in the DM button IDs so a button from an older DM
 * can never complete a newer session's request.
 */
final class PendingVerifications {

    static final String ACCEPT_PREFIX = "verify_accept_";
    static final String DENY_PREFIX = "verify_deny_";

    /** A resolved button press: whether it was accepted and the request it belongs to. */
    record Decision(boolean accepted, SignInLocation signInLocation, CompletableFuture<Boolean> future) {
    }

    private record Pending(CompletableFuture<Boolean> future, SignInLocation signInLocation, long sessionGeneration) {
    }

    private final Map<Long, Pending> pending = new ConcurrentHashMap<>();

    static String acceptId(long sessionGeneration) {
        return ACCEPT_PREFIX + sessionGeneration;
    }

    static String denyId(long sessionGeneration) {
        return DENY_PREFIX + sessionGeneration;
    }

    /**
     * Registers a new request for the Discord user, cancelling (completing with false) any older one.
     *
     * @return future completed with true on accept, false on deny or cancel
     */
    CompletableFuture<Boolean> register(long discordId, SignInLocation signInLocation, long sessionGeneration) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Pending previous = pending.put(discordId, new Pending(future, signInLocation, sessionGeneration));
        if (previous != null) previous.future.complete(false);
        return future;
    }

    /** Cancels the user's in-flight request, if any, completing it with false. */
    void cancel(long discordId) {
        Pending previous = pending.remove(discordId);
        if (previous != null) previous.future.complete(false);
    }

    /**
     * Matches a button press against the user's in-flight request and removes it. The caller completes
     * {@link Decision#future()} once it has acted on the decision.
     *
     * @return the decision, or null when the button is malformed, from another session, or the request expired
     */
    Decision resolve(long discordId, String customId) {
        if (customId == null) return null;
        boolean accepted;
        String generationPart;
        if (customId.startsWith(ACCEPT_PREFIX)) {
            accepted = true;
            generationPart = customId.substring(ACCEPT_PREFIX.length());
        } else if (customId.startsWith(DENY_PREFIX)) {
            accepted = false;
            generationPart = customId.substring(DENY_PREFIX.length());
        } else {
            return null;
        }
        long generation;
        try {
            generation = Long.parseLong(generationPart);
        } catch (NumberFormatException e) {
            return null;
        }
        Pending current = pending.get(discordId);
        if (current == null || current.sessionGeneration != generation) return null;
        if (!pending.remove(discordId, current)) return null;
        return new Decision(accepted, current.signInLocation, current.future);
    }
}
