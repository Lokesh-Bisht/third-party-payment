package dev.lokeshbisht.intent_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FieldValidationError {

    private String field;

    private String message;
}
