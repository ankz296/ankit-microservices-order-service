package dev.ankit.platform.order_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class UserClient {

    private final WebClient webClient;

    public UserClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .filter((request, next) -> {
                    log.error("🔥 OUTGOING HEADERS: {}", request.headers());
                    return next.exchange(request);
                })
                .build();
    }

    public UserInternalDto getUser(UUID userId) {
        try {
            return webClient.get()
                    .uri("http://user-service/internal/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserInternalDto.class)
                    .block(Duration.ofSeconds(2));
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new UserNotFoundException("User not found: " + userId, ex);
            }
            throw ex;
        }
    }

    public record UserInternalDto(UUID userId, boolean active) {}

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message, Throwable cause) { super(message, cause); }
    }
}