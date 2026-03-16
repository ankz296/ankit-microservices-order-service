package dev.ankit.platform.order_service.services;


import dev.ankit.platform.order_service.client.ProductClient;
import dev.ankit.platform.order_service.client.UserClient;
import dev.ankit.platform.order_service.exception.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DownstreamValidationService {

    private final UserClient userClient;
    private final ProductClient productClient;

    public DownstreamValidationService(UserClient userClient, ProductClient productClient) {
        this.userClient = userClient;
        this.productClient = productClient;
    }

    @Retry(name = "userClient")
    @CircuitBreaker(name = "userClient", fallbackMethod = "userFallbackStrict")
    public UserClient.UserInternalDto fetchUser(UUID userId) {
        return userClient.getUser(userId);
    }

    @Retry(name = "productClient")
    @CircuitBreaker(name = "productClient", fallbackMethod = "productFallbackStrict")
    public ProductClient.ProductInternalDto fetchProduct(String productId) {
        return productClient.getProduct(productId);
    }

    // Strict fail fallback
    private UserClient.UserInternalDto userFallbackStrict(UUID userId, Throwable ex) {
        throw new DownstreamUnavailableException("USER-SERVICE unavailable for userId=" + userId, ex);
    }

    private ProductClient.ProductInternalDto productFallbackStrict(UUID productId, Throwable ex) {
        throw new DownstreamUnavailableException("PRODUCT-SERVICE unavailable for productId=" + productId, ex);
    }
}