package top.ilovemyhome.dagtask.si;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import top.ilovemyhome.dagtask.si.enums.OrderType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//should none related to business
@JsonDeserialize(builder = TaskOrder.Builder.class)
public class TaskOrder {

    public String getKey() {
        return key;
    }

    private Long id;
    private final String name;
    private final String key;
    private final OrderType orderType;
    private Map<String, String> attributes;
    private LocalDateTime createDt;
    private LocalDateTime lastUpdateDt;

    public enum Field {
        id("ID", true),
        name("NAME"),
        key("KEY"),
        orderType("ORDER_TYPE"),
        attributes("ATTRIBUTES"),
        createDt("CREATE_DT"),
        lastUpdateDt("LAST_UPDATE_DT")
        ;

        private final String dbColumn;
        private final boolean isId;

        Field(String dbColumn) {
            this.dbColumn = dbColumn;
            this.isId = false;
        }

        Field(String dbColumn, boolean isId) {
            this.dbColumn = dbColumn;
            this.isId = isId;
        }

        public String getDbColumn() {
            return dbColumn;
        }

        public boolean isId() {
            return isId;
        }
    }

    public static final Map<String, String> FIELD_COLUMN_MAP
        = Collections.unmodifiableMap(Stream.of(Field.values())
        .collect(Collectors.toMap(Field::name, Field::getDbColumn)));

    public static final String ID_FIELD = Field.id.name();

    private TaskOrder(Long id, String name, OrderType orderType
        , String key, Map<String, String> attributes
        , LocalDateTime createDt, LocalDateTime lastUpdateDt) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(orderType);
        Objects.requireNonNull(key);

        this.id = id;
        this.name = name;
        this.key = key;
        this.orderType = orderType;
        if (Objects.nonNull(attributes) && !attributes.isEmpty()) {
            this.attributes = new TreeMap<>(String::compareToIgnoreCase);
            this.attributes.putAll(attributes);
        }
        LocalDateTime now = LocalDateTime.now();
        this.createDt = Objects.isNull(createDt) ? now : createDt;
        this.lastUpdateDt = Objects.isNull(lastUpdateDt) ? now : lastUpdateDt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public Map<String, String> getAttributes() {
        if (Objects.nonNull(attributes) && !attributes.isEmpty()) {
            return Collections.unmodifiableMap(attributes);
        }
        return null;
    }

    public LocalDateTime getCreateDt() {
        return createDt;
    }

    public LocalDateTime getLastUpdateDt() {
        return lastUpdateDt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskOrder that = (TaskOrder) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "TaskOrder{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", key='" + key + '\'' +
            ", orderType=" + orderType +
            ", attributes=" + attributes +
            ", createDt=" + createDt +
            ", lastUpdateDt=" + lastUpdateDt +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String name;
        private String key;
        private OrderType orderType;
        private Map<String, String> attributes;
        private LocalDateTime createDt;
        private LocalDateTime lastUpdateDt;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withOrderType(OrderType orderType) {
            this.orderType = orderType;
            return this;
        }

        public Builder withAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder withCreateDt(LocalDateTime createDt) {
            this.createDt = createDt;
            return this;
        }

        public Builder withLastUpdateDt(LocalDateTime lastUpdateDt) {
            this.lastUpdateDt = lastUpdateDt;
            return this;
        }

        public TaskOrder build() {
            return new TaskOrder(id, name, orderType, key, attributes, createDt, lastUpdateDt);
        }
    }
}
