package dev.lokeshbisht.intent_service.enums;

public enum IntentStatus {
    INITIATED, AWAITING_CONSENT, CONSENT_VERIFIED, REJECTED;

    public boolean canTransitionTo(IntentStatus target) {
        if (this == REJECTED) return false;
        return target.ordinal() > this.ordinal();
    }
}
