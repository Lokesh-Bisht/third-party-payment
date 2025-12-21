package dev.lokeshbisht.intent_service.service.impl;

import dev.lokeshbisht.intent_service.constants.CommonConstants;
import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.dto.request.ImmediatePaymentDetails;
import dev.lokeshbisht.intent_service.dto.request.PaymentDetails;
import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.entity.PaymentIntent;
import dev.lokeshbisht.intent_service.entity.PaymentIntentStatus;
import dev.lokeshbisht.intent_service.enums.ErrorCodes;
import dev.lokeshbisht.intent_service.exceptions.DuplicateResourceException;
import dev.lokeshbisht.intent_service.exceptions.ForbiddenException;
import dev.lokeshbisht.intent_service.exceptions.InternalServerErrorException;
import dev.lokeshbisht.intent_service.repository.IntentRepository;
import dev.lokeshbisht.intent_service.repository.IntentStatusRepository;
import dev.lokeshbisht.intent_service.service.PaymentIntentService;
import dev.lokeshbisht.intent_service.service.UtilService;
import dev.lokeshbisht.intent_service.service.intent.IntentTypeProcessor;
import dev.lokeshbisht.intent_service.service.intent.IntentTypeProcessorRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.lokeshbisht.intent_service.constants.CommonConstants.CREATE_INTENT_CODE;
import static dev.lokeshbisht.intent_service.constants.CommonConstants.CREATE_INTENT_REASON_CODE;

@Service
@RequiredArgsConstructor
public class PaymentIntentServiceImpl implements PaymentIntentService {

    private final UtilService utilService;

    private final IntentRepository intentRepository;

    private final IntentStatusRepository intentStatusRepository;

    private final TransactionalOperator txOperator;

    private final IntentTypeProcessorRegistry intentTypeProcessorRegistry;

    private final ObjectMapper objectMapper;

    private final ReactiveStringRedisTemplate lockRedisTemplate;

    private final ReactiveRedisTemplate<String, CreateIntentResponse> reactiveRedisTemplate;

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private static final Duration LOCK_EXPIRY = Duration.ofSeconds(1000);

    private static final Duration LOCK_WAIT_RETRY = Duration.ofMillis(200);

    private static final int LOCK_WAIT_ATTEMPTS = 10;

    private static final Logger logger = LoggerFactory.getLogger(PaymentIntentServiceImpl.class);

