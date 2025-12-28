package dev.lokeshbisht.intent_service.service.lock;

public record LockToken (
    String lockKey,
    String owner,
    long fencingToken
) {}
