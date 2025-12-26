package dev.lokeshbisht.intent_service.service.lock;

import dev.lokeshbisht.intent_service.exceptions.IntentServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static dev.lokeshbisht.intent_service.enums.ErrorCodes.INT_SER_INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
public class RedisDistributedLockService implements DistributedLockService {

    @Value("${lock.intent-status.retry.min-backoff}")
    private Duration minBackoff;

    @Value("${lock.intent-status.retry.max-attempts")
    private int maxAttempts;

    @Value("${lock.intent-status.retry.max-backoff}")
    private Duration maxBackoff;

    private final ReactiveStringRedisTemplate lockRedisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisDistributedLockService.class);

    private static final String FENCING_PREFIX = "lock:fencing:";

    private static final String UNLOCK_LUA = """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
    """;

    @Override
    public Mono<LockToken> acquire(String lockKey, Duration ttl, Duration maxWait) {
        final String lockOwner = UUID.randomUUID().toString();
        String fencingKey = FENCING_PREFIX + lockKey;

        Mono<Long> fencingMono = lockRedisTemplate.opsForValue().increment(fencingKey);

        Mono<Boolean> lockMono = lockRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockOwner, ttl);

        return Mono.defer(() ->
            fencingMono.flatMap(fencingToken ->
                lockMono.flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return Mono.just(new LockToken(lockKey, lockOwner, fencingToken));
                    }
                    return Mono.empty();
                })
            ))
            .retryWhen(
                Retry.backoff(maxAttempts, minBackoff)
                    .maxBackoff(maxBackoff)
            )
            .timeout(maxWait)
            .switchIfEmpty(Mono.error(
                new IntentServiceException(HttpStatus.INTERNAL_SERVER_ERROR, INT_SER_INTERNAL_SERVER_ERROR, "Lock busy")
            ))
            .doOnNext(t ->
                logger.info("Lock acquired {} fencingToken={}", lockKey, t.fencingToken())
            );
    }

    @Override
    public Mono<Void> release(LockToken lockToken) {
        return lockRedisTemplate.execute(
            RedisScript.of(UNLOCK_LUA, Long.class),
            List.of(lockToken.lockKey()),
            lockToken.owner()
        ).doOnNext(res -> {
            if (res == 0) {
                logger.warn("Unlock skipped (expired or stolen): {}", lockToken.lockKey());
            }
        }).then();
    }
}
