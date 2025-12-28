package dev.lokeshbisht.intent_service.entity;

import dev.lokeshbisht.intent_service.enums.IntentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_intent_status")
public class PaymentIntentStatus {

    @Id
    @Column("id")
    private UUID id;

    @NotNull
    @Column("intent_id")
    private UUID intentId;

    @NotNull
    @Column("status")
    private IntentStatus status;

    @Column("status_reason_code")
    private String statusReasonCode;

    @Column("fencing_token")
    private long fencingToken;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
