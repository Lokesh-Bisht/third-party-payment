package dev.lokeshbisht.intent_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.lokeshbisht.intent_service.enums.PaymentType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImmediatePaymentDetails (

    PaymentType paymentType,

    Account creditorAccount,

    Account debtorAccount,

    InstructedAmount instructedAmount
) implements PaymentDetails {}
