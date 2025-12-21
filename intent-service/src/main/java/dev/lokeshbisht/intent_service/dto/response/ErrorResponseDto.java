package dev.lokeshbisht.intent_service.dto.response;

import dev.lokeshbisht.intent_service.enums.ErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ErrorResponseDto {

    private ErrorCodes errorCode;

    private String message;

    private List<FieldValidationError> errors;

    public ErrorResponseDto(ErrorCodes errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
