CREATE TABLE payment_intent (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  intent_id UUID NOT NULL UNIQUE,
  idempotency_key UUID NOT NULL,
  creditor_account_id VARCHAR(50) NOT NULL,
  debtor_account_id VARCHAR(50),
  amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),
  psu_id UUID NOT NULL,
  scheme VARCHAR(15),
  purpose_code VARCHAR(30) NOT NULL,
  payment_type VARCHAR(20) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT unique_payment_request UNIQUE (psu_id, idempotency_key)
);


CREATE TABLE payment_intent_status (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  intent_id UUID NOT NULL,
  status VARCHAR(30) NOT NULL,
  fencing_token BIGINT NOT NULL,
  status_reason_code VARCHAR(200),
  created_at timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT fk_payment_intent_status_intent
    FOREIGN KEY (intent_id)
      REFERENCES payment_intent(intent_id)
      ON UPDATE CASCADE
      ON DELETE CASCADE
);


CREATE TABLE immediate_payment_intent_details (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  intent_id UUID NOT NULL UNIQUE,
  payment_details JSONB NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT fk_immediate_payment_intent_details_intent
    FOREIGN KEY (intent_id)
      REFERENCES payment_intent(intent_id)
      ON UPDATE CASCADE
      ON DELETE CASCADE
);

