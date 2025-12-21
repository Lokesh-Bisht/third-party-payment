package dev.lokeshbisht.intent_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.enums.PurposeCode;
import dev.lokeshbisht.intent_service.enums.Scheme;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateIntentRequest {

    private UUID intentId;

    private UUID idempotencyKey;

    private IntentStatus status;

    private PaymentType paymentType;

    private Scheme scheme;

    private PaymentDetails paymentDetails;

    private UUID psuId;

    private PurposeCode purposeCode;
}
