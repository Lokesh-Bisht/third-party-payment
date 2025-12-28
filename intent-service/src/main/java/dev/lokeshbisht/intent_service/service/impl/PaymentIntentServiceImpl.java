package dev.lokeshbisht.intent_service.service.impl;

import dev.lokeshbisht.intent_service.constants.CommonConstants;
import dev.lokeshbisht.intent_service.dto.request.*;
import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.dto.response.IntentResponse;
import dev.lokeshbisht.intent_service.entity.PaymentIntent;
import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import dev.lokeshbisht.intent_service.enums.ErrorCodes;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import dev.lokeshbisht.intent_service.exceptions.DuplicateResourceException;
import dev.lokeshbisht.intent_service.exceptions.ForbiddenException;
import dev.lokeshbisht.intent_service.exceptions.IntentServiceException;
import dev.lokeshbisht.intent_service.exceptions.InternalServerErrorException;
import dev.lokeshbisht.intent_service.repository.IntentRepository;
import dev.lokeshbisht.intent_service.repository.IntentStatusRepository;
import dev.lokeshbisht.intent_service.service.PaymentIntentService;
import dev.lokeshbisht.intent_service.service.UtilService;
import dev.lokeshbisht.intent_service.service.intent.IntentTypeProcessor;
import dev.lokeshbisht.intent_service.service.intent.IntentTypeProcessorRegistry;
import dev.lokeshbisht.intent_service.service.lock.IntentStatusLockProperties;
import dev.lokeshbisht.intent_service.service.lock.RedisDistributedLockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static dev.lokeshbisht.intent_service.constants.CommonConstants.*;
import static dev.lokeshbisht.intent_service.enums.ErrorCodes.*;

@Service
@RequiredArgsConstructor
public class PaymentIntentServiceImpl implements PaymentIntentService {

    private final UtilService utilService;

    private final RedisDistributedLockService redisLockService;

    private final IntentStatusLockProperties lockProps;

    private final IntentRepository intentRepository;

    private final IntentStatusRepository intentStatusRepository;

    private final TransactionalOperator txOperator;

    private final IntentTypeProcessorRegistry intentTypeProcessorRegistry;

    private final ObjectMapper objectMapper;

    private final ReactiveRedisTemplate<String, CreateIntentResponse> reactiveRedisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(PaymentIntentServiceImpl.class);

    @Override
    public Mono<CreateIntentResponse> createPaymentIntent(CreateIntentRequest createIntentRequest) {
        UUID psuId = createIntentRequest.getPsuId();
        UUID intentId = createIntentRequest.getIntentId();
        UUID idempotencyKey = createIntentRequest.getIdempotencyKey();
        logger.info("Begin - createPaymentIntent for intentId={} idempotencyKey={} and psuId={}", intentId, idempotencyKey, psuId);

        final String lockKey = "lock:intent:create:" + intentId;
        final String cacheKey = "idem_psuId:intent:key:" + idempotencyKey + ':' + psuId + ":" + intentId;

        return redisLockService.acquire(lockKey, lockProps.getTtl(), lockProps.getMaxWait())
            .flatMap(lockToken -> {
                // Winner path: try cache->db->create
                logger.info("Lock acquired for intent={} lockKey={}, lockOwner={}, fencingToken={}",
                    intentId,
                    lockToken.lockKey(),
                    lockToken.owner(),
                    lockToken.fencingToken()
                );

                return handleCreateOrFetch(cacheKey, idempotencyKey, createIntentRequest)
                    .doFinally(signal -> redisLockService.release(lockToken)
                        .doOnError(e -> logger.error("Failed to release lock {}", lockToken.lockKey(), e))
                        .subscribe());
            })
            .switchIfEmpty(Mono.defer(() -> {
                // Loser path
                logger.info("Lock not acquired for intent={}, performing idempotent fetch", intentId);
                return fetchExistingRecord(createIntentRequest);
            }));

    }

