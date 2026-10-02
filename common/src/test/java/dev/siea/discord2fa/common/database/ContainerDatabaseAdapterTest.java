package dev.siea.discord2fa.common.database;

import dev.siea.discord2fa.common.testutil.MapConfig;
import org.junit.jupiter.api.Nested;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Runs the {@link DatabaseAdapterContract} against real PostgreSQL, MySQL and MariaDB servers.
 * Skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
class ContainerDatabaseAdapterTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Container
    static final MariaDBContainer MARIADB = new MariaDBContainer("mariadb:11.4");

    private static DatabaseAdapter connect(String type, JdbcDatabaseContainer<?> c) {
        return new DatabaseAdapter(new MapConfig()
                .set("database.type", type)
                .set("database.url", c.getJdbcUrl())
                .set("database.username", c.getUsername())
                .set("database.password", c.getPassword()));
    }

    private static void dropTables(JdbcDatabaseContainer<?> c) throws Exception {
        try (Connection conn = DriverManager.getConnection(c.getJdbcUrl(), c.getUsername(), c.getPassword());
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS linked_players");
            st.execute("DROP TABLE IF EXISTS bot_state");
            st.execute("DROP TABLE IF EXISTS login_locations");
        }
    }

    @Nested
    class Postgres extends DatabaseAdapterContract {
        @Override
        protected DatabaseAdapter createAdapter() throws Exception {
            dropTables(POSTGRES);
            return connect("postgresql", POSTGRES);
        }
    }

    @Nested
    class MySql extends DatabaseAdapterContract {
        @Override
        protected DatabaseAdapter createAdapter() throws Exception {
            dropTables(MYSQL);
            return connect("mysql", MYSQL);
        }
    }

    @Nested
    class MariaDb extends DatabaseAdapterContract {
        @Override
        protected DatabaseAdapter createAdapter() throws Exception {
            dropTables(MARIADB);
            return connect("mariadb", MARIADB);
        }
    }
}
