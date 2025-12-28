package dev.lokeshbisht.intent_service.service.lock;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface DistributedLockService {

    Mono<LockToken> acquire(String lockKey, Duration ttl, Duration maxWait);

    Mono<Void> release(LockToken lockToken);
}
