package dev.lokeshbisht.intent_service.entity;

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
@Table(name = "immediate_payment_intent_details")
public class ImmediatePaymentIntentDetails {

    @Id
    @Column("id")
    private UUID id;

    @NotNull
    @Column("intent_id")
    private UUID intentId;

    @NotNull
    @Column("payment_details")
    private String paymentDetails;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
