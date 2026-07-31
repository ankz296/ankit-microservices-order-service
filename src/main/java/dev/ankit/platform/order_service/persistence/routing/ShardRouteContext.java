package dev.ankit.platform.order_service.persistence.routing;

public record ShardRouteContext(String shardId, DatabaseRole role, String replicaId) {

    public static ShardRouteContext write(String shardId) {
        return new ShardRouteContext(shardId, DatabaseRole.WRITE, "primary");
    }

    public static ShardRouteContext read(String shardId, String replicaId) {
        return new ShardRouteContext(shardId, DatabaseRole.READ, replicaId);
    }
}
