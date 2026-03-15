package dev.siea.discord2fa.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.siea.discord2fa.common.config.ConfigAdapter;
import dev.siea.discord2fa.common.database.models.LinkedPlayer;
import dev.siea.discord2fa.common.database.models.SignInLocation;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Database adapter backed by HikariCP. Supports SQLite, MySQL, and PostgreSQL.
 * Uses {@link ConfigAdapter} for database settings (database.type, database.url, database.username, database.password).
 */
public final class DatabaseAdapter {

    private final HikariDataSource dataSource;
    private final DatabaseConfig.Type dbType;

    public DatabaseAdapter(ConfigAdapter config) {
        DatabaseConfig dbConfig = new DatabaseConfig(config);
        this.dbType = dbConfig.getType();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(dbConfig.getType().getDriverClass());
        hikariConfig.setJdbcUrl(dbConfig.getJdbcUrl());
        if (dbConfig.getUsername() != null && !dbConfig.getUsername().isBlank()) {
            hikariConfig.setUsername(dbConfig.getUsername());
        }
        if (dbConfig.getPassword() != null) {
            hikariConfig.setPassword(dbConfig.getPassword());
        }
        hikariConfig.setPoolName("Discord2FA-Pool");

        this.dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    private void createTables() {
        String linkedPlayersSql = """
            CREATE TABLE IF NOT EXISTS linked_players (
                minecraft_uuid VARCHAR(36) PRIMARY KEY,
                discord_id BIGINT NOT NULL,
                time_linked TIMESTAMP NOT NULL
            )
            """;

        String botStateSql = """
            CREATE TABLE IF NOT EXISTS bot_state (
                key VARCHAR(64) PRIMARY KEY,
                value VARCHAR(512) NOT NULL
            )
            """;

        String loginLocationsSql = switch (dbType) {
            case SQLITE -> """
                CREATE TABLE IF NOT EXISTS login_locations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ip_address VARCHAR(45) NOT NULL,
                    version VARCHAR(64) NOT NULL,
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    time_of_login TIMESTAMP NOT NULL
                )
                """;
            case MYSQL -> """
                CREATE TABLE IF NOT EXISTS login_locations (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    ip_address VARCHAR(45) NOT NULL,
                    version VARCHAR(64) NOT NULL,
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    time_of_login TIMESTAMP(6) NOT NULL
                )
                """;
            case POSTGRESQL -> """
                CREATE TABLE IF NOT EXISTS login_locations (
                    id BIGSERIAL PRIMARY KEY,
                    ip_address VARCHAR(45) NOT NULL,
                    version VARCHAR(64) NOT NULL,
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    time_of_login TIMESTAMP(6) NOT NULL
                )
                """;
        };

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(linkedPlayersSql);
            st.execute(botStateSql);
            st.execute(loginLocationsSql);
            try {
                st.execute("CREATE INDEX IF NOT EXISTS idx_login_locations_minecraft_uuid ON login_locations(minecraft_uuid)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_login_locations_time ON login_locations(time_of_login)");
            } catch (SQLException ignored) {
                // Index may already exist
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create database tables", e);
        }
    }

    public LinkedPlayer getLinkedPlayer(UUID minecraftUuid) {
        String sql = "SELECT minecraft_uuid, discord_id, time_linked FROM linked_players WHERE minecraft_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, minecraftUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long discordId = rs.getLong("discord_id");
                    Instant timeLinked = rs.getTimestamp("time_linked").toInstant();
                    return new LinkedPlayer(minecraftUuid, discordId, timeLinked);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get linked player for " + minecraftUuid, e);
        }
        return null;
    }

