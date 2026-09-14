package com.course.inspection.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S77：地面路网 BFS 最短路径单测。 */
class GroundNetworkTest {

    @Test
    void pathStartsAtNearestToFromAndEndsAtNearestToTo() {
        // 取路网中两个已知节点坐标（NODES 第 0 与第 8 个）
        List<double[]> path = GroundNetwork.shortestPath(116.3969, 39.9100, 116.3980, 39.9089);
        assertFalse(path.isEmpty());
        assertEquals(2, path.get(0).length);
        assertEquals(2, path.get(path.size() - 1).length);
    }

    @Test
    void sameNodeReturnsShortPath() {
        List<double[]> path = GroundNetwork.shortestPath(116.3969, 39.9100, 116.3969, 39.9100);
        assertFalse(path.isEmpty());
        // 起点终点同一节点时路径很短（≤2 点）
        assertTrue(path.size() <= 2, "同点路径应≤2 点，实际=" + path.size());
    }

    @Test
    void everyStepIsAdjacent() {
        List<double[]> path = GroundNetwork.shortestPath(116.3969, 39.9100, 116.3980, 39.9089);
        for (int i = 1; i < path.size(); i++) {
            double dLng = path.get(i)[0] - path.get(i - 1)[0];
            double dLat = path.get(i)[1] - path.get(i - 1)[1];
            double dist = Math.sqrt(dLng * dLng + dLat * dLat);
            // 相邻路网点间距应小于一个街区（约 2e-3 度 ≈ 180m，含浮点余量）
            assertTrue(dist < 2e-3, "相邻点距离过大: " + dist);
        }
    }
}
