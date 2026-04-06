package top.ilovemyhome.dagtask.core.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class DagTest {

    @Test
    public void checkNoCycle() {
        assertDoesNotThrow(() -> {
            DagNode n1 = new DagNode(1L, Set.of(2L, 3L));
            DagNode n2 = new DagNode(2L, Set.of(4L));
            DagNode n3 = new DagNode(3L, Set.of(5L, 6L));
            DagNode n4 = new DagNode(4L, Set.of(7L));
            DagNode n5 = new DagNode(5L, Set.of(8L));
            DagNode n6 = new DagNode(6L, Set.of(8L));
            DagNode n7 = new DagNode(7L, null);
            DagNode n8 = new DagNode(8L, null);
            DagNode n9 = new DagNode(9L, Set.of(6L));
            DagNode n10 = new DagNode(10L, Set.of(6L));
            DagNode n11 = new DagNode(11L, null);
            List<DagNode> allNodes = List.of(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11);
            List<String> taskPath = new ArrayList<>();
            DagHelper.visitDAG(allNodes, taskPath);
            System.out.println(taskPath);
        });
    }

    @Test
    public void checkSingleNode() {
        assertDoesNotThrow(() -> {
            DagNode n1 = new DagNode(1L, null);
            List<DagNode> allNodes = List.of(n1);
            List<String> taskPath = new ArrayList<>();
            DagHelper.visitDAG(allNodes, taskPath);
            System.out.println(taskPath);
        });
    }

    @Test
    public void checkLineNodes() {
        DagNode n1 = new DagNode(1L, Set.of(2L));
        DagNode n2 = new DagNode(2L, Set.of(3L));
        DagNode n3 = new DagNode(3L, Set.of(4L));
        DagNode n4 = new DagNode(4L, null);
        List<DagNode> allNodes = List.of(n1, n2, n3, n4);
        List<String> taskPath = new ArrayList<>();
        DagHelper.visitDAG(allNodes, taskPath);
        System.out.println(taskPath);
    }


    @Test
    public void checkHaveCycle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DagNode n1 = new DagNode(1L, Set.of(2L, 3L));
            DagNode n2 = new DagNode(2L, Set.of(4L));
            DagNode n3 = new DagNode(3L, Set.of(5L, 6L));
            DagNode n4 = new DagNode(4L, Set.of(7L));
            DagNode n5 = new DagNode(5L, Set.of(8L));
            DagNode n6 = new DagNode(6L, Set.of(8L));
            DagNode n7 = new DagNode(7L, Set.of(8L));
            DagNode n8 = new DagNode(8L, Set.of(4L));
            DagNode n9 = new DagNode(9L, Set.of(6L));
            DagNode n10 = new DagNode(10L, Set.of(6L));
            DagNode n11 = new DagNode(11L, null);
            List<DagNode> allNodes = List.of(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11);
            List<String> taskPath = new ArrayList<>();
            DagHelper.visitDAG(allNodes, taskPath);
            System.out.println(taskPath);
        });
    }


}
