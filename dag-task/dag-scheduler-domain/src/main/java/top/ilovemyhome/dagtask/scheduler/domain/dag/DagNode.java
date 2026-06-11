package top.ilovemyhome.dagtask.scheduler.domain.dag;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable node in a DAG used for cycle detection and path enumeration.
 * Identity is based solely on {@code id} (equals/hashCode ignore name and successors).
 */
public final class DagNode {

    private final Long id;
    private final Set<Long> successors;
    private final String name;

    public DagNode(Long id, Set<Long> successors) {
        this.id = id;
        this.successors = successors;
        this.name = "n" + id;
    }

    public DagNode(Long id, String name, Set<Long> successors) {
        this.id = id;
        this.successors = successors;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Set<Long> getSuccessors() {
        return successors;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DagNode dagNode = (DagNode) o;
        return Objects.equals(getId(), dagNode.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
