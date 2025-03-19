package io.github.codeed.dbupgrader.utils;

import java.util.*;

public class TopologicalSort {
    /**
     * Performs topological sort on a directed graph
     * @param graph Map where key is node and value is set of nodes it depends on
     * @return Sorted list of nodes, or null if graph has cycles
     */
    public static List<String> sort(Map<String, Set<String>> graph) {
        // Count incoming edges for each node
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : graph.keySet()) {
            inDegree.put(node, 0);
        }
        
        for (Set<String> edges : graph.values()) {
            for (String edge : edges) {
                inDegree.merge(edge, 1, Integer::sum);
            }
        }
        
        // Add nodes with no dependencies to queue
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            result.add(node);
            
            // For each dependent node, decrease inDegree by 1
            for (String neighbor : graph.keySet()) {
                if (graph.get(neighbor).contains(node)) {
                    inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                    if (inDegree.get(neighbor) == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
        }
        
        // If result size is less than graph size, there's a cycle
        return result.size() == graph.size() ? result : null;
    }
} 