    /**
     * Returns the linked player for the given Discord user id, if any.
     */
    public Optional<LinkedPlayer> getLinkedByDiscord(long id) {
        String sql = "SELECT minecraft_uuid, discord_id, time_linked FROM linked_players WHERE discord_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID minecraftUuid = UUID.fromString(rs.getString("minecraft_uuid"));
                    long discordId = rs.getLong("discord_id");
                    Instant timeLinked = rs.getTimestamp("time_linked").toInstant();
                    return Optional.of(new LinkedPlayer(minecraftUuid, discordId, timeLinked));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get linked player for Discord id " + id, e);
        }
        return Optional.empty();
    }

    public void saveLinkedPlayer(LinkedPlayer player) {
        String sql = switch (dbType) {
            case SQLITE, POSTGRESQL -> "INSERT INTO linked_players (minecraft_uuid, discord_id, time_linked) VALUES (?, ?, ?) ON CONFLICT (minecraft_uuid) DO UPDATE SET discord_id = excluded.discord_id, time_linked = excluded.time_linked";
            case MYSQL -> "INSERT INTO linked_players (minecraft_uuid, discord_id, time_linked) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id), time_linked = VALUES(time_linked)";
        };
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getMinecraftUuid().toString());
            ps.setLong(2, player.getDiscordId());
            ps.setTimestamp(3, Timestamp.from(player.getTimeLinked()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save linked player " + player.getMinecraftUuid(), e);
        }
    }

    /**
     * Removes the Discord link for the given Minecraft player. No-op if not linked.
     */
    public void removeLinkedPlayer(UUID minecraftUuid) {
        String sql = "DELETE FROM linked_players WHERE minecraft_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, minecraftUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove linked player " + minecraftUuid, e);
        }
    }

    public List<SignInLocation> getSignInLocations(UUID minecraftUuid) {
        String sql = "SELECT id, ip_address, version, minecraft_uuid, time_of_login FROM login_locations WHERE minecraft_uuid = ? ORDER BY time_of_login DESC";
        List<SignInLocation> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, minecraftUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLoginLocation(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get sign-in locations for " + minecraftUuid, e);
        }
        return list;
    }

    public void addLoginLocation(String ipAddress, String version, UUID minecraftUuid, Instant timeOfLogin) {
        String sql = "INSERT INTO login_locations (ip_address, version, minecraft_uuid, time_of_login) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ipAddress);
            ps.setString(2, version);
            ps.setString(3, minecraftUuid.toString());
            ps.setTimestamp(4, Timestamp.from(timeOfLogin));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add login location for " + minecraftUuid, e);
        }
    }

    public boolean hasRecentSignInLocation(UUID minecraftUuid, String ipAddress, String version) {
        String sql = "SELECT 1 FROM login_locations WHERE minecraft_uuid = ? AND ip_address = ? AND version = ? AND time_of_login >= ? LIMIT 1";
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, minecraftUuid.toString());
            ps.setString(2, ipAddress);
            ps.setString(3, version);
            ps.setTimestamp(4, Timestamp.from(thirtyDaysAgo));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check recent sign-in for " + minecraftUuid, e);
        }
    }

    /**
     * Deletes all sign-in locations older than the given number of days. Use to keep the table small;
     * locations older than 30 days are not used by {@link #hasRecentSignInLocation} anyway.
     *
     * @param days age in days; records with time_of_login before (now - days) are deleted
     * @return number of rows deleted
     */
    public int purgeSignInLocationsOlderThan(int days) {
        Instant cutoff = Instant.now().minusSeconds(days * 24L * 60 * 60);
        String sql = "DELETE FROM login_locations WHERE time_of_login < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to purge old sign-in locations", e);
        }
    }

    private static SignInLocation mapLoginLocation(ResultSet rs) throws SQLException {
        return new SignInLocation(
            rs.getLong("id"),
            rs.getString("ip_address"),
            rs.getString("version"),
            UUID.fromString(rs.getString("minecraft_uuid")),
            rs.getTimestamp("time_of_login").toInstant()
        );
    }

    /** Key for the stored link message id in the configured Discord channel. */
    public static final String STATE_LINK_MESSAGE_ID = "link_message_id";

    /**
     * Returns persisted bot state value for the given key, or null if missing.
     */
    public String getState(String key) {
        String sql = "SELECT value FROM bot_state WHERE key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("value") : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get state for key " + key, e);
        }
    }

    /**
     * Persists a bot state value. Runs on the calling thread; use from async tasks if needed.
     */
    public void setState(String key, String value) {
        String sql = switch (dbType) {
            case SQLITE, POSTGRESQL -> "INSERT INTO bot_state (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = excluded.value";
            case MYSQL -> "INSERT INTO bot_state (key, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)";
        };
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to set state for key " + key, e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
