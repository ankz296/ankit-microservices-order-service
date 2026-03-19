package dev.ankit.platform.order_service.services;


import dev.ankit.platform.order_service.client.ProductClient;
import dev.ankit.platform.order_service.client.UserClient;
import dev.ankit.platform.order_service.exception.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class DownstreamValidationService {

    /**
     * Flow is: first Retry happens on a single request (as per max‑attempts).
     * If failures cross the threshold over multiple requests, the Circuit Breaker opens.
     * While OPEN, no new requests are allowed—fallback responds immediately.
     * After the wait period, the circuit goes HALF_OPEN, where limited calls are allowed;
     * Retry still applies but is bounded by the HALF_OPEN limit. If these test calls succeed, the circuit CLOSES;
     * if any fail, it re‑OPENS.
     */

    private final UserClient userClient;
    private final ProductClient productClient;
    private static final java.util.concurrent.atomic.AtomicInteger PRODUCT_ATTEMPT = new java.util.concurrent.atomic.AtomicInteger();

    public DownstreamValidationService(UserClient userClient, ProductClient productClient) {
        this.userClient = userClient;
        this.productClient = productClient;
    }

    @Retry(name = "userClient")
    @CircuitBreaker(name = "userClient", fallbackMethod = "userFallbackStrict")
    public UserClient.UserInternalDto fetchUser(UUID userId) {

        log.info("Calling user-service userid={}", userId);

        return userClient.getUser(userId);
    }

    //minimum-number-of-calls -> reach
    // failure-rate-threshold exceed
    // in our config 5 calls minimum, 50% failure rate, 10 seconds open state after it turn to half_open
    // in half_open  limited test calls - in our config its 3 -> if call success its turn to close or again turn to open state
    @Retry(name = "productClient")
    @CircuitBreaker(name = "productClient", fallbackMethod = "productFallbackStrict")
    public ProductClient.ProductInternalDto fetchProduct(String productId) {

        int attempt = PRODUCT_ATTEMPT.incrementAndGet();

        log.info("Calling product-service productId={}, attempt={}", productId, attempt);

        return productClient.getProduct(productId);
    }

    //Retry exhausted, Circuit OPEN, Timeout
    // Strict fail fallback
    private UserClient.UserInternalDto userFallbackStrict(UUID userId, Throwable ex) {
        log.error("User service fallback triggered userId={}, reason={}",
                userId, ex.toString());
        throw new DownstreamUnavailableException("USER-SERVICE unavailable for userId=" + userId, ex);
    }

    private ProductClient.ProductInternalDto productFallbackStrict(String productId, Throwable ex) {

        log.error("Product service fallback triggered productId={}, reason={}",
                productId, ex.toString());

        throw new DownstreamUnavailableException(
                "PRODUCT-SERVICE unavailable for productId=" + productId, ex);
    }
}