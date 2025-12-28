package dev.lokeshbisht.intent_service.service.lock;

import dev.lokeshbisht.intent_service.exceptions.LockBusyException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisDistributedLockService implements DistributedLockService {

    private final ReactiveStringRedisTemplate lockRedisTemplate;

    private final IntentStatusLockRetryProperties lockRetryProps;

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

                    return Mono.error(new LockBusyException("Lock busy " + lockKey));
                })
            ))
            .retryWhen(
                Retry.backoff(lockRetryProps.getMaxAttempts(), lockRetryProps.getMinBackoff())
                    .maxBackoff(lockRetryProps.getMaxBackoff())
                    .filter(LockBusyException.class::isInstance)  // Retry only on lock busy
            )
            .timeout(maxWait)
            .switchIfEmpty(Mono.error(
                new LockBusyException("Lock busy " + lockKey)
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
