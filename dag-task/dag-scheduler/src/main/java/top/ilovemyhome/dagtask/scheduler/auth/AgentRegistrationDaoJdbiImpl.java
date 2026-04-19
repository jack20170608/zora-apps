package top.ilovemyhome.dagtask.scheduler.auth;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgentRegistrationDaoJdbiImpl extends BaseDaoJdbiImpl<AgentRegistration> implements AgentRegistrationDao {

    public static final String TABLE_NAME = "agent_registrations";

    public AgentRegistrationDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName(TABLE_NAME)
            .withIdField("id")
            .withIdAutoGenerate(true)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentRegistration.class, new AgentRegistrationRowMapper());
    }

    @Override
    public AgentRegistration insert(AgentRegistration registration) {
        String sql = """
            INSERT INTO agent_registrations (
                registration_id, agent_name, description, labels, callback_url,
                nonce, client_address, status, notes, processed_by, processed_at,
                created_at, expires_at
            ) VALUES (:registrationId, :agentName, :description, :labels, :callbackUrl,
                :nonce, :clientAddress, :status, :notes, :processedBy, :processedAt,
                :createdAt, :expiresAt)
            """;
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("registrationId", registration.registrationId());
        params.put("agentName", registration.agentName());
        params.put("description", registration.description());
        params.put("labels", registration.labelsJson());
        params.put("callbackUrl", registration.callbackUrl());
        params.put("nonce", registration.nonce());
        params.put("clientAddress", registration.clientAddress());
        params.put("status", registration.status().name());
        params.put("notes", registration.notes());
        params.put("processedBy", registration.processedBy());
        params.put("processedAt", registration.processedAt() != null ? Timestamp.from(registration.processedAt()) : null);
        params.put("createdAt", Timestamp.from(registration.createdAt()));
        params.put("expiresAt", Timestamp.from(registration.expiresAt()));
        update(sql, params, null);
        return registration;
    }

    @Override
    public Optional<AgentRegistration> findByRegistrationId(String registrationId) {
        String sql = "SELECT * FROM " + table.getName() + " WHERE registration_id = :registrationId";
        List<AgentRegistration> results = find(sql, Map.of("registrationId", registrationId), null);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<AgentRegistration> findByStatus(AgentRegistration.Status status, int limit) {
        String sql = String.format(
            "SELECT * FROM %s WHERE status = :status ORDER BY created_at DESC LIMIT %d",
            table.getName(), limit
        );
        return find(sql, Map.of("status", status.name()), null);
    }

    @Override
    public List<AgentRegistration> findExpiredPending(Instant now, int limit) {
        String sql = String.format(
            "SELECT * FROM %s WHERE status = 'PENDING' AND expires_at < :now ORDER BY created_at LIMIT %d",
            table.getName(), limit
        );
        return find(sql, Map.of("now", Timestamp.from(now)), null);
    }

    @Override
    public int deleteExpiredPending(Instant now) {
        String sql = String.format(
            "DELETE FROM %s WHERE status = 'PENDING' AND expires_at < :now",
            table.getName()
        );
        return update(sql, Map.of("now", Timestamp.from(now)), null);
    }

    @Override
    public void updateStatus(String registrationId, AgentRegistration.Status status,
                            String processedBy, String notes) {
        String sql = String.format(
            "UPDATE %s SET status = :status, processed_by = :processedBy, " +
            "processed_at = NOW(), notes = :notes WHERE registration_id = :registrationId",
            table.getName()
        );
        update(sql, Map.of(
            "status", status.name(),
            "processedBy", processedBy,
            "notes", notes,
            "registrationId", registrationId
        ), null);
    }

    private static class AgentRegistrationRowMapper implements RowMapper<AgentRegistration> {
        @Override
        public AgentRegistration map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return new AgentRegistration(
                rs.getLong("id"),
                rs.getString("registration_id"),
                rs.getString("agent_name"),
                rs.getString("description"),
                rs.getString("labels"),
                rs.getString("callback_url"),
                rs.getString("nonce"),
                rs.getString("client_address"),
                AgentRegistration.Status.valueOf(rs.getString("status")),
                rs.getString("notes"),
                rs.getString("processed_by"),
                rs.getTimestamp("processed_at") != null
                    ? rs.getTimestamp("processed_at").toInstant()
                    : null,
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant()
            );
        }
    }
}
