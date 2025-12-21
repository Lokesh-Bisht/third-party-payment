package dev.lokeshbisht.intent_service.exceptions;

import dev.lokeshbisht.intent_service.enums.ErrorCodes;

public class DuplicateResourceException extends RuntimeException {

    private final ErrorCodes errorCode;

    public DuplicateResourceException(ErrorCodes errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCodes getErrorCode() { return this.errorCode; }
}
