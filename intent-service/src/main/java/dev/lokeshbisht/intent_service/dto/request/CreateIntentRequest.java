package dev.lokeshbisht.intent_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.enums.PurposeCode;
import dev.lokeshbisht.intent_service.enums.Scheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateIntentRequest {

    @NotNull
    private UUID intentId;

    @NotNull
    private UUID idempotencyKey;

    @NotNull
    private IntentStatus status;

    @NotNull
    private PaymentType paymentType;

    private Scheme scheme;

    @Valid
    @NotNull
    private PaymentDetails paymentDetails;

    private UUID psuId;

    @NotNull
    private PurposeCode purposeCode;
}
