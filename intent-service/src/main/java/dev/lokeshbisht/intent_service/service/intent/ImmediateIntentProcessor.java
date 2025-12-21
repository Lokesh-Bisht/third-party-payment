package dev.lokeshbisht.intent_service.service.intent;

import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.entity.ImmediatePaymentIntentDetails;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.repository.ImmediatePaymentIntentDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ImmediateIntentProcessor implements IntentTypeProcessor {

    private final ImmediatePaymentIntentDetailsRepository immediatePaymentIntentDetailsRepository;

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.IMMEDIATE;
    }

    @Override
    public Mono<Void> processPostCreation(CreateIntentRequest request, String paymentIntentJson) {
        return immediatePaymentIntentDetailsRepository.saveJson(request.getIntentId(), paymentIntentJson).then();
    }
}
