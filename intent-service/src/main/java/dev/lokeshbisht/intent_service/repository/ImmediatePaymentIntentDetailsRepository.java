package dev.lokeshbisht.intent_service.repository;

import dev.lokeshbisht.intent_service.entity.ImmediatePaymentIntentDetails;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ImmediatePaymentIntentDetailsRepository extends ReactiveCrudRepository<ImmediatePaymentIntentDetails, Long> {

    @Query("""
        INSERT INTO immediate_payment_intent_details
        (intent_id, payment_details)
        VALUES (:intentId, CAST(:paymentDetails AS jsonb))
    """)
    Mono<Void> saveJson(UUID intentId, String paymentDetails);
}
