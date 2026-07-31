package dev.ankit.platform.order_service.persistence.routing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReplicaSelector {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public String selectReplica(String shardId, Map<String, OrderAdvancedTopologyProperties.NodeDefinition> replicas) {
        if (replicas == null || replicas.isEmpty()) {
            return "primary";
        }

        List<String> replicaIds = new ArrayList<>(replicas.keySet());
        AtomicInteger counter = counters.computeIfAbsent(shardId, ignored -> new AtomicInteger());
        return replicaIds.get(Math.floorMod(counter.getAndIncrement(), replicaIds.size()));
    }
}
