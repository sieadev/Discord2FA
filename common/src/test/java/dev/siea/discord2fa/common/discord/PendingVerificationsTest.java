package dev.siea.discord2fa.common.discord;

import dev.siea.discord2fa.common.database.models.SignInLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class PendingVerificationsTest {

    private static final long DISCORD_ID = 42L;
    private final PendingVerifications pending = new PendingVerifications();
    private final SignInLocation location = new SignInLocation(0, "203.0.113.7", "1.21", UUID.randomUUID(), Instant.now());

    @Test
    void acceptButtonForCurrentGenerationResolvesAsAccepted() {
        CompletableFuture<Boolean> future = pending.register(DISCORD_ID, location, 3);

        PendingVerifications.Decision decision = pending.resolve(DISCORD_ID, PendingVerifications.acceptId(3));

        assertNotNull(decision);
        assertTrue(decision.accepted());
        assertSame(location, decision.signInLocation());
        assertSame(future, decision.future());
        assertFalse(future.isDone(), "the caller completes the future after acting on the decision");
    }

    @Test
    void denyButtonResolvesAsDenied() {
        pending.register(DISCORD_ID, location, 3);

        PendingVerifications.Decision decision = pending.resolve(DISCORD_ID, PendingVerifications.denyId(3));

        assertNotNull(decision);
        assertFalse(decision.accepted());
    }

    @Test
    void buttonFromAnOlderSessionCannotResolveTheCurrentRequest() {
        pending.register(DISCORD_ID, location, 4);

        assertNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(3)));
        assertNotNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(4)), "current request must still be pending");
    }

    @Test
    void anotherDiscordUserCannotResolveTheRequest() {
        pending.register(DISCORD_ID, location, 1);

        assertNull(pending.resolve(DISCORD_ID + 1, PendingVerifications.acceptId(1)));
        assertNotNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(1)));
    }

    @Test
    void aRequestResolvesOnlyOnce() {
        pending.register(DISCORD_ID, location, 1);
        assertNotNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(1)));

        assertNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(1)));
        assertNull(pending.resolve(DISCORD_ID, PendingVerifications.denyId(1)));
    }

    @Test
    void registeringANewRequestCancelsTheOldOne() {
        CompletableFuture<Boolean> first = pending.register(DISCORD_ID, location, 1);
        pending.register(DISCORD_ID, location, 2);

        assertEquals(Boolean.FALSE, first.getNow(null));
        assertNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(1)));
    }

    @Test
    void cancelCompletesWithFalseAndInvalidatesButtons() {
        CompletableFuture<Boolean> future = pending.register(DISCORD_ID, location, 1);

        pending.cancel(DISCORD_ID);

        assertEquals(Boolean.FALSE, future.getNow(null));
        assertNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(1)));
    }

    @Test
    void cancelWithoutPendingRequestIsANoop() {
        assertDoesNotThrow(() -> pending.cancel(DISCORD_ID));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", "link", "verify_accept", "verify_accept_", "verify_accept_abc", "verify_accept_1x",
            "verify_deny_", "verify_maybe_1", "verify_accept_1_2", "VERIFY_ACCEPT_1", "xverify_accept_1"
    })
    void malformedButtonIdsNeverResolve(String customId) {
        CompletableFuture<Boolean> future = pending.register(DISCORD_ID, location, 1);

        assertNull(pending.resolve(DISCORD_ID, customId));
        assertFalse(future.isDone());
        assertNotNull(pending.resolve(DISCORD_ID, PendingVerifications.acceptId(1)), "malformed presses must not consume the request");
    }

    @Test
    void buttonIdsEmbedTheGeneration() {
        assertEquals("verify_accept_7", PendingVerifications.acceptId(7));
        assertEquals("verify_deny_7", PendingVerifications.denyId(7));
    }
}