    @Override
    public Mono<IntentResponse> getPaymentIntent(UUID intentId, UUID psuId) {
        logger.info("Begin - getPaymentIntent for intentId: {} and psuId: {}", intentId, psuId);
        return intentRepository.findByIntentId(intentId)
            .switchIfEmpty(
                Mono.error(new IntentServiceException(HttpStatus.NOT_FOUND, INT_SER_INTENT_NOT_FOUND, INTENT_NOT_FOUND_MSG))
            )
            .flatMap(paymentIntent -> {

                // Intent Exists but Belongs to Different PSU
                if (!psuId.equals(paymentIntent.getPsuId())) {
                    return Mono.error(new ForbiddenException(INT_SER_FORBIDDEN, INTENT_ACCESS_FORBIDDEN_MSG));
                }

                IntentTypeProcessor<?> processor = intentTypeProcessorRegistry.resolve(paymentIntent.getPaymentType());

                Mono<? extends PaymentDetails> paymentDetailsMono = processor.getPaymentIntentDetails(intentId);

                Mono<PaymentIntentStatus> paymentIntentStatusMono = intentStatusRepository.findLatestStatus(intentId);

                return Mono.zip(paymentDetailsMono, paymentIntentStatusMono)
                    .map(tuple -> {
                        PaymentDetails paymentDetails = tuple.getT1();
                        PaymentIntentStatus paymentIntentStatus = tuple.getT2();

                        return utilService.mapToIntentResponse(paymentIntent, paymentIntentStatus, paymentDetails);
                    });
            });
    }

    @Override
    public Mono<IntentResponse> updatePaymentIntentStatus(UUID intentId, UUID psuId, UpdateIntentStatusRequest updateIntentStatusRequest) {
        logger.info("Begin - updatePaymentIntentStatus.");

        final String lockKey = "update_intent:psuId:intent:lock:" + psuId + ':' + intentId;

        return redisLockService.acquire(lockKey, LOCK_EXPIRY)
            .flatMap(acquired -> {
                if (!acquired) {
                    return Mono.error(new IntentServiceException(HttpStatus.CONFLICT, INT_SER_FORBIDDEN, "a"));
                }
                return updatePaymentIntentStatusInternal(intentId, psuId, updateIntentStatusRequest.getIntentStatus(), updateIntentStatusRequest.getStatusReasonRequestDto())
                    .doFinally(signal -> redisLockService.release(lockKey).subscribe());
            });
    }

    private Mono<CreateIntentResponse> handleCreateOrFetch(String cacheKey, UUID idempotencyKey, CreateIntentRequest createIntentRequest) {
        // 1) Check if the intentId exists in cache
        Mono<CreateIntentResponse> cacheCheck = reactiveRedisTemplate.opsForValue().get(cacheKey)
            .map(intentResponse -> {
                logger.info("Found the payment request in cache. CacheKey={}", cacheKey);
                return intentResponse;
            })
            .onErrorResume(ex -> {
                // Redis read failed -> log and continue (we will fallback to DB)
                logger.error("Redis cache read failed for key {}: {}", cacheKey, ex.getMessage());
                return Mono.empty();
            });

        // 2) DB lookup (permanent source of truth) -> check if intent already exists in database
        Mono<CreateIntentResponse> dbCheck = dbCheck(idempotencyKey, createIntentRequest.getPsuId());

        // Not in DB → create intent transactionally
        Mono<CreateIntentResponse> createNewIntent = createNewIntent(createIntentRequest)
            .onErrorResume(err -> {
                if (utilService.isUniqueConstraintViolation(err)) {
                    logger.info("Unique constraint detected for intentId={}, fetching existing record.", createIntentRequest.getIntentId());
                    return fetchExistingRecord(createIntentRequest);
                }
                logger.error("An error occurred while creating the intent entity.", err);
                return Mono.error(new InternalServerErrorException(CommonConstants.INTERNAL_SERVER_ERROR_MSG));
            });

        return cacheCheck
            .switchIfEmpty(dbCheck)
            .switchIfEmpty(intentExistsForPsuCheck(createIntentRequest).then(createNewIntent))
            .flatMap(response -> cacheIntentResponse(cacheKey, response).thenReturn(response));
    }

