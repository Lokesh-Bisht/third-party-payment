package dev.lokeshbisht.intent_service.repository;

import dev.lokeshbisht.intent_service.entity.PaymentIntent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface IntentRepository extends ReactiveCrudRepository<PaymentIntent, UUID> {

    Mono<PaymentIntent> findByIdempotencyKeyAndPsuId(UUID idempotencyKey, UUID psuId);

    Mono<PaymentIntent> findByIntentId(UUID intentId);
}
