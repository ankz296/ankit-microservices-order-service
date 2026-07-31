package dev.ankit.platform.order_service.persistence.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShardKeyResolver {

    private final OrderAdvancedTopologyProperties topologyProperties;

    public String resolveShard(UUID key) {
        int shardCount = Math.max(topologyProperties.getShardCount(), 1);
        int shardIndex = Math.floorMod(key.hashCode(), shardCount);
        return "order-shard-" + shardIndex;
    }

    public String resolveShard(String key) {
        return resolveShard(UUID.nameUUIDFromBytes(key.getBytes()));
    }
}
