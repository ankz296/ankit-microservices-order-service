package dev.ankit.platform.order_service.config;


import dev.ankit.platform.order_service.exception.BusinessException;
import dev.ankit.platform.order_service.exception.DownstreamUnavailableException;
import dev.ankit.platform.order_service.exception.ErrorResponse;
import dev.ankit.platform.order_service.exception.OrderNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "dev.ankit.platform.order_service")
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex, HttpServletRequest req) {

        log.warn("Order not found path={}, message={}", req.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {

        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed path={}, errors={}", req.getRequestURI(), msg);

        return buildResponse(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {

        log.warn("Business exception path={}, message={}", req.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamUnavailable(DownstreamUnavailableException ex, HttpServletRequest req) {

        log.error("Downstream unavailable path={}, message={}", req.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {

        log.error("Unhandled exception path={}, error={}", req.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
    }

    // 🔥 Common builder
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest req) {

        String traceId = org.slf4j.MDC.get("traceId");

        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .message(message)
                        .path(req.getRequestURI())
                        .status(status.value())
                        .timestamp(OffsetDateTime.now())
                        .traceId(traceId)   // 🔥 ADD THIS FIELD
                        .build()
        );
    }
}