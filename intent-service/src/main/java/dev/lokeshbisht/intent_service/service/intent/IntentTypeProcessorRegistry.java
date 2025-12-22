package dev.lokeshbisht.intent_service.service.intent;

import dev.lokeshbisht.intent_service.dto.request.PaymentDetails;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.exceptions.IntentServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

import static dev.lokeshbisht.intent_service.enums.ErrorCodes.INT_SER_UNSUPPORTED_PAYMENT_TYPE;

@Component
@RequiredArgsConstructor
public class IntentTypeProcessorRegistry {

    private final List<IntentTypeProcessor<? extends PaymentDetails>> processors;

    public IntentTypeProcessor<? extends PaymentDetails> resolve(PaymentType type) {
        return processors.stream()
            .filter(p -> p.supports(type))
            .findFirst()
            .orElseThrow(() -> new IntentServiceException(HttpStatus.BAD_REQUEST, INT_SER_UNSUPPORTED_PAYMENT_TYPE, "Unsupported payment type: " + type));
    }
}
