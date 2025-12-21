package dev.lokeshbisht.intent_service.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "paymentType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ImmediatePaymentDetails.class, name = "IMMEDIATE"),
    @JsonSubTypes.Type(value = RecurringPaymentDetails.class, name = "RECURRING")
})
public sealed interface PaymentDetails permits
    ImmediatePaymentDetails,
    RecurringPaymentDetails {
}
