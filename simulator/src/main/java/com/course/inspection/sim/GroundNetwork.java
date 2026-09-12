package com.course.inspection.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * 园区地面路网（S33，用户定制：机器狗不能飞，点到点走地面最短路径）。
 * 9 节点 3×3 路网覆盖园区；BFS 求最短路径（无权重差，BFS 即最短）。
 */
public final class GroundNetwork {

    private static final double[][] NODES = {
            {116.3966, 39.9086}, {116.3974, 39.9086}, {116.3984, 39.9086},
            {116.3966, 39.9094}, {116.3974, 39.9094}, {116.3984, 39.9094},
            {116.3966, 39.9104}, {116.3974, 39.9104}, {116.3984, 39.9104}
    };

    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {3, 4}, {4, 5}, {6, 7}, {7, 8},
            {0, 3}, {3, 6}, {1, 4}, {4, 7}, {2, 5}, {5, 8}
    };

    private GroundNetwork() {
    }

    /** 求 from→to 的最短路径（含起点与终点，坐标序列）。 */
    public static List<double[]> shortestPath(double fromLng, double fromLat, double toLng, double toLat) {
        int s = nearestNode(fromLng, fromLat);
        int t = nearestNode(toLng, toLat);

        int n = NODES.length;
        int[] prev = new int[n];
        Arrays.fill(prev, -1);
        boolean[] visited = new boolean[n];
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(s);
        visited[s] = true;

        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == t) {
                break;
            }
            for (int[] e : EDGES) {
                int next = -1;
                if (e[0] == cur) {
                    next = e[1];
                } else if (e[1] == cur) {
                    next = e[0];
                }
                if (next >= 0 && !visited[next]) {
                    visited[next] = true;
                    prev[next] = cur;
                    queue.add(next);
                }
            }
        }

        List<Integer> route = new ArrayList<>();
        for (int at = t; at != -1; at = prev[at]) {
            route.add(at);
        }
        java.util.Collections.reverse(route);

        List<double[]> result = new ArrayList<>();
        result.add(new double[]{fromLng, fromLat});
        for (int idx : route) {
            if (idx != s) {
                result.add(NODES[idx]);
            }
        }
        result.add(new double[]{toLng, toLat});
        return result;
    }

    private static int nearestNode(double lng, double lat) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < NODES.length; i++) {
            double d = dist(lng, lat, NODES[i][0], NODES[i][1]);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private static double dist(double aLng, double aLat, double bLng, double bLat) {
        double dx = (bLng - aLng) * 111_000d * Math.cos(Math.toRadians(aLat));
        double dy = (bLat - aLat) * 111_000d;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
