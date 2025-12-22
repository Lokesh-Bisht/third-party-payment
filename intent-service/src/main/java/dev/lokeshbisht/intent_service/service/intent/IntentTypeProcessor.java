package dev.lokeshbisht.intent_service.service.intent;

import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.dto.request.PaymentDetails;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IntentTypeProcessor<T extends PaymentDetails> {

    boolean supports(PaymentType type);

    Mono<Void> processPostCreation(CreateIntentRequest createIntentRequest, String paymentIntentJson);

    Mono<T> getPaymentIntentDetails(UUID intentId);
}
