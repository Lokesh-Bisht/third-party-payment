package dev.lokeshbisht.intent_service.service.intent;

import dev.lokeshbisht.intent_service.enums.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IntentTypeProcessorRegistry {

    private final List<IntentTypeProcessor> processors;

    public IntentTypeProcessor resolve(PaymentType type) {
        return processors.stream()
            .filter(p -> p.supports(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported payment type: " + type));
    }
}
