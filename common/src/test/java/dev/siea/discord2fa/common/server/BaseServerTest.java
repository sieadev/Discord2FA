package dev.siea.discord2fa.common.server;

import dev.siea.discord2fa.common.database.DatabaseAdapter;
import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.discord.DiscordBot;
import dev.siea.discord2fa.common.event.EventType;
import dev.siea.discord2fa.common.player.CommonPlayer;
import dev.siea.discord2fa.common.testutil.Await;
import dev.siea.discord2fa.common.testutil.MapConfig;
import dev.siea.discord2fa.common.testutil.NoopLogger;
import dev.siea.discord2fa.common.testutil.RecordingPlayer;
import org.javacord.api.entity.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Behaviour of the verification state machine: who gets restricted, what lifts the restriction,
 * and that stale sessions or Discord responses can never unlock a newer login.
 * Uses a real SQLite database and a mocked Discord bot; messages are their lang keys.
 */
class BaseServerTest {

    private static final String IP = "203.0.113.7";
    private static final String VERSION = "1.21.11";
    private static final long DISCORD_ID = 111_222_333_444_555_666L;

    @TempDir
    Path dataFolder;

    private DatabaseAdapter db;
    private DiscordBot bot;
    /** Verify requests sent to the mocked bot, keyed by session generation. */
    private final Map<Long, CompletableFuture<Boolean>> verifyRequests = new ConcurrentHashMap<>();
    private TestServer server;

    /** Exposes the protected join hooks with recording callbacks. */
    static final class TestServer extends BaseServer {
        final AtomicInteger skipped = new AtomicInteger();
        final AtomicInteger required = new AtomicInteger();

        TestServer(MapConfig config, DatabaseAdapter db, DiscordBot bot) {
            super(new ServerConfig(config), db, bot, new NoopLogger(), key -> key, Runnable::run);
        }

        void join(CommonPlayer player) {
            join(player, false);
        }

        void join(CommonPlayer player, boolean forceVerify) {
            addPlayer(player, forceVerify, skipped::incrementAndGet, required::incrementAndGet);
        }

        /** Waits until the join decision (skip or verification required) has been made. */
        void awaitDecisions(int count) {
            Await.until(count + " join decision(s)", () -> skipped.get() + required.get() >= count);
        }
    }

    @BeforeEach
    void setUp() {
        db = new DatabaseAdapter(new MapConfig(), dataFolder);
        bot = mock(DiscordBot.class);
        when(bot.isConnected()).thenReturn(true);
        when(bot.attemptVerify(any(), any(), anyLong())).thenAnswer(inv -> {
            CompletableFuture<Boolean> f = new CompletableFuture<>();
            verifyRequests.put(inv.getArgument(2), f);
            return f;
        });
        server = newServer(defaultConfig());
    }

    @AfterEach
    void tearDown() {
        server.shutdown();
    }

    private static MapConfig defaultConfig() {
        return new MapConfig()
                .set("allowedCommands", List.of("/link"))
                .set("allowedActions", List.of("CHAT"))
                .set("forceLink", false)
                .set("rememberSignInLocation", true);
    }

    private TestServer newServer(MapConfig config) {
        if (server != null) server.shutdown();
        db = new DatabaseAdapter(new MapConfig(), dataFolder);
        server = new TestServer(config, db, bot);
        return server;
    }

    private void link(UUID uuid) {
        db.saveLinkedPlayer(new LinkedPlayer(uuid, DISCORD_ID, Instant.now()));
    }

