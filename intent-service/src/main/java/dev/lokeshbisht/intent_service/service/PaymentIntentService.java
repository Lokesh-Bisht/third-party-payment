package dev.lokeshbisht.intent_service.service;

import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.dto.request.UpdateIntentStatusRequest;
import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.dto.response.IntentResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentIntentService {

    Mono<CreateIntentResponse> createPaymentIntent(CreateIntentRequest createIntentRequest);

    Mono<IntentResponse> getPaymentIntent(UUID intentId, UUID psuId);

    Mono<IntentResponse> updatePaymentIntentStatus(UUID intentId, UUID psuId, UpdateIntentStatusRequest updateIntentStatusRequest);
}
