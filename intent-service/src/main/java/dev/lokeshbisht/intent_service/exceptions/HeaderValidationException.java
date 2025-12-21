package dev.lokeshbisht.intent_service.exceptions;

import dev.lokeshbisht.intent_service.dto.response.FieldValidationError;
import dev.lokeshbisht.intent_service.enums.ErrorCodes;

import java.util.List;

public class HeaderValidationException extends RuntimeException {

    private final ErrorCodes errorCode;

    private final List<FieldValidationError> fieldValidationErrors;

    public HeaderValidationException(ErrorCodes errorCode, String message, List<FieldValidationError> fieldValidationErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldValidationErrors = fieldValidationErrors;
    }

    public ErrorCodes getErrorCode() { return this.errorCode; }

    public List<FieldValidationError> getFieldValidationErrors() { return this.fieldValidationErrors; }
}
