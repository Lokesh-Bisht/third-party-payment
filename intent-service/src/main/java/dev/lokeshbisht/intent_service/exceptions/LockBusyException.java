package dev.lokeshbisht.intent_service.exceptions;

public class LockBusyException extends RuntimeException {

    public LockBusyException(String message) {
        super(message);
    }
}
