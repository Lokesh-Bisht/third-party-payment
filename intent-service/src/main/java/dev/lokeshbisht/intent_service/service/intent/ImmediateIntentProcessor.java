package dev.lokeshbisht.intent_service.service.intent;

import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.dto.request.ImmediatePaymentDetails;
import dev.lokeshbisht.intent_service.entity.ImmediatePaymentIntentDetails;
import dev.lokeshbisht.intent_service.enums.PaymentType;
import dev.lokeshbisht.intent_service.repository.ImmediatePaymentIntentDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ImmediateIntentProcessor implements IntentTypeProcessor<ImmediatePaymentDetails> {

    private final ObjectMapper objectMapper;

    private final ImmediatePaymentIntentDetailsRepository immediatePaymentIntentDetailsRepository;

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.IMMEDIATE;
    }

    @Override
    public Mono<Void> processPostCreation(CreateIntentRequest request, String paymentIntentJson) {
        return immediatePaymentIntentDetailsRepository.saveJson(request.getIntentId(), paymentIntentJson).then();
    }

    @Override
    public Mono<ImmediatePaymentDetails> getPaymentIntentDetails(UUID intentId) {
        return immediatePaymentIntentDetailsRepository.findByIntentId(intentId)
            .map(this::mapToDto);
    }

    private ImmediatePaymentDetails mapToDto(ImmediatePaymentIntentDetails entity) {
        return objectMapper.readValue(entity.getPaymentDetails(), ImmediatePaymentDetails.class);
    }
}
