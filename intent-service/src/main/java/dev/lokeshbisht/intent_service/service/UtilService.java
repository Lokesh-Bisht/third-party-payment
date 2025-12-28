package dev.lokeshbisht.intent_service.service;

import dev.lokeshbisht.intent_service.dto.request.PaymentDetails;
import dev.lokeshbisht.intent_service.dto.request.StatusReasonRequestDto;
import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.dto.response.IntentResponse;
import dev.lokeshbisht.intent_service.entity.PaymentIntent;
import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import dev.lokeshbisht.intent_service.enums.IntentStatus;

import java.util.UUID;

public interface UtilService {

    PaymentIntentStatus createPaymentIntentStatus(UUID intentId, IntentStatus status, String statusReasonCode, long fencingToken);

    CreateIntentResponse mapToCreateIntentResponse(PaymentIntentStatus paymentIntentStatus);

    IntentResponse mapToIntentResponse(PaymentIntent paymentIntent, PaymentIntentStatus paymentIntentStatus, PaymentDetails paymentDetails);

    boolean isUniqueConstraintViolation(Throwable t);

    String fetchStatusReasonCodeForStatus(StatusReasonRequestDto statusReasonRequestDto);
}
