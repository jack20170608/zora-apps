package top.ilovemyhome.dagtask.agent.client;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal JAX-RS Response implementation for agent-side client results.
 * <p>
 * The agent core only needs status and optional entity values. Using this class
 * avoids requiring a server-side RuntimeDelegate implementation in dag-agent.
 */
public final class SimpleAgentResponse extends Response {

    private final int status;
    private final Object entity;
    private final StatusType statusInfo;
    private final MultivaluedMap<String, Object> metadata = new MultivaluedHashMap<>();

    private SimpleAgentResponse(int status, Object entity) {
        this.status = status;
        this.entity = entity;
        this.statusInfo = resolveStatus(status);
    }

    public static SimpleAgentResponse okResponse() {
        return of(Status.OK.getStatusCode(), null);
    }

    public static SimpleAgentResponse serverErrorResponse(Object entity) {
        return of(Status.INTERNAL_SERVER_ERROR.getStatusCode(), entity);
    }

    public static SimpleAgentResponse of(int status, Object entity) {
        return new SimpleAgentResponse(status, entity);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public StatusType getStatusInfo() {
        return statusInfo;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return entityType.cast(entity);
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        @SuppressWarnings("unchecked")
        T value = (T) entity;
        return value;
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return readEntity(entityType);
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return readEntity(entityType);
    }

    @Override
    public boolean hasEntity() {
        return entity != null;
    }

    @Override
    public boolean bufferEntity() {
        return false;
    }

    @Override
    public void close() {
        // Nothing to close.
    }

    @Override
    public MediaType getMediaType() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public int getLength() {
        return -1;
    }

    @Override
    public Set<String> getAllowedMethods() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return Collections.emptyMap();
    }

    @Override
    public EntityTag getEntityTag() {
        return null;
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Date getLastModified() {
        return null;
    }

    @Override
    public URI getLocation() {
        return null;
    }

    @Override
    public Set<Link> getLinks() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        return null;
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return new MultivaluedHashMap<>();
    }

    @Override
    public String getHeaderString(String name) {
        return null;
    }

    private static StatusType resolveStatus(int status) {
        Status known = Status.fromStatusCode(status);
        if (known != null) {
            return known;
        }
        return new SimpleStatusType(status);
    }

    private record SimpleStatusType(int statusCode) implements StatusType {
        private SimpleStatusType {
            if (statusCode < 100 || statusCode > 599) {
                throw new IllegalArgumentException("HTTP status must be between 100 and 599: " + statusCode);
            }
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public Status.Family getFamily() {
            return Status.Family.familyOf(statusCode);
        }

        @Override
        public String getReasonPhrase() {
            return Objects.toString(Status.fromStatusCode(statusCode), "");
        }
    }
}
