package dev.lokeshbisht.intent_service.repository;

import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface IntentStatusRepository extends ReactiveCrudRepository<PaymentIntentStatus, UUID> {

    @Query("SELECT * FROM payment_intent_status WHERE intent_id = :intentId " +
        "ORDER BY fencing_token DESC LIMIT 1")
    Mono<PaymentIntentStatus> findLatestStatus(UUID intentId);
}
