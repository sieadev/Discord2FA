package dev.siea.discord2fa.common.discord;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LinkCodesTest {

    /** Clock that only moves when told to. */
    private static final class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final ManualClock clock = new ManualClock();
    private final LinkCodes<String> codes = new LinkCodes<>(clock, Duration.ofMinutes(15));

    @Test
    void issuedCodeRedeemsToTheUserItWasIssuedTo() {
        String code = codes.issue(1L, "alice").orElseThrow();

        assertEquals(Optional.of("alice"), codes.consume(code));
    }

    @Test
    void codeCanOnlyBeRedeemedOnce() {
        String code = codes.issue(1L, "alice").orElseThrow();
        codes.consume(code);

        assertEquals(Optional.empty(), codes.consume(code));
    }

    @Test
    void unknownCodeAndNullAreRejected() {
        codes.issue(1L, "alice");

        assertEquals(Optional.empty(), codes.consume("nope1234"));
        assertEquals(Optional.empty(), codes.consume(null));
    }

    @Test
    void codesAreCaseSensitive() {
        String code = codes.issue(1L, "alice").orElseThrow();
        String flipped = flipCase(code);
        if (flipped.equals(code)) return; // all digits; nothing to flip

        assertEquals(Optional.empty(), codes.consume(flipped));
        assertEquals(Optional.of("alice"), codes.consume(code));
    }

    @Test
    void aUserGetsAtMostOneOutstandingCode() {
        assertTrue(codes.issue(1L, "alice").isPresent());

        assertTrue(codes.hasPendingCode(1L));
        assertEquals(Optional.empty(), codes.issue(1L, "alice"));
    }

    @Test
    void redeemingFreesTheUserToRequestAnotherCode() {
        String code = codes.issue(1L, "alice").orElseThrow();
        codes.consume(code);

        assertFalse(codes.hasPendingCode(1L));
        assertTrue(codes.issue(1L, "alice").isPresent());
    }

    @Test
    void codesFromDifferentUsersDoNotInterfere() {
        String a = codes.issue(1L, "alice").orElseThrow();
        String b = codes.issue(2L, "bob").orElseThrow();

        assertNotEquals(a, b);
        assertEquals(Optional.of("bob"), codes.consume(b));
        assertEquals(Optional.of("alice"), codes.consume(a));
    }

    @Test
    void expiredCodeIsRejectedAndConsumed() {
        String code = codes.issue(1L, "alice").orElseThrow();
        clock.advance(Duration.ofMinutes(15));

        assertEquals(Optional.empty(), codes.consume(code));
        clock.advance(Duration.ofSeconds(-1));
        assertEquals(Optional.empty(), codes.consume(code), "a rejected code must not become valid again");
    }

    @Test
    void codeIsValidUntilJustBeforeExpiry() {
        String code = codes.issue(1L, "alice").orElseThrow();
        clock.advance(Duration.ofMinutes(15).minusMillis(1));

        assertEquals(Optional.of("alice"), codes.consume(code));
    }

    @Test
    void expiryLetsTheUserRequestANewCode() {
        String old = codes.issue(1L, "alice").orElseThrow();
        clock.advance(Duration.ofMinutes(16));

        assertFalse(codes.hasPendingCode(1L));
        String fresh = codes.issue(1L, "alice").orElseThrow();
        assertEquals(Optional.empty(), codes.consume(old));
        assertEquals(Optional.of("alice"), codes.consume(fresh));
    }

    @Test
    void codesHaveTheExpectedShapeAndAreNotRepeated() {
        Set<String> seen = new HashSet<>();
        for (long id = 0; id < 2_000; id++) {
            String code = codes.issue(id, "u" + id).orElseThrow();
            assertEquals(LinkCodes.CODE_LENGTH, code.length());
            assertTrue(code.matches("[A-Za-z0-9]+"), code);
            assertTrue(seen.add(code), "duplicate code " + code);
        }
    }

    @Test
    void defaultTtlIsFifteenMinutes() {
        assertEquals(Duration.ofMinutes(15), LinkCodes.DEFAULT_TTL);
    }

    private static String flipCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return sb.toString();
    }
}
