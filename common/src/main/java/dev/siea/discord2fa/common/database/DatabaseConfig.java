package dev.siea.discord2fa.common.database;

import dev.siea.discord2fa.common.config.ConfigAdapter;

import java.nio.file.Path;

/**
 * Database settings loaded from the config adapter.
 * Expected config keys:
 * <ul>
 *   <li>database.type - "sqlite", "mysql", "mariadb", or "postgresql"</li>
 *   <li>database.url - JDBC URL, or for SQLite a filename (e.g. discord2fa.db) when dataFolder is set</li>
 *   <li>database.username - optional for SQLite, required for MySQL/MariaDB/PostgreSQL</li>
 *   <li>database.password - optional for SQLite, required for MySQL/MariaDB/PostgreSQL</li>
 * </ul>
 * When {@code dataFolder} is non-null and type is SQLite, {@code database.url} is resolved relative to the data folder (default filename: discord2fa.db).
 */
public final class DatabaseConfig {

    public enum Type {
        SQLITE("org.sqlite.JDBC", "jdbc:sqlite:discord2fa.db"),
        MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/discord2fa"),
        MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://localhost:3306/discord2fa"),
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/discord2fa");

        private final String driverClass;
        private final String defaultUrl;

        Type(String driverClass, String defaultUrl) {
            this.driverClass = driverClass;
            this.defaultUrl = defaultUrl;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public String getDefaultUrl() {
            return defaultUrl;
        }
    }

    private final Type type;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConfig(ConfigAdapter config) {
        this(config, null);
    }

    /**
     * @param dataFolder plugin data folder (where config.yml lives). When non-null and type is SQLite, database.url is resolved relative to this folder; empty url uses "discord2fa.db".
     */
    public DatabaseConfig(ConfigAdapter config, Path dataFolder) {
        String typeStr = config.getString("database.type");
        if (typeStr == null || typeStr.isBlank()) {
            typeStr = "sqlite";
        }
        switch (typeStr.toLowerCase()) {
            case "mysql":
                this.type = Type.MYSQL;
                break;
            case "mariadb":
                this.type = Type.MARIADB;
                break;
            case "postgresql":
            case "postgres":
                this.type = Type.POSTGRESQL;
                break;
            case "sqlite":
            default:
                this.type = Type.SQLITE;
                break;
        }
        String url = config.getString("database.url");
        if (type == Type.SQLITE && dataFolder != null) {
            String fileName = (url != null && !url.isBlank()) ? url : "discord2fa.db";
            Path resolved = dataFolder.resolve(fileName).toAbsolutePath().normalize();
            this.jdbcUrl = "jdbc:sqlite:" + resolved.toString();
        } else {
            this.jdbcUrl = (url != null && !url.isBlank()) ? url : type.getDefaultUrl();
        }
        this.username = config.getString("database.username");
        this.password = config.getString("database.password");
    }

    public Type getType() {
        return type;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
