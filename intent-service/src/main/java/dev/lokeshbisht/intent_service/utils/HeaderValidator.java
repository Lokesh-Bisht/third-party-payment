package dev.lokeshbisht.intent_service.utils;

import dev.lokeshbisht.intent_service.dto.response.FieldValidationError;
import dev.lokeshbisht.intent_service.enums.ErrorCodes;
import dev.lokeshbisht.intent_service.exceptions.HeaderValidationException;
import dev.lokeshbisht.intent_service.exceptions.IntentServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.lokeshbisht.intent_service.constants.CommonConstants.*;

@Component
public class HeaderValidator {

    public void validatePsuIdHeader(String psuId) {
        boolean validationFailed = false;
        List<FieldValidationError> fieldValidationErrors = new ArrayList<>();
        if (!StringUtils.hasText(psuId)) {
            validationFailed = true;
            fieldValidationErrors.add(new FieldValidationError(PSU_ID_HEADER, MISSING_HEADER_VALUE));
        } else if (uuidValidationFailedCheck(psuId)) {
            validationFailed = true;
            fieldValidationErrors.add(new FieldValidationError(PSU_ID_HEADER, PSU_ID_INVALID_HEADER_VALUE));
        }
        validationFailedCheck(validationFailed, fieldValidationErrors);
    }

    public void validateIntentId(String intentId) {
        if (uuidValidationFailedCheck(intentId)) {
            throw new IntentServiceException(HttpStatus.BAD_REQUEST, ErrorCodes.INT_SER_BAD_REQUEST, INVALID_INTENT_ID_ERROR_MSG);
        }
    }

    private void validationFailedCheck(boolean validationFailed, List<FieldValidationError> fieldValidationErrors) {
        if (validationFailed) {
            throw new HeaderValidationException(ErrorCodes.INVALID_HEADERS, HEADER_VALIDATION_FAILED_ERROR_MSG, fieldValidationErrors);
        }
    }

    private boolean uuidValidationFailedCheck(String id) {
        try {
            UUID.fromString(id);
            return false;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }
}
