package dev.ankit.platform.order_service.persistence.routing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(OrderAdvancedTopologyProperties.class)
public class OrderAdvancedPersistenceConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "order.persistence.advanced-topology", name = "enabled", havingValue = "true")
    public DataSource orderRoutingDataSource(OrderAdvancedTopologyProperties topologyProperties) {
        Map<Object, Object> targets = new LinkedHashMap<>();

        topologyProperties.getShards().forEach((shardId, shard) -> {
            targets.put(
                    ShardRoutingDataSource.routeKey(shardId, DatabaseRole.WRITE, "primary"),
                    buildDataSource(shard.getWrite())
            );
            shard.getReplicas().forEach((replicaId, replica) -> targets.put(
                    ShardRoutingDataSource.routeKey(shardId, DatabaseRole.READ, replicaId),
                    buildDataSource(replica)
            ));
        });

        ShardRoutingDataSource routingDataSource = new ShardRoutingDataSource();
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(
                targets.get(ShardRoutingDataSource.routeKey(
                        topologyProperties.getDefaultShardId(),
                        DatabaseRole.WRITE,
                        "primary"))
        );
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    private DataSource buildDataSource(OrderAdvancedTopologyProperties.NodeDefinition nodeDefinition) {
        return DataSourceBuilder.create()
                .url(nodeDefinition.getUrl())
                .username(nodeDefinition.getUsername())
                .password(nodeDefinition.getPassword())
                .driverClassName(nodeDefinition.getDriverClassName())
                .build();
    }
}
