package dev.siea.discord2fa.common.testutil;

import dev.siea.discord2fa.common.database.models.SignInLocation;
import dev.siea.discord2fa.common.player.CommonPlayer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** {@link CommonPlayer} that records everything the server does to it. */
public final class RecordingPlayer extends CommonPlayer {

    public final List<String> messages = new CopyOnWriteArrayList<>();
    public final List<String> titles = new CopyOnWriteArrayList<>();
    public final List<String> kicks = new CopyOnWriteArrayList<>();
    public volatile int verifiedCount;

    public RecordingPlayer(UUID uuid, SignInLocation location) {
        super(uuid, location);
    }

    public static RecordingPlayer at(UUID uuid, String ip, String version) {
        return new RecordingPlayer(uuid, new SignInLocation(0, ip, version, uuid, Instant.now()));
    }

    @Override
    protected void onVerifiedImpl() {
        verifiedCount++;
    }

    @Override
    public String getName() {
        return "Player-" + getUniqueId().toString().substring(0, 8);
    }

    @Override
    public void sendMessage(String message) {
        messages.add(message);
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int duration, int fadeOut) {
        titles.add(title);
    }

    @Override
    public void kick(String reason) {
        kicks.add(reason);
    }
}
