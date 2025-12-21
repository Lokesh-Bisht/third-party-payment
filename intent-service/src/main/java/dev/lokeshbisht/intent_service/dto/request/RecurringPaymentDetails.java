package dev.lokeshbisht.intent_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.enums.RecurrenceFrequency;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecurringPaymentDetails(

    PaymentType paymentType,

    Account creditorAccount,

    Account debtorAccount,

    InstructedAmount instructedAmount,

    OffsetDateTime startDate,

    OffsetDateTime endDate,

    RecurrenceFrequency frequency,

    boolean autoDebitEnabled
) implements PaymentDetails {}
