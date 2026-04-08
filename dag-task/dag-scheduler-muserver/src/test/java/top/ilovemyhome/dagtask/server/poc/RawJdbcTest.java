package top.ilovemyhome.dagtask.server.poc;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Disabled
public class RawJdbcTest {

    @Test
    void testRawConn() throws SQLException {
        // Bypass connection pool entirely - use raw DriverManager
        String url = "jdbc:postgresql://192.168.0.188:15432/postgres";
        String user = "postgres";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            logger.info("Raw connection: autoCommit={}", conn.getAutoCommit());

            // Insert
            try (Statement stmt = conn.createStatement()) {
                int rows = stmt.executeUpdate("insert into t_test values(1, 'test1')");
                logger.info("Raw insert done, rows affected: {}", rows);
            }

            // Query before commit - should see it
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("select count(*) from t_test where id = 1")) {
                rs.next();
                int count = rs.getInt(1);
                logger.info("Before commit, in same connection: count={}", count);
            }

            // Manual commit
            conn.commit();
            logger.info("Manual commit done");

            // Query after commit in same connection
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("select count(*) from t_test where id = 1")) {
                rs.next();
                int count = rs.getInt(1);
                logger.info("After commit, in same connection: count={}", count);
            }
        }

        // Open new connection after close - check if it's still there
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("select count(*) from t_test where id = 1")) {
                rs.next();
                int count = rs.getInt(1);
                logger.info("After close, new connection: count={}", count);
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RawJdbcTest.class);

}
