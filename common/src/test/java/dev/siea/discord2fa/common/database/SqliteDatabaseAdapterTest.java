package dev.siea.discord2fa.common.database;

import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.testutil.MapConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDatabaseAdapterTest extends DatabaseAdapterContract {

    @TempDir
    Path dataFolder;

    @Override
    protected DatabaseAdapter createAdapter() {
        return new DatabaseAdapter(new MapConfig(), dataFolder);
    }

    @Test
    void createsTheDatabaseFileInTheDataFolder() {
        assertTrue(Files.exists(dataFolder.resolve("discord2fa.db")));
    }

    @Test
    void createsMissingParentDirectories() {
        Path nested = dataFolder.resolve("a/b");
        DatabaseAdapter adapter = new DatabaseAdapter(new MapConfig().set("database.url", "a/b/db.sqlite"), dataFolder);
        try {
            assertTrue(Files.exists(nested.resolve("db.sqlite")));
        } finally {
            adapter.close();
        }
    }

    @Test
    void dataSurvivesReopening() {
        UUID uuid = UUID.randomUUID();
        db.saveLinkedPlayer(new LinkedPlayer(uuid, 5L, Instant.now()));
        db.close();

        db = createAdapter();
        assertEquals(5L, db.getLinkedPlayer(uuid).getDiscordId());
    }

    @Test
    void closeIsIdempotent() {
        db.close();
        assertDoesNotThrow(db::close);
    }

    @Test
    void unreachableDatabaseFailsWithAClearError() {
        MapConfig config = new MapConfig()
                .set("database.type", "postgresql")
                .set("database.url", "jdbc:postgresql://127.0.0.1:1/none?connectTimeout=1")
                .set("database.username", "x")
                .set("database.password", "x");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new DatabaseAdapter(config, dataFolder));
        assertTrue(e.getMessage().startsWith("Could not connect to the database"), e.getMessage());
    }
}
