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
public class ManualHikariTest {

    @Test
    void testManualHikari() throws SQLException {
        // Manually build HikariDataSource with standard config
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:postgresql://192.168.0.188:15432/postgres");
        hc.setUsername("postgres");
        hc.setPassword("");
        hc.setDriverClassName("org.postgresql.Driver");
        hc.setMaximumPoolSize(5);
        hc.setMinimumIdle(1);
        hc.setAutoCommit(false);
        hc.setReadOnly(false);

        HikariDataSource dataSource = new HikariDataSource(hc);
        logger.info("Manual HikariDataSource built, autoCommit=false");

        var jdbi = Jdbi.create(dataSource);

        int result = jdbi.inTransaction(h -> {
            logger.info("In transaction: isInTransaction={}, connection autoCommit={}"
                , h.isInTransaction(), h.getConnection().getAutoCommit());
            Update update = h.createUpdate("insert into t_test values(2, 'test-hikari');");
            int rows = update.execute();
            logger.info("Insert done, rows={}", rows);
            return rows;
        });
        logger.info("Transaction done, result={}", result);

        Integer count = jdbi.withHandle(h ->
            h.createQuery("select count(*) from t_test where id = 2")
                .mapTo(Integer.class)
                .one()
        );
        logger.info("After commit, count={}", count);

        dataSource.close();
        logger.info("DataSource closed");
    }

    private static final Logger logger = LoggerFactory.getLogger(ManualHikariTest.class);

}
