package dev.lokeshbisht.intent_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.lokeshbisht.intent_service.dto.request.PaymentDetails;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.enums.PurposeCode;
import dev.lokeshbisht.intent_service.enums.Scheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntentResponse {

    private UUID intentId;

    private UUID idempotencyKey;

    private IntentStatus status;

    private PaymentType paymentType;

    private Scheme scheme;

    private PaymentDetails paymentDetails;

    private UUID psuId;

    private PurposeCode purposeCode;

    private OffsetDateTime createdAt;

    @JsonProperty(namespace = "statusUpdatedAt")
    private OffsetDateTime updatedAt;
}
