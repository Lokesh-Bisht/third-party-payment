package dev.lokeshbisht.intent_service.service;

import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import dev.lokeshbisht.intent_service.enums.IntentStatus;

import java.util.UUID;

public interface UtilService {

    PaymentIntentStatus createPaymentIntentStatus(UUID intentId, IntentStatus status, String statusReasonCode);

    CreateIntentResponse mapToIntentResponse(PaymentIntentStatus paymentIntentStatus);

    boolean isUniqueConstraintViolation(Throwable t);
}