    private Mono<CreateIntentResponse> dbCheck(UUID idempotencyKey, UUID psuId) {
        return intentRepository.findByIdempotencyKeyAndPsuId(idempotencyKey, psuId)
            .flatMap(existingPaymentIntent -> {
                // Same intentId but different PSU.
                if (!psuId.equals(existingPaymentIntent.getPsuId())) {
                    return Mono.error(
                        new ForbiddenException(ErrorCodes.INT_SER_FORBIDDEN, CommonConstants.INTENT_ACCESS_FORBIDDEN_MSG)
                    );
                }

                return intentStatusRepository.findLatestStatus(existingPaymentIntent.getIntentId())
                    .map(utilService::mapToCreateIntentResponse);
            })
            .doOnNext(res -> logger.info("The idempotency key and psuId combination already exists in the database."));
    }

    private Mono<Void> intentExistsForPsuCheck(CreateIntentRequest request) {
        return intentRepository.findByIntentId(request.getIntentId())
            .flatMap(existingPaymentIntent -> {
                UUID psuId = request.getPsuId();

                // Same intentId but different PSU.
                if (!psuId.equals(existingPaymentIntent.getPsuId())) {
                    return Mono.error(
                        new ForbiddenException(ErrorCodes.INT_SER_FORBIDDEN, CommonConstants.INTENT_ACCESS_FORBIDDEN_MSG)
                    );
                }

                // Same intentId + same PSU → idempotent replay allowed
                return Mono.empty();
            });
    }

    private Mono<CreateIntentResponse> createNewIntent(CreateIntentRequest createIntentRequest) {
        return Mono.defer(() -> {
            logger.info("Create a new intent record.");
            return txOperator.transactional(
                createPaymentIntentEntity(createIntentRequest)
                    .flatMap(intent -> {
                        String statusReasonCode = CREATE_INTENT_REASON_CODE + "|" + CREATE_INTENT_CODE;

                        PaymentIntentStatus intentStatus = utilService.createPaymentIntentStatus(
                            intent.getIntentId(), createIntentRequest.getStatus(), statusReasonCode
                        );

                        IntentTypeProcessor<?> processor = intentTypeProcessorRegistry.resolve(createIntentRequest.getPaymentType());

                        return processor.processPostCreation(createIntentRequest, getIntentRequestJson(createIntentRequest))
                            .then(intentStatusRepository.save(intentStatus))
                            .map(utilService::mapToCreateIntentResponse)
                            .doOnNext(response -> {
                                response.setIsConflicted(false);
                                logger.info("Successfully created a new intent record.");
                            });
                    })
            );
        });
    }

    private Mono<CreateIntentResponse> fetchExistingRecord(CreateIntentRequest createIntentRequest) {
        UUID intentId = createIntentRequest.getIntentId();

        return intentRepository.findByIntentId(intentId)
            .flatMap(existingIntent -> intentStatusRepository.findLatestStatus(intentId)
                .map(utilService::mapToCreateIntentResponse)
                .flatMap(response -> {
                    response.setIsConflicted(true);
                    return Mono.just(response);
                })
            );
    }

    private Mono<PaymentIntent> createPaymentIntentEntity(CreateIntentRequest createIntentRequest) {
        PaymentIntent paymentIntent = PaymentIntent.builder()
            .intentId(createIntentRequest.getIntentId())
            .idempotencyKey(createIntentRequest.getIdempotencyKey())
            .paymentType(createIntentRequest.getPaymentType())
            .psuId(createIntentRequest.getPsuId())
            .scheme(createIntentRequest.getScheme())
            .purposeCode(createIntentRequest.getPurposeCode())
            .build();

        PaymentDetails paymentDetails = createIntentRequest.getPaymentDetails();
        if (paymentDetails instanceof ImmediatePaymentDetails immediate) {
            paymentIntent.setCreditorAccountId(immediate.creditorAccount().getIban());
            paymentIntent.setDebtorAccountId(immediate.debtorAccount().getIban());
            paymentIntent.setAmount(immediate.instructedAmount().getAmount());
        }
        return intentRepository.save(paymentIntent);
    }

