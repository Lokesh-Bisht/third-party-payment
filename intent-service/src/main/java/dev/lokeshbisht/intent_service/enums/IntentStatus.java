package dev.lokeshbisht.intent_service.enums;

public enum IntentStatus {
    INITIATED, AWAITING_CONSENT, CONSENT_VERIFIED, REJECTED;

    public boolean canTransitionTo(IntentStatus target) {
        return switch (this) {
            case INITIATED ->
                target == AWAITING_CONSENT || target == REJECTED;
            case AWAITING_CONSENT ->
                target == CONSENT_VERIFIED || target == REJECTED;
            default -> false;
        };
    }
}
