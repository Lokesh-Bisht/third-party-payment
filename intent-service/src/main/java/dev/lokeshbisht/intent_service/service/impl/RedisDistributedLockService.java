package dev.lokeshbisht.intent_service.service.impl;

import dev.lokeshbisht.intent_service.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisDistributedLockService implements DistributedLockService {

    private final ReactiveStringRedisTemplate lockRedisTemplate;

    public static final Logger logger = LoggerFactory.getLogger(RedisDistributedLockService.class);

    @Override
    public Mono<Boolean> acquire(String lockKey, Duration ttl) {
        final String lockOwner = UUID.randomUUID().toString();

        return lockRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockOwner, ttl)
            .onErrorResume(e -> {
                logger.warn("Redis lock acquisition error for {}: {}", lockKey, e.toString());
                return Mono.just(false);
            });
    }

    @Override
    public Mono<Void> release(String lockKey) {
        return lockRedisTemplate.delete(lockKey).then();
    }
}
