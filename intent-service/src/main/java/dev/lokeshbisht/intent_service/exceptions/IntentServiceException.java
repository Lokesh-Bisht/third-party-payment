package dev.lokeshbisht.intent_service.exceptions;

import dev.lokeshbisht.intent_service.enums.ErrorCodes;
import org.springframework.http.HttpStatus;

public class IntentServiceException extends RuntimeException {

    private final HttpStatus status;

    private final ErrorCodes errorCode;

    public IntentServiceException(HttpStatus status, ErrorCodes errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return this.status; }

    public ErrorCodes getErrorCode() { return this.errorCode; }
}
