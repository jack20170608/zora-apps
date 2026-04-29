package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.agent.AgentWhitelist;
import top.ilovemyhome.dagtask.si.dto.AgentWhitelistSearchCriteria;
import top.ilovemyhome.dagtask.si.persistence.AgentWhitelistDao;
import top.ilovemyhome.zora.jdbi.SqlGenerator;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AgentWhitelistDaoJdbiImpl extends BaseDaoJdbiImpl<AgentWhitelist> implements AgentWhitelistDao {

    private final Jdbi jdbi;

    public AgentWhitelistDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agent_whitelist")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(AgentWhitelist.FIELD_COLUMN_MAP)
            .build(), jdbi);
        this.jdbi = jdbi;
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentWhitelist.class, new AgentWhitelistRowMapper());
    }

    @Override
    public List<AgentWhitelist> search(AgentWhitelistSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + criteria.whereClause();
        return find(sql, criteria.normalParams(), criteria.listParam());
    }

    @Override
    public List<AgentWhitelist> findAllEnabled() {
        String sql = String.format("select * from %s where enabled = true", table.getName());
        return find(sql, Map.of(), null);
    }

    @Override
    public boolean existsByAgentId(String agentId) {
        if (agentId == null) {
            return false;
        }
        String sql = String.format(
            "select count(*) from %s where enabled = true and agent_id = :agentId",
            table.getName()
        );
        return count(sql, Map.of("agentId", agentId), null) > 0;
    }

    @Override
    public List<String> findIpSegmentsByAgentId(String agentId) {
        String sql = """
            select ip_segment from t_agent_whitelist
            where enabled = true
            and ip_segment is not null
            and agent_id = :agentId;
            """;
        return jdbi.withHandle(handle ->
            handle.createQuery(sql)
                .bind("agentId", agentId)
                .mapTo(String.class)
                .list()
        );
    }

    private static class AgentWhitelistRowMapper implements RowMapper<AgentWhitelist> {
        @Override
        public AgentWhitelist map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return AgentWhitelist.builder()
                .withId(rs.getLong(AgentWhitelist.Field.id.getDbColumn()))
                .withIpSegment(rs.getString(AgentWhitelist.Field.ipSegment.getDbColumn()))
                .withAgentId(rs.getString(AgentWhitelist.Field.agentId.getDbColumn()))
                .withDescription(rs.getString(AgentWhitelist.Field.description.getDbColumn()))
                .withEnabled(rs.getBoolean(AgentWhitelist.Field.enabled.getDbColumn()))
                .withCreatedAt(rs.getTimestamp(AgentWhitelist.Field.createdAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentWhitelist.Field.createdAt.getDbColumn()).toInstant() : null)
                .withUpdatedAt(rs.getTimestamp(AgentWhitelist.Field.updatedAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentWhitelist.Field.updatedAt.getDbColumn()).toInstant() : null)
                .build();
        }
    }
}
