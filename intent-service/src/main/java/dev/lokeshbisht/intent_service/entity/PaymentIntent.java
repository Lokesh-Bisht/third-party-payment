package dev.lokeshbisht.intent_service.entity;

import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.enums.PurposeCode;
import dev.lokeshbisht.intent_service.enums.Scheme;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_intent")
public class PaymentIntent {

    @Id
    @Column("id")
    private UUID id;

    @NotNull
    @Column("intent_id")
    private UUID intentId;

    @NotNull
    @Column("idempotency_key")
    private UUID idempotencyKey;

    @NotNull
    @Column("creditor_account_id")
    private String creditorAccountId;

    @Column("debtor_account_id")
    private String debtorAccountId;

    @Column("amount")
    private BigDecimal amount;

    @NotNull
    @Column("psu_id")
    private UUID psuId;

    @Column("scheme")
    private Scheme scheme;

    @NotNull
    @Column("purpose_code")
    private PurposeCode purposeCode;

    @NotNull
    @Column("payment_type")
    private PaymentType paymentType;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