    private Mono<Void> cacheIntentResponse(String cacheKey, CreateIntentResponse createIntentResponse) {
        return reactiveRedisTemplate.opsForValue()
            .setIfAbsent(cacheKey, createIntentResponse, lockProps.getTtl())
            .doOnError(err -> logger.error("An error occurred while writing idempotencyKey to redis.", err))
            .then();
    }

    private String getIntentRequestJson(CreateIntentRequest request) {
        return objectMapper.writeValueAsString(request.getPaymentDetails());
    }

    private Mono<IntentResponse> updatePaymentIntentStatusInternal(UUID intentId, UUID psuId, IntentStatus targetStatus, StatusReasonRequestDto statusReasonRequestDto) {
        return intentRepository.findByIntentId(intentId)
            .switchIfEmpty(Mono.error(
                new IntentServiceException(HttpStatus.NOT_FOUND, INT_SER_INTENT_NOT_FOUND, INTENT_NOT_FOUND_MSG)
            ))
            .flatMap(paymentIntent -> {

                // PSU Validation -> intentId is valid but exists for a different PSU
                if (!psuId.equals(paymentIntent.getPsuId())) {
                    return Mono.error(
                        new ForbiddenException(INT_SER_FORBIDDEN, INTENT_ACCESS_FORBIDDEN_MSG)
                    );
                }

                IntentTypeProcessor<?> processor = intentTypeProcessorRegistry.resolve(paymentIntent.getPaymentType());

                Mono<? extends PaymentDetails> paymentDetailsMono = processor.getPaymentIntentDetails(intentId);

                Mono<PaymentIntentStatus> paymentIntentStatusMono = intentStatusRepository.findLatestStatus(intentId);

                return Mono.zip(paymentDetailsMono, paymentIntentStatusMono)
                    .flatMap(tuple -> {
                        PaymentDetails paymentDetails = tuple.getT1();
                        PaymentIntentStatus paymentIntentStatus = tuple.getT2();
                        IntentStatus currentStatus = paymentIntentStatus.getStatus();

                        if (currentStatus == targetStatus) {
                            return Mono.just(utilService.mapToIntentResponse(paymentIntent, paymentIntentStatus, paymentDetails));
                        }

                        // The current status of the intent is a terminal status
                        if (currentStatus.equals(IntentStatus.CONSENT_VERIFIED) || currentStatus.equals(IntentStatus.REJECTED)) {
                            return Mono.error(new IntentServiceException(HttpStatus.CONFLICT, INT_SER_INTENT_ALREADY_FINALIZED, INTENT_INVALID_STATUS_TRANSITION));
                        }

                        if (!currentStatus.canTransitionTo(targetStatus)) {
                            return Mono.error(new IntentServiceException(HttpStatus.CONFLICT, INT_SER_INVALID_STATUS_TRANSITION, INTENT_INVALID_STATUS_TRANSITION));
                        }

                        String statusReasonCode = utilService.fetchStatusReasonCodeForStatus(statusReasonRequestDto);

                        // Persist new intent status record
                        PaymentIntentStatus newStatus = utilService.createPaymentIntentStatus(
                            paymentIntent.getIntentId(),
                            targetStatus,
                            statusReasonCode
                            );

                        return  intentStatusRepository.save(newStatus)
                            .doOnSuccess(v -> logger.info("Successfully updated the payment intent status."))
                            .thenReturn(
                                utilService.mapToIntentResponse(paymentIntent, newStatus, paymentDetails)
                            );
                    });
            });
    }
}
