package dev.ankit.platform.order_service.persistence.routing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "order.persistence.advanced-topology")
public class OrderAdvancedTopologyProperties {

    private boolean enabled;
    private int shardCount = 1;
    private String defaultShardId = "order-shard-0";
    private Map<String, ShardDefinition> shards = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class ShardDefinition {
        private NodeDefinition write = new NodeDefinition();
        private Map<String, NodeDefinition> replicas = new LinkedHashMap<>();
    }

    @Getter
    @Setter
    public static class NodeDefinition {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
    }
}
