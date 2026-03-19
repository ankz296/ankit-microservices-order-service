package dev.ankit.platform.order_service.exception;


import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String message;
    private String path;
    private int status;
    private OffsetDateTime timestamp;

    private String traceId; // 🔥 VERY IMPORTANT
}