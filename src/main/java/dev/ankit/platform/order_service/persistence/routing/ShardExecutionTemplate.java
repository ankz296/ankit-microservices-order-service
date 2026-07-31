package dev.ankit.platform.order_service.persistence.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ShardExecutionTemplate {

    private final OrderAdvancedTopologyProperties topologyProperties;
    private final ReplicaSelector replicaSelector;

    public <T> T executeRead(String shardId, Supplier<T> callback) {
        if (!topologyProperties.isEnabled()) {
            return callback.get();
        }

        OrderAdvancedTopologyProperties.ShardDefinition shard =
                topologyProperties.getShards().getOrDefault(shardId, new OrderAdvancedTopologyProperties.ShardDefinition());
        String replicaId = replicaSelector.selectReplica(shardId, shard.getReplicas());
        return executeWithContext(ShardRouteContext.read(shardId, replicaId), callback);
    }

    public <T> T executeWrite(String shardId, Supplier<T> callback) {
        if (!topologyProperties.isEnabled()) {
            return callback.get();
        }
        return executeWithContext(ShardRouteContext.write(shardId), callback);
    }

    private <T> T executeWithContext(ShardRouteContext routeContext, Supplier<T> callback) {
        try {
            ShardRouteContextHolder.set(routeContext);
            return callback.get();
        } finally {
            ShardRouteContextHolder.clear();
        }
    }
}
