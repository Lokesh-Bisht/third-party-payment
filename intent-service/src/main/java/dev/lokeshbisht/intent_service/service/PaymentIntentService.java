package dev.lokeshbisht.intent_service.service;

import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import reactor.core.publisher.Mono;

public interface PaymentIntentService {

    Mono<CreateIntentResponse> createPaymentIntent(CreateIntentRequest createIntentRequest);
}
