package dev.siea.discord2fa.common.database;

import dev.siea.discord2fa.common.config.ConfigAdapter;

/**
 * Database settings loaded from the config adapter.
 * Expected config keys:
 * <ul>
 *   <li>database.type - "sqlite", "mysql", or "postgresql"</li>
 *   <li>database.url - JDBC URL (e.g. jdbc:sqlite:./data/discord2fa.db, jdbc:mysql://localhost:3306/discord2fa)</li>
 *   <li>database.username - optional for SQLite, required for MySQL/PostgreSQL</li>
 *   <li>database.password - optional for SQLite, required for MySQL/PostgreSQL</li>
 * </ul>
 */
public final class DatabaseConfig {

    public enum Type {
        SQLITE("org.sqlite.JDBC", "jdbc:sqlite:./data/discord2fa.db"),
        MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/discord2fa"),
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
        String typeStr = config.getString("database.type");
        if (typeStr == null || typeStr.isBlank()) {
            typeStr = "sqlite";
        }
        switch (typeStr.toLowerCase()) {
            case "mysql":
                this.type = Type.MYSQL;
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
        this.jdbcUrl = (url != null && !url.isBlank()) ? url : type.getDefaultUrl();
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
