package top.ilovemyhome.dagtask.server.poc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

@Disabled
public class FixedHikariTest {

    @Test
    void testFixedHikari() {
        // Fix: set autoCommit = true in HikariCP, let Jdbi handle transactions
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:postgresql://192.168.0.188:15432/postgres");
        hc.setUsername("postgres");
        hc.setPassword("");
        hc.setDriverClassName("org.postgresql.Driver");
        hc.setMaximumPoolSize(5);
        hc.setMinimumIdle(1);
        // 👇 THE FIX: set autoCommit = true at pool level
        hc.setAutoCommit(true);
        hc.setReadOnly(false);

        HikariDataSource dataSource = new HikariDataSource(hc);
        logger.info("HikariDataSource built with autoCommit=true (fix applied)");

        var jdbi = Jdbi.create(dataSource);

        int result = 0;
        try {
            result = jdbi.inTransaction(h -> {
                logger.info("In transaction: isInTransaction={}, connection autoCommit={}"
                    , h.isInTransaction(), h.getConnection().getAutoCommit());
                Update update = h.createUpdate("insert into t_test values(3, 'test-fixed');");
                int rows = update.execute();
                logger.info("Insert done, rows={}", rows);
                return rows;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.info("Transaction done, result={}", result);

        Integer count = jdbi.withHandle(h ->
            h.createQuery("select count(*) from t_test where id = 3")
                .mapTo(Integer.class)
                .one()
        );
        logger.info("After commit, count={}", count);

        dataSource.close();
        logger.info("DataSource closed. Check your database - you should see the row now!");
    }

    private static final Logger logger = LoggerFactory.getLogger(FixedHikariTest.class);

}
