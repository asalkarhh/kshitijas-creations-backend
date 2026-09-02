package com.asalkartechworks.kshitijascreations.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String requestId,
        Map<String, String> fieldErrors
) {
    public static ApiError of(HttpStatus status, String code, String message, String path, Map<String, String> fieldErrors) {
        String requestId = MDC.get("requestId");
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                requestId,
                fieldErrors
        );
    }
}
