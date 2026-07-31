package dev.ankit.platform.order_service.persistence.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class ShardRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        ShardRouteContext routeContext = ShardRouteContextHolder.get();
        if (routeContext == null) {
            return null;
        }
        return routeKey(routeContext.shardId(), routeContext.role(), routeContext.replicaId());
    }

    public static String routeKey(String shardId, DatabaseRole role, String nodeId) {
        return shardId + "|" + role.name() + "|" + nodeId;
    }
}
