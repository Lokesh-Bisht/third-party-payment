package dev.lokeshbisht.intent_service.utils;

import dev.lokeshbisht.intent_service.constants.CommonConstants;
import dev.lokeshbisht.intent_service.dto.response.FieldValidationError;
import dev.lokeshbisht.intent_service.enums.ErrorCodes;
import dev.lokeshbisht.intent_service.exceptions.HeaderValidationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.lokeshbisht.intent_service.constants.CommonConstants.*;

@Component
public class HeaderValidator {

    public void validateCreateIntentHeaders(String psuId) {
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

    private void validationFailedCheck(boolean validationFailed, List<FieldValidationError> fieldValidationErrors) {
        if (validationFailed) {
            throw new HeaderValidationException(ErrorCodes.HEADER_VALIDATION_ERROR, HEADER_VALIDATION_FAILED_ERROR_MSG, fieldValidationErrors);
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
