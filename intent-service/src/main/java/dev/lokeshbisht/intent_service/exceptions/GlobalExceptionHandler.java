package dev.lokeshbisht.intent_service.exceptions;

import dev.lokeshbisht.intent_service.dto.response.ErrorResponseDto;
import dev.lokeshbisht.intent_service.dto.response.FieldValidationError;
import dev.lokeshbisht.intent_service.enums.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InternalServerErrorException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleInternalServerErrorException(InternalServerErrorException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ErrorCodes.INT_SER_INTERNAL_SERVER_ERROR, ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponseDto));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleDuplicateResourceException(DuplicateResourceException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getErrorCode(), ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT.value()).body(errorResponseDto));
    }

    @ExceptionHandler(ForbiddenException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleForbiddenException(ForbiddenException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getErrorCode(), ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN.value()).body(errorResponseDto));
    }

    @ExceptionHandler(IntentServiceException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleIntentServiceException(IntentServiceException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getErrorCode(), ex.getMessage());
        return Mono.just(ResponseEntity.status(ex.getStatus()).body(errorResponseDto));
    }

    @ExceptionHandler(HeaderValidationException.class )
    public Mono<ResponseEntity<ErrorResponseDto>> handleHeaderErrors(HeaderValidationException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getErrorCode(), ex.getMessage(), ex.getFieldValidationErrors());
        return Mono.just(ResponseEntity.badRequest().body(errorResponseDto));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleServerWebInputException(ServerWebInputException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ErrorCodes.MALFORMED_REQUEST_BODY, "Request body is malformed or contains invalid data");
        return Mono.just(ResponseEntity.badRequest().body(errorResponseDto));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<FieldValidationError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
            .toList();

        log.error("Validation failed. Errors: {}", fieldErrors);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ErrorCodes.INT_SER_BAD_REQUEST, "Validation failed", fieldErrors);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponseDto));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ErrorCodes.INT_SER_INTERNAL_SERVER_ERROR, "An error occurred while processing the request");
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponseDto));
    }
}