    private RecordingPlayer joinLinked() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        RecordingPlayer player = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(player);
        server.awaitDecisions(1);
        return player;
    }

    private CompletableFuture<Boolean> awaitVerifyRequest(long generation) {
        Await.until("verify request for generation " + generation, () -> verifyRequests.containsKey(generation));
        return verifyRequests.get(generation);
    }

    private void assertRestricted(CommonPlayer player) {
        UUID uuid = player.getUniqueId();
        assertTrue(server.isPlayerPendingOrVerifying(uuid), "player should be restricted");
        for (EventType type : List.of(EventType.MOVE, EventType.BLOCK_BREAK, EventType.BLOCK_PLACE,
                EventType.DROP, EventType.INVENTORY, EventType.COMMAND)) {
            assertFalse(server.onEvent(uuid, type), type + " should be blocked");
        }
        assertFalse(server.onCommand(uuid, "spawn"));
        assertTrue(server.onCommand(uuid, "link"), "/link stays available");
    }

    private void assertUnrestricted(CommonPlayer player) {
        UUID uuid = player.getUniqueId();
        assertFalse(server.isPlayerPendingOrVerifying(uuid), "player should not be restricted");
        for (EventType type : EventType.values()) {
            assertTrue(server.onEvent(uuid, type), type + " should be allowed");
        }
        assertTrue(server.onCommand(uuid, "spawn"));
    }

    // ------------------------------------------------------------------ join decisions

    @Test
    void unlinkedPlayerWithoutForceLinkPlaysFreely() {
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);

        server.join(player);
        server.awaitDecisions(1);

        assertEquals(1, server.skipped.get());
        assertUnrestricted(player);
        verify(bot, never()).attemptVerify(any(), any(), anyLong());
    }

    @Test
    void unlinkedPlayerWithForceLinkIsRestrictedUntilLinked() {
        newServer(defaultConfig().set("forceLink", true));
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);

        server.join(player);
        server.awaitDecisions(1);

        assertEquals(1, server.required.get());
        assertRestricted(player);
        assertTrue(player.messages.contains("forceLink"));
        verify(bot, never()).attemptVerify(any(), any(), anyLong());
    }

    @Test
    void linkedPlayerFromNewLocationMustVerifyOnDiscord() {
        RecordingPlayer player = joinLinked();

        assertEquals(1, server.required.get());
        assertRestricted(player);
        verify(bot).attemptVerify(argThat(l -> l.getDiscordId() == DISCORD_ID), same(player.getSigninLocation()), eq(1L));
    }

    @Test
    void acceptingOnDiscordLiftsTheRestriction() {
        RecordingPlayer player = joinLinked();

        awaitVerifyRequest(1).complete(true);

        assertUnrestricted(player);
        assertEquals(1, player.verifiedCount);
        assertTrue(player.messages.contains("verifySuccess"));
        assertTrue(player.kicks.isEmpty());
    }

    @Test
    void denyingOnDiscordKicksAndKeepsTheRestriction() {
        RecordingPlayer player = joinLinked();

        awaitVerifyRequest(1).complete(false);

        assertEquals(List.of("verifyDenied"), player.kicks);
        assertEquals(0, player.verifiedCount);
        assertRestricted(player);
    }

    @Test
    void failedDiscordRequestCountsAsDenied() {
        reset(bot);
        when(bot.isConnected()).thenReturn(true);
        when(bot.attemptVerify(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(false));

        RecordingPlayer player = joinLinked();

        Await.until("kick", () -> !player.kicks.isEmpty());
        assertRestricted(player);
    }

    @Test
    void rememberedLocationSkipsVerification() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        db.addLoginLocation(IP, VERSION, uuid, Instant.now().minus(Duration.ofDays(1)));
        RecordingPlayer player = RecordingPlayer.at(uuid, IP, VERSION);

        server.join(player);
        server.awaitDecisions(1);

        assertEquals(1, server.skipped.get());
        assertUnrestricted(player);
        verify(bot, never()).attemptVerify(any(), any(), anyLong());
    }

    @Test
    void rememberedLocationIsIgnoredWhenTheFeatureIsOff() {
        newServer(defaultConfig().set("rememberSignInLocation", false));
        UUID uuid = UUID.randomUUID();
        link(uuid);
        db.addLoginLocation(IP, VERSION, uuid, Instant.now());
        RecordingPlayer player = RecordingPlayer.at(uuid, IP, VERSION);

        server.join(player);
        server.awaitDecisions(1);

        assertRestricted(player);
    }

    @Test
    void differentIpOrClientVersionRequiresVerification() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        db.addLoginLocation(IP, VERSION, uuid, Instant.now());

        RecordingPlayer otherIp = RecordingPlayer.at(uuid, "198.51.100.1", VERSION);
        server.join(otherIp);
        server.awaitDecisions(1);
        assertRestricted(otherIp);

        RecordingPlayer otherVersion = RecordingPlayer.at(uuid, IP, "26.3");
        server.join(otherVersion);
        server.awaitDecisions(2);
        assertRestricted(otherVersion);
        assertEquals(0, server.skipped.get());
    }

    @Test
    void locationOlderThanThirtyDaysRequiresVerification() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        db.addLoginLocation(IP, VERSION, uuid, Instant.now().minus(Duration.ofDays(31)));
        RecordingPlayer player = RecordingPlayer.at(uuid, IP, VERSION);

        server.join(player);
        server.awaitDecisions(1);

        assertRestricted(player);
    }

    @Test
    void sessionTakeoverIgnoresRememberedLocation() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        db.addLoginLocation(IP, VERSION, uuid, Instant.now());
        RecordingPlayer player = RecordingPlayer.at(uuid, IP, VERSION);

        server.join(player, true);
        server.awaitDecisions(1);

        assertRestricted(player);
        verify(bot).attemptVerify(any(), any(), anyLong());
    }

    @Test
    void playerWithoutKnownLocationMustVerify() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        RecordingPlayer player = new RecordingPlayer(uuid, null);

        server.join(player);
        server.awaitDecisions(1);

        assertRestricted(player);
    }

    @Test
    void playerIsRestrictedWhileTheJoinLookupIsStillRunning() throws Exception {
        // Block the DB thread: the first player's verify request is dispatched on it (serverExecutor is direct).
        CountDownLatch release = new CountDownLatch(1);
        reset(bot);
        when(bot.isConnected()).thenReturn(true);
        when(bot.attemptVerify(any(), any(), anyLong())).thenAnswer(inv -> {
            release.await(5, TimeUnit.SECONDS);
            return new CompletableFuture<Boolean>();
        });
        UUID blocker = UUID.randomUUID();
        link(blocker);
        server.join(RecordingPlayer.at(blocker, IP, VERSION));
        Await.until("db thread blocked", () -> mockingDetails(bot).getInvocations().stream()
                .anyMatch(i -> i.getMethod().getName().equals("attemptVerify")));

        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION); // unlinked: will be skipped
        server.join(player);

        assertTrue(server.isPlayerPendingOrVerifying(player.getUniqueId()));
        assertFalse(server.onEvent(player.getUniqueId(), EventType.BLOCK_BREAK));
        assertFalse(server.onCommand(player.getUniqueId(), "op"));

        release.countDown();
        server.awaitDecisions(2);
        assertUnrestricted(player);
    }

    // ------------------------------------------------------------------ fail closed

    @Test
    void discordOutageFailsClosed() {
        when(bot.isConnected()).thenReturn(false);
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION); // even unlinked players

        server.join(player);

        assertEquals(1, server.required.get());
        assertEquals(0, server.skipped.get());
        assertTrue(player.messages.contains("verifyUnavailable"));
        assertRestricted(player);
        assertFalse(server.shouldSkipVerificationBlocking(player));
        verify(bot, never()).attemptVerify(any(), any(), anyLong());
    }

    @Test
    void shouldSkipVerificationBlockingMatchesTheJoinDecision() {
        UUID linked = UUID.randomUUID();
        link(linked);
        UUID remembered = UUID.randomUUID();
        link(remembered);
        db.addLoginLocation(IP, VERSION, remembered, Instant.now());

        assertTrue(server.shouldSkipVerificationBlocking(RecordingPlayer.at(UUID.randomUUID(), IP, VERSION)));
        assertFalse(server.shouldSkipVerificationBlocking(RecordingPlayer.at(linked, IP, VERSION)));
        assertTrue(server.shouldSkipVerificationBlocking(RecordingPlayer.at(remembered, IP, VERSION)));
        assertFalse(server.shouldSkipVerificationBlocking(RecordingPlayer.at(remembered, IP, VERSION), true));
    }

    // ------------------------------------------------------------------ sessions

    @Test
    void rejoiningStartsANewSessionAndCancelsTheOldDiscordRequest() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        RecordingPlayer first = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(first);
        server.awaitDecisions(1);
        awaitVerifyRequest(1);

        RecordingPlayer second = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(second);
        server.awaitDecisions(2);

        verify(bot, atLeast(2)).cancelPendingVerify(DISCORD_ID);
        awaitVerifyRequest(2);
        assertTrue(server.isCurrentSession(uuid, 2));
        assertFalse(server.isCurrentSession(uuid, 1));
        assertTrue(server.isActiveSessionPlayer(uuid, second));
    }

    @Test
    void approvalForAnOldSessionDoesNotUnlockTheNewOne() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        RecordingPlayer first = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(first);
        server.awaitDecisions(1);
        CompletableFuture<Boolean> oldRequest = awaitVerifyRequest(1);

        RecordingPlayer second = RecordingPlayer.at(uuid, "198.51.100.1", VERSION);
        server.join(second);
        server.awaitDecisions(2);
        awaitVerifyRequest(2);

        oldRequest.complete(true);

        assertRestricted(second);
        assertEquals(0, first.verifiedCount);
        assertEquals(0, second.verifiedCount);
        assertFalse(first.messages.contains("verifySuccess"));
    }

    @Test
    void verifyingAnOldWrapperDoesNotUnlockTheNewSession() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        RecordingPlayer first = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(first);
        server.awaitDecisions(1);
        RecordingPlayer second = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(second);
        server.awaitDecisions(2);

        first.onVerified(); // stale callback from the first session

        assertRestricted(second);
    }

    @Test
    void lateDisconnectOfAReplacedConnectionDoesNotClearTheNewSession() {
        UUID uuid = UUID.randomUUID();
        link(uuid);
        RecordingPlayer first = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(first);
        server.awaitDecisions(1);
        RecordingPlayer second = RecordingPlayer.at(uuid, IP, VERSION);
        server.join(second);
        server.awaitDecisions(2);
        clearInvocations(bot);

        server.endPlayerSession(uuid, first);

        assertRestricted(second);
        assertTrue(server.isActiveSessionPlayer(uuid, second));
        verify(bot, never()).cancelPendingVerify(anyLong());
    }

    @Test
    void disconnectClearsStateAndCancelsTheDiscordRequest() {
        RecordingPlayer player = joinLinked();

        server.endPlayerSession(player.getUniqueId(), player);

        assertFalse(server.isPlayerPendingOrVerifying(player.getUniqueId()));
        assertFalse(server.isActiveSessionPlayer(player.getUniqueId(), player));
        verify(bot, atLeastOnce()).cancelPendingVerify(DISCORD_ID);
    }

    @Test
    void approvalArrivingAfterDisconnectAndRejoinIsIgnored() {
        RecordingPlayer player = joinLinked();
        CompletableFuture<Boolean> request = awaitVerifyRequest(1);
        server.endPlayerSession(player.getUniqueId(), player);
        RecordingPlayer again = RecordingPlayer.at(player.getUniqueId(), IP, VERSION);
        server.join(again);
        server.awaitDecisions(2);

        request.complete(true);

        assertRestricted(again);
    }

    // ------------------------------------------------------------------ actions & commands while restricted

    @Test
    void allowedActionsStillWorkWhileRestricted() {
        RecordingPlayer player = joinLinked();

        assertTrue(server.onEvent(player.getUniqueId(), EventType.CHAT));
        assertFalse(server.onEvent(player.getUniqueId(), EventType.MOVE));
    }

    @Test
    void blockedActionsRemindLinkedPlayersToVerify() {
        RecordingPlayer player = joinLinked();

        server.onEvent(player.getUniqueId(), EventType.MOVE);
        server.onCommand(player.getUniqueId(), "spawn");

        assertEquals(List.of("verifyTitle", "verifyTitle"), player.titles);
    }

    @Test
    void allowedCommandCheckIsNormalized() {
        RecordingPlayer player = joinLinked();
        UUID uuid = player.getUniqueId();

        assertTrue(server.onCommand(uuid, "LINK"));
        assertTrue(server.onCommand(uuid, "/link"));
        assertFalse(server.onCommand(uuid, "discord2fa:link"));
        assertFalse(server.onCommand(uuid, ""));
        assertFalse(server.onCommand(uuid, null));
    }

    @Test
    void deniedCommandMessageDependsOnLinkState() {
        RecordingPlayer linked = joinLinked();
        assertEquals("notVerified", server.getCommandDeniedMessage(linked.getUniqueId(), "spawn").join());
        assertNull(server.getCommandDeniedMessage(linked.getUniqueId(), "link").join());

        newServer(defaultConfig().set("forceLink", true));
        RecordingPlayer unlinked = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);
        server.join(unlinked);
        server.awaitDecisions(1);
        assertEquals("forceLink", server.getCommandDeniedMessage(unlinked.getUniqueId(), "spawn").join());

        assertNull(server.getCommandDeniedMessage(UUID.randomUUID(), "spawn").join(), "unrestricted players are never denied");
    }

    @Test
    void handleCommandRefusesNonAllowedCommandsWhileRestricted() {
        RecordingPlayer player = joinLinked();

        assertTrue(server.handleCommand(player, "unlink", List.of()).join());

        assertTrue(player.messages.contains("notVerified"));
        assertNotNull(db.getLinkedPlayer(player.getUniqueId()), "unlink must not run before verification");
    }

    @Test
    void unknownCommandsAreNotHandled() {
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);

        assertFalse(server.handleCommand(player, "spawn", List.of()).join());
    }

    // ------------------------------------------------------------------ /link

    private User discordUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(DISCORD_ID);
        return user;
    }

    @Test
    void linkWithValidCodeLinksAndVerifiesAForceLinkedPlayer() {
        newServer(defaultConfig().set("forceLink", true));
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);
        server.join(player);
        server.awaitDecisions(1);
        User user = discordUser();
        when(bot.consumeLinkCode("Ab12Cd34")).thenReturn(Optional.of(user));

        assertTrue(server.handleCommand(player, "link", List.of("Ab12Cd34")).join());

        assertEquals(DISCORD_ID, db.getLinkedPlayer(player.getUniqueId()).getDiscordId());
        assertTrue(player.messages.contains("linkSuccess"));
        assertEquals(1, player.verifiedCount);
        assertTrue(player.isLinked());
        assertUnrestricted(player);
        verify(bot).giveVerifiedRole(user);
    }

    @Test
    void linkWithInvalidCodeKeepsThePlayerRestricted() {
        newServer(defaultConfig().set("forceLink", true));
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);
        server.join(player);
        server.awaitDecisions(1);
        when(bot.consumeLinkCode(anyString())).thenReturn(Optional.empty());

        assertTrue(server.handleCommand(player, "link", List.of("wrong")).join());

        assertTrue(player.messages.contains("invalidCode"));
        assertNull(db.getLinkedPlayer(player.getUniqueId()));
        assertRestricted(player);
    }

    @Test
    void linkWithoutCodeAsksForOne() {
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);

        assertTrue(server.handleCommand(player, "link", List.of()).join());

        assertTrue(player.messages.contains("noCode"));
        verify(bot, never()).consumeLinkCode(any());
    }

    @Test
    void linkCannotBeUsedToBypassVerificationOfAnAlreadyLinkedAccount() {
        RecordingPlayer player = joinLinked();
        when(bot.consumeLinkCode(anyString())).thenReturn(Optional.of(mock(User.class)));

        assertTrue(server.handleCommand(player, "link", List.of("Ab12Cd34")).join());

        assertTrue(player.messages.contains("alreadyLinked"));
        verify(bot, never()).consumeLinkCode(any());
        assertEquals(DISCORD_ID, db.getLinkedPlayer(player.getUniqueId()).getDiscordId(), "link must not be replaced");
        assertRestricted(player);
    }

    // ------------------------------------------------------------------ /unlink

    @Test
    void verifiedPlayerCanUnlink() {
        RecordingPlayer player = joinLinked();
        awaitVerifyRequest(1).complete(true);

        assertTrue(server.handleCommand(player, "unlink", List.of()).join());

        assertNull(db.getLinkedPlayer(player.getUniqueId()));
        assertFalse(player.isLinked());
        assertTrue(player.messages.contains("unlinkSuccess"));
        verify(bot).revokeVerifiedRole(DISCORD_ID);
    }

    @Test
    void unlinkWhenNotLinkedSaysSo() {
        RecordingPlayer player = RecordingPlayer.at(UUID.randomUUID(), IP, VERSION);

        assertTrue(server.handleCommand(player, "unlink", List.of()).join());

        assertTrue(player.messages.contains("notLinked"));
        verify(bot, never()).revokeVerifiedRole(anyLong());
    }

    // ------------------------------------------------------------------ status

    @Test
    void statusReportsBotState() {
        assertTrue(String.join("\n", server.getStatusInfoMessage()).contains("2/2 services running"));

        when(bot.isConnected()).thenReturn(false);
        when(bot.isConfigured()).thenReturn(false);
        assertTrue(String.join("\n", server.getStatusInfoMessage()).contains("not configured"));

        when(bot.isConfigured()).thenReturn(true);
        assertTrue(String.join("\n", server.getStatusInfoMessage()).contains("failed to connect"));
    }

    @Nested
    class Shutdown {
        @Test
        void shutdownStopsTheBotAndIsRepeatable() {
            server.shutdown();
            assertDoesNotThrow(server::shutdown);
            verify(bot, atLeastOnce()).shutdown();
        }
    }
}
