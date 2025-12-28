CREATE INDEX idx_payment_intent_intent_id ON payment_intent(intent_id);
CREATE INDEX idx_payment_intent_psu_id ON payment_intent(psu_id);
CREATE INDEX idx_payment_intent_creditor ON payment_intent(creditor_account_id);
CREATE INDEX idx_payment_intent_debtor ON payment_intent(debtor_account_id);


CREATE INDEX idx_intent_status_intent_created ON payment_intent_status (intent_id, created_at DESC);
CREATE INDEX idx_intent_status_intent_fencing ON payment_intent_status (intent_id, fencing_token DESC);

CREATE INDEX idx_immediate_payment_intent_details_intent_id ON immediate_payment_intent_details(intent_id);