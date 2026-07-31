package dev.ankit.platform.order_service.persistence.routing;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShardRouteContextHolder {

    private static final ThreadLocal<ShardRouteContext> CONTEXT = new ThreadLocal<>();

    public static void set(ShardRouteContext routeContext) {
        CONTEXT.set(routeContext);
    }

    public static ShardRouteContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
