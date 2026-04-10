package top.ilovemyhome.dagtask.server.poc;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.rdb.config.RdbConfig;
import top.ilovemyhome.zora.rdb.pool.DataSourcePoolBuilder;

import java.sql.Connection;
import java.sql.SQLException;

@Disabled
public class DbTest {

    @Test
    void testConn() throws SQLException {
        RdbConfig rdbConfig = RdbConfig.builder()
            .jdbcUrl("jdbc:postgresql://192.168.0.188:15432/postgres")
            .username("postgres")
            .password("")
            .driverClassName(org.postgresql.Driver.class.getName())
            .maximumPoolSize(5)
            .minimumIdle(1)
            .autoCommit(true)
            .readOnly(false)
            .build();
        var dataSource = DataSourcePoolBuilder.create(rdbConfig).build();
        logger.info("DataSource initialized successfully, class={}", dataSource.getClass().getName());

        // Print debug info about the connection
        try (Connection conn = dataSource.getConnection()) {
            logger.info("Got connection: autoCommit={}, readOnly={}, catalog={}"
                , conn.getAutoCommit(), conn.isReadOnly(), conn.getCatalog());
        }

        var jdbi = Jdbi.create(dataSource);

        int result = jdbi.inTransaction(h -> {
            logger.info("In transaction handle: isInTransaction={}, autoCommit={}"
                , h.isInTransaction(), h.getConnection().getAutoCommit());
            Update update  = h.createUpdate("""
                insert into t_test
                values(111, 'test1');
                """);
            update.execute();
            update  = h.createUpdate("""
                insert into t_test
                values('2a2', 'test2');
                """);
            int rows = update.execute();
            logger.info("Insert executed, affected rows={}", rows);
            return rows;
        });
        logger.info("Transaction completed, result is {}", result);

        // Check immediately within the same JVM - should see it
        Integer count = jdbi.withHandle(h ->
            h.createQuery("select count(*) from t_test where id = 1")
                .mapTo(Integer.class)
                .one()
        );
        logger.info("After transaction commit, query in new handle: count={}", count);

        // Close the data source to properly release connections
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
                logger.info("DataSource closed");
            } catch (Exception e) {
                logger.warn("Failed to close DataSource", e);
            }
        }
    }


    private static final Logger logger = LoggerFactory.getLogger(DbTest.class);

}
