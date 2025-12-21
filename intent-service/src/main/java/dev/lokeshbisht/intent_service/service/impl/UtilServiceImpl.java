package dev.lokeshbisht.intent_service.service.impl;

import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import dev.lokeshbisht.intent_service.service.UtilService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UtilServiceImpl implements UtilService {

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
    public CreateIntentResponse mapToIntentResponse(PaymentIntentStatus paymentIntentStatus) {
        return CreateIntentResponse.builder()
            .intentId(paymentIntentStatus.getIntentId().toString())
            .status(paymentIntentStatus.getStatus())
            .createdAt(paymentIntentStatus.getCreatedAt())
            .build();
    }

    // DB-specific unique constraint detection — adapt for your DB / R2DBC driver
    public boolean isUniqueConstraintViolation(Throwable t) {
        String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
        return msg.contains("unique") || msg.contains("duplicate") || msg.contains("constraint");
    }
}
