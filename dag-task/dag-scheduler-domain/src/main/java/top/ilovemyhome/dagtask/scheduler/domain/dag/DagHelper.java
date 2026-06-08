package top.ilovemyhome.dagtask.scheduler.domain.dag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure-domain helper for traversing a DAG represented as a list of {@link DagNode}s.
 * Detects cycles and collects all task paths from start nodes to leaf nodes.
 */
public final class DagHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DagHelper.class);

    public static void visitDAG(List<DagNode> nodes, List<String> taskPath){
        if (isEmpty(nodes)){
            return;
        }
        Map<Long, DagNode> maps = nodes.stream().collect(Collectors.toMap(DagNode::getId, Function.identity()));
        Stack<DagNode> stack = new Stack<>();
        getAllStartNode(nodes, maps).forEach(id -> {
            visit(stack, maps.get(id), maps, taskPath);
        });
    }

    private static Set<Long> getAllStartNode(List<DagNode> allNodes, Map<Long, DagNode> nodeMap){
        // JDK replacement for Guava Sets.newHashSet(Iterable): copy keys into a new HashSet.
        Set<Long> result = new HashSet<>(nodeMap.keySet());
        allNodes.forEach(n -> {
            if (Objects.nonNull(n.getSuccessors()) && !n.getSuccessors().isEmpty()){
                n.getSuccessors().forEach(result::remove);
            }
        });
        return result;
    }

    private static void visit(Stack<DagNode> path, DagNode currentNode, Map<Long, DagNode> nodeMap, List<String> taskPath){
        Objects.requireNonNull(currentNode);
        //check if the path already contain the current node, means have cycle
        // JDK replacement for Guava Sets.newHashSet(Iterable): copy stack content into a new HashSet.
        Set<DagNode> unique = new HashSet<>(path);
        if (unique.contains(currentNode)){
            throw new IllegalArgumentException("The DAG contains cycle.");
        }
        path.push(currentNode);
        if (!isEmpty(currentNode.getSuccessors())){
            currentNode.getSuccessors().forEach(id -> {
                DagNode nextNode = nodeMap.get(id);
                visit(path, nextNode, nodeMap, taskPath);
            });
            path.pop();
        }else {
            StringBuilder builder = new StringBuilder();
            for(DagNode n : path) {
                builder.append(n.getName()).append("->");
            }
            taskPath.add(builder.substring(0, builder.toString().length() - 2));
            path.pop();
        }
    }

    private static boolean isEmpty(Collection collection){
        return Objects.isNull(collection) || collection.isEmpty();
    }
}
