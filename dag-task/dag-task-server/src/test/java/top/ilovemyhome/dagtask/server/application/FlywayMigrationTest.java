package top.ilovemyhome.dagtask.server.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify Flyway database migration works correctly with embedded PostgreSQL.
 */
class FlywayMigrationTest {

    private static EmbeddedPostgres embeddedPostgres;

    @BeforeAll
    static void startEmbeddedPostgres() throws IOException {
        embeddedPostgres = EmbeddedPostgres.builder()
                .start();
    }

    @AfterAll
    static void stopEmbeddedPostgres() throws IOException {
        if (embeddedPostgres != null) {
            embeddedPostgres.close();
        }
    }

    @Test
    void testFlywayMigrationSuccess() throws SQLException {
        // Load test configuration and override with actual embedded postgres URL
        Config config = ConfigFactory.load("config/application-test.conf")
                .withValue("database.jdbcUrl", ConfigValueFactory.fromAnyRef(embeddedPostgres.getJdbcUrl("postgres", "postgres")))
                .withValue("database.username", ConfigValueFactory.fromAnyRef("postgres"))
                .withValue("database.password", ConfigValueFactory.fromAnyRef("postgres"));

        // Create AppContext which will initialize DataSource and run Flyway
        AppContext appContext = new AppContext("test", config);
        DataSource dataSource = appContext.getDataSource();

        // Verify DataSource is not null and connection can be obtained
        assertThat(dataSource).isNotNull();

        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();

            // Check that tables were created by Flyway
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"});

            boolean foundTaskOrder = false;
            boolean foundTask = false;
            boolean foundFlywayHistory = false;

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if ("t_task_order".equalsIgnoreCase(tableName)) {
                    foundTaskOrder = true;
                }
                if ("t_task".equalsIgnoreCase(tableName)) {
                    foundTask = true;
                }
                if ("flyway_schema_history".equalsIgnoreCase(tableName)) {
                    foundFlywayHistory = true;
                }
            }

            assertThat(foundTaskOrder)
                .as("Table 't_task_order' should be created by Flyway migration")
                .isTrue();
            assertThat(foundTask)
                .as("Table 't_task' should be created by Flyway migration")
                .isTrue();
            assertThat(foundFlywayHistory)
                .as("Table 'flyway_schema_history' should be created by Flyway")
                .isTrue();
        }
    }
}
