package dev.lokeshbisht.intent_service.service.lock;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface DistributedLockService {

    Mono<Boolean> acquire(String lockKey, Duration ttl);

    Mono<Void> release(String lockKey);
}
