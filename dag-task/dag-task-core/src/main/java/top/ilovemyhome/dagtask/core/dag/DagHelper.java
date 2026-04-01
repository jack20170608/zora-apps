package top.ilovemyhome.dagtask.core.dag;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DagHelper {

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
        Set<Long> result = Sets.newHashSet(nodeMap.keySet());
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
        Set<DagNode> unique = Sets.newHashSet(path);
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

    private static final Logger LOGGER = LoggerFactory.getLogger(DagHelper.class);
}
