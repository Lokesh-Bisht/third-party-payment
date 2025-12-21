package dev.lokeshbisht.intent_service.service.intent;

import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.dto.request.PaymentDetails;
import dev.lokeshbisht.intent_service.entity.PaymentIntent;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import reactor.core.publisher.Mono;

public interface IntentTypeProcessor {

    boolean supports(PaymentType type);

    Mono<Void> processPostCreation(CreateIntentRequest createIntentRequest, String paymentIntentJson);
}
