package dev.lokeshbisht.intent_service.service.impl;

import dev.lokeshbisht.intent_service.dto.request.PaymentDetails;
import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.dto.response.IntentResponse;
import dev.lokeshbisht.intent_service.entity.ImmediatePaymentIntentDetails;
import dev.lokeshbisht.intent_service.entity.PaymentIntent;
import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import dev.lokeshbisht.intent_service.service.UtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilServiceImpl implements UtilService {

    private ObjectMapper objectMapper;

    @Override
    public PaymentIntentStatus createPaymentIntentStatus(UUID intentId, IntentStatus status, String statusReasonCode) {
        return PaymentIntentStatus.builder()
            .intentId(intentId)
            .status(status)
            .statusReasonCode(statusReasonCode)
            .createdAt(OffsetDateTime.now())
            .build();
    }

    @Override
    public CreateIntentResponse mapToCreateIntentResponse(PaymentIntentStatus paymentIntentStatus) {
        return CreateIntentResponse.builder()
            .intentId(paymentIntentStatus.getIntentId().toString())
            .status(paymentIntentStatus.getStatus())
            .createdAt(paymentIntentStatus.getCreatedAt())
            .build();
    }

    @Override
    public IntentResponse mapToIntentResponse(PaymentIntent paymentIntent, PaymentIntentStatus paymentIntentStatus, PaymentDetails paymentDetails) {
        IntentResponse response = IntentResponse.builder()
            .intentId(paymentIntent.getIntentId())
            .idempotencyKey(paymentIntent.getIdempotencyKey())
            .psuId(paymentIntent.getPsuId())
            .paymentType(paymentIntent.getPaymentType())
            .scheme(paymentIntent.getScheme())
            .purposeCode(paymentIntent.getPurposeCode())
            .createdAt(paymentIntent.getCreatedAt())
            .build();

        response.setStatus(paymentIntentStatus.getStatus());
        response.setUpdatedAt(paymentIntentStatus.getCreatedAt());

        response.setPaymentDetails(paymentDetails);

        return response;
    }

    // DB-specific unique constraint detection — adapt for your DB / R2DBC driver
    public boolean isUniqueConstraintViolation(Throwable t) {
        String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
        return msg.contains("unique") || msg.contains("duplicate") || msg.contains("constraint");
    }
}