    @Override
    public Mono<CreateIntentResponse> createPaymentIntent(CreateIntentRequest createIntentRequest) {
        UUID psuId = createIntentRequest.getPsuId();
        UUID idempotencyKey = createIntentRequest.getIdempotencyKey();
        logger.info("Begin - createPaymentIntent for idempotencyKey: {} and psuId: {}", idempotencyKey, psuId);

        final String lockKey = "idem_psuId:intent:lock:" + idempotencyKey + ':' + psuId;
        final String cacheKey = "idem_psuId:intent:key:" + idempotencyKey + ':' + psuId;

        final String lockOwner = UUID.randomUUID().toString();
        final AtomicBoolean lockAcquired = new AtomicBoolean(false);

        return lockRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockOwner, LOCK_EXPIRY)
            .onErrorResume(e -> {
                logger.warn("Redis lock acquisition error for {}: {}", lockKey, e.toString());
                return Mono.just(false);
            })
            .flatMap(acquired -> {
                if (Boolean.TRUE.equals(acquired)) {
                    lockAcquired.set(true);
                    logger.info("Lock acquired for lockKey={}", lockKey);
                    // Winner path: try cache->db->create
                    return handleCreateOrFetch(cacheKey, idempotencyKey, createIntentRequest)
                        .flatMap(response -> releaseLock(lockKey).thenReturn(response));
                } else {
                    logger.info("Lock not acquired for lockKey={}, entering wait path", lockKey);
                    // Loser path: wait briefly + poll cache/db for result
                    return waitAndFetch(cacheKey, idempotencyKey, createIntentRequest.getPsuId())
                        .onErrorResume(e -> {
                            // If waiting failed (timeout), fallback to a safe DB-only create attempt
                            logger.warn("Wait-and-fetch timed out for {}, attempting DB-check-create fallback", idempotencyKey);
                            return dbFallbackCreate(idempotencyKey, createIntentRequest);
                        });
                }
            });
    }

    private Mono<CreateIntentResponse> handleCreateOrFetch(String cacheKey, UUID idempotencyKey, CreateIntentRequest createIntentRequest) {
        // 1) Check if the intentId exists in cache
        Mono<CreateIntentResponse> cacheCheck = reactiveRedisTemplate.opsForValue().get(cacheKey)
            .flatMap(intentResponse -> {
                if (intentResponse == null) return Mono.empty();
                intentResponse.setIsConflicted(true);
                logger.info("Found the payment request in cache. CacheKey={}", cacheKey);
                return Mono.just(intentResponse);
            })
            .onErrorResume(ex -> {
                // Redis read failed -> log and continue (we will fallback to DB)
                logger.error("Redis read failed for key {}: {}", cacheKey, ex.getMessage());
                return Mono.empty();
            });

        // 2) DB lookup (permanent source of truth) -> check if intent already exists in database
        Mono<CreateIntentResponse> dbCheck = dbCheck(idempotencyKey, createIntentRequest.getPsuId());

        // Not in DB → create intent transactionally
        Mono<CreateIntentResponse> createNewIntent = createNewIntent(createIntentRequest)
            .onErrorResume(err -> {
                if (utilService.isUniqueConstraintViolation(err)) {
                    return fetchExistingRecord(createIntentRequest);
                }
                logger.error("An error occurred while creating the intent entity.", err);
                return Mono.error(new InternalServerErrorException(CommonConstants.INTERNAL_SERVER_ERROR_MSG));
            });

        return cacheCheck
            .switchIfEmpty(dbCheck)
            .switchIfEmpty(Mono.defer(() -> intentExistsForPsuCheck(createIntentRequest).then(createNewIntent)))
            .flatMap(response -> cacheIntentResponse(cacheKey, response).thenReturn(response));
    }

    // Loser path: wait + poll cache/db
    private Mono<CreateIntentResponse> waitAndFetch(String cacheKey, UUID idempotencyKey, UUID psuId) {
        Mono<CreateIntentResponse> dbCheck = dbCheck(idempotencyKey, psuId);

        // a single attempt to fetch from cache then DB
        Mono<CreateIntentResponse> attempt = Mono.defer(() -> reactiveRedisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(dbCheck));

        // Poll immediately, then every LOCK_WAIT_RETRY, up to LOCK_WAIT_ATTEMPTS times
        return Flux.interval(Duration.ZERO, LOCK_WAIT_RETRY)
            .take(LOCK_WAIT_ATTEMPTS)
            .concatMap(tick -> attempt)               // run one attempt per tick
            .filter(Objects::nonNull)    // filter out empties (Mono.empty => no emission)
            .next()                                  // take first emitted item and convert to Mono
            .switchIfEmpty(Mono.defer(() -> {
                logger.error("Timeout waiting for intent creation.");
                return Mono.error(new InternalServerErrorException(CommonConstants.INTERNAL_SERVER_ERROR_MSG));
            }));
    }

    // Fallback when wait times out: perform safe DB-check-create (no lock)
    private Mono<CreateIntentResponse> dbFallbackCreate(UUID idempotencyKey, CreateIntentRequest createIntentRequest) {

        Mono<CreateIntentResponse> dbCheck = dbCheck(idempotencyKey, createIntentRequest.getPsuId());

        Mono<CreateIntentResponse> createNewIntent = createNewIntent(createIntentRequest)
            .onErrorResume(err -> {
                if (utilService.isUniqueConstraintViolation(err)) {
                    return fetchExistingRecord(createIntentRequest);
                }
                logger.error("An error occurred while creating the intent entity.", err);
                return Mono.error(new InternalServerErrorException(CommonConstants.INTERNAL_SERVER_ERROR_MSG));
            });

        return dbCheck.switchIfEmpty(createNewIntent);
    }

    private Mono<CreateIntentResponse> dbCheck(UUID idempotencyKey, UUID psuId) {
        return intentRepository.findByIdempotencyKeyAndPsuId(idempotencyKey, psuId)
            .flatMap(intent ->
                intentStatusRepository.findLatestStatus(intent.getIntentId())
                    .map(utilService::mapToIntentResponse)
            )
            .flatMap(response -> {
                response.setIsConflicted(true);
                return Mono.just(response);
            }).doOnNext(res -> logger.info("The idempotency key and psuId combination already exists in the database."));
    }

    private Mono<Void> intentExistsForPsuCheck(CreateIntentRequest request) {
        return intentRepository.findByIntentId(request.getIntentId())
            .flatMap(existingPaymentIntent -> {
                UUID psuId = request.getPsuId();
                // Same intentId and PSU.
                if (psuId.equals(existingPaymentIntent.getPsuId())) {
                    return Mono.error(
                        new DuplicateResourceException(ErrorCodes.INT_SER_INTENT_ALREADY_EXISTS, CommonConstants.INTENT_ALREADY_EXISTS_MEG)
                    );
                }

                // Same intentId but different PSU.
                return Mono.error(
                    new ForbiddenException(ErrorCodes.INT_SER_FORBIDDEN, CommonConstants.INTENT_ACCESS_FORBIDDEN_MSG)
                );
            }).then(); // completes successfully if intent does not exist
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

                        IntentTypeProcessor processor = intentTypeProcessorRegistry.resolve(createIntentRequest.getPaymentType());

                        return processor.processPostCreation(createIntentRequest, getIntentRequestJson(createIntentRequest))
                            .then(intentStatusRepository.save(intentStatus))
                            .map(utilService::mapToIntentResponse)
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

        logger.info("Unique constraint detected for intentId={}, fetching existing record.", intentId);

        return intentRepository.findByIntentId(intentId)
            .flatMap(existingIntent -> intentStatusRepository.findLatestStatus(intentId)
                .map(utilService::mapToIntentResponse)
                .flatMap(response -> {
                    response.setIsConflicted(true);
                    return Mono.just(response);
                })
            );
    }

    private Mono<Void> releaseLock(String lockKey) {
        return lockRedisTemplate.delete(lockKey).then();
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
            .setIfAbsent(cacheKey, createIntentResponse, IDEMPOTENCY_TTL)
            .doOnError(err -> logger.error("An error occurred while writing idempotencyKey to redis.", err))
            .then();
    }

    private String getIntentRequestJson(CreateIntentRequest request) {
        return objectMapper.writeValueAsString(request.getPaymentDetails());
    }

}
