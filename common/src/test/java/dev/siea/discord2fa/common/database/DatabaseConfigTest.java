package dev.siea.discord2fa.common.database;

import dev.siea.discord2fa.common.testutil.MapConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @TempDir
    Path dataFolder;

    @Test
    void defaultsToSqliteInsideTheDataFolder() {
        DatabaseConfig config = new DatabaseConfig(new MapConfig(), dataFolder);

        assertEquals(DatabaseConfig.Type.SQLITE, config.getType());
        assertEquals("jdbc:sqlite:" + dataFolder.resolve("discord2fa.db").toAbsolutePath().normalize(), config.getJdbcUrl());
    }

    @Test
    void sqliteUrlIsResolvedRelativeToTheDataFolder() {
        DatabaseConfig config = new DatabaseConfig(new MapConfig().set("database.url", "data/custom.db"), dataFolder);

        assertEquals("jdbc:sqlite:" + dataFolder.resolve("data/custom.db").toAbsolutePath().normalize(), config.getJdbcUrl());
    }

    @Test
    void withoutDataFolderSqliteUsesTheDefaultUrl() {
        assertEquals("jdbc:sqlite:discord2fa.db", new DatabaseConfig(new MapConfig()).getJdbcUrl());
    }

    @ParameterizedTest
    @CsvSource({
            "mysql, MYSQL", "MySQL, MYSQL", "mariadb, MARIADB", "postgresql, POSTGRESQL",
            "postgres, POSTGRESQL", "sqlite, SQLITE", "unknown, SQLITE", "'', SQLITE"
    })
    void typeIsParsedCaseInsensitively(String raw, DatabaseConfig.Type expected) {
        assertEquals(expected, new DatabaseConfig(new MapConfig().set("database.type", raw)).getType());
    }

    @Test
    void remoteDatabasesUseTheConfiguredUrlAndCredentials() {
        DatabaseConfig config = new DatabaseConfig(new MapConfig()
                .set("database.type", "postgresql")
                .set("database.url", "jdbc:postgresql://db:5432/d2fa")
                .set("database.username", "user")
                .set("database.password", "secret"), dataFolder);

        assertEquals("jdbc:postgresql://db:5432/d2fa", config.getJdbcUrl());
        assertEquals("user", config.getUsername());
        assertEquals("secret", config.getPassword());
    }

    @Test
    void remoteDatabaseWithoutUrlFallsBackToTypeDefault() {
        DatabaseConfig config = new DatabaseConfig(new MapConfig().set("database.type", "mysql"), dataFolder);

        assertEquals(DatabaseConfig.Type.MYSQL.getDefaultUrl(), config.getJdbcUrl());
    }
}
