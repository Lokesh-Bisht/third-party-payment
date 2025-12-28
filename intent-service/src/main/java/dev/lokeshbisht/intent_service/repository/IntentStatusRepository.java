package dev.lokeshbisht.intent_service.repository;

import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
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

    @Query("""
    INSERT INTO payment_intent_status
    (intent_id, status, status_reason_code, fencing_token)
    SELECT :intentId, :status, :statusReasonCode, :fencingToken
    WHERE NOT EXISTS (
        SELECT 1 FROM payment_intent_status
        WHERE intent_id = :intentId
          AND fencing_token >= :fencingToken
    )
    RETURNING id, intent_id, status, status_reason_code, fencing_token, created_at
""")
    Mono<PaymentIntentStatus> insertIfLatest(UUID intentId, IntentStatus status, String statusReasonCode, long fencingToken);
}
