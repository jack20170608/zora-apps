package top.ilovemyhome.dagtask.allinone.muserver;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.allinone.muserver.application.AllInOneAppContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration test that verifies AllInOneAppContext can initialize, start, and stop
 * against an embedded PostgreSQL database.
 */
class AllInOneStartupTest {

    private EmbeddedPostgres embeddedPostgres;
    private AllInOneAppContext appContext;

    @BeforeEach
    void setUp() throws IOException {
        embeddedPostgres = EmbeddedPostgres.start();
    }

    @AfterEach
    void tearDown() {
        if (appContext != null) {
            assertThatNoException().isThrownBy(() -> appContext.stop());
        }
        if (embeddedPostgres != null) {
            assertThatNoException().isThrownBy(() -> embeddedPostgres.close());
        }
    }

    @Test
    void contextShouldStartAndStopWithEmbeddedDatabase() {
        String jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres");

        Config config = ConfigFactory.parseString(String.format("""
            database {
                url = "%s"
                jdbcUrl = ${database.url}
                username = "postgres"
                password = "postgres"
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                minimumIdle = 1
                autoCommit = true
                readOnly = false
                pool {
                    maxSize = ${database.maximumPoolSize}
                }
            }
            agent {
                maxConcurrentTasks = 2
                maxPendingTasks = 10
                taskLogDir = "target/test-logs"
            }
            security {
                cookie {
                    name = "dag_token"
                    valueType = "JWT"
                }
                whiteList = [
                    "/**/openapi.json",
                    "/**/api.html",
                    "/dag-task/api/v1/agent/health",
                    "/dag-task/api/v1/agent/ping",
                    "/dag-task/login"
                ]
                users = [
                    {
                        id = "1"
                        name = "admin"
                        displayName = "Test Admin"
                        roles = ["admin", "read", "write"]
                        passwordHashVal = "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b"
                        attributes = {
                            email = "admin@test.local"
                        }
                    }
                ]
                jwt {
                    issuer = "dag-task-allinone-test"
                    audience = "dag-task-client"
                    publicKeyLocation = "classpath:key/public.key"
                    privateKeyLocation = "classpath:key/private.key"
                    ttl = "1h"
                }
            }
            flyway {
                location = "db/migration/postgresql"
                locations = ["db/migration/postgresql"]
                baselineOnMigrate = true
                baselineVersion = "0"
                baselineDescription = "Baseline"
                table = "flyway_schema_history"
                defaultSchema = "public"
            }
            scheduler {
                scanInterval = 30
            }
            """, jdbcUrl)).resolve();

        appContext = new AllInOneAppContext("test", config);

        assertThat(appContext.getDatabaseBootstrap()).isNotNull();
        assertThat(appContext.getDatabaseBootstrap().getJdbi()).isNotNull();
        assertThat(appContext.getAdminAppContext()).isNotNull();
        assertThat(appContext.getEmbeddedAgentBootstrap()).isNotNull();
        assertThat(appContext.getInProcessTaskDispatcher()).isNotNull();
        assertThat(appContext.getInProcessSchedulerClient()).isNotNull();

        assertThatNoException().isThrownBy(() -> appContext.start());

        assertThat(appContext.getDatabaseBootstrap().getJdbi()).isNotNull();
    }
}
