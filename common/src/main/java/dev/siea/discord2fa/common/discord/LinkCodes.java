package dev.siea.discord2fa.common.discord;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time link codes handed out in Discord DMs and redeemed in-game with /link. Codes are generated with
 * {@link SecureRandom}, are valid once, expire after {@link #DEFAULT_TTL}, and each Discord user has at
 * most one outstanding code.
 *
 * @param <U> the Discord user type stored with a code
 */
final class LinkCodes<U> {

    static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    static final int CODE_LENGTH = 8;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private record Entry<U>(U user, long discordId, Instant expiresAt) {
    }

    private final Map<String, Entry<U>> codes = new ConcurrentHashMap<>();
    private final Map<Long, String> codeByDiscordId = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final Duration ttl;

    LinkCodes() {
        this(Clock.systemUTC(), DEFAULT_TTL);
    }

    LinkCodes(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    /** True if the user already has an unexpired code. */
    synchronized boolean hasPendingCode(long discordId) {
        String code = codeByDiscordId.get(discordId);
        if (code == null) return false;
        Entry<U> entry = codes.get(code);
        if (entry == null || isExpired(entry)) {
            removeCode(code);
            return false;
        }
        return true;
    }

    /**
     * Issues a new code for the user, or empty if they already have an unexpired one.
     */
    synchronized Optional<String> issue(long discordId, U user) {
        if (hasPendingCode(discordId)) return Optional.empty();
        String code;
        do {
            code = generate();
        } while (codes.containsKey(code));
        codes.put(code, new Entry<>(user, discordId, clock.instant().plus(ttl)));
        codeByDiscordId.put(discordId, code);
        return Optional.of(code);
    }

    /**
     * Redeems a code. The code is removed whether or not it has expired, so it can only be tried once successfully.
     *
     * @return the user the code was issued to, or empty if unknown or expired
     */
    synchronized Optional<U> consume(String code) {
        if (code == null) return Optional.empty();
        Entry<U> entry = codes.get(code);
        if (entry == null) return Optional.empty();
        removeCode(code);
        return isExpired(entry) ? Optional.empty() : Optional.of(entry.user);
    }

    private void removeCode(String code) {
        Entry<U> entry = codes.remove(code);
        if (entry != null) codeByDiscordId.remove(entry.discordId, code);
    }

    private boolean isExpired(Entry<U> entry) {
        return !clock.instant().isBefore(entry.expiresAt);
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
