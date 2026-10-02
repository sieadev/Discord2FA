package dev.siea.discord2fa.common.database;

import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.database.models.SignInLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behaviour every supported database must share. Subclasses provide an adapter for one backend;
 * each test gets an empty schema.
 */
abstract class DatabaseAdapterContract {

    protected DatabaseAdapter db;

    /** Returns an adapter connected to an empty database. */
    protected abstract DatabaseAdapter createAdapter() throws Exception;

    @BeforeEach
    void open() throws Exception {
        db = createAdapter();
    }

    @AfterEach
    void close() {
        if (db != null) db.close();
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    @Test
    void unknownPlayerIsNotLinked() {
        assertNull(db.getLinkedPlayer(UUID.randomUUID()));
        assertEquals(Optional.empty(), db.getLinkedByDiscord(123L));
    }

    @Test
    void savedLinkCanBeReadByMinecraftUuidAndDiscordId() {
        UUID uuid = UUID.randomUUID();
        Instant linkedAt = now();
        db.saveLinkedPlayer(new LinkedPlayer(uuid, 987654321012345678L, linkedAt));

        LinkedPlayer byUuid = db.getLinkedPlayer(uuid);
        assertNotNull(byUuid);
        assertEquals(987654321012345678L, byUuid.getDiscordId(), "Discord snowflakes must round-trip as 64-bit");
        // MySQL/MariaDB store time_linked as TIMESTAMP (whole seconds, rounded)
        long driftMillis = Math.abs(Duration.between(linkedAt, byUuid.getTimeLinked()).toMillis());
        assertTrue(driftMillis <= 1000, "time_linked drifted by " + driftMillis + "ms");

        LinkedPlayer byDiscord = db.getLinkedByDiscord(987654321012345678L).orElseThrow();
        assertEquals(uuid, byDiscord.getMinecraftUuid());
    }

    @Test
    void savingAgainReplacesTheLink() {
        UUID uuid = UUID.randomUUID();
        db.saveLinkedPlayer(new LinkedPlayer(uuid, 1L, now()));
        db.saveLinkedPlayer(new LinkedPlayer(uuid, 2L, now()));

        assertEquals(2L, db.getLinkedPlayer(uuid).getDiscordId());
        assertEquals(Optional.empty(), db.getLinkedByDiscord(1L));
    }

    @Test
    void removingALinkOnlyAffectsThatPlayer() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        db.saveLinkedPlayer(new LinkedPlayer(a, 1L, now()));
        db.saveLinkedPlayer(new LinkedPlayer(b, 2L, now()));

        db.removeLinkedPlayer(a);
        db.removeLinkedPlayer(UUID.randomUUID()); // no-op

        assertNull(db.getLinkedPlayer(a));
        assertNotNull(db.getLinkedPlayer(b));
    }

    @Test
    void recentSignInLocationMatchesOnPlayerIpAndVersion() {
        UUID uuid = UUID.randomUUID();
        db.addLoginLocation("203.0.113.7", "1.21.11", uuid, now());

        assertTrue(db.hasRecentSignInLocation(uuid, "203.0.113.7", "1.21.11"));
        assertFalse(db.hasRecentSignInLocation(uuid, "203.0.113.8", "1.21.11"), "different IP");
        assertFalse(db.hasRecentSignInLocation(uuid, "203.0.113.7", "26.3"), "different client version");
        assertFalse(db.hasRecentSignInLocation(UUID.randomUUID(), "203.0.113.7", "1.21.11"), "different player");
    }

    @Test
    void signInLocationsOlderThanThirtyDaysAreNotTrusted() {
        UUID uuid = UUID.randomUUID();
        db.addLoginLocation("203.0.113.7", "1.21", uuid, now().minus(Duration.ofDays(31)));
        db.addLoginLocation("203.0.113.9", "1.21", uuid, now().minus(Duration.ofDays(29)));

        assertFalse(db.hasRecentSignInLocation(uuid, "203.0.113.7", "1.21"));
        assertTrue(db.hasRecentSignInLocation(uuid, "203.0.113.9", "1.21"));
    }

    @Test
    void ipv6AddressesFit() {
        UUID uuid = UUID.randomUUID();
        String ip = "ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255"; // 45 chars, the column maximum
        db.addLoginLocation(ip, "1.21", uuid, now());

        assertTrue(db.hasRecentSignInLocation(uuid, ip, "1.21"));
    }

    @Test
    void signInLocationsAreListedNewestFirst() {
        UUID uuid = UUID.randomUUID();
        Instant t = now();
        db.addLoginLocation("10.0.0.1", "1.21", uuid, t.minus(Duration.ofDays(2)));
        db.addLoginLocation("10.0.0.2", "1.21", uuid, t);
        db.addLoginLocation("10.0.0.3", "1.21", UUID.randomUUID(), t);

        List<SignInLocation> locations = db.getSignInLocations(uuid);

        assertEquals(List.of("10.0.0.2", "10.0.0.1"), locations.stream().map(SignInLocation::getIpAddress).toList());
        assertEquals(uuid, locations.get(0).getMinecraftUuid());
        assertEquals(t, locations.get(0).getTimeOfLogin());
    }

    @Test
    void purgeDeletesOnlyOldLocations() {
        UUID uuid = UUID.randomUUID();
        db.addLoginLocation("10.0.0.1", "1.21", uuid, now().minus(Duration.ofDays(40)));
        db.addLoginLocation("10.0.0.2", "1.21", uuid, now().minus(Duration.ofDays(31)));
        db.addLoginLocation("10.0.0.3", "1.21", uuid, now());

        assertEquals(2, db.purgeSignInLocationsOlderThan(30));
        assertEquals(List.of("10.0.0.3"), db.getSignInLocations(uuid).stream().map(SignInLocation::getIpAddress).toList());
    }

    @Test
    void botStateUpsertsByKey() {
        assertNull(db.getState(DatabaseAdapter.STATE_LINK_MESSAGE_ID));

        db.setState(DatabaseAdapter.STATE_LINK_MESSAGE_ID, "111");
        db.setState(DatabaseAdapter.STATE_LINK_MESSAGE_ID, "222");
        db.setState("other", "x");

        assertEquals("222", db.getState(DatabaseAdapter.STATE_LINK_MESSAGE_ID));
        assertEquals("x", db.getState("other"));
    }

    @Test
    void valuesAreBoundNotInterpolated() {
        UUID uuid = UUID.randomUUID();
        String hostile = "1.21'; DROP TABLE linked_players; --";
        db.saveLinkedPlayer(new LinkedPlayer(uuid, 1L, now()));
        db.addLoginLocation("10.0.0.1", hostile, uuid, now());
        db.setState("k'--", "v'); DELETE FROM bot_state; --");

        assertTrue(db.hasRecentSignInLocation(uuid, "10.0.0.1", hostile));
        assertFalse(db.hasRecentSignInLocation(uuid, "10.0.0.1' OR '1'='1", "1.21"));
        assertEquals("v'); DELETE FROM bot_state; --", db.getState("k'--"));
        assertNotNull(db.getLinkedPlayer(uuid));
    }
}
