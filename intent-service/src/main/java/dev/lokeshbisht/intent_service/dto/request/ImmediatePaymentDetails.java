package dev.lokeshbisht.intent_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImmediatePaymentDetails (

    PaymentType paymentType,

    @Valid
    @NotNull
    Account creditorAccount,

    Account debtorAccount,

    @Valid
    @NotNull
    InstructedAmount instructedAmount
) implements PaymentDetails {}
