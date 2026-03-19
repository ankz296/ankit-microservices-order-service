package dev.ankit.platform.order_service.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ProductClient {

    private final WebClient webClient;

    public ProductClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public ProductInternalDto getProduct(String productId) {
        try {
            return webClient.get()
                    .uri("http://product-service/internal/products/{id}", productId)
                    .retrieve() // Timeout, Network glitch,5xx errors (server error)
                    //When should NOT retry?
                    //❌ 400 validation error
                    //❌ Business rule failure
                    //❌ Resource not found (404)
                    .bodyToMono(ProductInternalDto.class)
                    .block(Duration.ofSeconds(2));//timeout - Thread blocking avoid , Resource exhaustion
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProductNotFoundException("Product not found: " + productId, ex);
            }
            throw ex;
        }
    }

    public record ProductInternalDto(String productId, boolean available, Integer stock, BigDecimal price) {}

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message, Throwable cause) { super(message, cause); }
    }
}